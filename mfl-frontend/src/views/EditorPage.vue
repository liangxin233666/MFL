<script setup lang="ts">
import { ref } from 'vue';
import { DocumentPlusIcon, TagIcon } from '@heroicons/vue/24/solid';

const title = ref('');
const content = ref('');
const tags = ref('');

// 这是一个简化的预览，需要一个 markdown-it 这样的库
const compiledMarkdown = (rawMarkdown: string) => {
  return rawMarkdown
      .replace(/^# (.*$)/g, '<h1 class="text-3xl font-bold">$1</h1>')
      .replace(/^## (.*$)/g, '<h2 class="text-2xl font-bold mt-4">$1</h2>')
      .replace(/\*\*(.*)\*\*/g, '<strong>$1</strong>')
      .replace(/\n/g, '<br />');
};
</script>

<template>
  <div class="space-y-6">
    <div class="card bg-base-100 shadow-md">
      <div class="card-body">
        <div class="form-control">
          <input
              type="text"
              v-model="title"
              placeholder="在这里输入你的文章标题..."
              class="input input-ghost text-3xl font-bold p-2 h-auto focus:bg-transparent"
          />
        </div>
        <div class="form-control">
          <label class="input input-bordered flex items-center gap-2 h-10 mt-2">
            <TagIcon class="w-5 h-5 text-base-content/50" />
            <input type="text" v-model="tags" class="grow" placeholder="添加标签，用逗号分隔" />
          </label>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <div class="card bg-base-100 shadow-md">
        <div class="card-body">
          <textarea
              v-model="content"
              class="textarea textarea-ghost w-full h-96 text-base leading-relaxed resize-none focus:bg-transparent"
              placeholder="开始你的创作吧！支持 Markdown 语法..."
          ></textarea>
        </div>
      </div>
      <div class="card bg-base-100 shadow-md hidden md:block">
        <div class="card-body">
          <article class="prose max-w-none" v-html="compiledMarkdown(content) || '<p class=\'text-base-content/50\'>实时预览</p>'"></article>
        </div>
      </div>
    </div>

    <div class="flex justify-end">
      <button class="btn bg-pink-500 hover:bg-pink-600 text-white text-base h-12 px-6">
        <DocumentPlusIcon class="w-5 h-5"/>
        发布文章
      </button>
    </div>
  </div>
</template>