<!-- src/views/LoginPage.vue -->
<script setup lang="ts">
import { reactive } from 'vue';
import { useAuthStore } from '../stores/auth';
import { UserIcon, LockClosedIcon, ArrowLeftStartOnRectangleIcon } from '@heroicons/vue/24/solid';
import {useRouter} from "vue-router";

const router = useRouter();

const authStore = useAuthStore();
const form = reactive({
  email: '',
  password: '',
});

const handleSubmit = async () => {
  try {
    await authStore.login(form);
    await router.push('/');
  } catch (error: any) {
    console.log(error);
    const errorMsg = error.response?.data?.errors ? JSON.stringify(error.response.data.errors) : '未知错误114514';
    alert(`登录失败: ${errorMsg}`);
  }
};
</script>

<template>
  <div class="flex justify-center items-center py-12">
    <div class="card w-full max-w-md bg-base-100 shadow-xl">
      <div class="card-body">
        <h2 class="card-title text-2xl font-bold justify-center mb-6">欢迎回来！</h2>
        <form @submit.prevent="handleSubmit">
          <div class="form-control mb-4">
            <label class="input input-bordered flex items-center gap-2 h-12">
              <UserIcon class="w-5 h-5 text-base-content/50" />
              <input type="email" v-model="form.email" class="grow" placeholder="邮箱" required />
            </label>
          </div>
          <div class="form-control mb-6">
            <label class="input input-bordered flex items-center gap-2 h-12">
              <LockClosedIcon class="w-5 h-5 text-base-content/50" />
              <input type="password" v-model="form.password" class="grow" placeholder="密码" required />
            </label>
          </div>
          <div class="form-control">
            <button type="submit" class="btn h-12 text-base bg-pink-500 hover:bg-pink-600 border-none text-white">
              <ArrowLeftStartOnRectangleIcon class="w-5 h-5" />
              <span>登 录</span>
            </button>
          </div>
        </form>
        <div class="divider text-sm">还没有账号？</div>
        <router-link to="/register" class="btn btn-ghost h-12 text-base">立即注册</router-link>
      </div>
    </div>
  </div>
</template>