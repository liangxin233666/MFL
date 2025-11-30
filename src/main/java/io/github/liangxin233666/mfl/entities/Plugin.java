package io.github.liangxin233666.mfl.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Entity
@Table(name = "plugins")
@Getter
@Setter
public class Plugin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String slug;

    private String description;
    private String version;
    private String type;
    private String fileUrl; // 存的是 MinIO 的完整 http 地址
    private String iconUrl;

    private String authorName;

    private Integer downloads = 0; // 简单的计数器

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = OffsetDateTime.now(); updatedAt = OffsetDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = OffsetDateTime.now(); }
}