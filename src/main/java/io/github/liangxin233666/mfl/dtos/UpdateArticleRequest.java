package io.github.liangxin233666.mfl.dtos;


public record UpdateArticleRequest(ArticleDto article) {
    public record ArticleDto(String title, String description, String body) {}
}