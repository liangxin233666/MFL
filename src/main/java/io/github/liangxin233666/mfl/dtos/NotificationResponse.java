package io.github.liangxin233666.mfl.dtos;

import io.github.liangxin233666.mfl.events.NotificationEvent;
import java.time.OffsetDateTime;
import java.util.List;

public record NotificationResponse(
        Long id,
        ActorDto actor,             // 触发者的简要信息
        NotificationEvent.EventType type,
        ResourceDto resource,       // 相关资源的简要信息
        boolean isRead,
        OffsetDateTime createdAt,
        String payload
) {
    public record ActorDto(String username, String image) {}
    public record ResourceDto(Long id, String slug) {}
}