<!-- src/App.vue -->
<script setup lang="ts">
import {onMounted, onUnmounted, ref} from 'vue';
import { useAuthStore } from './stores/auth';
import { DocumentPlusIcon, Squares2X2Icon, VideoCameraIcon } from '@heroicons/vue/24/outline';
import {
  MagnifyingGlassIcon,
  BellIcon,
  StarIcon,
  ClockIcon,
  RectangleStackIcon,
  ArrowUpOnSquareIcon,
  UserCircleIcon,
  Cog6ToothIcon,
  ArrowLeftStartOnRectangleIcon,
  PuzzlePieceIcon,
} from '@heroicons/vue/24/outline';
import {useNotificationStore} from "./stores/notification.ts";
import router from "./router";

const authStore = useAuthStore();
const notificationStore = useNotificationStore(); // 使用 store

let pollingTimer: number | null = null;

// 启动轮询
const startPolling = () => {
  // 先立即抓一次
  if (authStore.isAuthenticated) {
    notificationStore.fetchUnreadCount();
  }

  // 每 30 秒轮询一次 (根据需求调整时间)
  pollingTimer = setInterval(() => {
    if (authStore.isAuthenticated) {
      notificationStore.fetchUnreadCount();
    }
  }, 180000);
};
const globalSearchQuery = ref('');

// 导航栏搜索跳转
const handleGlobalSearch = () => {
  const q = globalSearchQuery.value.trim();
  if (q) {
    router.push({
      name: 'Search',
      query: { query: q } // 传参到 SearchPage
    });
    // 可选：搜索后清空输入框，也可以不清空，看个人喜好
    // globalSearchQuery.value = '';
  }
};

onUnmounted(() => {
  if (pollingTimer) clearInterval(pollingTimer);
});

onMounted(() => {
  authStore.checkAuth();
  startPolling();
});

const handleLogout = () => {
  authStore.logout();
};
</script>

<template>
  <div class="flex flex-col min-h-screen bg-base-200/50 font-sans">
    <header class="bg-base-100/80 backdrop-blur sticky top-0 z-50 border-b border-base-200">
      <div class="navbar container mx-auto py-1 min-h-fit">

        <div class="navbar-start">
          <router-link to="/" class="btn btn-ghost text-2xl font-black normal-case text-pink-500 hover:bg-transparent">
            RealWorld
          </router-link>
        </div>

        <div class="join w-full max-w-md relative">

          <!-- 输入框 -->
          <input
              type="text"
              v-model="globalSearchQuery"
              @keyup.enter="handleGlobalSearch"
              placeholder="搜索你感兴趣的内容..."
              class="input input-bordered input-sm join-item w-full bg-base-100 focus:bg-white transition-colors pl-4 pr-10"
              style="border-radius: 9999px 0 0 9999px;"
          />

          <!-- 搜索按钮 -->
          <button
              @click="handleGlobalSearch"
              class="btn btn-sm join-item bg-base-200 border-base-200 hover:bg-base-300"
              style="border-radius: 0 9999px 9999px 0;"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 opacity-70" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </button>
        </div>

        <div class="navbar-end">

          <div v-if="authStore.isAuthenticated && authStore.user" class="flex items-center gap-x-2">


            <router-link
                to="/notifications"
                class="btn btn-ghost btn-circle hidden sm:inline-flex"
                active-class="text-pink-500"
            >
              <div class="flex flex-col items-center justify-center">
                <div class="relative">
                  <BellIcon class="h-6 w-6" />
                  <!-- 使用 store 中的 unreadCount -->
                  <span
                      v-if="notificationStore.unreadCount > 0"
                      class="absolute top-0 right-0 block h-2.5 w-2.5 rounded-full bg-red-500 ring-2 ring-base-100 transform translate-x-1/4 -translate-y-1/4"
                  ></span>
                </div>
                <span class="text-xs mt-0.5">消息</span>
              </div>
            </router-link>


            <router-link
                to="/feed"
                class="btn btn-ghost btn-circle hidden sm:inline-flex"
                active-class="text-pink-500"
            >
            <div class="flex flex-col items-center justify-center">
              <RectangleStackIcon class="h-6 w-6" />
              <span class="text-xs mt-0.5">动态</span>
            </div>
            </router-link>


            <router-link :to="'/profile/' + authStore.user.username +'?tab=favorited'"
                class="btn btn-ghost btn-circle hidden sm:inline-flex"
            >
              <div class="flex flex-col items-center">
                <StarIcon class="h-6 w-6" />
                <span class="text-xs">收藏</span>
              </div>
            </router-link>


            <router-link to="/history" active-class="text-pink-500" class="btn btn-ghost btn-circle hidden sm:inline-flex">
              <div class="flex flex-col items-center">
                <ClockIcon class="h-6 w-6" />
                <span class="text-xs">历史</span>
              </div>
            </router-link>

            <router-link
                to="/market"
                active-class="text-pink-500"
                class="btn btn-ghost btn-circle hidden sm:inline-flex"
            >
              <div class="flex flex-col items-center">
                <PuzzlePieceIcon class="h-6 w-6" />
                <span class="text-xs">插件</span>
              </div>
            </router-link>

            <!-- 找到导航栏右侧区域，替换原来的投稿按钮代码 -->

            <div class="dropdown dropdown-hover dropdown-end">
              <!--
                1. 触发器改为 router-link
                - role="button": 让样式看起来像按钮
                - to="/editor": 点击直接跳转发布页
                - tabindex="0": 配合 dropdown 使用，保证可访问性
              -->
              <router-link
                  to="/editor"
                  class="btn bg-pink-500 hover:bg-pink-600 text-white border-none gap-2 px-6 m-1"
              >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5" />
                </svg>
                投稿
              </router-link>

              <!--
                2. 下拉菜单内容
                - 放在同一个 div 内，鼠标移下来时仍然处于 hover 状态，不会消失
              -->
              <ul tabindex="0" class="dropdown-content z-[1] menu p-2 shadow-lg bg-base-100 rounded-box w-52 border border-base-200">
                <!-- 专栏投稿 (虽然按钮本身也能去，但菜单里保留一个明确选项符合B站习惯) -->
                <li>
                  <router-link to="/editor" class="py-3 font-bold hover:text-pink-500">
                    <DocumentPlusIcon class="w-5 h-5"/>
                    专栏投稿
                  </router-link>
                </li>

                <!-- 视频投稿 (占位) -->
                <li>
                  <a class="py-3 text-base-content/50 cursor-not-allowed">
                    <VideoCameraIcon class="w-5 h-5"/>
                    视频投稿 (开发中)
                  </a>
                </li>

                <div class="divider my-0"></div>

                <!-- 投稿管理 -->
                <li>
                  <router-link to="/creator/content" class="py-3 hover:text-pink-500">
                    <Squares2X2Icon class="w-5 h-5"/>
                    投稿管理
                  </router-link>
                </li>
              </ul>
            </div>

            <div class="dropdown dropdown-end dropdown-hover">
              <router-link :to="'/profile/' + authStore.user.username" tabindex="0" role="button" class="btn btn-ghost btn-circle avatar">
                <div class="w-10 rounded-full">
                  <img :src="authStore.userImage " alt="User avatar" />
                </div>
              </router-link>
              <div tabindex="0" class="dropdown-content z-[1] p-4 shadow-xl bg-base-100 rounded-box w-72">
                <div class="text-center">
                  <h3 class="font-bold text-lg">{{ authStore.user.username }}</h3>
                </div>
                <div class="divider my-2"></div>
                <ul class="menu menu-sm">
                  <li>
                    <router-link :to="'/profile/' + authStore.user.username">
                      <UserCircleIcon class="w-5 h-5"/> 个人中心
                    </router-link>
                  </li>
                  <li>
                    <router-link to="/settings">
                      <Cog6ToothIcon class="w-5 h-5"/> 设置
                    </router-link>
                  </li>
                  <div class="divider my-1"></div>
                  <li>
                    <a @click.prevent="handleLogout">
                      <ArrowLeftStartOnRectangleIcon class="w-5 h-5"/> 退出登录
                    </a>
                  </li>
                </ul>
              </div>
            </div>
          </div>

          <div v-else class="flex items-center gap-x-3">
            <router-link to="/login" class="btn btn-ghost rounded-lg">登录</router-link>
            <router-link to="/register" class="btn bg-pink-500 hover:bg-pink-600 text-white rounded-lg">
              注册
            </router-link>
          </div>
        </div>

      </div>
    </header>

    <main class="flex-grow py-8">
      <router-view :key="$route.fullPath" />
    </main>

  </div>
</template>