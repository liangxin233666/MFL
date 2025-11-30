package io.github.liangxin233666.mfl.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqAuditConfig {

    // 队列 1：审核队列
    public static final String AUDIT_QUEUE = "article.audit.queue";

    // 队列 2：【新增】向量与保存队列
    public static final String VECTOR_SAVE_QUEUE = "article.vector.queue";

    @Bean
    public Queue auditQueue() {
        return new Queue(AUDIT_QUEUE, true);
    }

    @Bean
    public Queue vectorQueue() {
        return new Queue(VECTOR_SAVE_QUEUE, true);
    }
}