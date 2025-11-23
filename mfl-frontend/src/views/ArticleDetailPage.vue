<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRoute } from 'vue-router';
import apiClient from '../api/apiClient';
import type { Article, Comment } from '../types/api';
import { marked } from 'marked';
import { useAuthStore } from '../stores/auth';

// 导入所有需要的组件和图标
import UserInfo from '../components/UserInfo.vue';
import CommentItem from '../components/CommentItem.vue';
import { ShareIcon, StarIcon, ChatBubbleBottomCenterTextIcon, PlusIcon, CheckIcon } from '@heroicons/vue/24/solid';

// --- 核心状态 ---
const route = useRoute();
const authStore = useAuthStore();
const article = ref<Article | null>(null);
const isLoading = ref(true);

// --- 评论区专用状态 ---
const comments = ref<Comment[]>([]);
const isCommentsLoading = ref(false);
const commentsCurrentPage = ref(0);
const hasMoreComments = ref(true);
const commentsPerPage = 5;

// --- 发布顶层评论状态 ---
const newTopLevelComment = ref('');
const isSubmittingTopComment = ref(false);

// --- 点赞/关注状态 ---
const isFavoriting = ref(false);
const isFollowing = ref(false);


// --- API 调用与逻辑 ---
const fetchComments = async (page = 0) => {
  if (isCommentsLoading.value) return;
  isCommentsLoading.value = true;
  try {
    const slug = route.params.slug as string;
    const response = await apiClient.get<{ comments: Comment[] }>(`/articles/${slug}/comments?limit=${commentsPerPage}&offset=${page * commentsPerPage}`);
    if (page === 0) comments.value = response.data.comments;
    else comments.value.push(...response.data.comments);
    hasMoreComments.value = response.data.comments.length === commentsPerPage;
    commentsCurrentPage.value = page;
  } finally {
    isCommentsLoading.value = false;
  }
};

const reloadAllComments = () => {
  hasMoreComments.value = true;
  fetchComments(0);
};

const postTopLevelComment = async () => {
  if (!newTopLevelComment.value.trim() || !article.value) return;
  isSubmittingTopComment.value = true;
  try {
    await apiClient.post(`/articles/${article.value.slug}/comments`, { comment: { body: newTopLevelComment.value } });
    newTopLevelComment.value = '';
    reloadAllComments();
  } catch (error) { alert('评论发布失败'); }
  finally { isSubmittingTopComment.value = false; }
};


onMounted(async () => {
  const slug = route.params.slug as string;
  isLoading.value = true;
  try {
    const articleRes = await apiClient.get<{ article: Article }>(`/articles/${slug}`);
    article.value = articleRes.data.article;
    await fetchComments(0);
  } catch (error) {
    console.error("获取文章详情失败:", error);
  } finally {
    isLoading.value = false;
  }
});


const toggleFavorite = async () => {
  if (!authStore.isAuthenticated || !article.value) return alert('请先登录');
  isFavoriting.value = true;
  const original = { fav: article.value.favorited, count: article.value.favoritesCount || 0 };
  article.value.favorited = !original.fav;
  article.value.favoritesCount = original.fav ? original.count - 1 : original.count + 1;
  try {
    original.fav ? await apiClient.delete(`/articles/${article.value.slug}/favorite`) : await apiClient.post(`/articles/${article.value.slug}/favorite`, {});
  } catch (error) {
    article.value.favorited = original.fav;
    article.value.favoritesCount = original.count;
    alert('操作失败');
  } finally { isFavoriting.value = false; }
};

const toggleFollow = async () => {
  if (!authStore.isAuthenticated || !article.value) return alert('请先登录');
  isFollowing.value = true;
  const originalFollowing = article.value.author.following;
  article.value.author.following = !originalFollowing;
  try {
    const { username } = article.value.author;
    originalFollowing ? await apiClient.delete(`/profiles/${username}/follow`) : await apiClient.post(`/profiles/${username}/follow`, {});
  } catch (error) {
    article.value.author.following = originalFollowing;
    alert('操作失败');
  } finally { isFollowing.value = false; }
};

const renderedBody = computed(() => article.value?.body ? marked.parse(article.value.body) : '');
</script>

<template>
  <div v-if="isLoading" class="text-center py-20"><span class="loading loading-spinner loading-lg"></span></div>
  <div v-else-if="article" class="grid grid-cols-1 lg:grid-cols-3 gap-8">
    <!-- 左侧主内容区 -->
    <div class="lg:col-span-2">
      <div class="card bg-base-100 shadow-md">
        <div class="card-body">
          <h1 class="text-3xl font-bold mb-2">{{ article.title }}</h1>
          <div class="flex items-center gap-4 text-sm text-base-content/70 mb-4">
            <UserInfo :profile="article.author" size="xs" />
            <span class="text-xs text-base-content/50">发布于 {{ new Date(article.createdAt).toLocaleDateString() }}</span>
          </div>
          <div class="divider"></div>

          <!-- 修改点：添加了 class="article-content" 以便 CSS 控制内部图片 -->
          <article class="prose max-w-none lg:prose-lg article-content" v-html="renderedBody"></article>

          <div class="card-actions justify-end mt-8">
            <button @click="toggleFavorite" :disabled="isFavoriting" class="btn gap-2" :class="{ 'btn-ghost': !article.favorited, 'bg-pink-100 text-pink-500': article.favorited }">
              <span v-if="isFavoriting" class="loading loading-spinner loading-xs"></span>
              <StarIcon class="w-5 h-5"/> {{ article.favorited ? '已点赞' : '点赞' }} ({{ article.favoritesCount || 0 }})
            </button>
            <button class="btn btn-ghost"><ShareIcon class="w-5 h-5"/> 分享</button>
          </div>
        </div>
      </div>

      <!-- 全新的评论区 -->
      <div id="comment-section" class="card bg-base-100 shadow-md mt-8">
        <div class="card-body">
          <h3 class="text-xl font-bold mb-4 flex items-center gap-2"><ChatBubbleBottomCenterTextIcon class="w-6 h-6 text-pink-500"/> <span>评论区</span></h3>
          <div v-if="authStore.isAuthenticated" class="flex items-start gap-4">
            <div class="avatar"><div class="w-12 h-12 rounded-full"><img :src="authStore.userImage"/></div></div>
            <textarea v-model="newTopLevelComment" class="textarea textarea-bordered flex-grow" placeholder="留下你的精彩评论吧！" rows="2"></textarea>
            <button @click="postTopLevelComment" class="btn bg-pink-500 text-white" :disabled="isSubmittingTopComment || !newTopLevelComment.trim()">
              <span v-if="isSubmittingTopComment" class="loading loading-spinner loading-xs"></span>发布
            </button>
          </div>
          <div v-else class="text-center p-4 bg-base-200 rounded-lg"><p>你需要 <router-link to="/login" class="text-pink-500 font-bold">登录</router-link> 才能发表评论哦~</p></div>
          <div class="divider my-4"></div>
          <div v-if="comments.length > 0" class="space-y-2">
            <CommentItem v-for="comment in comments" :key="comment.id" :comment="comment" :article-slug="article.slug" @comment-posted="reloadAllComments" />
          </div>
          <div v-else-if="!isCommentsLoading" class="text-center text-base-content/50 py-6">还没有评论，快来抢沙发吧！</div>
          <div class="text-center mt-4" v-if="hasMoreComments">
            <button @click="fetchComments(commentsCurrentPage + 1)" class="btn btn-ghost" :disabled="isCommentsLoading">
              <span v-if="isCommentsLoading" class="loading loading-spinner"></span>加载更多评论
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧作者区 -->
    <aside class="space-y-8">
      <div class="card bg-base-100 shadow-md">
        <div class="card-body items-center text-center">
          <UserInfo :profile="article.author" size="lg" />
          <p class="text-sm text-base-content/70 -mt-2">{{ article.author.bio || '这位用户很神秘，什么也没留下...' }}</p>
          <div class="card-actions justify-end mt-2 w-full">
            <button @click="toggleFollow" :disabled="isFollowing" class="btn btn-sm w-full gap-1" :class="{ 'bg-pink-500 text-white': !article.author.following, 'btn-outline': article.author.following }">
              <span v-if="isFollowing" class="loading loading-spinner loading-xs"></span>
              <CheckIcon v-if="!isFollowing && article.author.following" class="w-4 h-4" />
              <PlusIcon v-if="!isFollowing && !article.author.following" class="w-4 h-4"/>
              {{ isFollowing ? '处理中...' : (article.author.following ? '已关注' : '关注') }}
            </button>
          </div>
        </div>
      </div>
    </aside>
  </div>
  <div v-else class="text-center py-20 text-xl font-bold">文章不存在或加载失败</div>
</template>

<style scoped>
/*
   核心修改：使用 :deep() 穿透控制 v-html 里的图片
   限制最大高度为 550px，防止图片太占位置
*/
:deep(.article-content img) {
  max-height: 550px;   /* 限制高度，不再铺满全屏 */
  width: auto;         /* 宽度自适应 */
  max-width: 100%;     /* 手机端不撑破屏幕 */
  margin: 1.5rem auto; /* 居中显示 */
  display: block;      /* 块级显示 */
  border-radius: 0.5rem;
}
</style>