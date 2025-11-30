// composables/usePluginSystem.ts
import { ref, watch} from 'vue';
import type { Plugin, LocalPluginState } from '../types/api';

const STORAGE_KEY = 'mfl_installed_plugins';

// --- 全局单例状态 ---
// 这样切换路由时状态不会丢失
const installedPlugins = ref<Record<number, LocalPluginState>>({});

// --- 初始化读取本地存储 ---
if (typeof window !== 'undefined') {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        if (raw) {
            installedPlugins.value = JSON.parse(raw);
        }
    } catch (e) {
        console.error('[Plugin System] 读取本地存储失败', e);
    }
}

// --- 自动监听变化并写入本地存储 ---
watch(
    installedPlugins,
    (newVal) => {
        if (typeof window !== 'undefined') {
            try {
                localStorage.setItem(STORAGE_KEY, JSON.stringify(newVal));
            } catch (e) {
                console.error('[Plugin System] 写入本地存储失败', e);
            }
        }
    },
    { deep: true }
);

export function usePluginSystem() {
    /**
     * 核心逻辑：动态加载 JS 脚本
     */
    const loadScript = (plugin: LocalPluginState) => {
        const scriptId = `plugin-script-${plugin.id}`;

        // 1. 检查是否已存在
        if (document.getElementById(scriptId)) {
            return;
        }

        // 2. ZIP 文件不执行
        if (plugin.fileUrl && plugin.fileUrl.endsWith('.zip')) {
            return;
        }

        // 3. 动态插入 Script
        try {
            const script = document.createElement('script');
            script.id = scriptId;
            script.src = plugin.fileUrl;
            script.async = true;
            document.body.appendChild(script);
            console.log(`[Plugin System] 已加载插件: ${plugin.name}`);
        } catch (e) {
            console.error(`[Plugin System] 加载插件失败: ${plugin.name}`, e);
        }
    };

    /**
     * 核心逻辑：移除插件脚本
     * 注意：这只能移除 DOM 节点，无法清除已运行 JS 的内存副作用 (EventListeners, Intervals 等)
     * 因此通常需要刷新页面
     */
    const unloadScript = (pluginId: number) => {
        const el = document.getElementById(`plugin-script-${pluginId}`);
        if (el) {
            el.remove();
            // 强制刷新以彻底清除副作用
            console.log(`[Plugin System] 已停止插件 (正在刷新): ${pluginId}`);
            window.location.reload();
        }
    };

    // --- 操作方法 ---

    const installPlugin = (plugin: Plugin) => {
        // 将远程 Plugin 转换为本地 LocalPluginState，保存所有必要信息
        const localState: LocalPluginState = {
            id: plugin.id,
            slug: plugin.slug,
            name: plugin.name,
            description: plugin.description,
            version: plugin.version,
            fileUrl: plugin.fileUrl,
            iconUrl: plugin.iconUrl,
            authorName: plugin.authorName,
            isEnabled: true, // 默认开启
            installedAt: Date.now(),
        };

        // 写入响应式对象 (会自动触发 watch 保存到 localStorage)
        installedPlugins.value[plugin.id] = localState;

        // 立即执行
        loadScript(localState);
    };

    const uninstallPlugin = (pluginId: number) => {
        const pluginName = installedPlugins.value[pluginId]?.name;
        if (confirm(`确定要卸载插件 "${pluginName}" 吗？页面将会刷新。`)) {
            delete installedPlugins.value[pluginId];
            unloadScript(pluginId); // 触发刷新
        }
    };

    const togglePlugin = (pluginId: number) => {
        const plugin = installedPlugins.value[pluginId];
        if (!plugin) return;

        plugin.isEnabled = !plugin.isEnabled;

        if (plugin.isEnabled) {
            loadScript(plugin);
        } else {
            unloadScript(pluginId); // 触发刷新
        }
    };

    const isInstalled = (pluginId: number) => {
        return !!installedPlugins.value[pluginId];
    };

    const isEnabled = (pluginId: number) => {
        return installedPlugins.value[pluginId]?.isEnabled;
    };

    // 应用初始化时调用 (通常在 App.vue 或主布局中)
    const initPlugins = () => {
        Object.values(installedPlugins.value).forEach((p) => {
            if (p.isEnabled) {
                loadScript(p);
            }
        });
    };

    return {
        installedPlugins, // 暴露给 UI 用于列表展示
        installPlugin,
        uninstallPlugin,
        togglePlugin,
        isInstalled,
        isEnabled,
        initPlugins,
    };
}