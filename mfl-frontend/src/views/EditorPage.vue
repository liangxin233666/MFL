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
  coverImageUrl: null as string | null, // 明确类型为 string 或 null
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
    // 关键修复：确保将可能的 undefined 转换为 null
    form.coverImageUrl = article.coverImageUrl || null;
  } catch (error) {
    await router.push('/');
  }
};

const compiledMarkdown = computed(() => {
  return form.body ? marked(form.body) : '<p class="text-base-content/50">实时预览</p>';
});

// 处理封面上传
const handleCoverUpload = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  if (!input.files?.length) return;
  const tempUrl = await uploadFile(input.files[0]);
  if (tempUrl) {
    form.coverImageUrl = tempUrl;
  }
};

// 在正文中插入图片
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
    form.body = form.body.substring(0, start) + markdownImage + form.body.substring(start);
  }
};

const handleSubmit = async () => {
  isSubmitting.value = true;
  const articleData = {
    article: {
      title: form.title,
      description: form.description,
      body: form.body,
      tagList: form.tagList.split(',').map(tag => tag.trim()).filter(Boolean),
      coverImageUrl: form.coverImageUrl, // 直接使用，因为它已经是 string | null
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
  <form @submit.prevent="handleSubmit" class="space-y-6">
    <div class="card bg-base-100 shadow-md">
      <div class="card-body">

        <!-- 封面上传 -->
        <div class="form-control">
          <label for="cover-upload" class="cursor-pointer border-2 border-dashed rounded-lg p-4 text-center hover:border-pink-500 transition-colors" :class="{'border-pink-500': form.coverImageUrl}">
            <div v-if="form.coverImageUrl" class="relative group">
              <img :src="form.coverImageUrl" class="w-full max-h-60 object-cover rounded-md"/>
              <div class="absolute inset-0 bg-black/60 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity rounded-md"><span class="text-white font-bold">更换封面</span></div>
            </div>
            <div v-else class="flex flex-col items-center gap-2 text-base-content/60"><PhotoIcon class="w-12 h-12"/><span>添加文章封面</span></div>
          </label>
          <input id="cover-upload" type="file" @change="handleCoverUpload" class="hidden" accept="image/*"/>
        </div>

        <!-- 标题, 描述, 标签 -->
        <input type="text" v-model="form.title" placeholder="文章标题..." class="input input-ghost text-3xl font-bold p-2 h-auto focus:bg-transparent mt-4" required/>
        <input type="text" v-model="form.description" placeholder="文章简短描述..." class="input input-bordered w-full focus:border-pink-500" required/>
        <label class="input input-bordered flex items-center gap-2 h-10 mt-2">
          <TagIcon class="w-5 h-5 text-base-content/50" />
          <input type="text" v-model="form.tagList" class="grow" placeholder="添加标签，用英文逗号(,)分隔" />
        </label>
      </div>
    </div>

    <!-- 编辑器 -->
    <div class="card bg-base-100 shadow-md">
      <div class="p-2 border-b flex items-center gap-2">
        <button type="button" @click="triggerBodyImageUpload" :disabled="isUploading" class="btn btn-sm btn-ghost gap-2">
          <span v-if="isUploading" class="loading loading-spinner loading-xs"></span>
          <ArrowUpOnSquareIcon v-else class="w-5 h-5"/>
          插入图片
        </button>
        <input type="file" ref="imageUploaderInput" @change="handleBodyImageInsert" class="hidden" accept="image/*" />
      </div>
      <div class="card-body pt-0">
        <textarea ref="markdownEditor" v-model="form.body" class="textarea textarea-ghost w-full min-h-[40vh] text-base leading-relaxed resize-none focus:bg-transparent" placeholder="开始创作吧！支持 Markdown..." required></textarea>
      </div>
    </div>

    <!-- 发布按钮 -->
    <div class="flex justify-end">
      <button type="submit" class="btn bg-pink-500 hover:bg-pink-600 text-white text-base h-12 px-6" :disabled="isSubmitting">
        <span v-if="isSubmitting" class="loading loading-spinner"></span>
        <DocumentPlusIcon v-else class="w-5 h-5"/>
        {{ isSubmitting ? '发布中...' : (isEditMode ? '更新文章' : '发布文章') }}
      </button>
    </div>
  </form>
</template>