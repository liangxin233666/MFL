package io.github.liangxin233666.mfl.services;

import io.github.liangxin233666.mfl.config.RabbitMqConfig;
import io.github.liangxin233666.mfl.events.NotificationEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationProducer {

    private final RabbitTemplate rabbitTemplate;

    public NotificationProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    // @Async 再次登场：即使发消息很快，我们也尽量不阻塞主线程
    @Async
    public void sendNotification(NotificationEvent event) {
        // 不要给自己发通知 (自己点赞自己)
        if (event.actorId().equals(event.targetUserId())) {
            return;
        }

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE_NAME,
                RabbitMqConfig.ROUTING_KEY,
                event
        );
    }
}
