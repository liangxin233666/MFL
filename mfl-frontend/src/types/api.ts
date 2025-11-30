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
    auditStatus: 'PENDING' | 'APPROVED' | 'REJECTED';
}

export interface Comment {
    id: number;
    createdAt: string;
    updatedAt: string;
    body: string;
    author: Profile;
    replies: Comment[];

}

export type EventType =
    | 'ARTICLE_LIKED'
    | 'COMMENT_CREATED'
    | 'COMMENT_REPLIED'
    | 'COMMENT_LIKED'
    | 'ARTICLE_APPROVED' // 如果后端有这个，记得加上
    | 'ARTICLE_REJECTED'; // 如果后端有这个，记得加上

export interface Notification {
    id: number;
    // 发起人
    actor: {
        username: string;
        image: string | null;
    };
    // 事件类型
    type: EventType;
    // 资源定位
    resource: {
        id: number;
        slug: string | null;
    } | null;
    // 状态
    isRead: boolean;
    createdAt: string;

    // **注意**：如果不加这个，审核拒绝理由就没法显示了
    // 建议后端 DTO 依然保留 payload
    payload?: string;
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

export interface Plugin {
    id: number;
    name: string;
    slug: string;
    description: string;
    version: string;
    fileUrl: string; // MinIO / OSS 地址
    iconUrl: string | null;
    authorName: string;
    downloads: number; // 动态数据，本地不存
    updatedAt: string; // 动态数据，本地不存
}

// 本地存储的插件结构 (剔除下载量等动态数据，保留所有展示信息)
export interface LocalPluginState {
    id: number;      // 对应 Plugin.id
    slug: string;
    name: string;
    description: string;
    version: string;
    fileUrl: string;
    iconUrl: string | null;
    authorName: string;

    // 本地特有状态
    isEnabled: boolean;
    installedAt: number;
}

