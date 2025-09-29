package io.github.liangxin233666.mfl.dtos;

import java.util.List;

public record MultipleCommentsResponse(List<CommentResponse.CommentDto> comments) {}