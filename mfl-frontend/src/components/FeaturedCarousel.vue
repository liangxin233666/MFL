<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { ChevronLeftIcon, ChevronRightIcon } from '@heroicons/vue/24/solid';

// 1. 模拟轮播图数据
const slides = ref([
  {
    image: 'http://localhost:9000/realworld-media/assets/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-07-29%20232554.png',
    title: '常盘台夏日祭',
    buttonText: '立即前往',
  },
  {
    image: 'http://localhost:9000/realworld-media/assets/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE(126).png',
    title: 'power!',
    buttonText: '查看详情',
  },
  {
    image: 'http://localhost:9000/realworld-media/assets/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE(54).png',
    title: '动作游戏天尊',
    buttonText: '观看视频',
  },
]);

const activeIndex = ref(0);
let intervalId: number | null = null;

// 2. 切换逻辑
const next = () => {
  activeIndex.value = (activeIndex.value + 1) % slides.value.length;
};

const prev = () => {
  activeIndex.value = (activeIndex.value - 1 + slides.value.length) % slides.value.length;
};

const goToSlide = (index: number) => {
  activeIndex.value = index;
};

// 3. 自动播放
const startAutoPlay = () => {
  intervalId = window.setInterval(next, 3000);
};

const stopAutoPlay = () => {
  if (intervalId) {
    clearInterval(intervalId);
  }
};

onMounted(() => {
  startAutoPlay();
});

onUnmounted(() => {
  stopAutoPlay();
});
</script>

<template>
  <div
      @mouseenter="stopAutoPlay"
      @mouseleave="startAutoPlay"
      class="relative aspect-video w-full rounded-lg overflow-hidden shadow-md group"
  >
    <!-- Slides Container -->
    <div class="w-full h-full">
      <div
          v-for="(slide, index) in slides"
          :key="index"
          class="absolute w-full h-full transition-opacity duration-500 ease-in-out"
          :class="index === activeIndex ? 'opacity-100' : 'opacity-0'"
      >
        <img :src="slide.image" :alt="slide.title" class="w-full h-full object-cover">
        <!-- 遮罩和文字 -->
        <div class="absolute bottom-0 left-0 w-full h-2/3 bg-gradient-to-t from-black/70 to-transparent"></div>
        <div class="absolute bottom-5 left-5 text-white">
          <h2 class="text-xl font-bold">{{ slide.title }}</h2>
        </div>
        <button class="absolute bottom-4 right-4 btn btn-sm bg-pink-500 hover:bg-pink-600 border-none text-white">
          {{ slide.buttonText }}
        </button>
      </div>
    </div>

    <!-- Controls: Arrows -->
    <button @click="prev" class="absolute top-1/2 left-2 -translate-y-1/2 btn btn-circle btn-sm opacity-0 group-hover:opacity-100 transition-opacity">
      <ChevronLeftIcon class="w-5 h-5" />
    </button>
    <button @click="next" class="absolute top-1/2 right-2 -translate-y-1/2 btn btn-circle btn-sm opacity-0 group-hover:opacity-100 transition-opacity">
      <ChevronRightIcon class="w-5 h-5" />
    </button>

    <!-- Indicators: Dots -->
    <div class="absolute bottom-3 left-1/2 -translate-x-1/2 flex space-x-2">
      <button
          v-for="(_, index) in slides"
          :key="index"
          @click="goToSlide(index)"
          class="w-2 h-2 rounded-full transition-colors"
          :class="index === activeIndex ? 'bg-white' : 'bg-white/50 hover:bg-white/75'"
      ></button>
    </div>
  </div>
</template>