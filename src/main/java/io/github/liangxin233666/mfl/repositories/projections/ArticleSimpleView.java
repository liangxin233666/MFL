
package io.github.liangxin233666.mfl.repositories.projections;

import io.github.liangxin233666.mfl.entities.Article;

import java.time.OffsetDateTime;

/**
 * 核心优化：只查列表需要的字段，绝对不查 Body
 */
public interface ArticleSimpleView {
    Long getId();
    String getSlug();
    String getTitle();
    String getDescription();
    OffsetDateTime getCreatedAt();
    OffsetDateTime getUpdatedAt();
    Integer getFavoritesCount();
    String getCoverImageUrl();
    Article.ArticleStatus getStatus(); // 虽然前端不需要，但业务逻辑过滤需要

    // 嵌套投影：处理 Author 关联
    AuthorView getAuthor();

    interface AuthorView {
        String getUsername();
        String getImage();
    }
}