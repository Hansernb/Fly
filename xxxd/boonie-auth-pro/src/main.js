import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useSessionStore } from './stores/session'
import './style.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// 挂载前恢复登录态（localStorage key 与旧版一致，老用户无需重新登录）
useSessionStore(pinia).restore()

app.mount('#app')
