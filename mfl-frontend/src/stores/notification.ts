// src/stores/notification.ts
import { defineStore } from 'pinia';
import apiClient from '../api/apiClient';
import { useAuthStore } from './auth';

export const useNotificationStore = defineStore('notification', {
    state: () => ({
        unreadCount: 0,
        isPolling: false, // 防止重复轮询
    }),
    actions: {
        // 1. 从后端获取最新未读数
        async fetchUnreadCount() {
            const auth = useAuthStore();
            if (!auth.isAuthenticated) return;

            try {
                const res = await apiClient.get<{ count: number }>('/notifications/count');
                this.unreadCount = res.data.count;
            } catch (error) {
                console.error('获取未读数失败', error);
            }
        },

        // 2. 本地减 1 (用于点击单条通知时)
        decrementCount() {
            if (this.unreadCount > 0) {
                this.unreadCount--;
            }
        },

        // 3. 本地清零 (用于全部已读时)
        clearCount() {
            this.unreadCount = 0;
        }
    }
});