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

    /**
     * 【优化核心】批量更新 API
     * 直接执行 UPDATE 语句，不把数据加载到内存中。
     *
     * @Modifying: 告诉 JPA 这是一个写操作
     * clearAutomatically = true: 极为重要！因为我们绕过 Hibernate 缓存直接改了数据库，
     * 所以必须清除当前 Hibernate Session 上下文，防止出现"数据不一致"（脏读）的问题。
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.targetUser.id = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") Long userId);
}