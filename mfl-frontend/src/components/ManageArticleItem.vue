<!-- src/components/ManageArticleItem.vue -->
<script setup lang="ts">
import { computed } from 'vue';
import type { Article } from '../types/api';
import {
  PlayIcon,
  ChatBubbleBottomCenterTextIcon,
  HandThumbUpIcon,
  PencilSquareIcon,
  TrashIcon
} from '@heroicons/vue/24/outline';

const props = defineProps<{
  article: Article;
}>();

const emit = defineEmits(['delete']);

// 简单的删除确认
const confirmDelete = () => {
  if (confirm(`确定要删除文章《${props.article.title}》吗？此操作不可恢复。`)) {
    emit('delete', props.article.slug);
  }
};

// 格式化时间
const formattedDate = computed(() => {
  return new Date(props.article.createdAt).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
});
</script>

<template>
  <div class="flex flex-col sm:flex-row gap-4 p-4 border-b hover:bg-base-50 transition-colors">
    <!-- 1. 封面图 (左侧) -->
    <router-link :to="`/article/${article.slug}`" class="flex-shrink-0 group relative w-full sm:w-40 h-24 rounded-md overflow-hidden bg-base-200">
      <img
          :src="article.coverImageUrl || `https://picsum.photos/seed/${article.slug}/300/200`"
          class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
      />
      <!-- 视频/文章时长占位 (可选) -->
      <div class="absolute bottom-1 right-1 bg-black/60 text-white text-xs px-1 rounded">
        文章
      </div>
    </router-link>

    <!-- 2. 内容信息 (中间) -->
    <div class="flex-grow flex flex-col justify-between py-1">
      <div>
        <router-link :to="`/article/${article.slug}`" class="text-base font-bold hover:text-pink-500 line-clamp-1 text-base-content" :title="article.title">
          {{ article.title }}
        </router-link>
        <div class="text-xs text-base-content/50 mt-1">
          {{ formattedDate }}
        </div>
      </div>

      <!-- 数据统计行 -->
      <div class="flex items-center gap-6 text-xs text-base-content/60 mt-2 sm:mt-0">
        <div class="flex items-center gap-1" title="点赞/收藏">
          <HandThumbUpIcon class="w-4 h-4" />
          <span>{{ article.favoritesCount }}</span>
        </div>
        <div class="flex items-center gap-1" title="评论">
          <ChatBubbleBottomCenterTextIcon class="w-4 h-4" />
          <span>0</span> <!-- 如果有评论数字段请替换 -->
        </div>
      </div>
    </div>

    <!-- 3. 操作按钮 (右侧) -->
    <div class="flex flex-row sm:flex-col items-center sm:items-end justify-center sm:justify-center gap-2 mt-2 sm:mt-0">
      <router-link :to="`/editor/${article.slug}`" class="btn btn-sm btn-ghost border-base-300 hover:border-pink-500 hover:text-pink-500 font-normal">
        <PencilSquareIcon class="w-4 h-4" />
        编辑
      </router-link>

      <!-- 更多操作 / 删除 -->
      <div class="dropdown dropdown-end">
        <button tabindex="0" class="btn btn-sm btn-ghost btn-square">
          <span class="text-xl pb-2">...</span>
        </button>
        <ul tabindex="0" class="dropdown-content z-[1] menu p-2 shadow bg-base-100 rounded-box w-32 border">
          <li><a @click="confirmDelete" class="text-error hover:bg-error/10"><TrashIcon class="w-4 h-4"/> 删除</a></li>
        </ul>
      </div>
    </div>
  </div>
</template>