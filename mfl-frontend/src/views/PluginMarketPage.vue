<script setup lang="ts">
import { ref, onMounted, computed, reactive } from 'vue';
import axios from 'axios';
import apiClient from '../api/apiClient';
import type { Plugin, LocalPluginState } from '../types/api';
import { usePluginSystem } from '../composables/usePluginSystem';
import {
  PuzzlePieceIcon,
  ArrowDownTrayIcon,
  TrashIcon,
  PlayIcon,
  PauseIcon,
  PlusIcon
} from '@heroicons/vue/24/outline';

const {
  installedPlugins,
  installPlugin,
  uninstallPlugin,
  togglePlugin,
  isInstalled,
  isEnabled
} = usePluginSystem();

// --- 状态管理 ---
const allPlugins = ref<Plugin[]>([]);
const activeTab = ref<'market' | 'local'>('market');

const showUploadModal = ref(false);
const isUploading = ref(false);

// --- API 获取 ---
const fetchPlugins = async () => {
  try {
    const res = await apiClient.get<Plugin[]>('/plugins');
    allPlugins.value = res.data;
  } catch (error) {
    console.error("加载市场失败", error);
  }
};

// --- 本地数据源 ---
const myInstalledList = computed<LocalPluginState[]>(() => {
  return Object.values(installedPlugins.value);
});

// --- 动作逻辑 ---
const handleInstall = async (plugin: Plugin) => {
  try {
    // 埋点
    await apiClient.post(`/plugins/${plugin.id}/install`);
  } catch(e) { /* ignore */ }

  if (plugin.fileUrl && plugin.fileUrl.endsWith('.zip')) {
    window.open(plugin.fileUrl, '_blank');
    // ZIP 也可以 "安装" 进本地列表，方便后续查找，但不执行脚本
  }

  installPlugin(plugin);
};

// --- 上传逻辑 ---
const uploadForm = reactive({
  name: '',
  slug: '',
  version: '1.0.0',
  description: '',
  authorName: '',
  iconFile: null as File | null,
  pluginFile: null as File | null
});

const onFileChange = (field: 'icon' | 'file', e: Event) => {
  const files = (e.target as HTMLInputElement).files;
  if (!files?.[0]) return;
  if (field === 'icon') uploadForm.iconFile = files[0];
  else uploadForm.pluginFile = files[0];
};

const getUploadUrl = async (file: File) => {
  const { data } = await apiClient.get<string>(`/plugins/upload-url`, {
    params: { filename: file.name, contentType: file.type }
  });
  return data;
};

const handlePublish = async () => {
  if(!uploadForm.name || !uploadForm.pluginFile || !uploadForm.slug) {
    alert('缺少必要信息'); return;
  }
  isUploading.value = true;
  try {
    // 1. 上传插件文件
    const uploadUrl = await getUploadUrl(uploadForm.pluginFile);
    await axios.put(uploadUrl, uploadForm.pluginFile, {
      headers: { 'Content-Type': uploadForm.pluginFile.type }
    });
    const pureFileUrl = uploadUrl.split('?')[0];

    // 2. 上传图标 (可选)
    let pureIconUrl = '';
    if (uploadForm.iconFile) {
      const iconUrl = await getUploadUrl(uploadForm.iconFile);
      await axios.put(iconUrl, uploadForm.iconFile, {
        headers: { 'Content-Type': uploadForm.iconFile.type }
      });
      pureIconUrl = iconUrl.split('?')[0];
    }

    // 3. 提交后端
    await apiClient.post('/plugins', {
      name: uploadForm.name,
      slug: uploadForm.slug,
      version: uploadForm.version,
      description: uploadForm.description,
      authorName: uploadForm.authorName || 'User',
      tempFileUrl: pureFileUrl,
      tempIconUrl: pureIconUrl
    });

    alert('发布成功!');
    showUploadModal.value = false;
    await fetchPlugins();
  } catch (error) {
    console.error(error);
    alert('发布失败');
  } finally {
    isUploading.value = false;
  }
};

onMounted(() => fetchPlugins());
</script>

<template>
  <div class="container mx-auto max-w-6xl py-8 px-4">
    <!-- Header -->
    <div class="flex flex-col md:flex-row justify-between items-center mb-8 gap-4">
      <div class="flex items-center gap-3">
        <div class="bg-indigo-500 text-white p-2 rounded-lg">
          <PuzzlePieceIcon class="w-8 h-8"/>
        </div>
        <div>
          <h1 class="text-2xl font-bold">插件市场</h1>
          <p class="text-base-content/60 text-sm">安装、管理与分享您的插件</p>
        </div>
      </div>

      <div class="flex gap-2">
        <div class="tabs tabs-boxed">
          <a class="tab" :class="{'tab-active': activeTab === 'market'}" @click="activeTab='market'">发现</a>
          <a class="tab" :class="{'tab-active': activeTab === 'local'}" @click="activeTab='local'">已安装</a>
        </div>
        <button @click="showUploadModal=true" class="btn btn-primary gap-2">
          <PlusIcon class="w-5 h-5"/> 发布插件
        </button>
      </div>
    </div>

    <!-- 插件列表网格 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">

      <!-- 这里使用了 Union 类型来处理 v-for -->
      <template v-for="plugin in (activeTab === 'market' ? allPlugins : myInstalledList)" :key="plugin.id">

        <div class="card bg-base-100 shadow-md border hover:shadow-lg transition-all flex flex-col h-full">
          <div class="card-body flex-row gap-4 p-5 pb-2">
            <!-- Icon -->
            <div class="w-16 h-16 flex-shrink-0 bg-base-200 rounded-xl overflow-hidden flex items-center justify-center">
              <img v-if="plugin.iconUrl" :src="plugin.iconUrl" class="w-full h-full object-cover" alt="plugin icon"/>
              <PuzzlePieceIcon v-else class="w-8 h-8 text-base-content/30"/>
            </div>

            <!-- Text -->
            <div class="flex-grow min-w-0 overflow-hidden">
              <h2 class="card-title text-lg truncate block" :title="plugin.name">{{ plugin.name }}</h2>

              <div class="flex flex-wrap gap-2 text-xs text-base-content/50 mt-1">
                <span>v{{ plugin.version }}</span>
                <span>· {{ plugin.authorName }}</span>
              </div>

              <p class="text-sm mt-2 text-base-content/70 line-clamp-2 h-10 break-words leading-tight">
                {{ plugin.description || '暂无描述' }}
              </p>
            </div>
          </div>

          <!-- Bottom Actions -->
          <div class="px-5 pb-5 pt-3 mt-auto flex justify-between items-center border-t border-base-100/50">

            <!-- Left: Status/Downloads -->
            <div class="text-xs text-base-content/40">
              <!-- 'downloads' 只有远程 Plugin 类型有，LocalPluginState 没有，所以需要判断 key 是否存在 -->
              <span v-if="'downloads' in plugin">{{ plugin.downloads || 0 }} 次下载</span>
              <span v-else class="badge badge-sm" :class="isEnabled(plugin.id) ? 'badge-success text-white' : 'badge-ghost'">
                {{ isEnabled(plugin.id) ? '运行中' : '已暂停' }}
              </span>
            </div>

            <!-- Right: Buttons -->
            <div class="flex gap-2">

              <!-- >>>>>> Tab: Market <<<<<< -->
              <template v-if="activeTab === 'market'">
                <div v-if="isInstalled(plugin.id)" class="btn btn-sm btn-disabled btn-ghost opacity-50">
                  已安装
                </div>
                <button v-else @click="handleInstall(plugin as Plugin)" class="btn btn-sm btn-ghost bg-base-200 hover:bg-indigo-50 text-indigo-600">
                  <ArrowDownTrayIcon class="w-4 h-4"/> 获取
                </button>
              </template>

              <!-- >>>>>> Tab: Local <<<<<< -->
              <template v-else>
                <!-- ZIP 资源 -->
                <button
                    v-if="plugin.fileUrl && plugin.fileUrl.endsWith('.zip')"
                    class="btn btn-sm btn-ghost btn-disabled text-xs"
                >
                  ZIP资源
                </button>

                <!-- Toggle Button (JS Only) -->
                <button
                    v-else
                    @click="togglePlugin(plugin.id)"
                    class="btn btn-sm w-20"
                    :class="isEnabled(plugin.id) ? 'btn-neutral' : 'btn-outline'"
                >
                  <PauseIcon v-if="isEnabled(plugin.id)" class="w-4 h-4" />
                  <PlayIcon v-else class="w-4 h-4" />
                  {{ isEnabled(plugin.id) ? '暂停' : '启用' }}
                </button>

                <!-- Uninstall Button -->
                <div class="tooltip" data-tip="彻底删除并刷新">
                  <button
                      @click="uninstallPlugin(plugin.id)"
                      class="btn btn-sm btn-square btn-ghost text-error hover:bg-red-50"
                  >
                    <TrashIcon class="w-4 h-4"/>
                  </button>
                </div>
              </template>
            </div>
          </div>
        </div>

      </template>
    </div>

    <!-- Empty States -->
    <div v-if="(activeTab === 'market' ? allPlugins : myInstalledList).length === 0" class="text-center py-20 text-base-content/50">
      {{ activeTab === 'market' ? '市场空空如也，快去发布吧！' : '还没有安装插件哦' }}
    </div>

    <!-- Upload Modal -->
    <dialog class="modal" :class="{'modal-open': showUploadModal}">
      <!-- (Upload Modal Content 保持不变，代码已在上方包含) -->
      <div class="modal-box w-11/12 max-w-xl">
        <h3 class="font-bold text-lg mb-4">发布新插件</h3>
        <div class="space-y-4">
          <div class="form-control w-full">
            <label class="label"><span class="label-text">插件名</span></label>
            <input v-model="uploadForm.name" type="text" placeholder="例如: 暗黑模式 Pro" class="input input-bordered w-full" />
          </div>

          <div class="flex gap-4">
            <div class="form-control w-1/2">
              <label class="label"><span class="label-text">Slug (唯一ID)</span></label>
              <input v-model="uploadForm.slug" placeholder="dark-mode" class="input input-bordered w-full" />
            </div>
            <div class="form-control w-1/2">
              <label class="label"><span class="label-text">版本</span></label>
              <input v-model="uploadForm.version" placeholder="1.0.0" class="input input-bordered w-full" />
            </div>
          </div>

          <div class="form-control w-1/2">
            <label class="label"><span class="label-text">描述</span></label>
            <textarea v-model="uploadForm.description" class="textarea textarea-bordered h-24" placeholder="描述你的插件功能..."></textarea>
          </div>

          <div class="form-control">
            <label class="label"><span class="label-text">文件 (.js 或 .zip)</span></label>
            <input type="file" @change="e => onFileChange('file', e)" class="file-input file-input-bordered w-full" accept=".js,.zip"/>
          </div>


          <div class="form-control">
            <label class="label"><span class="label-text">图标 (可选)</span></label>
            <input type="file" @change="e => onFileChange('icon', e)" class="file-input file-input-bordered w-full" accept="image/*"/>
          </div>

        </div>

        <div class="modal-action">
          <button class="btn" @click="showUploadModal = false">取消</button>
          <button class="btn btn-primary" :disabled="isUploading" @click="handlePublish">
            <span v-if="isUploading" class="loading loading-spinner"></span>
            确认发布
          </button>
        </div>
      </div>
    </dialog>
  </div>
</template>