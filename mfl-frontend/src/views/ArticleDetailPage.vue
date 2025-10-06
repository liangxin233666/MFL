<script setup lang="ts">
import { ref } from 'vue';
import { ShareIcon, StarIcon, ChatBubbleBottomCenterTextIcon, PlusIcon } from '@heroicons/vue/24/solid';
import ArticlePreview from '../components/ArticlePreview.vue'; // 复用文章预览

// 模拟数据
const article = ref({
  title: '【Java 后端】探索 Spring Boot 的未来发展方向',
  author: 'Albert Pai',
  avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026705d',
  publishDate: '2025-10-06',
  tags: ['Java', 'SpringBoot', '后端', '技术分享'],
  // 使用 Markdown 格式
  content: `
## 引言

Spring Boot 作为 Java 生态中最受欢迎的微服务框架，极大地简化了 Spring 应用的创建和部署。随着云原生和响应式编程的兴起，Spring Boot 也在不断进化以适应新的技术趋势。

### 1. 更好的 GraalVM 支持

为了实现更快的启动速度和更低的内存占用，Spring Boot 正在积极拥抱 GraalVM Native Image。这意味着未来的 Spring Boot 应用可以被编译成本地可执行文件，实现“秒级启动”。

\`\`\`java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
\`\`\`

### 2. 响应式编程的深化

Project Loom 和虚拟线程的引入将为 Java 的并发编程带来革命性的变化。Spring Boot 6 将会更深入地集成这些特性，让开发者可以更简单地编写出高性能的异步代码。

- **简化异步代码**：避免回调地狱。
- **提升吞吐量**：用更少的资源处理更多的请求。

## 结论

Spring Boot 的未来是光明的。它正朝着更云原生、更高效、更易用的方向发展。对于 Java 开发者来说，持续关注这些变化并学习新技术是至关重要的。
  `,
});

const recommendations = ref([
  { imageUrl: 'https://picsum.photos/seed/rec1/400/225', author: 'Eric Simons', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026704d', title: '【前端开发】如何构建可扩展的 Web 应用', favoritesCount: 29000, commentsCount: 245 },
  { imageUrl: 'https://picsum.photos/seed/rec2/400/225', author: 'Jane Smith', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026707d', title: '从零开始，使用 Vite 和 Vue 构建一个全栈应用', favoritesCount: 63000, commentsCount: 60 },
]);
</script>

<template>
  <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
    <div class="lg:col-span-2">
      <div class="card bg-base-100 shadow-md">
        <div class="card-body">
          <h1 class="text-3xl font-bold mb-2">{{ article.title }}</h1>
          <div class="flex items-center gap-4 text-sm text-base-content/70 mb-4">
            <span>发布于 {{ article.publishDate }}</span>
            <div class="flex gap-2">
              <div v-for="tag in article.tags" :key="tag" class="badge badge-ghost badge-sm">{{ tag }}</div>
            </div>
          </div>

          <div class="divider"></div>

          <article class="prose max-w-none lg:prose-lg" v-html="article.content"></article>

          <div class="card-actions justify-end mt-8">
            <button class="btn btn-ghost"><StarIcon class="w-5 h-5"/> 收藏</button>
            <button class="btn btn-ghost"><ShareIcon class="w-5 h-5"/> 分享</button>
          </div>
        </div>
      </div>

      <div class="card bg-base-100 shadow-md mt-8">
        <div class="card-body">
          <h3 class="text-xl font-bold mb-4 flex items-center gap-2">
            <ChatBubbleBottomCenterTextIcon class="w-6 h-6 text-pink-500"/>
            <span>评论区</span>
          </h3>
          <div class="flex items-start gap-4">
            <div class="avatar">
              <div class="w-12 h-12 rounded-full">
                <img src="https://i.pravatar.cc/150?u=a042581f4e29026704d" />
              </div>
            </div>
            <textarea class="textarea textarea-bordered flex-grow" placeholder="留下你的精彩评论吧！"></textarea>
            <button class="btn bg-pink-500 hover:bg-pink-600 text-white">发布</button>
          </div>

          <div class="mt-6 text-center text-base-content/50 py-6">
            还没有评论，快来抢沙发吧！
          </div>
        </div>
      </div>
    </div>

    <aside class="space-y-8">
      <div class="card bg-base-100 shadow-md">
        <div class="card-body items-center text-center">
          <div class="avatar">
            <div class="w-20 rounded-full">
              <img :src="article.avatar" />
            </div>
          </div>
          <h2 class="card-title mt-2">{{ article.author }}</h2>
          <p class="text-sm text-base-content/70">一位热爱分享技术的开发者</p>
          <div class="card-actions justify-end mt-2">
            <button class="btn bg-pink-500 hover:bg-pink-600 text-white btn-sm w-full">
              <PlusIcon class="w-4 h-4"/>
              关注
            </button>
          </div>
        </div>
      </div>

      <div class="card bg-base-100 shadow-md">
        <div class="card-body">
          <h3 class="card-title text-lg">相关推荐</h3>
          <div class="space-y-4 mt-2">
            <div v-for="(rec, index) in recommendations" :key="index" class="flex gap-3 group">
              <div class="w-2/5 flex-shrink-0 rounded-md overflow-hidden">
                <img :src="rec.imageUrl" class="w-full h-full object-cover aspect-video transition-transform duration-300 group-hover:scale-110" />
              </div>
              <div class="flex-grow">
                <a class="font-medium text-sm leading-tight hover:text-pink-500 transition-colors two-line-clamp" :title="rec.title">
                  {{ rec.title }}
                </a>
                <span class="text-xs text-base-content/70 mt-1 block">{{ rec.author }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </aside>
  </div>
</template>