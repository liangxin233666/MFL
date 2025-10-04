package io.github.liangxin233666.mfl.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NewCommentRequest(@Valid @NotNull CommentDto comment) {
    public record CommentDto(
            @NotBlank(message = "comment body cannot be empty")
            String body
    )
    {
        public CommentDto {

            body = body.trim();

        }
    }
}