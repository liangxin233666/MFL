// src/types/api.ts

export interface User {
    email: string;
    token: string;
    username: string;
    bio: string | null;
    image: string | null;
}

export interface Profile {
    username: string;
    bio: string | null;
    image: string | null;
    following: boolean| null;
}

export interface Article {
    slug: string;
    title: string;
    description: string;
    body: string | null;
    tagList: string[] | null;
    createdAt: string;
    updatedAt: string;
    favorited: boolean | null;
    favoritesCount: number | null;
    author: Profile;
    coverImageUrl: string | null;
}

export interface Comment {
    id: number;
    createdAt: string;
    updatedAt: string;
    body: string;
    author: Profile;
    replies: Comment[];

}

export type NotificationType = 'FOLLOW' | 'FAVORITE' | 'COMMENT' | 'REPLY';

export interface Notification {
    id: number;
    // 对应 ActorDto
    actor: {
        username: string;
        image: string | null;
    };
    // 对应 NotificationEvent.EventType
    type: 'ARTICLE_LIKED' | 'COMMENT_CREATED' | 'COMMENT_REPLIED' | 'COMMENT_LIKED';
    // 对应 ResourceDto (注意：这里现在没有 title 和 body 了)
    resource: {
        id: number;
        slug: string | null;
    } | null;
    isRead: boolean;
    createdAt: string;
}

export interface MultipleNotificationsResponse {
    notifications: Notification[];
    notificationsCount: number;
}

export interface HistoryRecord {
    viewedAt: string; // 时间戳
    article: {
        slug: string;
        title: string;
        coverImageUrl: string | null;
        author: {
            username: string;
            image: string | null;
        };
    };
}

