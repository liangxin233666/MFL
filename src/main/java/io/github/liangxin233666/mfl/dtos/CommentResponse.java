package io.github.liangxin233666.mfl.dtos;

import java.time.OffsetDateTime;
import java.util.List;

public record CommentResponse(CommentDto comment) {
    public record CommentDto(
            Long id,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String body,
            ProfileResponse.ProfileDto author,
            List<CommentDto> replies
    ) {}
}