package io.github.liangxin233666.mfl.services;

import io.github.liangxin233666.mfl.dtos.MultipleNotificationsResponse;
import io.github.liangxin233666.mfl.dtos.NotificationResponse;
import io.github.liangxin233666.mfl.entities.Notification;
import io.github.liangxin233666.mfl.entities.User;
import io.github.liangxin233666.mfl.repositories.NotificationRepository;
import io.github.liangxin233666.mfl.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // 1. 获取通知列表
    @Transactional(readOnly = true)
    public MultipleNotificationsResponse getNotifications(Pageable pageable, UserDetails currentUserDetails) {
        User currentUser = findUserByDetails(currentUserDetails);

        Page<Notification> page = notificationRepository.findByTargetUserIdOrderByCreatedAtDesc(currentUser.getId(), pageable);

        List<NotificationResponse> dtos = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return new MultipleNotificationsResponse(dtos, page.getTotalElements());
    }

    // 2. 获取未读数量
    @Transactional(readOnly = true)
    public long getUnreadCount(UserDetails currentUserDetails) {
        User currentUser = findUserByDetails(currentUserDetails);
        return notificationRepository.countByTargetUserIdAndIsReadFalse(currentUser.getId());
    }

    // 3. 标记单个通知为已读
    @Transactional
    public void markAsRead(Long notificationId, UserDetails currentUserDetails) {
        User currentUser = findUserByDetails(currentUserDetails);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        // 安全检查：不能标记别人的通知
        if (!notification.getTargetUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Not your notification");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    // 4. 标记全部为已读
    @Transactional
    public void markAllAsRead(UserDetails currentUserDetails) {
        User currentUser = findUserByDetails(currentUserDetails);

        // 直接调用 Repository 的批量更新方法
        // 这一行代码会直接翻译成 SQL:
        // UPDATE notifications SET is_read=true WHERE target_user_id=? AND is_read=false
        notificationRepository.markAllAsRead(currentUser.getId());
    }

    // --- Helpers ---

    private User findUserByDetails(UserDetails userDetails) {
        return userRepository.findById(Long.valueOf(userDetails.getUsername()))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private NotificationResponse mapToDto(Notification n) {
        return new NotificationResponse(
                n.getId(),
                new NotificationResponse.ActorDto(n.getActor().getUsername(), n.getActor().getImage()),
                n.getEventType(),
                new NotificationResponse.ResourceDto(n.getResourceId(), n.getResourceSlug()),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}