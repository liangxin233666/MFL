<script setup lang="ts">
import { computed } from 'vue';
import { EyeIcon, ChatBubbleOvalLeftEllipsisIcon } from '@heroicons/vue/24/solid';
import type { Profile } from '../types/api'; // **改动**: 引入 Profile 类型
import UserInfo from './UserInfo.vue';       // **改动**: 导入 UserInfo 组件

// **核心改动**: props 定义改变
const props = defineProps<{
  imageUrl: string;

  author: Profile; // **新**: 直接接收完整的 author 对象
  title: string;
  viewsCount: number;
  commentsCount: number;
}>();

const formatCount = (count: number) => {
  if (count >= 10000) return `${(count / 10000).toFixed(1)}万`;
  return count.toString();
};

const viewsDisplay = computed(() => formatCount(props.viewsCount));
const commentsDisplay = computed(() => formatCount(props.commentsCount));

</script>

<template>
  <div class="card bg-base-100 shadow-md transition-all duration-300 hover:shadow-xl hover:-translate-y-1 group rounded-lg overflow-hidden">
    <!-- 图片容器 -->
    <figure class="relative">
      <img :src="imageUrl" :alt="title" class="aspect-video w-full object-cover transition-transform duration-300 group-hover:scale-110" />
      <div class="absolute bottom-0 left-0 w-full h-1/2 bg-gradient-to-t from-black/60 to-transparent"></div>
      <div class="absolute bottom-2 left-3 right-3 flex justify-between items-center text-white text-xs font-bold">
        <div class="flex items-center gap-3">
          <div class="flex items-center gap-1">
            <EyeIcon class="w-4 h-4" />
            <span>{{ viewsDisplay }}</span>
          </div>
          <div class="flex items-center gap-1">
            <ChatBubbleOvalLeftEllipsisIcon class="w-4 h-4" />
            <span>{{ commentsDisplay }}</span>
          </div>
        </div>
      </div>
    </figure>

    <!-- 卡片主体内容 -->
    <div class="p-1">
      <!-- **核心改动**: 用 UserInfo 组件替换原来的 div -->
      <div class="flex items-start gap-1">
        <div class="flex-grow">
          <p class="font-medium text-sm leading-tight hover:text-pink-500 transition-colors two-line-clamp" :title="title">
            {{ title }}
          </p>
          <!-- 直接使用 UserInfo 组件，并传入整个 author 对象 -->
          <UserInfo :profile="author" size="xs" class="mt-1"/>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
.two-line-clamp {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1.4;
  overflow: hidden;
}
</style>