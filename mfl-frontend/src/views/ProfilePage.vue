<script setup lang="ts">
import { ref } from 'vue';
import { PlusIcon } from '@heroicons/vue/24/solid';
import ArticlePreview from '../components/ArticlePreview.vue';

const user = ref({
  username: '电磁炮打蚊子',
  avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026704d',
  banner: 'https://picsum.photos/seed/profile-banner/1200/300',
  bio: '知名游戏区UP主，分享最新最热的游戏资讯和攻略。',
  followers: 1250000,
  following: 89,
});

const articles = ref([
  { slug: 'game-report-1', imageUrl: 'https://picsum.photos/seed/p1/400/225', author: '电磁炮打蚊子', avatar: user.value.avatar, title: '【游戏速报】大的要来了！万众期待的XXX续作发布预告', viewsCount: 320000, commentsCount: 1200 },
  { slug: 'game-guide-2', imageUrl: 'https://picsum.photos/seed/p2/400/225', author: '电磁炮打蚊子', avatar: user.value.avatar, title: '十分钟上手！从萌新到高手的保姆级教程', viewsCount: 189000, commentsCount: 854 },
  { slug: 'game-story-3', imageUrl: 'https://picsum.photos/seed/p3/400/225', author: '电磁炮打蚊子', avatar: user.value.avatar, title: '剧情解析：你真的看懂这个结局了吗？', viewsCount: 450000, commentsCount: 2300 },
]);

const activeTab = ref('articles');
</script>

<template>
  <div>
    <!-- 用户信息横幅 -->
    <div class="card shadow-md overflow-hidden mb-8">
      <figure class="h-48">
        <img :src="user.banner" alt="User Banner" class="w-full h-full object-cover" />
      </figure>
      <div class="card-body p-4 bg-base-100">
        <div class="flex items-start gap-4">
          <div class="avatar -mt-16">
            <div class="w-24 h-24 rounded-full ring ring-pink-500 ring-offset-base-100 ring-offset-2">
              <img :src="user.avatar" />
            </div>
          </div>
          <div class="flex-grow">
            <h1 class="text-2xl font-bold">{{ user.username }}</h1>
            <p class="text-sm text-base-content/70 mt-1">{{ user.bio }}</p>
            <div class="flex gap-4 text-sm mt-2">
              <span><span class="font-bold">{{ (user.following) }}</span> 关注</span>
              <span><span class="font-bold">{{ (user.followers / 10000).toFixed(1) }}万</span> 粉丝</span>
            </div>
          </div>
          <div class="self-start">
            <button class="btn bg-pink-500 hover:bg-pink-600 text-white btn-sm">
              <PlusIcon class="w-4 h-4" />
              关注
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 内容区域 -->
    <div role="tablist" class="tabs tabs-bordered tabs-lg">
      <a role="tab" class="tab" :class="{'tab-active text-pink-500': activeTab === 'articles'}" @click="activeTab = 'articles'">我的投稿</a>
      <a role="tab" class="tab" :class="{'tab-active text-pink-500': activeTab === 'favorites'}" @click="activeTab = 'favorites'">我的收藏</a>
    </div>

    <!-- 文章列表 -->
    <div v-if="activeTab === 'articles'" class="mt-6">
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
        <router-link v-for="(article, index) in articles" :key="index" :to="'/article/' + article.slug">
          <ArticlePreview v-bind="article"/>
        </router-link>
      </div>
    </div>
    <!-- 收藏列表 -->
    <div v-if="activeTab === 'favorites'" class="mt-6 text-center text-base-content/50 py-10">
      这里是收藏的视频和文章~
    </div>
  </div>
</template>