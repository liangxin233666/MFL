<!-- src/views/NotificationsPage.vue -->
<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api/apiClient';
import type { Notification, MultipleNotificationsResponse } from '../types/api';
import NotificationItem from '../components/NotificationItem.vue';
import { useNotificationStore } from '../stores/notification';
import { BellIcon, EnvelopeOpenIcon } from '@heroicons/vue/24/outline';

const router = useRouter();
const notificationStore = useNotificationStore();
const notifications = ref<Notification[]>([]);
const isLoading = ref(false);

const fetchNotifications = async (page = 0) => {
  if (isLoading.value) return;
  isLoading.value = true;
  try {
    const response = await apiClient.get<MultipleNotificationsResponse>(`/notifications?page=${page}&size=20`);
    if (page === 0) notifications.value = response.data.notifications;
    else notifications.value.push(...response.data.notifications);
  } finally {
    isLoading.value = false;
  }
};

// 处理点击
const handleNotificationClick = async (notification: Notification) => {
  // 1. 标记已读 (保持不变)
  if (!notification.isRead) {
    try {
      await apiClient.put(`/notifications/${notification.id}/read`);
      notification.isRead = true;
      notificationStore.decrementCount();
    } catch (e) { console.error(e) }
  }

  // 2. 跳转逻辑 (适配新的 resource 结构)
  if (notification.resource && notification.resource.slug) {

    await router.push(`/article/${notification.resource.slug}`);

  } else if (notification.type === 'ARTICLE_REJECTED' && notification.resource?.slug) {

    await router.push(`/editor/${notification.resource.slug}`);

  } else if (['COMMENT_REPLIED', 'COMMENT_LIKED'].includes(notification.type) && notification.resource?.slug) {
    await router.push(`/article/${notification.resource.slug}`);
  }
};

// 一键已读
const markAllAsRead = async () => {
  if (notificationStore.unreadCount === 0) return;
  if (!confirm('全部标记已读？')) return;
  try {
    await apiClient.put('/api/notifications/read-all');
    notifications.value.forEach(n => n.isRead = true);
    notificationStore.clearCount();
  } catch(e) { alert('操作失败'); }
};

onMounted(() => {
  fetchNotifications(0);
  notificationStore.fetchUnreadCount();
});
</script>

<template>
  <!-- Template 代码基本不变，只要确保 notification.isRead 在列表循环里没有被手动硬编码成 .read 即可。
       NotificationItem 已经处理好了显示逻辑，这里只负责传参。 -->
  <div class="container mx-auto max-w-4xl py-6 px-4">
    <div class="card bg-base-100 shadow-md min-h-[80vh]">
      <div class="p-4 border-b flex items-center justify-between sticky top-0 bg-base-100/95 backdrop-blur z-10">
        <div class="flex items-center gap-2">
          <BellIcon class="w-6 h-6 text-pink-500" />
          <h1 class="text-xl font-bold">消息中心</h1>
          <span v-if="notificationStore.unreadCount > 0" class="badge badge-error text-white badge-sm">{{ notificationStore.unreadCount }}</span>
        </div>
        <button v-if="notificationStore.unreadCount > 0" @click="markAllAsRead" class="btn btn-sm btn-ghost gap-1 hover:text-pink-500">
          <EnvelopeOpenIcon class="w-4 h-4" />全部已读
        </button>
      </div>

      <div class="divide-y divide-base-200">
        <NotificationItem
            v-for="notification in notifications"
            :key="notification.id"
            :notification="notification"
            @click="handleNotificationClick"
        />

        <div v-if="isLoading" class="text-center py-8"><span class="loading loading-spinner text-pink-500"></span></div>
        <div v-else-if="notifications.length === 0" class="flex flex-col items-center justify-center py-20 text-base-content/40">
          <BellIcon class="w-16 h-16 mb-4 opacity-20"/>暂无消息
        </div>
      </div>
    </div>
  </div>
</template>