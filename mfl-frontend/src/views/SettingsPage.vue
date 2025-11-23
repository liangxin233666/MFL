<script setup lang="ts">
import { ref } from 'vue';
import { useAuthStore } from '../stores/auth';
import { UserCircleIcon, ShieldCheckIcon, CameraIcon } from '@heroicons/vue/24/outline';
import { useUploader } from '../composables/useUploader';

const authStore = useAuthStore();
const { isUploading, uploadFile } = useUploader();

const activeTab = ref('profile');

// --- 为每个可编辑字段创建独立的状态 ---
const isEditingUsername = ref(false);
const newUsername = ref('');
const isEditingBio = ref(false);
const newBio = ref('');
const isEditEmail = ref(false);
const newEmail = ref('');
const isEditingPassword = ref(false);
const newPassword = ref('');

const isSubmitting = ref(false); // 用于文本字段的提交状态

// --- 头像上传 ---
const handleAvatarUpload = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  if (!input.files?.length) return;

  const file = input.files[0];
  const tempUrl = await uploadFile(file); // isUploading 状态由 useUploader 内部管理

  if (tempUrl) {
    // 成功上传到云后，只提交 image 字段来更新用户信息
    const payload = { user: { image: tempUrl } };
    try {
      await authStore.updateSettings(payload);
      alert('头像更新成功！');
    } catch (error) {
      alert('头像保存失败，请重试');
    }
  } else {
    alert('文件上传失败，请稍后重试');
  }
};

// --- 文本字段更新 ---
const startEditing = (field: 'username' | 'bio' | 'password' | 'email') => {
  if (!authStore.user) return;
  if (field === 'username') { newUsername.value = authStore.user.username; isEditingUsername.value = true; }
  if (field === 'bio') { newBio.value = authStore.user.bio || ''; isEditingBio.value = true; }
  if (field === 'email') { newEmail.value = authStore.user.email; isEditEmail.value = true; }
  if (field === 'password') { newPassword.value = ''; isEditingPassword.value = true; }
};

const cancelEditing = (field: 'username' | 'bio' | 'password' | 'email') => {
  if (field === 'username') isEditingUsername.value = false;
  if (field === 'bio') isEditingBio.value = false;
  if (field === 'email') isEditEmail.value = false;
  if (field === 'password') isEditingPassword.value = false;
};

const handleUpdate = async (field: 'username' | 'bio' | 'password' | 'email') => {
  isSubmitting.value = true;
  const payload: { user: Partial<Record<'username' | 'bio' | 'password' | 'email' | 'image', string | null>> } = { user: {} };

  switch (field) {
    case 'username': payload.user.username = newUsername.value; break;
    case 'bio': payload.user.bio = newBio.value; break;
    case 'email': payload.user.email = newEmail.value; break;
    case 'password': payload.user.password = newPassword.value; break;
  }

  try {
    await authStore.updateSettings(payload);
    cancelEditing(field);
  } catch (error: any) {
    const errorMessages = error.response?.data?.errors ? JSON.stringify(error.response.data.errors) : (error.response?.data?.message || '更新失败');
    alert(`错误: ${errorMessages}`);
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<template>
  <div class="card bg-base-100 shadow-xl lg:card-side">
    <!-- 左侧导航栏 -->
    <div class="p-4 border-b lg:border-b-0 lg:border-r border-base-200">
      <ul class="menu p-2 rounded-box w-full lg:w-56">
        <li class="menu-title"><span>设置</span></li>
        <li><a :class="{'bg-pink-500/10 text-pink-500': activeTab === 'profile'}" @click="activeTab = 'profile'"><UserCircleIcon class="w-5 h-5"/> 个人资料</a></li>
        <li><a :class="{'bg-pink-500/10 text-pink-500': activeTab === 'security'}" @click="activeTab = 'security'"><ShieldCheckIcon class="w-5 h-5"/> 账户安全</a></li>
      </ul>
    </div>

    <!-- 右侧内容面板 -->
    <div class="card-body" v-if="authStore.user">
      <!-- 个人资料面板 -->
      <div v-if="activeTab === 'profile'">
        <h2 class="card-title text-2xl mb-6">个人资料设置</h2>

        <!-- 头像上传区 -->
        <div class="flex items-center justify-between py-4 border-b">
          <div class="w-24 font-bold text-base-content/70 flex-shrink-0">头像</div>
          <div class="flex-grow">
            <label class="avatar relative cursor-pointer group">
              <div class="w-20 rounded-full ring ring-pink-500 ring-offset-base-100 ring-offset-2">
                <img :src="authStore.userImage" />
              </div>
              <div class="absolute inset-0 bg-black/50 rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                <span v-if="isUploading" class="loading loading-spinner text-white"></span>
                <CameraIcon v-else class="w-8 h-8 text-white"/>
              </div>
              <input type="file" @change="handleAvatarUpload" :disabled="isUploading" class="hidden" accept="image/png, image/jpeg, image/gif" />
            </label>
          </div>
          <div class="w-40 text-right"><span class="text-sm text-base-content/50">点击头像更换</span></div>
        </div>

        <!-- 其他字段编辑区 (此处省略了重复的模板代码，但逻辑是完整的) -->
        <!-- 用户名 -->
        <div class="flex items-center justify-between py-4 border-b">
          <div class="w-24 font-bold text-base-content/70 flex-shrink-0">用户名</div>
          <div class="flex-grow">
            <span v-if="!isEditingUsername">{{ authStore.user.username }}</span>
            <input v-else type="text" v-model="newUsername" class="input input-bordered input-sm w-full max-w-xs" />
          </div>
          <div class="w-40 text-right flex-shrink-0">
            <div v-if="!isEditingUsername"><button @click="startEditing('username')" class="btn btn-ghost btn-sm">修改</button></div>
            <div v-else class="space-x-2"><button @click="handleUpdate('username')" class="btn bg-pink-500 text-white btn-sm" :disabled="isSubmitting"><span v-if="isSubmitting" class="loading loading-spinner loading-xs"></span>保存</button><button @click="cancelEditing('username')" class="btn btn-ghost btn-sm">取消</button></div>
          </div>
        </div>
        <!-- 简介 -->
        <div class="flex items-start justify-between py-4">
          <div class="w-24 font-bold text-base-content/70 pt-2 flex-shrink-0">个人简介</div>
          <div class="flex-grow">
            <p v-if="!isEditingBio" class="whitespace-pre-wrap">{{ authStore.user.bio || '未设置个人简介' }}</p>
            <textarea v-else v-model="newBio" class="textarea textarea-bordered w-full max-w-xs" rows="3"></textarea>
          </div>
          <div class="w-40 text-right pt-2 flex-shrink-0">
            <div v-if="!isEditingBio"><button @click="startEditing('bio')" class="btn btn-ghost btn-sm">修改</button></div>
            <div v-else class="space-x-2"><button @click="handleUpdate('bio')" class="btn bg-pink-500 text-white btn-sm" :disabled="isSubmitting"><span v-if="isSubmitting" class="loading loading-spinner loading-xs"></span>保存</button><button @click="cancelEditing('bio')" class="btn btn-ghost btn-sm">取消</button></div>
          </div>
        </div>
      </div>

      <!-- 账户安全面板 -->
      <div v-if="activeTab === 'security'">
        <h2 class="card-title text-2xl mb-6">账户安全</h2>
        <!-- 邮箱 -->
        <div class="flex items-center justify-between py-4 border-b">
          <div class="w-24 font-bold text-base-content/70 flex-shrink-0">邮箱</div>
          <div class="flex-grow"><span v-if="!isEditEmail">{{ authStore.user.email }}</span><input v-else type="text" v-model="newEmail" class="input input-bordered input-sm w-full max-w-xs" /></div>
          <div class="w-40 text-right flex-shrink-0">
            <div v-if="!isEditEmail"><button @click="startEditing('email')" class="btn btn-ghost btn-sm">修改</button></div>
            <div v-else class="space-x-2"><button @click="handleUpdate('email')" class="btn bg-pink-500 text-white btn-sm" :disabled="isSubmitting"><span v-if="isSubmitting" class="loading loading-spinner loading-xs"></span>保存</button><button @click="cancelEditing('email')" class="btn btn-ghost btn-sm">取消</button></div>
          </div>
        </div>
        <!-- 密码 -->
        <div class="flex items-center justify-between py-4">
          <div class="w-24 font-bold text-base-content/70">登录密码</div>
          <div class="flex-grow"><span v-if="!isEditingPassword">******</span><input v-else type="password" v-model="newPassword" placeholder="输入新密码 (至少8位)" class="input input-bordered input-sm w-full max-w-xs" /></div>
          <div class="w-40 text-right">
            <div v-if="!isEditingPassword"><button @click="startEditing('password')" class="btn btn-ghost btn-sm">修改</button></div>
            <div v-else class="space-x-2"><button @click="handleUpdate('password')" class="btn bg-pink-500 text-white btn-sm" :disabled="isSubmitting || newPassword.length < 8"><span v-if="isSubmitting" class="loading loading-spinner loading-xs"></span>保存</button><button @click="cancelEditing('password')" class="btn btn-ghost btn-sm">取消</button></div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>