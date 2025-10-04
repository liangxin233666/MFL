package io.github.liangxin233666.mfl.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public record NewArticleRequest(@Valid @NotNull ArticleDto article) {
    public record ArticleDto(
            @NotBlank(message = "title cannot be empty")
            String title,

            String description,

            @NotBlank(message = "body cannot be empty")
            String body,

            List<String> tagList
    ) {
        public ArticleDto {
        if (title != null) title = title.trim();
        if (description != null) description = description.trim();
        if (body != null) body = body.trim();

        // 处理标签列表：移除空标签和空格
        if (tagList != null) {
            tagList = tagList.stream()
                    .filter(tag -> tag != null && !tag.isBlank())
                    .map(String::trim)
                    .collect(Collectors.toList());
        } else {
            tagList = Collections.emptyList(); // 如果传入null，则视为空集合
        }
       }
    }
}