package io.github.liangxin233666.mfl.services;

import io.github.liangxin233666.mfl.config.RabbitMqConfig;
import io.github.liangxin233666.mfl.entities.Notification;
import io.github.liangxin233666.mfl.events.NotificationEvent;
import io.github.liangxin233666.mfl.repositories.NotificationRepository;
import io.github.liangxin233666.mfl.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // 对应生产者中定义的系统 ID 常量
    private static final Long SYSTEM_ACTOR_ID = -1L;

    public NotificationConsumer(NotificationRepository nr, UserRepository ur) {
        this.notificationRepository = nr;
        this.userRepository = ur;
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_NAME)
    @Transactional
    public void handleNotification(NotificationEvent event) {

        log.info("Processing notification event: type={}, resource={}, targetUser={}",
                event.type(), event.resourceSlug(), event.targetUserId());

        // 1. 数据完整性基础校验
        if (event.targetUserId() == null) {
            log.error("Invalid notification event (missing target): {}", event);
            return; // 放弃消息，避免死循环重试
        }

        try {
            Notification notification = new Notification();

            // 2. 设置目标接收者 (使用 getReferenceById 延迟加载，提高性能)
            // 如果用户被删除，这里会在 save 时抛出异常，这是符合预期的
            notification.setTargetUser(userRepository.getReferenceById(event.targetUserId()));

            // 3. 设置触发者 (Actor) - 处理系统消息
            if (SYSTEM_ACTOR_ID.equals(event.actorId())) {
                // 如果是系统消息，触发者设为 NULL
                // 前提：请确保你的 Notification 实体中，Actor 字段允许为 NULL
                notification.setActor(null);
            } else if (event.actorId() != null) {
                // 如果是正常用户消息
                notification.setActor(userRepository.getReferenceById(event.actorId()));
            } else {
                // actorId 为空的情况 (虽然理论上 Producer 应该避免，但作为兜底)
                notification.setActor(null);
            }

            // 4. 设置基本信息
            notification.setEventType(event.type());
            notification.setResourceId(event.resourceId());
            notification.setResourceSlug(event.resourceSlug());

            // 5. 设置附加内容 (例如：审核拒绝的原因)
            // 请确保你的 Notification 实体已经添加了 `private String content;` 字段
            notification.setContent(event.payload());

            notification.setRead(false);

            // 6. 持久化到 PostgreSQL
            notificationRepository.save(notification);

            log.debug("Notification saved successfully for user: {}", event.targetUserId());

        } catch (jakarta.persistence.EntityNotFoundException e) {
            // 如果因为用户已被物理删除导致找不到 User 实体
            log.warn("User not found for notification (target={} or actor={}), dropping message.",
                    event.targetUserId(), event.actorId());
            // 我们捕获此异常并不抛出，相当于确认消费，让消息移出队列
        } catch (Exception e) {
            // 捕获其他不可预知的异常
            log.error("Failed to process notification event: {}", event, e);
            // 如果你想利用 RabbitMQ 的重试机制 (Retry)，这里应该 throw e;
            // 如果不想重试（认为是不可恢复的 bug），则 swallow 异常
            // 建议：对于非逻辑错误（如 DB 连接断开），最好 throw e
        }
    }
}