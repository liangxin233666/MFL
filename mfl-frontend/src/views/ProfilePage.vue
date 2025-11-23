<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { PlusIcon, CheckIcon } from '@heroicons/vue/24/solid';
import apiClient from '../api/apiClient';
import type { Profile, Article } from '../types/api';
import ArticlePreview from '../components/ArticlePreview.vue';

// 1. --- 初始化状态和路由 ---
const route = useRoute();
const authStore = useAuthStore();

const profile = ref<Profile | null>(null);
const articles = ref<Article[]>([]);
const activeTab = ref<'author' | 'favorited'>('author'); // 'author' for posts, 'favorited' for collections

const isLoadingProfile = ref(true);
const isLoadingArticles = ref(false);
const isFollowing = ref(false);

const currentPage = ref(0);
const articlesPerPage = 8; // 每页加载8篇文章
const hasMore = ref(true);

// 2. --- 计算属性 ---
const isOwnProfile = computed(() => {
  return authStore.user?.username === profile.value?.username;
});
const profileUsername = computed(() => route.params.username as string);

// 3. --- API 调用函数 ---
const fetchProfile = async (username: string) => {
  try {
    const response = await apiClient.get<{ profile: Profile }>(`/profiles/${username}`);
    profile.value = response.data.profile;
  } catch (error) {
    console.error("获取个人资料失败:", error);
  }
};

const fetchArticles = async (username: string, tab: 'author' | 'favorited', page: number) => {
  if (isLoadingArticles.value) return;
  isLoadingArticles.value = true;

  const offset = page * articlesPerPage;
  let apiUrl = `/articles?limit=${articlesPerPage}&offset=${offset}`;

  // 根据 tab 构建不同的 API URL
  if (tab === 'author') {
    apiUrl += `&author=${username}`;
  } else {
    apiUrl += `&favoritedBy=${username}`;
  }

  try {
    const response = await apiClient.get<{ articles: Article[], articlesCount: number }>(apiUrl);

    if (page === 0) {
      // 如果是第一页，直接替换
      articles.value = response.data.articles;
    } else {
      // 否则，追加到现有列表
      articles.value.push(...response.data.articles);
    }

    // 判断是否还有更多文章
    hasMore.value = articles.value.length < response.data.articlesCount;
    currentPage.value = page;

  } catch (error) {
    console.error(`获取[${tab}]文章列表失败:`, error);
  } finally {
    isLoadingArticles.value = false;
  }
};

// 4. --- 交互逻辑 ---
const loadMore = () => {
  if (hasMore.value && profile.value) {
    fetchArticles(profile.value.username, activeTab.value, currentPage.value + 1);
  }
};

const changeTab = (tab: 'author' | 'favorited') => {
  if (activeTab.value === tab) return;

  activeTab.value = tab;
  // 重置文章列表和分页状态
  articles.value = [];
  currentPage.value = 0;
  hasMore.value = true;
  // 重新加载第一页数据
  if (profile.value) {
    fetchArticles(profile.value.username, tab, 0);
  }
};

const toggleFollow = async () => {
  if (!authStore.isAuthenticated || !profile.value) return;
  isFollowing.value = true;
  const wasFollowing = profile.value.following;
  profile.value.following = !wasFollowing; // Optimistic update

  try {
    const { username } = profile.value;
    if (wasFollowing) {
      await apiClient.delete(`/profiles/${username}/follow`);
    } else {
      await apiClient.post(`/profiles/${username}/follow`, {});
    }
  } catch (error) {
    profile.value.following = wasFollowing; // Revert on failure
    alert('操作失败');
  } finally {
    isFollowing.value = false;
  }
};

// 5. --- 生命周期钩子和侦听器 ---
onMounted(async () => {
  isLoadingProfile.value = true;
  await fetchProfile(profileUsername.value);
  isLoadingProfile.value = false;
  await fetchArticles(profileUsername.value, activeTab.value, 0);
});

// **关键**：侦听路由参数变化（例如从一个人的主页跳转到另一个人的主页）
watch(profileUsername, async (newUsername) => {
  isLoadingProfile.value = true;
  activeTab.value = 'author';
  articles.value = [];
  currentPage.value = 0;
  hasMore.value = true;

  await fetchProfile(newUsername);
  isLoadingProfile.value = false;
  await fetchArticles(newUsername, 'author', 0);
});
</script>

<template>
  <div v-if="isLoadingProfile" class="text-center py-20"><span class="loading loading-spinner loading-lg"></span></div>
  <div v-else-if="profile">
    <!-- 用户信息横幅 -->
    <div class="card shadow-md overflow-hidden mb-8">
      <figure class="h-48">
        <img :src="`https://picsum.photos/seed/${profile.username}/1200/300`" alt="User Banner" class="w-full h-full object-cover" />
      </figure>
      <div class="card-body p-4 bg-base-100">
        <div class="flex items-start gap-4">
          <div class="avatar -mt-16">
            <div class="w-24 h-24 rounded-full ring ring-pink-500 ring-offset-base-100 ring-offset-2">
              <img :src="profile.image || `https://source.boringavatars.com/beam/120/${profile.username}`" />
            </div>
          </div>
          <div class="flex-grow">
            <h1 class="text-2xl font-bold">{{ profile.username }}</h1>
            <p class="text-sm text-base-content/70 mt-1">{{ profile.bio || '这位用户什么也没留下...' }}</p>
          </div>
          <div v-if="!isOwnProfile" class="self-start">
            <button
                @click="toggleFollow"
                :disabled="isFollowing"
                class="btn btn-sm gap-1"
                :class="{ 'bg-pink-500 text-white': !profile.following, 'btn-outline': profile.following }"
            >
              <span v-if="isFollowing" class="loading loading-spinner loading-xs"></span>
              <CheckIcon v-else-if="profile.following" class="w-4 h-4" />
              <PlusIcon v-else class="w-4 h-4"/>
              {{ isFollowing ? '...' : (profile.following ? '已关注' : '关注') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Tab 切换 -->
    <div role="tablist" class="tabs tabs-bordered tabs-lg">
      <a role="tab" class="tab" :class="{'tab-active text-pink-500': activeTab === 'author'}" @click="changeTab('author')">投稿</a>
      <a role="tab" class="tab" :class="{'tab-active text-pink-500': activeTab === 'favorited'}" @click="changeTab('favorited')">收藏</a>
    </div>

    <!-- 文章列表 -->
    <div class="mt-6">
      <div v-if="articles.length > 0" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
        <router-link v-for="article in articles" :key="article.slug" :to="'/article/' + article.slug">
          <ArticlePreview
              :image-url="`https://picsum.photos/seed/${article.slug}/400/225`"
              :title="article.title"
              :views-count="article.favoritesCount || 0"
              :comments-count="0"
              :author="article.author"
              :article-slug="article.slug"
          />
        </router-link>
      </div>
      <div v-if="!isLoadingArticles && articles.length === 0" class="text-center text-base-content/50 py-10">
        {{ activeTab === 'author' ? '这里空空如也，还没有任何投稿哦~' : '这里空空如也，还没有任何收藏哦~' }}
      </div>
    </div>

    <!-- 加载更多 -->
    <div class="text-center mt-8" v-if="hasMore">
      <button class="btn" @click="loadMore" :disabled="isLoadingArticles">
        <span v-if="isLoadingArticles" class="loading loading-spinner"></span>
        加载更多
      </button>
    </div>
  </div>
  <div v-else class="text-center py-20 font-bold text-xl">用户不存在或加载失败</div>
</template>