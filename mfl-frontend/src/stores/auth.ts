import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import apiClient from '../api/apiClient';
import type { User } from '../types/api';
import {ASSETS} from "../config/assets.ts";

export const useAuthStore = defineStore('auth', () => {

    const user = ref<User | null>(null);


    const isAuthenticated = computed(() => !!user.value);

    const userImage = computed(() => {
        if (user.value?.image) {
            return user.value.image;
        }
        if (user.value) {
            // 为每个用户名生成一个固定的、好看的默认头像
            return ASSETS.defaults.avatarD;
        }

        return `https://api.dicebear.com/8.x/bottts-neutral/svg?seed=guest`;
    });




    function setToken(token: string) {
        localStorage.setItem('jwt_token', token);
    }

    function clearAuth() {
        user.value = null;
        localStorage.removeItem('jwt_token');
    }

    async function login(credentials: { email: string; password: string }) {
        const response = await apiClient.post<{ user: User }>('/users/login', { user: credentials });
        user.value = response.data.user;
        setToken(response.data.user.token);


    }

    async function register(details: { username: string, email: string; password: string }) {
        const response = await apiClient.post<{ user: User }>('/users', { user: details });
        user.value = response.data.user;
        setToken(response.data.user.token);

    }

    async function logout() {

        clearAuth();

    }

    async function checkAuth() {

        if (localStorage.getItem('jwt_token') && !user.value) {
            try {

                const response = await apiClient.get<{ user: User }>('/user');
                user.value = response.data.user;

            } catch (error) {
                console.error("Token verification failed:", error);

                clearAuth();
            }
        }
    }

    async function updateSettings(updates: {
        user: Partial<Record<"username" | "bio" | "password" | "email" | "image", string | null>>
    }) {

        const response = await apiClient.put<{ user: User }>('/users', updates);

        const updatedUser = response.data.user;

        if (user.value) {
            Object.assign(user.value, updatedUser);
        }


        return updatedUser;
    }

    return {
        user,
        isAuthenticated,
        userImage,
        login,
        register,
        logout,
        checkAuth,
        updateSettings
    };
});