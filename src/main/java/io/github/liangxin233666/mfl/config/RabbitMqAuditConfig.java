package io.github.liangxin233666.mfl.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqAuditConfig {
    public static final String AUDIT_QUEUE = "article.audit.queue";

    @Bean
    public Queue auditQueue() {
        return new Queue(AUDIT_QUEUE, true); // 持久化队列
    }
}