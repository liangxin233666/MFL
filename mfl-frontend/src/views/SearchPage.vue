<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import apiClient from '../api/apiClient';
import type { Article } from '../types/api';
import ArticlePreview from '../components/ArticlePreview.vue'; // 复用预览组件
import { MagnifyingGlassIcon, FaceFrownIcon } from '@heroicons/vue/24/outline';
import {ASSETS} from "../config/assets.ts";

const route = useRoute();

// --- 状态 ---
const articles = ref<Article[]>([]);
const totalCount = ref(0);
const isLoading = ref(false);
const currentPage = ref(0);
const pageSize = 12; // 搜索结果一页显示的条数
const searchQuery = ref('');

// --- 搜索逻辑 ---
const doSearch = async (query: string, page = 0) => {
  // 如果关键词为空，重置并返回
  if (!query || query.trim() === '') {
    articles.value = [];
    totalCount.value = 0;
    return;
  }

  if (page === 0) {
    isLoading.value = true;
    articles.value = [];
  }

  try {
    const params = {
      query: query,
      page: page,
      size: pageSize
    };

    // 对应后端的 /api/search?query=...&page=...&size=...
    const response = await apiClient.get<{ articles: Article[], articlesCount: number }>('/search', { params });

    if (page === 0) {
      articles.value = response.data.articles;
    } else {
      articles.value.push(...response.data.articles);
    }

    totalCount.value = response.data.articlesCount;
    currentPage.value = page;
  } catch (error) {
    console.error("搜索失败", error);
  } finally {
    isLoading.value = false;
  }
};

// --- 监听路由变化 ---
// 这是关键！当用户在Navbar再次搜索时，URL变为 /search?query=新词
// watch 会捕捉到并重新发起请求
watch(() => route.query.query, (newQuery) => {
  if (typeof newQuery === 'string') {
    searchQuery.value = newQuery;
    doSearch(newQuery, 0);
  }
});

// 加载更多
const loadMore = () => {
  if (articles.value.length < totalCount.value) {
    doSearch(searchQuery.value, currentPage.value + 1);
  }
};

onMounted(() => {
  // 从 URL 获取初始关键词 (例如用户刷新页面)
  const q = route.query.query;
  if (typeof q === 'string') {
    searchQuery.value = q;
    doSearch(q, 0);
  }
});
</script>

<template>
  <div class="container mx-auto px-4 py-8 max-w-7xl">

    <!-- 头部搜索概览 -->
    <div class="mb-8 border-b pb-4">
      <div class="flex items-center gap-2 mb-2">
        <MagnifyingGlassIcon class="w-8 h-8 text-pink-500"/>
        <h1 class="text-2xl font-bold">搜索结果</h1>
      </div>
      <p class="text-base-content/60" v-if="searchQuery">
        找到与 <span class="font-bold text-pink-500">"{{ searchQuery }}"</span> 相关的结果约 <span class="font-bold">{{ totalCount }}</span> 个
      </p>
    </div>

    <!-- 加载中 -->
    <div v-if="isLoading" class="text-center py-20">
      <span class="loading loading-spinner loading-lg text-pink-500"></span>
    </div>

    <!-- 空状态 (搜了但没结果) -->
    <div v-else-if="articles.length === 0 && searchQuery" class="flex flex-col items-center justify-center py-20 text-base-content/40">
      <FaceFrownIcon class="w-16 h-16 mb-4 opacity-20"/>
      <p class="text-lg">没有找到相关文章，换个关键词试试？</p>
    </div>

    <!-- 未输入关键词 -->
    <div v-else-if="!searchQuery" class="text-center py-20 text-base-content/40">
      请输入关键词开始搜索
    </div>

    <!-- 搜索结果列表 -->
    <div v-else>
      <!-- 使用 grid 布局 -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <router-link
            v-for="article in articles"
            :key="article.slug"
            :to="'/article/' + article.slug"
            class="group"
        >
          <!-- 复用之前的 ArticlePreview 组件 -->
          <ArticlePreview
              :image-url="article.coverImageUrl|| ASSETS.defaults.articleCoverD"
              :title="article.title"
              :views-count="article.favoritesCount || 0"
              :comments-count="0"
              :author="article.author"
              :article-slug="article.slug"
          />
        </router-link>
      </div>

      <!-- 加载更多 -->
      <div v-if="articles.length < totalCount" class="text-center mt-12">
        <button @click="loadMore" class="btn btn-ghost">
          <span v-if="isLoading" class="loading loading-spinner"></span>
          加载更多
        </button>
      </div>
    </div>
  </div>
</template>