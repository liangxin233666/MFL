package io.github.liangxin233666.mfl.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;


public record UpdateArticleRequest(@Valid @NotNull ArticleDto article) {
    public record ArticleDto(String title, String description, String body,String coverImageUrl)
    {  public ArticleDto
        {
            if (title != null) title = title.trim();
            if (description != null) description = description.trim();
            if (body != null) body = body.trim();
        }
    }
}