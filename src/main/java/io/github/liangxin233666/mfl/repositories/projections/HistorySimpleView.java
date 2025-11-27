package io.github.liangxin233666.mfl.repositories.projections;

import java.time.OffsetDateTime;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

// 指定JSON输出顺序
@JsonPropertyOrder({ "viewedAt", "article" })
public interface HistorySimpleView {

    OffsetDateTime getViewedAt();

    // 嵌套获取文章精简信息
    ArticleSimpleView getArticle();

    interface ArticleSimpleView {
        String getSlug();
        String getTitle();
        String getCoverImageUrl();

        // 再次嵌套获取作者精简信息
        AuthorSimpleView getAuthor();
    }

    interface AuthorSimpleView {
        String getUsername();
        String getImage();
    }
}