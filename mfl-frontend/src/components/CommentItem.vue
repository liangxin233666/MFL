<script setup lang="ts">
import { ref } from 'vue';
import type { Comment } from '../types/api';
import { useAuthStore } from '../stores/auth';
import apiClient from '../api/apiClient';
import UserInfo from './UserInfo.vue';

const props = defineProps<{
  comment: Comment;
  articleSlug: string;
}>();

const emit = defineEmits(['commentPosted']);

const authStore = useAuthStore();
const showReplyBox = ref(false);
const replyBody = ref('');
const isSubmittingReply = ref(false);

const toggleReplyBox = () => {
  if (!authStore.isAuthenticated) return alert('请先登录才能回复哦~');
  showReplyBox.value = !showReplyBox.value;
};

const postReply = async () => {
  if (!replyBody.value.trim()) return;
  isSubmittingReply.value = true;
  try {
    await apiClient.post(`/articles/${props.articleSlug}/comments/${props.comment.id}`, { comment: { body: replyBody.value } });
    replyBody.value = '';
    showReplyBox.value = false;
    emit('commentPosted'); // 通知父级刷新列表
  } catch (error) {
    alert('回复失败');
  } finally {
    isSubmittingReply.value = false;
  }
};
</script>

<template>
  <div class="flex items-start gap-3 py-2">
    <!-- 使用 UserInfo 组件展示头像 -->
    <UserInfo :profile="comment.author" size="sm" :show-name="false" />

    <div class="flex-grow">
      <!-- 使用 UserInfo 组件展示名字和时间 -->
      <UserInfo :profile="comment.author" :show-avatar="false" :timestamp="comment.createdAt"/>

      <!-- 评论内容 -->
      <p class="mt-1 text-base whitespace-pre-wrap">{{ comment.body }}</p>

      <!-- 操作栏：回复按钮 -->
      <div class="mt-1">
        <button @click="toggleReplyBox" class="text-sm text-base-content/60 hover:text-pink-500 font-bold">回复</button>
      </div>

      <!-- 回复输入框 (动态显示) -->
      <div v-if="showReplyBox" class="mt-3 flex items-start gap-2">
        <div class="avatar">
          <div class="w-8 h-8 rounded-full"><img :src="authStore.userImage" /></div>
        </div>
        <div class="flex-grow flex gap-2">
          <input type="text" v-model="replyBody" placeholder="留下你的回复..." class="input input-bordered input-sm w-full" @keyup.enter="postReply" />
          <button @click="postReply" class="btn btn-sm bg-pink-500 text-white" :disabled="isSubmittingReply || !replyBody.trim()">
            <span v-if="isSubmittingReply" class="loading loading-spinner loading-xs"></span>
            回复
          </button>
        </div>
      </div>

      <!-- **递归渲染区** -->
      <div v-if="comment.replies && comment.replies.length > 0" class="mt-2 border-l-2 pl-3 border-base-200">
        <CommentItem
            v-for="reply in comment.replies"
            :key="reply.id"
            :comment="reply"
            :article-slug="articleSlug"
            @comment-posted="$emit('commentPosted')"
        />
      </div>
    </div>
  </div>
</template>