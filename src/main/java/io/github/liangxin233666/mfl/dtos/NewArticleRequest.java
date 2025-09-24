package io.github.liangxin233666.mfl.dtos;

import java.util.Set;

public record NewArticleRequest(ArticleDto article) {
    public record ArticleDto(String title, String description, String body, Set<String> tagList) {}
}