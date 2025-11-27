<!-- src/views/HomePage.vue -->
<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { FireIcon, ChevronDownIcon, ArrowPathIcon } from '@heroicons/vue/24/solid';
import apiClient from '../api/apiClient';
import type { Article } from '../types/api';
import ArticlePreview from '../components/ArticlePreview.vue';
import FeaturedCarousel from '../components/FeaturedCarousel.vue';
import {ASSETS} from "../config/assets.ts";

// 模拟标签数据，您可以替换为从 /api/tags 获取
const tags = ref([
  { name: '番剧', active: false }, { name: '国创', active: false }, { name: '综艺', active: true },
  { name: '动画', active: false }, { name: '鬼畜', active: false }, { name: '舞蹈', active: false },
]);

const articles = ref<Article[]>([]);
const isLoading = ref(true);

const fetchArticles = async (offset = 0) => {
  try {
    isLoading.value = true;
    const response = await apiClient.get<{ articles: Article[] }>('/articles?limit=20&offset=' + offset);
    articles.value = response.data.articles;
  } catch (error) {
    console.error('获取文章列表失败:', error);
    alert('无法加载文章列表，请检查后端服务是否开启。');
  } finally {
    isLoading.value = false;
  }
};

const refreshArticles = () => {
  const randomOffset = Math.floor(Math.random() * 20); // 随机偏移量
  fetchArticles(randomOffset);
};

onMounted(() => {
  fetchArticles();
  fetchTags();
});
const fetchTags = async () => {
  try {
    const response = await apiClient.get<{ tags: string[] }>('/tags');

    tags.value = response.data.tags.map(tagName => ({
      name: tagName,
      active: false, // 默认所有标签都是非激活状态
    }));
  } catch (error) {
    console.error('获取标签列表失败:', error);
  }
};
</script>

<template>
  <div class="-mt-9">
    <!-- Banner -->
    <div class="relative w-full h-40 md:h-45">
      <img :src="ASSETS.pages.homeBanner" class="w-full h-full object-cover object-center" alt="Banner">
      <div class="absolute inset-0 bg-gradient-to-t from-base-200 via-base-200/50 to-transparent"></div>
    </div>

    <div class="container mx-auto space-y-6 -mt">
      <!-- Tag Bar -->
      <div class="flex items-center gap-x-1 sm:gap-x-3 text-sm bg-base-100/80 backdrop-blur p-2 rounded-lg shadow">
        <div class="flex-shrink-0 flex items-center gap-4">
          <a href="#" class="btn btn-ghost btn-sm rounded-full bg-pink-500/10 text-pink-500 hover:bg-pink-500/20">
            <FireIcon class="w-4 h-4" /> 热门
          </a>
        </div>
        <div class="flex-grow flex items-center overflow-x-auto no-scrollbar">
          <div class="flex items-center gap-x-1 sm:gap-x-3">
            <a v-for="tag in tags" :key="tag.name" href="#" class="btn btn-ghost btn-sm rounded-md font-normal" :class="{ 'text-pink-500': tag.active }">
              {{ tag.name }}
            </a>
          </div>
        </div>
        <div class="flex-shrink-0 ml-2">
          <a href="#" class="btn btn-ghost btn-sm rounded-md">
            更多 <ChevronDownIcon class="w-4 h-4" />
          </a>
        </div>
      </div>

      <!-- Content Area -->
      <div v-if="isLoading" class="text-center py-20">
        <span class="loading loading-dots loading-lg text-pink-500"></span>
      </div>
      <div v-else>
        <!-- Featured Content -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-5">
          <FeaturedCarousel />
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-5">

            <template v-for="article in articles.slice(0, 4)" :key="article.slug">
              <router-link v-if="article" :to="'/article/' + article.slug">
                <ArticlePreview
                    :image-url="article.coverImageUrl|| `https://picsum.photos/seed/${article.slug}/400/225`"
                    :title="article.title"
                    :views-count="article.favoritesCount || 0"
                    :comments-count="0"
                    :author="article.author"
                    :article-slug="article.slug"
                />
              </router-link>
            </template>
          </div>
        </div>

        <!-- More Content -->
        <div class="mt-8">
          <h2 class="text-xl font-bold mb-6 flex items-center gap-2">
            <span class="inline-block w-2 h-2 rounded-full bg-pink-500"></span>
            <span>新鲜内容</span>
          </h2>
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
            <!--
              =========================================================
              =======>   第 二 处 核 心 修 正   <=======
              =========================================================
              同样，在这里也使用 <template> 标签进行分离
            -->
            <template v-for="article in articles.slice(4)" :key="article.slug">
              <router-link v-if="article" :to="'/article/' + article.slug">
                <ArticlePreview
                    :image-url="article.coverImageUrl|| `https://picsum.photos/seed/${article.slug}/400/225`"
                    :title="article.title"
                    :views-count="article.favoritesCount || 0"
                    :comments-count="0"
                    :author="article.author"
                    :article-slug="article.slug"
                />
              </router-link>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- Refresh Button -->
    <div @click="refreshArticles" class="fixed top-64 right-4 z-40 hidden lg:flex flex-col items-center justify-center w-12 h-16 bg-base-100 rounded-lg shadow-md cursor-pointer text-base-content/70 hover:text-pink-500 hover:shadow-lg transition-all group">
      <ArrowPathIcon class="w-6 h-6 group-hover:rotate-180 transition-transform duration-300" />
      <span class="text-xs mt-1 font-medium">换一换</span>
    </div>
  </div>
</template>

<style scoped>
.no-scrollbar::-webkit-scrollbar { display: none; }
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
</style>