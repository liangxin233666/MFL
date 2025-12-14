package io.github.liangxin233666.mfl.services;

import io.github.liangxin233666.mfl.config.RabbitMqAuditConfig;
import io.github.liangxin233666.mfl.utils.PidController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;

@Slf4j
@Service
public class AuditAutoScaler {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectProvider<RabbitListenerEndpointRegistry> registryProvider;
    private final PidController pidController;

    // --- 核心配置 ---
    private static final int MIN_CONSUMERS = 2;
    private static final int MAX_CONSUMERS = 20;

    // 阈值：变化量小于2个线程就不折腾，防止微小波动
    private static final int THRESHOLD = 2;

    // 紧急阈值：积压超过这个数，无视冷却直接扩容
    private static final int EMERGENCY_THRESHOLD = 100;

    // 非对称冷却时间
    private static final long SCALE_UP_COOLDOWN_SECONDS = 5;  // 扩容快：5秒
    private static final long SCALE_DOWN_COOLDOWN_SECONDS = 60; // 缩容慢：60秒

    // PID 参数 (需要根据实际运行情况微调)
    // Kp: 积压100个 -> 增益5个线程
    // Ki: 消除稳态误差
    // Kd: 抑制突变
    private static final double KP = 0.05;
    private static final double KI = 0.005;
    private static final double KD = 0.02;

    private Instant lastScaleTime = Instant.now();

    public AuditAutoScaler(RabbitTemplate rabbitTemplate, ObjectProvider<RabbitListenerEndpointRegistry> registryProvider) {
        this.rabbitTemplate = rabbitTemplate;
        this.registryProvider = registryProvider;
        this.pidController = new PidController(KP, KI, KD, MIN_CONSUMERS, MAX_CONSUMERS);
    }

    /**
     * 每 5 秒巡检一次
     */
    @Scheduled(fixedRate = 5000)
    public void adjustConsumers() {
        RabbitListenerEndpointRegistry registry = registryProvider.getIfAvailable();
        if (registry == null) {
            log.warn("RabbitListenerEndpointRegistry 尚未初始化，跳过本次扩缩容");
            return;
        }

        // 1. 获取容器
        MessageListenerContainer container = registry.getListenerContainer(RabbitMqAuditConfig.AUDIT_LISTENER_ID);
        if (!(container instanceof SimpleMessageListenerContainer simpleContainer)) {
            return;
        }

        // 2. 获取实时指标
        long queueDepth = getQueueDepth();
        int currentConsumers = simpleContainer.getActiveConsumerCount();

        // 3. PID 计算理想线程数
        // 注意：这里无论是否在冷却期，都先不进行 PID 内部 integral 的累加，
        // 我们通过逻辑控制，只在真正决定调整时才采纳 PID 的建议。
        // 但由于 PidController 是有状态的，为了避免没调用导致状态丢失，
        // 我们的 PidController 逻辑已经处理了平滑过渡。
        int desiredConsumers = pidController.compute(0, queueDepth);

        // 4. 判断扩缩容意图
        boolean isScalingUp = desiredConsumers > currentConsumers;

        // 5. 冷却逻辑检查
        long secondsSinceLastScale = Duration.between(lastScaleTime, Instant.now()).getSeconds();
        long requiredCooldown = isScalingUp ? SCALE_UP_COOLDOWN_SECONDS : SCALE_DOWN_COOLDOWN_SECONDS;

        boolean isEmergency = queueDepth > EMERGENCY_THRESHOLD;
        boolean inCooldown = secondsSinceLastScale < requiredCooldown;

        // 如果处于冷却期，且不是紧急情况，直接跳过
        if (inCooldown && !isEmergency) {
            return;
        }

        // 6. 防抖动检查
        if (Math.abs(desiredConsumers - currentConsumers) < THRESHOLD) {
            // 变化太小，不调整
            return;
        }

        // 7. 执行调整
        log.info("PID 伸缩触发 | 积压: {} | 当前: {} -> 目标: {} | 模式: {}",
                queueDepth, currentConsumers, desiredConsumers,
                isEmergency ? "紧急扩容" : (isScalingUp ? "扩容" : "缩容"));

        simpleContainer.setConcurrentConsumers(desiredConsumers);
        // Max 设为目标值 + 缓冲区，防止 Spring 内部频繁起停
        simpleContainer.setMaxConcurrentConsumers(Math.max(desiredConsumers + 2, MAX_CONSUMERS));

        lastScaleTime = Instant.now();
    }

    private long getQueueDepth() {
        try {
            // 【核心修复】在这里显式地将 int 转为 Long，解决类型推断报错
            Long count = rabbitTemplate.execute(channel -> {
                int msgCount = channel.queueDeclarePassive(RabbitMqAuditConfig.AUDIT_QUEUE).getMessageCount();
                return (long) msgCount;
            });

            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("无法获取队列深度", e);
            return 0;
        }
    }
}