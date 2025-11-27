package io.github.liangxin233666.mfl.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "articles")
@Getter
@Setter
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String body;

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "article_favorites",
            joinColumns = @JoinColumn(name = "article_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> favoritedBy= new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "article_tags",
            joinColumns = @JoinColumn(name = "article_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags= new HashSet<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime updatedAt;

    @OneToMany(
            mappedBy = "article", // 这个关系是由 Comment 实体中的 "article" 字段来维护的。
            cascade = CascadeType.ALL, // 级联操作：如果删除这篇文章，与之关联的所有评论也会被自动删除。
            orphanRemoval = true // 如果从这个列表中移除一个评论，这个评论也会从数据库中被删除。
    )
    private List<Comment> comments = new ArrayList<>();

    @Column(nullable = false)
    private int favoritesCount = 0;

    @Column(length = 2048)
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    private ArticleStatus status = ArticleStatus.PENDING;

    public enum ArticleStatus {
        PENDING, PUBLISHED, REJECTED
    }
}