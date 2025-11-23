package io.github.liangxin233666.mfl.dtos;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;



public record ArticleResponse(ArticleDto article) {
    // 复用ProfileDto

    public record ArticleDto(
            String slug,
            String title,
            String description,
            String body,
            List<String> tagList,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            Boolean favorited,
            Integer favoritesCount,
            String coverImageUrl,
            ProfileResponse.ProfileDto author
    ) {}
}