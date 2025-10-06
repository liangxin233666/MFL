<script setup lang="ts">
import { ref } from 'vue';
import { UserIcon, LockClosedIcon, EnvelopeIcon, SparklesIcon } from '@heroicons/vue/24/solid';

const username = ref('');
const email = ref('');
const password = ref('');
</script>

<template>
  <div class="flex justify-center items-center py-12">
    <div class="card w-full max-w-md bg-base-100 shadow-xl">
      <div class="card-body">
        <h2 class="card-title text-2xl font-bold justify-center mb-6">
          加入我们，发现更多精彩！
        </h2>

        <!-- 表单 -->
        <form @submit.prevent>
          <div class="form-control mb-4">
            <label class="input input-bordered flex items-center gap-2 h-12">
              <UserIcon class="w-5 h-5 text-base-content/50" />
              <input type="text" v-model="username" class="grow" placeholder="用户名" />
            </label>
          </div>

          <div class="form-control mb-4">
            <label class="input input-bordered flex items-center gap-2 h-12">
              <EnvelopeIcon class="w-5 h-5 text-base-content/50" />
              <input type="email" v-model="email" class="grow" placeholder="邮箱" />
            </label>
          </div>

          <div class="form-control mb-6">
            <label class="input input-bordered flex items-center gap-2 h-12">
              <LockClosedIcon class="w-5 h-5 text-base-content/50" />
              <input type="password" v-model="password" class="grow" placeholder="密码" />
            </label>
          </div>

          <div class="form-control">
            <button class="btn btn-primary h-12 text-base bg-pink-500 hover:bg-pink-600 border-none text-white">
              <SparklesIcon class="w-5 h-5" />
              <span>注 册</span>
            </button>
          </div>
        </form>

        <div class="divider text-sm">已有账号？</div>

        <router-link to="/login" class="btn btn-ghost h-12 text-base">
          前往登录
        </router-link>
      </div>
    </div>
  </div>
</template>