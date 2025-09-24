package io.github.liangxin233666.mfl.dtos;

import java.time.OffsetDateTime;
import java.util.Set;

public record ArticleResponse(ArticleDto article) {
    // 复用ProfileDto
    public record ArticleDto(
            String slug,
            String title,
            String description,
            String body,
            Set<String> tagList,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            boolean favorited,
            int favoritesCount,
            ProfileResponse.ProfileDto author
    ) {}
}