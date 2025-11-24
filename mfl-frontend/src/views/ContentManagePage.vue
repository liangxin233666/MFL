<!-- src/views/ContentManagePage.vue -->
<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useAuthStore } from '../stores/auth';
import apiClient from '../api/apiClient';
import type { Article } from '../types/api';
import ManageArticleItem from '../components/ManageArticleItem.vue';
import {
  DocumentTextIcon,
  Squares2X2Icon,
  ChevronDownIcon
} from '@heroicons/vue/24/outline';

const authStore = useAuthStore();
const articles = ref<Article[]>([]);
const isLoading = ref(false);

// 筛选与排序状态
const filterType = ref('all'); // 'all', 'draft'(暂未实现)
const sortType = ref<'date' | 'views' | 'likes'>('date');

const fetchMyArticles = async () => {
  if (!authStore.user) return;

  isLoading.value = true;
  try {
    // 1. 根据前端的 sortType 决定传给后端的 sort 字符串
    // Spring 的格式通常是: "字段名,desc" (降序) 或 "字段名,asc" (升序)
    let sortParam = 'createdAt,desc'; // 默认：时间倒序
    if (sortType.value === 'likes') {
      sortParam = 'favoritesCount,desc'; // 收藏数倒序
    }

    // 2. 发送请求
    // 注意：limit 改成了 size
    const response = await apiClient.get<{ articles: Article[] }>('/articles', {
      params: {
        author: authStore.user.username,
        size: 20,     // <--- 改正：Spring Pageable 用 size
        page: 0,      // (可选) 如果要做翻页，这里动态传 currentPage
        sort: sortParam // <--- 新增：利用 Pageable 的排序功能
      }
    });

    // 3. 直接赋值，不需要前端 sort 了
    articles.value = response.data.articles;

  } catch (error) {
    console.error("获取稿件失败", error);
  } finally {
    isLoading.value = false;
  }
};

// 删除逻辑
const handleDelete = async (slug: string) => {
  try {
    await apiClient.delete(`/articles/${slug}`);
    // 删除成功后，从列表中移除
    articles.value = articles.value.filter(a => a.slug !== slug);
  } catch (error) {
    alert('删除失败');
  }
};

// 监听排序变化，重新获取或重新排序
watch(sortType, () => {
  fetchMyArticles();
});

onMounted(() => {
  fetchMyArticles();
});
</script>

<template>
  <div class="container mx-auto max-w-7xl py-6">
    <div class="grid grid-cols-12 gap-6">

      <!-- 左侧创作中心导航 (仿B站) -->
      <aside class="col-span-12 md:col-span-2">
        <div class="card bg-base-100 shadow-sm rounded-lg overflow-hidden sticky top-20">
          <div class="p-4 bg-pink-500 text-white font-bold flex items-center gap-2">
            <Squares2X2Icon class="w-5 h-5"/>
            创作中心
          </div>
          <ul class="menu w-full p-2">
            <li><a class="active bg-pink-50 text-pink-500 hover:bg-pink-100 font-bold"><DocumentTextIcon class="w-5 h-5"/> 稿件管理</a></li>
            <li><a class="text-base-content/70"><span class="w-5"></span> 申诉管理</a></li>
            <li><a class="text-base-content/70"><span class="w-5"></span> 字幕管理</a></li>
            <div class="divider my-1"></div>
            <li><a class="text-base-content/70">数据中心</a></li>
            <li><a class="text-base-content/70">粉丝管理</a></li>
          </ul>
        </div>
      </aside>

      <!-- 右侧主内容区 -->
      <main class="col-span-12 md:col-span-10">
        <div class="card bg-base-100 shadow-sm min-h-[600px]">
          <div class="card-body p-0">
            <!-- 顶部筛选栏 -->
            <div class="p-4 border-b flex flex-col sm:flex-row justify-between items-center gap-4">
              <!-- Tab -->
              <div class="tabs tabs-boxed bg-transparent p-0">
                <a class="tab tab-lg" :class="{'tab-active text-pink-500 !bg-transparent border-b-2 border-pink-500 rounded-none': filterType === 'all'}" @click="filterType = 'all'">全部稿件 <span class="text-xs ml-1 bg-base-200 px-1 rounded-full text-base-content/50">{{ articles.length }}</span></a>
                <a class="tab tab-lg" :class="{'tab-active': filterType === 'draft'}">草稿箱 <span class="text-xs ml-1 bg-base-200 px-1 rounded-full text-base-content/50">0</span></a>
              </div>

              <!-- 搜索和排序 -->
              <div class="flex items-center gap-3 w-full sm:w-auto">
                <input type="text" placeholder="搜索稿件..." class="input input-sm input-bordered w-full sm:w-48" />

                <div class="dropdown dropdown-end">
                  <label tabindex="0" class="btn btn-sm btn-ghost gap-1 border border-base-300 font-normal">
                    {{ sortType === 'date' ? '投稿时间排序' : '最多收藏排序' }}
                    <ChevronDownIcon class="w-4 h-4"/>
                  </label>
                  <ul tabindex="0" class="dropdown-content z-[1] menu p-2 shadow bg-base-100 rounded-box w-40 border">
                    <li><a @click="sortType = 'date'" :class="{'text-pink-500': sortType==='date'}">投稿时间排序</a></li>
                    <li><a @click="sortType = 'likes'" :class="{'text-pink-500': sortType==='likes'}">收藏数排序</a></li>
                  </ul>
                </div>
              </div>
            </div>

            <!-- 列表内容 -->
            <div v-if="isLoading" class="text-center py-20">
              <span class="loading loading-spinner loading-lg text-pink-500"></span>
            </div>

            <div v-else-if="articles.length > 0">
              <ManageArticleItem
                  v-for="article in articles"
                  :key="article.slug"
                  :article="article"
                  @delete="handleDelete"
              />
            </div>

            <div v-else class="flex flex-col items-center justify-center py-20 text-base-content/40">
              <DocumentTextIcon class="w-16 h-16 mb-4 opacity-20"/>
              <p>暂无稿件，快去创作吧~</p>
            </div>
          </div>
        </div>
      </main>

    </div>
  </div>
</template>