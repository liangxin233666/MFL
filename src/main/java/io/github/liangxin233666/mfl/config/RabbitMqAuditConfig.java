package io.github.liangxin233666.mfl.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

@Configuration
public class RabbitMqAuditConfig {

    public static final String AUDIT_QUEUE = "article.audit.queue.v2";
    public static final String VECTOR_SAVE_QUEUE = "article.vector.queue";
    public static final String AUDIT_LISTENER_ID = "auditListenerContainer";

    // 【新增】死信相关的常量定义
    public static final String DL_EXCHANGE = "audit.dlx";        // 死信交换机
    public static final String DL_QUEUE = "article.audit.dlq";   // 死信队列
    public static final String DL_ROUTING_KEY = "audit.dead";    // 死信路由键

    /**
     * 【修改】审核队列
     * 原本是：return new Queue(AUDIT_QUEUE, true);
     * 修改原因：为了让 5 次重试失败后的消息能自动去死信队列，必须在这里绑定 arguments。
     */
    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable(AUDIT_QUEUE)
                .withArgument("x-dead-letter-exchange", DL_EXCHANGE)      // 指定死信交换机
                .withArgument("x-dead-letter-routing-key", DL_ROUTING_KEY)// 指定路由键
                .build();
    }

    // 【新增】配置死信交换机
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DL_EXCHANGE);
    }

    // 【新增】配置死信队列（这里用来存放真正失败的消息）
    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DL_QUEUE, true);
    }

    // 【新增】绑定关系：DLX -> DLQ
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(DL_ROUTING_KEY);
    }

    // 【保持原样】向量队列不需要动
    @Bean
    public Queue vectorQueue() {
        return new Queue(VECTOR_SAVE_QUEUE, true);
    }


    /**
     * 自定义监听容器工厂
     * 保留了原本的 connectionFactory 和 messageConverter 参数
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();

        // 1. 保留你原有的基础设置
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter); // 序列化器保持不动

        // 2. PID 核心设置
        factory.setPrefetchCount(1);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(20);

        // 3. 【新增】注入重试逻辑
        // 代替 application.yml，手动实现：重试5次 -> 失败后丢给 RejectAndDontRequeueRecoverer
        // RejectAndDontRequeueRecoverer 会发送 basic.reject，配合上面的 queue arguments，消息就会进入死信队列
        RetryOperationsInterceptor retryInterceptor = RetryInterceptorBuilder.stateless()
                .maxAttempts(5) // 总共试5次（含第一次）
                .backOffOptions(1000, 2.0, 10000) // 指数退避：1秒, 2秒, 4秒...
                .recoverer(new RejectAndDontRequeueRecoverer()) // 最后失败的处理策略：不重新入队（直接去死信）
                .build();

        factory.setAdviceChain(retryInterceptor);

        return factory;
    }
}