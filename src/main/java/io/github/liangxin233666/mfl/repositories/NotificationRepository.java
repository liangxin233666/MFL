package io.github.liangxin233666.mfl.repositories;

import io.github.liangxin233666.mfl.entities.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 标准查询：获取列表
    Page<Notification> findByTargetUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 标准统计：获取未读数 (JPA会自动生成 SELECT COUNT(*)...)
    long countByTargetUserIdAndIsReadFalse(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.targetUser.id = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") Long userId);
}