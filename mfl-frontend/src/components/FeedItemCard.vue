<script setup lang="ts">
import type { Article } from '../types/api';
import { HandThumbUpIcon, ChatBubbleOvalLeftIcon, ArrowUturnRightIcon } from '@heroicons/vue/24/outline';
import UserInfo from './UserInfo.vue'; // 1. 导入新组件

defineProps<{ article: Article }>();
</script>

<template>
  <div class="card bg-base-100 shadow-sm rounded-lg p-4 mb-4">
    <!-- 2. 头部：使用新组件，并传入时间戳 -->
    <UserInfo :profile="article.author" :timestamp="article.createdAt" />

    <!-- 主体内容 -->
    <div class="ml-16 mt-2">
      <router-link :to="`/article/${article.slug}`">
        <h3 class="font-bold text-lg hover:text-pink-500 transition-colors">{{ article.title }}</h3>
        <p class="mt-1 text-base-content/80 two-line-clamp">{{ article.description }}</p>
        <figure class="mt-2 rounded-lg overflow-hidden">
          <img :src="`https://picsum.photos/seed/${article.slug}/600/300`" class="w-full object-cover aspect-[16/9]" />
        </figure>
      </router-link>
    </div>

    <!-- 底部操作栏 -->
    <div class="ml-16 mt-4 flex justify-end items-center gap-x-6 text-base-content/70">
      <button class="btn btn-ghost btn-sm gap-2">
        <ArrowUturnRightIcon class="w-5 h-5" /> 转发
      </button>
      <button class="btn btn-ghost btn-sm gap-2">
        <ChatBubbleOvalLeftIcon class="w-5 h-5" /> {{ 0 }}
      </button>
      <button class="btn btn-ghost btn-sm gap-2">
        <HandThumbUpIcon class="w-5 h-5" /> {{ article.favoritesCount || 0 }}
      </button>
    </div>
  </div>
</template>