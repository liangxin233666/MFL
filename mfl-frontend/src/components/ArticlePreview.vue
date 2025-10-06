
<script setup lang="ts">

import { EyeIcon, ChatBubbleOvalLeftEllipsisIcon } from '@heroicons/vue/24/solid';

const props = defineProps({
  imageUrl: { type: String, required: true },
  author: { type: String, required: true },
  avatar: { type: String, required: true },
  title: { type: String, required: true },
  viewsCount: { type: Number, required: true },
  commentsCount: { type: Number, default: 0 },
});


const formatCount = (count: number) => {
  if (count >= 10000) {
    return (count / 10000).toFixed(1) + '万';
  }
  return count.toString();
};
</script>

<template>
  <div class="card bg-base-100 shadow-md transition-all duration-300 hover:shadow-xl hover:-translate-y-1 group rounded-lg overflow-hidden">
    <figure class="relative">
      <img :src="imageUrl" :alt="title" class="aspect-video w-full object-cover transition-transform duration-300 group-hover:scale-110" />
      <div class="absolute bottom-0 left-0 w-full h-1/2 bg-gradient-to-t from-black/60 to-transparent"></div>
      <div class="absolute bottom-2 left-3 right-3 flex justify-between items-center text-white text-xs font-bold">
        <div class="flex items-center gap-3">
          <div class="flex items-center gap-1">
            <EyeIcon class="w-4 h-4" />
            <span>{{ formatCount(props.viewsCount) }}</span>
          </div>
          <div class="flex items-center gap-1">
            <ChatBubbleOvalLeftEllipsisIcon class="w-4 h-4" />
            <span>{{ formatCount(props.commentsCount) }}</span>
          </div>
        </div>
      </div>
    </figure>

    <div class="p-3 flex items-start gap-3">
      <div class="avatar mt-0.5">
        <div class="w-9 h-9 rounded-full">
          <img :src="avatar" :alt="author" />
        </div>
      </div>
      <div class="flex flex-col">
        <a class="font-medium text-sm leading-tight hover:text-pink-500 transition-colors two-line-clamp" :title="title">
          {{ title }}
        </a>
        <span class="text-xs text-base-content/70 mt-1">{{ author }}</span>
      </div>
    </div>
  </div>
</template>

<style>
.two-line-clamp {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  overflow: hidden;
}
</style>