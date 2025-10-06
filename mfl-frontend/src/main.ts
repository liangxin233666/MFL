import { createApp } from 'vue'
import { createPinia } from 'pinia' // 引入 Pinia
import './style.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)
const pinia = createPinia() // 创建 Pinia 实例

app.use(pinia) // 使用 Pinia
app.use(router)

app.mount('#app')