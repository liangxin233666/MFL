<script setup lang="ts">
import { MagnifyingGlassIcon } from '@heroicons/vue/24/outline';
import { ArrowUpOnSquareIcon } from '@heroicons/vue/24/solid';
</script>

<template>
  <div class="flex flex-col min-h-screen bg-base-200/50 font-sans">
    <header class="bg-base-100/80 backdrop-blur sticky top-0 z-50 border-b border-base-200">
      <div class="navbar container mx-auto py-1 min-h-fit">
        <!-- 导航栏左侧 -->
        <div class="navbar-start">
          <router-link to="/" class="btn btn-ghost text-2xl font-black normal-case text-pink-500 hover:bg-transparent">
            bilibili
          </router-link>
          <ul class="menu menu-horizontal px-1 font-medium hidden sm:flex">
            <li><router-link to="/" active-class="text-pink-500">首页</router-link></li>
            <li><router-link to="/login" active-class="text-pink-500">登录</router-link></li>
          </ul>
        </div>

        <!-- 导航栏中间 -->
        <div class="navbar-center hidden lg:flex">
          <div class="relative w-96">
            <input type="text" placeholder="搜点好东西..." class="input input-bordered h-9 w-full rounded-lg bg-base-200 focus:bg-base-100 focus:border-pink-500 transition-all" />
            <MagnifyingGlassIcon class="absolute top-1/2 right-3 h-5 w-5 -translate-y-1/2 text-base-content/50" />
          </div>
        </div>

        <div class="navbar-end">
          <div class="dropdown dropdown-end dropdown-hover">
            <!-- 头像触发器 -->
            <div tabindex="0" role="button" class="btn btn-ghost btn-circle avatar">
              <div class="w-10 rounded-full">
                <img src="https://i.pravatar.cc/150?u=a042581f4e29026704d" alt="User avatar" />
              </div>
            </div>
            <!-- 下拉菜单内容 -->
            <ul tabindex="0" class="dropdown-content menu menu-sm z-[1] mt-3 p-2 shadow bg-base-100 rounded-box w-52">
              <li>
                <router-link to="/editor" class="font-bold text-pink-500 hover:!text-pink-500">
                  <ArrowUpOnSquareIcon class="w-5 h-5"/>
                  投稿
                </router-link>
              </li>
              <li class="border-t my-1 border-base-200/50"></li>
              <li><router-link to="/profile/diancipaodawenzi">个人主页</router-link></li>
              <li><router-link to="/settings">设置</router-link></li>
              <li class="border-t my-1 border-base-200/50"></li>
              <li><a>退出登录</a></li>
            </ul>
          </div>
        </div>
      </div>
    </header>

    <!-- 主体内容区域 -->
    <main class="flex-grow py-8">
      <div class="container mx-auto">
        <router-view></router-view>
      </div>
    </main>

  </div>
</template>

<!-- 添加一个全局字体样式，让界面更好看 -->
<style>
body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji";
}
</style>