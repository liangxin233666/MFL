package io.github.liangxin233666.mfl.repositories.projections;


public interface ArticleLeastView {
    Long getId();
    String getSlug();
    String getTitle();
    Integer getFavoritesCount();
}