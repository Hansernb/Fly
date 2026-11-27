import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useSessionStore } from './stores/session'
import { reportVisit } from './api/content'
import { setUnauthorizedHandler } from './utils/request'
import './style.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// 挂载前恢复登录态（localStorage key 与旧版一致，老用户无需重新登录）
useSessionStore(pinia).restore()

// 401 统一处理（借鉴 vue3-element-admin 的响应拦截）：登录过期时清会话并提示。
// 注意：/api/me 匿名探测已在 api 层声明 skipUnauthorized，不会触发这里。
setUnauthorizedHandler(() => {
  const store = useSessionStore(pinia)
  if (store.session || store.adminSession) {
    store.logout()
    store.showToast('登录已过期，请重新登录')
  }
})

app.mount('#app')

// 访问埋点（借鉴 java_travel 的 OpLog）：每个标签页会话只上报一次，
// 后台看板据此统计真实 PV/UV。fire-and-forget，失败静默，绝不影响页面。
try {
  if (!sessionStorage.getItem('boonie_visit_reported')) {
    sessionStorage.setItem('boonie_visit_reported', '1')
    reportVisit(location.pathname || '/').catch(() => {})
  }
} catch {
  // 隐私模式下 sessionStorage 不可用时跳过埋点
}
