<!-- src/views/FeedPage.vue -->
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import apiClient from '../api/apiClient';
import type { Article } from '../types/api';
import FeedItemCard from '../components/FeedItemCard.vue';

const articles = ref<Article[]>([]);
const isLoading = ref(false);
const currentPage = ref(0);
const hasMore = ref(true); // 是否还有更多数据可加载
const loader = ref<HTMLElement | null>(null); // 用于无限滚动的“哨兵”元素

const loadMoreArticles = async () => {
  // 如果正在加载或没有更多数据了，则直接返回
  if (isLoading.value || !hasMore.value) return;

  isLoading.value = true;
  try {
    const offset = currentPage.value * 5;
    const response = await apiClient.get<{ articles: Article[], articlesCount: number }>(`/articles/feed?limit=5&offset=${offset}`);

    if (response.data.articles.length > 0) {
      articles.value.push(...response.data.articles);
      currentPage.value++;
    } else {
      // 如果返回的文章数组为空，说明已经没有更多了
      hasMore.value = false;
    }
  } catch (error) {
    console.error("加载动态失败:", error);
  } finally {
    isLoading.value = false;
  }
};

let observer: IntersectionObserver;
onMounted(() => {
  // 创建一个 Intersection Observer 实例
  observer = new IntersectionObserver(([entry]) => {
    // 当“哨兵”元素进入视口时，加载更多文章
    if (entry && entry.isIntersecting) {
      loadMoreArticles();
    }
  });

  // 开始观察“哨兵”元素
  if (loader.value) {
    observer.observe(loader.value);
  }
});

onUnmounted(() => {
  // 组件卸载时，停止观察
  if (observer) {
    observer.disconnect();
  }
});
</script>

<template>
  <div class="container mx-auto">
    <!-- Bilibili 风格三栏布局 -->
    <div class="grid grid-cols-12 gap-6">
      <!-- 左侧边栏 (Sticky) -->
      <aside class="hidden lg:block col-span-3">
        <div class="sticky top-20 space-y-2 card bg-base-100 shadow-sm p-2 rounded-lg">
          <a href="#" class="btn btn-ghost justify-start font-bold text-pink-500">我的关注</a>
        </div>
      </aside>

      <!-- 中间主内容区 -->
      <main class="col-span-12 lg:col-span-6">
        <div v-if="articles.length > 0">
          <FeedItemCard v-for="article in articles" :key="article.slug" :article="article" />
        </div>

        <!-- 加载指示器和“哨兵”元素 -->
        <div ref="loader" class="text-center py-8">
          <div v-if="isLoading" class="flex justify-center"><span class="loading loading-dots loading-lg"></span></div>
          <div v-if="!hasMore && articles.length > 0" class="text-base-content/50">没有更多动态了~</div>
          <div v-if="!isLoading && articles.length === 0" class="text-base-content/50">你关注的用户还没有发布动态哦</div>
        </div>
      </main>

      <!-- 右侧边栏 (Sticky) -->
      <aside class="hidden lg:block col-span-3">
        <div class="sticky top-20 space-y-4 card bg-base-100 shadow-sm p-4 rounded-lg">
          <h3 class="font-bold">热搜</h3>
          <ul class="space-y-3">
            <li v-for="i in 10" :key="i" class="flex items-center gap-3 hover:bg-base-200 p-1 rounded-md cursor-pointer">
              <span class="font-bold" :class="i <=3 ? 'text-red-500' : 'text-base-content/50'">{{ i }}</span>
              <span class="flex-grow text-sm">热搜词条第{{i}}位</span>
            </li>
          </ul>
        </div>
      </aside>
    </div>
  </div>
</template>