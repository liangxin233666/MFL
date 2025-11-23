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