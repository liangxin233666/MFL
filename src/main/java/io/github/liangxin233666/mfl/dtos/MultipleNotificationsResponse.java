package io.github.liangxin233666.mfl.dtos;

import java.util.List;

public record MultipleNotificationsResponse(
        List<NotificationResponse> notifications,
        long count // 总记录数，方便分页
) {}