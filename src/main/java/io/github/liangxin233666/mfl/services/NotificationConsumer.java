package io.github.liangxin233666.mfl.services;
import io.github.liangxin233666.mfl.config.RabbitMqConfig;
import io.github.liangxin233666.mfl.entities.Notification;
import io.github.liangxin233666.mfl.events.NotificationEvent;
import io.github.liangxin233666.mfl.repositories.UserRepository;
import io.github.liangxin233666.mfl.repositories.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Slf4j
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationConsumer(NotificationRepository nr, UserRepository ur) {
        this.notificationRepository = nr;
        this.userRepository = ur;
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_NAME)
    @Transactional
    public void handleNotification(NotificationEvent event) {
        // 去掉粗暴的 try-catch 块，让运行时异常可以冒泡
        // Spring AMQP 默认配置会捕获这些异常，并尝试重新投递消息（默认重试）

        log.info("Receiving notification event: type={}, user={}", event.type(), event.targetUserId());

        // 数据完整性校验
        if (event.actorId() == null || event.targetUserId() == null) {
            // 这种错误是不可恢复的（代码BUG），打印日志并放弃消息，不抛出异常，否则会无限循环重试
            log.error("Invalid notification event: {}", event);
            return;
        }

        Notification notification = new Notification();
        notification.setActor(userRepository.getReferenceById(event.actorId()));
        notification.setTargetUser(userRepository.getReferenceById(event.targetUserId()));

        notification.setEventType(event.type());
        notification.setResourceId(event.resourceId());
        notification.setResourceSlug(event.resourceSlug());
        notification.setRead(false);

        notificationRepository.save(notification);
    }
}