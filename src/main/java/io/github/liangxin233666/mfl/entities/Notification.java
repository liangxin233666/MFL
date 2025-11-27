package io.github.liangxin233666.mfl.entities;

import io.github.liangxin233666.mfl.events.NotificationEvent; // 复用EventType
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 懒加载是好习惯，我们只需引用ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationEvent.EventType eventType;

    @Column(nullable = false)
    private Long resourceId;

    private String resourceSlug;

    @Column(nullable = false)
    private boolean isRead = false;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private String content;
}