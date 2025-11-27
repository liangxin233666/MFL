<!-- src/components/NotificationItem.vue -->
<script setup lang="ts">
import { computed } from 'vue';
import type { Notification, Profile } from '../types/api';
import UserInfo from './UserInfo.vue';
import {
  HeartIcon,
  ChatBubbleLeftEllipsisIcon,
  ArrowUturnLeftIcon
} from '@heroicons/vue/24/solid';

const props = defineProps<{
  notification: Notification;
}>();

defineEmits(['click']);

// 1. 将后端的 ActorDto 转换为前端通用的 Profile 接口格式
// 这样 UserInfo 组件才能正常工作（UserInfo 可能还需要 bio/following 等字段，我们补全默认值）
const actorProfile = computed<Profile>(() => ({
  username: props.notification.actor.username,
  image: props.notification.actor.image,
  bio: null,      // 补全默认值
  following: false // 补全默认值
}));

// 2. 根据 EventType 映射显示配置
const config = computed(() => {
  switch (props.notification.type) {
    case 'ARTICLE_LIKED':
      return {
        icon: HeartIcon,
        color: 'text-pink-500',
        bg: 'bg-pink-100',
        text: '赞了你的文章'
      };
    case 'COMMENT_LIKED':
      return {
        icon: HeartIcon,
        color: 'text-pink-500',
        bg: 'bg-pink-100',
        text: '赞了你的评论'
      };
    case 'COMMENT_CREATED':
      return {
        icon: ChatBubbleLeftEllipsisIcon,
        color: 'text-green-500',
        bg: 'bg-green-100',
        text: '评论了你的文章'
      };
    case 'COMMENT_REPLIED':
      return {
        icon: ArrowUturnLeftIcon,
        color: 'text-purple-500',
        bg: 'bg-purple-100',
        text: '回复了你的评论'
      };
    default:
      return {
        icon: ChatBubbleLeftEllipsisIcon,
        color: 'text-gray-500',
        bg: 'bg-gray-100',
        text: '新消息'
      };
  }
});

const timeAgo = computed(() => {
  return new Date(props.notification.createdAt).toLocaleString();
});
</script>

<template>
  <div
      @click="$emit('click', notification)"
      class="relative flex gap-4 p-4 border-b transition-all cursor-pointer group hover:bg-base-100"
      :class="notification.isRead ? 'bg-base-100' : 'bg-pink-50/30 hover:bg-pink-50/50'"
  >
    <!-- 左侧：UserInfo 传入转换后的 profile -->
    <div class="pt-1">
      <UserInfo :profile="actorProfile" size="md" :show-name="false" />
    </div>

    <!-- 中间：内容 -->
    <div class="flex-grow space-y-1">
      <div class="flex flex-wrap items-center gap-2 text-sm">
        <span class="font-bold text-base-content hover:text-pink-500 transition-colors">
            {{ notification.actor.username }}
        </span>
        <span class="text-base-content/60">{{ config.text }}</span>

        <!-- 图标 -->
        <component :is="config.icon" class="w-4 h-4" :class="config.color" />

        <span class="text-xs text-base-content/40 ml-auto">{{ timeAgo }}</span>
      </div>

      <!--
        注意：后端 ResourceDto 目前只返回 id 和 slug，
        没有 title 或 body，所以这里不能展示 "xxxx的内容" 了。
        只展示一个 "点击查看详情" 的提示。
      -->
      <div class="text-xs text-base-content/50 bg-base-200/50 p-2 rounded border border-base-200 mt-2 inline-block">
        📄 点击查看详情
      </div>
    </div>

    <!-- 右侧：红点 -->
    <div v-if="!notification.isRead" class="absolute top-4 right-4 flex h-3 w-3">
      <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-pink-400 opacity-75"></span>
      <span class="relative inline-flex rounded-full h-3 w-3 bg-pink-500"></span>
    </div>
  </div>
</template>