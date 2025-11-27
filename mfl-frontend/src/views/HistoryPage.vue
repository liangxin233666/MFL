<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import apiClient from '../api/apiClient';
import type { HistoryRecord } from '../types/api';
import { ClockIcon, CalendarIcon, TrashIcon } from '@heroicons/vue/24/outline';
import { ASSETS } from '../config/assets'; // 确保你有这个配置文件，或者换成硬编码默认图

// --- 状态 ---
const rawHistoryList = ref<HistoryRecord[]>([]);
const isLoading = ref(true);

// --- 辅助函数：时间格式化 ---
const formatTime = (isoString: string) => {
  return new Date(isoString).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
};

// --- 核心逻辑：按日期分组 ---
// 将扁平的数组转换为 { "今天": [...], "2023-11-26": [...] } 的结构
const groupedHistory = computed(() => {
  const groups: Record<string, HistoryRecord[]> = {};

  rawHistoryList.value.forEach(record => {
    const date = new Date(record.viewedAt);
    const today = new Date();

    let groupKey = date.toLocaleDateString('zh-CN');

    // 处理 "今天" 和 "昨天" 的人性化显示
    if (date.toDateString() === today.toDateString()) {
      groupKey = '今天';
    } else {
      const yesterday = new Date(today);
      yesterday.setDate(yesterday.getDate() - 1);
      if (date.toDateString() === yesterday.toDateString()) {
        groupKey = '昨天';
      }
    }

    if (!groups[groupKey]) {
      groups[groupKey] = [];
    }
    groups[groupKey].push(record);
  });

  return groups;
});

// --- API 调用 ---
const fetchHistory = async () => {
  isLoading.value = true;
  try {
    // 后端 controller 直接返回了 Page 对象
    const response = await apiClient.get<{ content: HistoryRecord[] }>('/history');
    rawHistoryList.value = response.data.content;
  } catch (error) {
    console.error("加载历史记录失败", error);
  } finally {
    isLoading.value = false;
  }
};

// (可选功能) 虽然你的后端自动修剪，但这里展示一下如何前端响应
const clearHistoryHint = () => {
  alert('系统会自动保留最近 30 条浏览记录，无需手动清理哦 (极致性能策略)');
};

onMounted(() => {
  fetchHistory();
});
</script>

<template>
  <div class="container mx-auto max-w-5xl py-8 px-4">
    <!-- 头部 -->
    <div class="flex items-center justify-between mb-8 border-b pb-4">
      <div class="flex items-center gap-3 text-pink-500">
        <ClockIcon class="w-8 h-8" />
        <h1 class="text-2xl font-bold">浏览历史</h1>
      </div>
      <!-- 提示信息 -->
      <div class="text-sm text-base-content/50 flex items-center gap-2">
        <span>只展示最近 30 条</span>
        <button @click="clearHistoryHint" class="btn btn-ghost btn-sm btn-circle" title="关于清理">
          <TrashIcon class="w-4 h-4" />
        </button>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="isLoading" class="flex justify-center py-20">
      <span class="loading loading-spinner loading-lg text-pink-500"></span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="rawHistoryList.length === 0" class="text-center py-20 text-base-content/40">
      <CalendarIcon class="w-16 h-16 mx-auto mb-4 opacity-20"/>
      <p class="text-lg">暂无历史记录，快去探索感兴趣的文章吧！</p>
    </div>

    <!-- 历史列表 (时间轴布局) -->
    <div v-else class="space-y-8">

      <!-- 遍历每一天 -->
      <div v-for="(records, dateKey) in groupedHistory" :key="dateKey">

        <!-- 日期标题 (如: 今天, 昨天) -->
        <h2 class="text-lg font-bold text-base-content/80 mb-4 ml-2 border-l-4 border-pink-500 pl-3 sticky top-20 bg-base-100 z-10 py-1">
          {{ dateKey }}
        </h2>

        <!-- 网格列表 -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-1 gap-4 relative">
          <!-- 垂直时间连接线 (仅在大屏下用于装饰，模拟 B 站 web 端布局逻辑，这里简化为卡片列表) -->

          <div
              v-for="item in records"
              :key="item.viewedAt"
              class="group card card-side bg-base-100 shadow-sm hover:shadow-md transition-all border border-base-200 h-32 md:h-40 overflow-hidden"
          >
            <!-- 1. 封面图 (左侧) -->
            <figure class="w-40 md:w-56 h-full flex-shrink-0 relative overflow-hidden">
              <router-link :to="`/article/${item.article.slug}`" class="block w-full h-full">
                <img
                    :src="item.article.coverImageUrl || ASSETS.defaults.articleCoverD"
                    class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                    :alt="item.article.title"
                />
                <!-- 时间戳浮层 -->
                <div class="absolute bottom-1 right-1 bg-black/60 text-white text-xs px-1.5 py-0.5 rounded backdrop-blur-sm">
                  {{ formatTime(item.viewedAt) }}
                </div>
              </router-link>
            </figure>

            <!-- 2. 内容 (右侧) -->
            <div class="card-body p-4 justify-between">
              <div>
                <router-link :to="`/article/${item.article.slug}`" class="card-title text-base md:text-lg font-bold line-clamp-2 hover:text-pink-500 transition-colors" :title="item.article.title">
                  {{ item.article.title }}
                </router-link>
              </div>

              <!-- 底部作者栏 -->
              <div class="flex items-center justify-between text-sm text-base-content/60">
                <router-link :to="`/profile/${item.article.author.username}`" class="flex items-center gap-2 hover:text-pink-500 transition-colors">
                  <div class="avatar">
                    <div class="w-6 h-6 rounded-full">
                      <img :src="item.article.author.image || ASSETS.defaults.avatarD" />
                    </div>
                  </div>
                  <span>{{ item.article.author.username }}</span>
                </router-link>

                <!-- 只在大屏显示的具体时间 -->
                <span class="hidden md:block text-xs text-base-content/40">
                     浏览于 {{ formatTime(item.viewedAt) }}
                  </span>
              </div>
            </div>

            <!-- 删除单条按钮 (仅前端演示，需后端API支持才可用，此处作为装饰或未来扩展) -->
            <!-- <button class="btn btn-square btn-ghost btn-sm absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity">
                <TrashIcon class="w-4 h-4 text-base-content/40 hover:text-red-500"/>
            </button> -->

          </div>
        </div>
      </div>

      <div class="text-center text-xs text-base-content/30 py-8">
        — 仅展示近期足迹 —
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 可以在这里添加一些时间轴的具体样式微调 */
</style>