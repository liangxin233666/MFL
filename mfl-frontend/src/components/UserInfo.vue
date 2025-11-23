<script setup lang="ts">
import { computed } from 'vue';
import type { Profile } from '../types/api';

const props = withDefaults(defineProps<{
  // 接收一个完整的 profile 对象
  profile: Profile | null;
  // (可选) 控制头像尺寸
  size?: 'xs' | 'sm' | 'md' | 'lg';
  // (可选) 是否显示名字
  showName?: boolean;
  // (可选) 是否显示头像
  showAvatar?: boolean;
  // (可选) 显示额外信息，如时间戳
  timestamp?: string | null;
}>(), {
  // 设置 props 的默认值
  size: 'md',
  showName: true,
  showAvatar: true,
  timestamp: null,
});

// 头像尺寸的映射
const sizeClasses = computed(() => {
  switch (props.size) {
    case 'xs': return 'w-8 h-8';
    case 'sm': return 'w-10 h-10';
    case 'lg': return 'w-20 h-20';
    case 'md': default: return 'w-12 h-12';
  }
});

// 默认头像的逻辑，只存在于此
const userImage = computed(() => {
  if (!props.profile) return `https://source.boringavatars.com/beam/120/guest`;
  return props.profile.image || `https://source.boringavatars.com/beam/120/${props.profile.username}`;
});
</script>

<template>
  <div v-if="profile" class="inline-flex items-center gap-3 group">
    <!-- 头像 -->
    <router-link v-if="showAvatar" :to="`/profile/${profile.username}`" class="avatar flex-shrink-0">
      <div class="rounded-full ring-pink-500/0 group-hover:ring-2 transition-all duration-200" :class="sizeClasses">
        <img :src="userImage" :alt="profile.username" />
      </div>
    </router-link>

    <!-- 名字和时间戳 -->
    <div v-if="showName">
      <router-link :to="`/profile/${profile.username}`" class="font-bold text-sm hover:text-pink-500 transition-colors">
        {{ profile.username }}
      </router-link>
      <p v-if="timestamp" class="text-xs text-base-content/60">
        {{ new Date(timestamp).toLocaleString() }}
      </p>
    </div>
  </div>
</template>