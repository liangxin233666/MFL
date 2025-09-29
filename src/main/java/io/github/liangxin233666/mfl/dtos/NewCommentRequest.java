package io.github.liangxin233666.mfl.dtos;

public record NewCommentRequest(CommentDto comment) {
    public record CommentDto(String body) {}
}