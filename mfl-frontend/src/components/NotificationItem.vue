<script setup lang="ts">
import { computed } from 'vue';
import type { Notification } from '../types/api';
import UserInfo from './UserInfo.vue';
import {
  HeartIcon,
  ChatBubbleLeftEllipsisIcon,
  ArrowUturnLeftIcon,
  CheckBadgeIcon,
  ExclamationCircleIcon,
  MegaphoneIcon,
  UserCircleIcon
} from '@heroicons/vue/24/solid';

const props = defineProps<{
  notification: Notification;
}>();

const emit = defineEmits(['click']);

const isSystemMsg = computed(() => {
  // actor 不存在 (null) 或者 id 为 -1 (具体看后端设定) 通常代表系统
  // 或者特定的系统事件类型
  return ['ARTICLE_APPROVED', 'ARTICLE_REJECTED'].includes(props.notification.type) || !props.notification.actor;
});

// 配置每种类型的显示样式和文案
const config = computed(() => {
  switch (props.notification.type) {
    case 'ARTICLE_LIKED':
      return {
        icon: HeartIcon,
        color: 'text-pink-500',
        bg: 'bg-pink-100',
        text: '赞了你的文章',
        targetText: '查看文章'
      };
    case 'COMMENT_CREATED':
      return {
        icon: ChatBubbleLeftEllipsisIcon,
        color: 'text-green-500',
        bg: 'bg-green-100',
        text: '评论了你的文章',
        targetText: '查看评论详情'
      };
    case 'COMMENT_REPLIED':
      return {
        icon: ArrowUturnLeftIcon,
        color: 'text-purple-500',
        bg: 'bg-purple-100',
        text: '回复了你的评论',
        targetText: '查看对话'
      };
    case 'ARTICLE_APPROVED':
      return { icon: CheckBadgeIcon, color: 'text-green-600', bg: 'bg-green-100', text: '投稿审核通过', targetText: '查看文章' };
    case 'ARTICLE_REJECTED':
      return { icon: ExclamationCircleIcon, color: 'text-red-500', bg: 'bg-red-100', text: '投稿被退回', targetText: '去修改' };

    default:
      return { icon: MegaphoneIcon, color: 'text-gray-500', bg: 'bg-gray-100', text: '新通知', targetText: '查看详情' };
  }
});

const handleClick = () => {
  emit('click', props.notification);
};

const timeAgo = computed(() => {
  return new Date(props.notification.createdAt).toLocaleString();
});
</script>

<template>
  <div
      @click="handleClick"
      class="relative flex gap-4 p-4 border-b hover:bg-base-50 transition-colors cursor-pointer group items-start"
      :class="{ 'bg-pink-50/40': !notification.isRead }"
  >
    <!-- 左侧：头像 或 系统图标 -->
    <div class="flex-shrink-0">
      <div v-if="isSystemMsg" class="w-10 h-10 rounded-full flex items-center justify-center" :class="config.bg">
        <component :is="config.icon" class="w-6 h-6" :class="config.color"/>
      </div>
      <!-- 适配：以前叫 sender, 现在叫 actor -->
      <!-- UserInfo组件需要传入 Profile 格式 {username, image, ...} -->
      <UserInfo v-else :profile="{ username: notification.actor.username, image: notification.actor.image, bio: '', following: false }" size="md" :show-name="false" />
    </div>

    <!-- 中间：内容区域 -->
    <div class="flex-grow">
      <div class="flex items-center gap-2 mb-1">
        <span class="font-bold text-base-content">
            {{ isSystemMsg ? '系统通知' : notification.actor.username }}
        </span>
        <span class="text-xs text-base-content/60">{{ timeAgo }}</span>
      </div>

      <!-- 行为描述 -->
      <div class="text-sm mb-1 flex items-center gap-2">
        <span class="flex items-center gap-1 text-base-content/80 font-medium">
           {{ config.text }}
           <component v-if="!isSystemMsg" :is="config.icon" class="w-4 h-4" :class="config.color"/>
        </span>
      </div>

      <!-- [审核驳回理由] payload (如果后端传了这个字段) -->
      <div v-if="notification.payload" class="mt-2 p-3 bg-base-200 rounded-lg text-sm border-l-4"
           :class="notification.type === 'ARTICLE_REJECTED' ? 'border-red-500 bg-red-50 text-base-content/80' : 'border-base-300'">
        <span v-if="notification.type === 'ARTICLE_REJECTED'" class="font-bold text-red-500 block mb-1"></span>
        {{ notification.payload }}
      </div>


      <div v-if="notification.resource?.slug" class="text-xs text-base-content/50 mt-2 inline-flex items-center gap-1 bg-base-200/50 px-2 py-1 rounded hover:text-pink-500 transition-colors">
        <span>📄 {{ config.targetText }}</span>

      </div>
    </div>

    <!-- 右侧：未读红点 -->
    <div v-if="!notification.isRead" class="absolute top-4 right-4">
      <div class="w-2.5 h-2.5 bg-red-500 rounded-full ring-2 ring-white"></div>
    </div>
  </div>
</template>