<!-- src/App.vue -->
<script setup lang="ts">
import { onMounted } from 'vue';
import { useAuthStore } from './stores/auth';

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
} from '@heroicons/vue/24/outline';

const authStore = useAuthStore();

onMounted(() => {
  authStore.checkAuth();
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

        <div class="navbar-center hidden lg:flex">
          <div class="relative w-96">
            <input type="text" placeholder="搜点好东西..." class="input input-bordered h-9 w-full rounded-lg bg-base-200 focus:bg-base-100 focus:border-pink-500 transition-all" />
            <MagnifyingGlassIcon class="absolute top-1/2 right-3 h-5 w-5 -translate-y-1/2 text-base-content/50" />
          </div>
        </div>

        <div class="navbar-end">

          <div v-if="authStore.isAuthenticated && authStore.user" class="flex items-center gap-x-2">
            <a href="#" class="btn btn-ghost btn-circle hidden sm:inline-flex">
              <div class="flex flex-col items-center">
                <div class="indicator">
                  <BellIcon class="h-6 w-6" />
                  <span class="badge badge-xs badge-primary indicator-item bg-pink-500 border-none text-white"></span>
                </div>
                <span class="text-xs">通知</span>
              </div>
            </a>

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

            <a href="#" class="btn btn-ghost btn-circle hidden sm:inline-flex">
              <div class="flex flex-col items-center">
                <StarIcon class="h-6 w-6" />
                <span class="text-xs">收藏</span>
              </div>
            </a>
            <a href="#" class="btn btn-ghost btn-circle hidden sm:inline-flex">
              <div class="flex flex-col items-center">
                <ClockIcon class="h-6 w-6" />
                <span class="text-xs">历史</span>
              </div>
            </a>

            <router-link to="/editor" class="btn bg-pink-500 hover:bg-pink-600 text-white rounded-lg hidden sm:inline-flex mx-2">
              <ArrowUpOnSquareIcon class="h-5 w-5" />
              投稿
            </router-link>

            <div class="dropdown dropdown-end dropdown-hover">
              <router-link :to="'/profile/' + authStore.user.username" tabindex="0" role="button" class="btn btn-ghost btn-circle avatar">
                <div class="w-10 rounded-full">
                  <img :src="authStore.userImage" alt="User avatar" />
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
      <router-view></router-view>
    </main>

  </div>
</template>