package io.github.liangxin233666.mfl.dtos;

import java.util.List;

public record MultipleArticlesResponse(
        List<ArticleResponse.ArticleDto> articles,
        long articlesCount
) {}