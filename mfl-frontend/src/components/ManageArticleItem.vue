<!-- src/components/ManageArticleItem.vue -->
<script setup lang="ts">
import { ref, computed } from 'vue';
import type { Article } from '../types/api';
import {
  ChatBubbleBottomCenterTextIcon,
  StarIcon,
  PencilSquareIcon,
  TrashIcon,
  ExclamationTriangleIcon, // 警告图标
  ClockIcon,               // 待审核图标
  XCircleIcon,             // 驳回图标
} from '@heroicons/vue/24/outline';
import { ASSETS } from "../config/assets";

const props = defineProps<{
  article: Article;
}>();

const emit = defineEmits(['delete']);

// 1. 模态框控制
const deleteModal = ref<HTMLDialogElement | null>(null);
const isDeleting = ref(false);

const openDeleteModal = () => {
  deleteModal.value?.showModal();
};

const confirmDelete = async () => {
  isDeleting.value = true;
  // 这里不写 try-catch 的逻辑是：通常 emit 出去后，
  // 父组件处理完会移除当前组件，如果失败由父组件 alert。
  // 我们只负责让按钮转圈。
  emit('delete', props.article.slug);
};

// 2. 状态标签配置 (根据后端 auditStatus 字段)
const statusConfig = computed(() => {
  // 如果后端还没有返回 auditStatus，默认视为 'APPROVED' (或者不显示标签)
  // 您可以在 Article 接口中补充这个字段
  const status = props.article.auditStatus || 'APPROVED';

  switch (status) {
    case 'PENDING':
      return { text: '审核中', color: 'bg-orange-100 text-orange-600 border-orange-200', icon: ClockIcon };
    case 'REJECTED':
      return { text: '未过审', color: 'bg-red-100 text-red-600 border-red-200', icon: XCircleIcon };
    default: // APPROVED
      return { text: '', color: '', icon: null };
  }
});

// 3. 格式化时间
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
  <div class="flex flex-col sm:flex-row gap-4 p-4 border-b hover:bg-base-50 transition-colors group relative">

    <!-- 1. 封面图区域 (左侧) -->
    <div class="flex-shrink-0 relative w-full sm:w-40 h-24 rounded-md overflow-hidden bg-base-200">
      <router-link :to="`/article/${article.slug}`" class="block w-full h-full">
        <img
            :src="article.coverImageUrl || ASSETS.defaults.articleCoverD"
            class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
            :class="{ 'grayscale opacity-80': article.auditStatus === 'REJECTED' }"
         alt=""/>
      </router-link>

      <!-- 文章类型标签 -->
      <div class="absolute bottom-1 right-1 bg-black/60 text-white text-xs px-1 rounded z-10">
        文章
      </div>

      <!-- 审核状态标签 (左上角) -->
      <div v-if="statusConfig.text"
           class="absolute top-0 left-0 text-xs px-2 py-0.5 font-bold rounded-br-md shadow-sm flex items-center gap-1 z-10 border-b border-r"
           :class="statusConfig.color">
        <component :is="statusConfig.icon" class="w-3 h-3"/>
        {{ statusConfig.text }}
      </div>

      <!-- 驳回遮罩 (显示未过审大字) -->
      <div v-if="article.auditStatus === 'REJECTED'" class="absolute inset-0 flex items-center justify-center bg-black/40 pointer-events-none">
          <span class="text-white font-bold text-sm border-2 border-white px-2 py-0.5 rounded -rotate-12 opacity-90">
            退回
          </span>
      </div>
    </div>

    <!-- 2. 内容信息 (中间) -->
    <div class="flex-grow flex flex-col justify-between py-1 min-w-0">
      <div>
        <router-link :to="`/article/${article.slug}`" class="text-base font-bold text-base-content hover:text-pink-500 line-clamp-1 block" :title="article.title">
          {{ article.title }}
        </router-link>
        <div class="text-xs text-base-content/50 mt-1 flex items-center gap-2">
          {{ formattedDate }}
        </div>
      </div>

      <!-- 数据统计行 -->
      <div class="flex items-center gap-6 text-xs text-base-content/60 mt-2 sm:mt-0">
        <div class="flex items-center gap-1" title="点赞/收藏">
          <StarIcon class="w-3.5 h-3.5" />
          <span>{{ article.favoritesCount || 0 }}</span>
        </div>
        <div class="flex items-center gap-1" title="评论">
          <ChatBubbleBottomCenterTextIcon class="w-3.5 h-3.5" />
          <span>0</span> <!-- 若有 commentCount 请替换 -->
        </div>
      </div>
    </div>

    <!-- 3. 操作按钮 (右侧) -->
    <div class="flex flex-row sm:flex-col items-center sm:items-end justify-center sm:justify-center gap-2 mt-2 sm:mt-0 sm:pl-4 sm:border-l">
      <!-- 无论是否过审，都允许编辑 -->
      <router-link :to="`/editor/${article.slug}`" class="btn btn-sm btn-ghost border-base-300 hover:border-pink-500 hover:text-pink-500 font-normal w-full sm:w-auto">
        <PencilSquareIcon class="w-4 h-4" />
        编辑
      </router-link>

      <!-- 更多下拉菜单 -->
      <div class="dropdown dropdown-end dropdown-hover sm:dropdown-hover">
        <button tabindex="0" class="btn btn-sm btn-ghost btn-square">
          <span class="text-xl pb-3 leading-none">...</span>
        </button>
        <ul tabindex="0" class="dropdown-content z-[10] menu p-2 shadow bg-base-100 rounded-box w-32 border">
          <!-- 关键修改：点击触发模态框，而不是直接 confirm -->
          <li><a @click="openDeleteModal" class="text-error hover:bg-error/10"><TrashIcon class="w-4 h-4"/> 删除</a></li>
        </ul>
      </div>
    </div>

    <!-- ================= 4. 删除确认模态框 ================= -->
    <dialog ref="deleteModal" class="modal">
      <div class="modal-box">
        <div class="flex items-center gap-3 text-error mb-4">
          <ExclamationTriangleIcon class="w-8 h-8" />
          <h3 class="font-bold text-lg">确认删除稿件？</h3>
        </div>
        <p class="py-4 text-base">
          您确定要删除文章 <span class="font-bold text-base-content">{{ article.title }}</span> 吗？
        </p>
        <div class="alert alert-warning text-xs py-2 mb-4 rounded-md">
          <ExclamationTriangleIcon class="w-4 h-4"/>
          <span>此操作不可撤销，删除后该内容及其评论将永久消失。</span>
        </div>

        <div class="modal-action">
          <!-- 取消按钮：method="dialog" 自动关闭模态框 -->
          <form method="dialog">
            <button class="btn" :disabled="isDeleting">取消</button>
          </form>

          <!-- 确认按钮 -->
          <button @click="confirmDelete" class="btn btn-error text-white" :disabled="isDeleting">
            <span v-if="isDeleting" class="loading loading-spinner"></span>
            <span v-else>确认删除</span>
          </button>
        </div>
      </div>
      <!-- 点击遮罩关闭 -->
      <form method="dialog" class="modal-backdrop">
        <button>close</button>
      </form>
    </dialog>

  </div>
</template>