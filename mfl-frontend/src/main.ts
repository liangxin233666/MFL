import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router' // 确保 router 在 pinia 之前
import './style.css'
import App from './App.vue'
import { usePluginSystem } from './composables/usePluginSystem';
// 1. 创建 Pinia 实例
const pinia = createPinia()

// 2. **创建 Pinia 插件，将 router 实例添加到每个 store**
//    这会让 this.router 在所有 store 的 action 中可用
pinia.use((context) => {
    context.store.router = router
})

// 3. 创建 Vue 应用实例
const app = createApp(App)

const { initPlugins } = usePluginSystem();

// 4. 按顺序使用插件

app.use(pinia)
app.use(router)
initPlugins();
// 5. 挂载应用
app.mount('#app')
