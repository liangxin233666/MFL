package io.github.liangxin233666.mfl.controllers;

import io.github.liangxin233666.mfl.dtos.MultipleNotificationsResponse;
import io.github.liangxin233666.mfl.services.NotificationService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 获取当前登录用户的通知列表
     * GET /api/notifications?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<MultipleNotificationsResponse> getNotifications(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserDetails currentUser) {

        MultipleNotificationsResponse response = notificationService.getNotifications(pageable, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取未读通知数量 (用于前端小红点轮询)
     * GET /api/notifications/count
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal UserDetails currentUser) {
        long count = notificationService.getUnreadCount(currentUser);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * 标记某一条通知为已读 (用户点击该通知时调用)
     * PUT /api/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {

        notificationService.markAsRead(id, currentUser);
        return ResponseEntity.noContent().build(); // 返回 204
    }

    /**
     * 标记所有通知为已读 (用户点击"全部已读"按钮时调用)
     * PUT /api/notifications/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal UserDetails currentUser) {
        notificationService.markAllAsRead(currentUser);
        return ResponseEntity.noContent().build(); // 返回 204
    }
}