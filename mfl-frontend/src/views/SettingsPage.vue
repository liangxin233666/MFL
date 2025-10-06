<script setup lang="ts">
import { ref } from 'vue';
import { UserCircleIcon, ShieldCheckIcon, BellIcon, ArrowUpOnSquareIcon } from '@heroicons/vue/24/outline';

const activeTab = ref('profile');

// 模拟用户数据
const settings = ref({
  avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026704d',
  username: '电磁炮打蚊子',
  email: 'bilibili-user@example.com',
  bio: '知名游戏区UP主，分享最新最热的游戏资讯和攻略。',
});
</script>

<template>
  <div class="card bg-base-100 shadow-xl lg:card-side">
    <!-- 左侧导航 -->
    <div class="p-4 border-b lg:border-b-0 lg:border-r border-base-200">
      <ul class="menu p-2 rounded-box w-full lg:w-56">
        <li class="menu-title"><span>设置</span></li>
        <li>
          <a :class="{'bg-pink-500/10 text-pink-500': activeTab === 'profile'}" @click="activeTab = 'profile'">
            <UserCircleIcon class="w-5 h-5" /> 个人资料
          </a>
        </li>
        <li>
          <a :class="{'bg-pink-500/10 text-pink-500': activeTab === 'security'}" @click="activeTab = 'security'">
            <ShieldCheckIcon class="w-5 h-5" /> 账户安全
          </a>
        </li>
        <li>
          <a :class="{'bg-pink-500/10 text-pink-500': activeTab === 'notifications'}" @click="activeTab = 'notifications'">
            <BellIcon class="w-5 h-5" /> 通知设置
          </a>
        </li>
      </ul>
    </div>

    <!-- 右侧内容区 -->
    <div class="card-body">
      <!-- 个人资料 -->
      <div v-if="activeTab === 'profile'">
        <h2 class="card-title text-2xl mb-6">个人资料设置</h2>
        <div class="form-control w-full max-w-lg">
          <label class="label"><span class="label-text">头像</span></label>
          <div class="flex items-center gap-4">
            <div class="avatar">
              <div class="w-16 rounded-full">
                <img :src="settings.avatar" />
              </div>
            </div>
            <button class="btn btn-sm btn-outline">更换头像</button>
          </div>
        </div>
        <div class="form-control w-full max-w-lg mt-4">
          <label class="label"><span class="label-text">用户名</span></label>
          <input type="text" v-model="settings.username" class="input input-bordered w-full" />
        </div>
        <div class="form-control w-full max-w-lg mt-4">
          <label class="label"><span class="label-text">个人简介</span></label>
          <textarea v-model="settings.bio" class="textarea textarea-bordered h-24" placeholder="介绍一下自己吧"></textarea>
        </div>
        <button class="btn bg-pink-500 hover:bg-pink-600 text-white mt-8">
          <ArrowUpOnSquareIcon class="w-5 h-5" />
          保存修改
        </button>
      </div>

      <!-- 账户安全 -->
      <div v-if="activeTab === 'security'">
        <h2 class="card-title text-2xl mb-6">账户安全</h2>
        <div class="form-control w-full max-w-lg">
          <label class="label"><span class="label-text">邮箱地址</span></label>
          <input type="email" :value="settings.email" disabled class="input input-bordered w-full" />
        </div>
        <div class="form-control w-full max-w-lg mt-4">
          <label class="label"><span class="label-text">修改密码</span></label>
          <input type="password" placeholder="输入新密码" class="input input-bordered w-full" />
        </div>
        <button class="btn bg-pink-500 hover:bg-pink-600 text-white mt-8">更新密码</button>
      </div>

      <!-- 通知设置 -->
      <div v-if="activeTab === 'notifications'">
        <h2 class="card-title text-2xl mb-6">通知设置</h2>
        <div class="form-control">
          <label class="label cursor-pointer">
            <span class="label-text">有人评论我的文章时通知我</span>
            <input type="checkbox" checked class="toggle toggle-primary toggle-pink" />
          </label>
        </div>
        <div class="form-control">
          <label class="label cursor-pointer">
            <span class="label-text">有人关注我时通知我</span>
            <input type="checkbox" checked class="toggle toggle-primary toggle-pink" />
          </label>
        </div>
        <div class="form-control">
          <label class="label cursor-pointer">
            <span class="label-text">接收站内活动推送</span>
            <input type="checkbox" class="toggle toggle-primary toggle-pink" />
          </label>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 自定义 DaisyUI 切换按钮颜色 */
.toggle-pink:checked {
  background-color: #ec4899; /* pink-500 */
  border-color: #ec4899;
}
</style>