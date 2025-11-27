<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { DocumentPlusIcon, TagIcon, PhotoIcon, ArrowUpOnSquareIcon } from '@heroicons/vue/24/solid';
import { marked } from 'marked';
import apiClient from '../api/apiClient';
import type { Article } from '../types/api';
import { useUploader } from '../composables/useUploader';

const route = useRoute();
const router = useRouter();
const { isUploading, uploadFile } = useUploader();

const markdownEditor = ref<HTMLTextAreaElement | null>(null);
const imageUploaderInput = ref<HTMLInputElement | null>(null);

const isEditMode = ref(false);
const articleSlug = ref<string | null>(null);
const isSubmitting = ref(false);


const form = reactive({
  title: '',
  description: '',
  body: '',
  tagList: '',
  coverImageUrl: null as string | null,
});

onMounted(async () => {
  if (route.params.slug) {
    isEditMode.value = true;
    articleSlug.value = route.params.slug as string;
    await fetchArticleData(articleSlug.value);
  }
});

const fetchArticleData = async (slug: string) => {
  try {
    const response = await apiClient.get<{ article: Article }>(`/articles/${slug}`);
    const { article } = response.data;
    form.title = article.title;
    form.description = article.description;
    form.body = article.body || '';
    form.tagList = article.tagList?.join(', ') || '';
    form.coverImageUrl = article.coverImageUrl || null;
  } catch (error) {
    await router.push('/');
  }
};

const compiledMarkdown = computed(() => {
  return form.body ? marked(form.body) : '<p class="text-gray-400 italic text-center mt-10">右侧预览区域</p>';
});

const handleCoverUpload = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  if (!input.files?.length) return;
  const tempUrl = await uploadFile(input.files[0]);
  if (tempUrl) {
    form.coverImageUrl = tempUrl;
  }
};

const triggerBodyImageUpload = () => {
  imageUploaderInput.value?.click();
};

const handleBodyImageInsert = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  if (!input.files?.length) return;
  const file = input.files[0];
  const tempUrl = await uploadFile(file);

  if (tempUrl && markdownEditor.value) {
    const editor = markdownEditor.value;
    const markdownImage = `\n![${file.name}](${tempUrl})\n`;
    const start = editor.selectionStart;
    const end = editor.selectionEnd;
    form.body = form.body.substring(0, start) + markdownImage + form.body.substring(end);

    // 重新聚焦
    setTimeout(() => {
      editor.focus();
      editor.setSelectionRange(start + markdownImage.length, start + markdownImage.length);
    }, 50);
  }
};

const handleSubmit = async () => {
  isSubmitting.value = true;
  const articleData = {
    article: {
      title: form.title,
      description: form.description,
      body: form.body,
      tagList: form.tagList.split(';').map(tag => tag.trim()).filter(Boolean),
      coverImageUrl: form.coverImageUrl,
    }
  };

  try {
    const response = isEditMode.value && articleSlug.value
        ? await apiClient.put<{ article: Article }>(`/articles/${articleSlug.value}`, articleData)
        : await apiClient.post<{ article: Article }>('/articles', articleData);
    await router.push(`/article/${response.data.article.slug}`);
  } catch (error: any) {
    alert(`发布失败: ${JSON.stringify(error.response?.data?.errors)}`);
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<template>
  <form @submit.prevent="handleSubmit" class="space-y-6 max-w-6xl mx-auto pb-10">

    <!-- ================= 1. 顶部信息与封面 ================= -->
    <div class="card bg-base-100 shadow-md">
      <div class="card-body">

        <!-- 封面上传：调整了高度 h-52 (约200px)，并限制了内部图片尺寸 -->
        <div class="form-control w-full mb-4">
          <div class="relative w-full h-52 bg-base-50 rounded-xl overflow-hidden group hover:bg-base-100 transition-colors border border-base-200">

            <!-- 左上角装饰框 -->
            <div class="absolute top-0 left-0 w-12 h-12 border-t-4 border-l-4 border-pink-400 border-dashed rounded-tl-xl pointer-events-none z-10"></div>
            <!-- 右下角装饰框 -->
            <div class="absolute bottom-0 right-0 w-12 h-12 border-b-4 border-r-4 border-pink-400 border-dashed rounded-br-xl pointer-events-none z-10"></div>

            <label for="cover-upload" class="cursor-pointer w-full h-full flex items-center justify-center z-20 relative">

              <div v-if="form.coverImageUrl" class="h-full w-full p-2 flex items-center justify-center">
                <!-- 关键修改：h-full w-auto mx-auto -> 强制图片高度撑满容器，宽度自适应，居中 -->
                <img :src="form.coverImageUrl" class="h-full w-auto mx-auto object-contain shadow-sm rounded" />

                <div class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                  <span class="text-white text-sm font-bold flex items-center gap-2 bg-black/50 px-3 py-1 rounded-full backdrop-blur-sm">
                    <PhotoIcon class="w-4 h-4"/> 更换封面
                  </span>
                </div>
              </div>

              <div v-else class="flex flex-col items-center gap-2 text-base-content/50 group-hover:text-pink-500 transition-colors">
                <PhotoIcon class="w-10 h-10 opacity-70"/>
                <span class="text-sm font-medium">点击设置封面</span>
              </div>
            </label>
            <input id="cover-upload" type="file" @change="handleCoverUpload" class="hidden" accept="image/*"/>
          </div>
        </div>

        <!-- 标题输入 -->
        <div class="space-y-3">
          <input type="text" v-model="form.title" placeholder="文章标题" class="input input-ghost text-2xl font-bold w-full focus:bg-transparent px-0" required/>
          <input type="text" v-model="form.description" placeholder="简短描述..." class="input input-sm input-bordered w-full focus:border-pink-500" required/>
          <div class="flex items-center gap-2">
            <TagIcon class="w-4 h-4 text-base-content/50" />
            <input type="text" v-model="form.tagList" class="input input-sm input-ghost grow focus:bg-transparent px-1" placeholder="标签 (用;分隔)" />
          </div>
        </div>

      </div>
    </div>

    <!-- ================= 2. 双栏编辑器核心区域 ================= -->
    <div class="card bg-base-100 shadow-md overflow-hidden">
      <!-- 顶部工具栏 -->
      <div class="bg-base-50 px-4 py-2 border-b flex items-center justify-between">
        <div class="text-sm font-bold text-base-content/70">正文内容</div>
        <div>
          <button type="button" @click="triggerBodyImageUpload" :disabled="isUploading" class="btn btn-xs sm:btn-sm btn-ghost gap-1 border border-base-300 bg-white">
            <span v-if="isUploading" class="loading loading-spinner loading-xs"></span>
            <ArrowUpOnSquareIcon v-else class="w-4 h-4"/>
            插入图片
          </button>
          <input type="file" ref="imageUploaderInput" @change="handleBodyImageInsert" class="hidden" accept="image/*" />
        </div>
      </div>

      <!-- 双栏布局 Grid -->
      <div class="grid grid-cols-1 lg:grid-cols-2 h-[600px] divide-y lg:divide-y-0 lg:divide-x divide-base-200">

        <!-- 左侧：编辑区 -->
        <div class="flex flex-col h-full">
          <textarea
              ref="markdownEditor"
              v-model="form.body"
              class="textarea textarea-ghost w-full h-full resize-none focus:outline-none focus:ring-0 p-4 text-base font-mono leading-relaxed overflow-y-auto"
              placeholder="在此输入 Markdown 内容..."
              required
          ></textarea>
        </div>

        <!-- 右侧：预览区 -->
        <div class="h-full bg-base-50/30 overflow-y-auto custom-scroll">
          <!--
             关键类名: article-preview
             我们在 style 中专门针对它里面的 img 做了限制
          -->
          <div
              class="prose prose-sm max-w-none p-6 article-preview"
              v-html="compiledMarkdown"
          ></div>
        </div>

      </div>
    </div>

    <!-- 底部发布栏 -->
    <div class="flex justify-end pt-4">
      <button type="submit" class="btn bg-pink-500 hover:bg-pink-600 text-white w-32" :disabled="isSubmitting">
        <span v-if="isSubmitting" class="loading loading-spinner loading-xs"></span>
        {{ isSubmitting ? '发布中...' : (isEditMode ? '更新' : '发布') }}
      </button>
    </div>
  </form>
</template>

<!--
  添加 scoped 样式
  专门控制预览区域的图片大小
-->
<style>
/* 注意：这里不要用 scoped，因为 v-html 渲染的内容不受 scoped CSS 影响，
   或者使用 :deep(.article-preview img) */

.article-preview img {
  max-height: 350px; /* 限制正文图片最大高度，不再铺满全屏 */
  width: auto;       /* 宽度自动，保持比例 */
  margin: 1rem auto; /* 居中显示 */
  border-radius: 0.5rem;
  box-shadow: 0 1px 3px 0 rgb(0 0 0 / 0.1);
  display: block;
}

/* 简单的滚动条美化（可选） */
.custom-scroll::-webkit-scrollbar {
  width: 8px;
}
.custom-scroll::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scroll::-webkit-scrollbar-thumb {
  background-color: #cbd5e1;
  border-radius: 4px;
}
</style>