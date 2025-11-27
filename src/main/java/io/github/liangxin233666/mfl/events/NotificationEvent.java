package io.github.liangxin233666.mfl.events;

import java.io.Serializable;

// 这是一个通用的事件对象
public record NotificationEvent(
        Long actorId,       // 谁做的动作? (User ID)
        Long targetUserId,  // 接收通知的人是谁? (User ID)
        EventType type,     // 什么动作? (点赞? 评论?)
        Long resourceId,    // 相关的资源ID (文章ID 或 评论ID)
        String resourceSlug // 相关的文章Slug (用于生成链接)
) implements Serializable {

    public enum EventType {
        ARTICLE_LIKED,    // 文章被点赞
        COMMENT_CREATED,  // 文章收到评论
        COMMENT_REPLIED,  // 评论收到回复
        COMMENT_LIKED     // 评论被点赞 (这是你未来可能的扩展)
    }
}
