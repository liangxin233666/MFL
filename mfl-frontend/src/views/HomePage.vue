<script setup lang="ts">
import { ref } from 'vue';
import { FireIcon, ChevronDownIcon } from '@heroicons/vue/24/solid';
import ArticlePreview from '../components/ArticlePreview.vue';
import FeaturedCarousel from '../components/FeaturedCarousel.vue'; // 引入新创建的轮播组件

const tags = ref([
  { name: '番剧', active: false }, { name: '国创', active: false }, { name: '综艺', active: true },
  { name: '动画', active: false }, { name: '鬼畜', active: false }, { name: '舞蹈', active: false },
  { name: '娱乐', active: false }, { name: '科技数码', active: false }, { name: '美食', active: false },
  { name: '汽车', active: false }, { name: '体育运动', active: false },
]);

const articles = ref([
  { slug: 'exploring-future-of-spring-boot', imageUrl: 'https://picsum.photos/seed/b2/400/225', author: 'Albert Pai', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026705d', title: '【Java 后端】探索 Spring Boot 的未来发展方向', viewsCount: 192000, commentsCount: 371 },
  { slug: 'how-to-build-scalable-web-apps', imageUrl: 'https://picsum.photos/seed/a1/400/225', author: 'Eric Simons', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026704d', title: '【前端开发】如何构建可扩展的 Web 应用', viewsCount: 29000, commentsCount: 245 },
  { slug: 'tailwind-css-vs-bem', imageUrl: 'https://picsum.photos/seed/c3/400/225', author: 'John Doe', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026706d', title: 'Tailwind CSS vs. BEM：现代 CSS 方法论的深度思辨', viewsCount: 67000, commentsCount: 147 },
  { slug: 'full-stack-app-with-vite-and-vue', imageUrl: 'https://picsum.photos/seed/d4/400/225', author: 'Jane Smith', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026707d', title: '从零开始，使用 Vite 和 Vue 构建一个全栈应用', viewsCount: 63000, commentsCount: 60 },
  { slug: 'lol-new-player-guide', imageUrl: 'https://picsum.photos/seed/lol1/400/225', author: '电竞一条龙', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026708d', title: '坏了，这局我们这边没刷新牢玩家，比分全程逆风', viewsCount: 595000, commentsCount: 638 },
  { slug: 'most-toxic-player', imageUrl: 'https://picsum.photos/seed/lol2/400/225', author: '峡谷鬼见愁', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026709d', title: '《打野玩家最恐惧的一集》', viewsCount: 555000, commentsCount: 730 },
  { slug: 'health-and-life', imageUrl: 'https://picsum.photos/seed/life1/400/225', author: '养生堂主', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026710d', title: '低能量人也能做到的【健康生活习惯系统】', viewsCount: 1542000, commentsCount: 955 },
  { slug: 'food-tour', imageUrl: 'https://picsum.photos/seed/food1/400/225', author: '美食作家王刚', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026711d', title: '不做第二个西贝？绿茶餐厅撤下“现做”招牌', viewsCount: 606000, commentsCount: 2245 },
]);
</script>

<template>
  <div class="space-y-8">
    <!-- 1. 分类标签栏 -->
    <div class="flex items-center gap-x-1 sm:gap-x-3 text-sm">
      <div class="flex-shrink-0 flex items-center gap-4">
        <a href="#" class="btn btn-ghost btn-sm rounded-full bg-pink-500/10 text-pink-500 hover:bg-pink-500/20">
          <FireIcon class="w-4 h-4" /> 热门
        </a>
      </div>
      <div class="flex-grow flex items-center overflow-x-auto no-scrollbar">
        <div class="flex items-center gap-x-1 sm:gap-x-3">
          <a
              v-for="tag in tags" :key="tag.name"
              href="#"
              class="btn btn-ghost btn-sm rounded-md font-normal"
              :class="{ 'text-pink-500': tag.active }"
          >
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

    <!-- 2. 头部特色内容 (轮播+精选) -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-5">
      <!-- 左侧轮播 -->
      <FeaturedCarousel />
      <!-- 右侧 2x2 精选 -->
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-5">
        <router-link
            v-for="article in articles.slice(0, 4)"
            :key="article.slug"
            :to="'/article/' + article.slug"
        >
          <ArticlePreview v-bind="article" />
        </router-link>
      </div>
    </div>

    <!-- 3. 更多推荐内容 -->
    <div>
      <h2 class="text-xl font-bold mb-6 flex items-center gap-2">
        <span class="inline-block w-2 h-2 rounded-full bg-pink-500"></span>
        <span>新鲜内容</span>
      </h2>
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
        <router-link
            v-for="article in articles.slice(4)"
            :key="article.slug"
            :to="'/article/' + article.slug"
        >
          <ArticlePreview v-bind="article" />
        </router-link>
      </div>
    </div>

  </div>
</template>

<style scoped>
.no-scrollbar::-webkit-scrollbar {
  display: none;
}
.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>