<template>
  <!-- 权限拦截提示（如：默认权限用户尝试进入后台） -->
  <Transition name="toast">
    <div v-if="toast" class="app-toast" role="status">{{ toast }}</div>
  </Transition>

  <!-- 后台管理系统（已通过后台登录） -->
  <AdminPanel
    v-if="view === 'admin'"
    :user="adminSession.user"
    :token="adminSession.token"
    @exit="exitAdmin"
  />

  <!-- 后台登录页（仅登录，无注册） -->
  <AdminLogin v-else-if="view === 'adminLogin'" @success="onAdminLogin" @back="goHome" />

  <!-- 已登录：进入森林游乐园主页 -->
  <ParkHome
    v-else-if="session"
    :user="session.user"
    :token="session.token"
    @logout="onLogout"
    @admin="enterAdminLogin"
  />

  <!-- 未登录：森林登录 / 注册页 -->
  <div v-else class="auth-page">
    <ForestPanel />
    <AuthCard @success="onLogin" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import ForestPanel from './components/ForestPanel.vue'
import AuthCard from './components/AuthCard.vue'
import ParkHome from './components/ParkHome.vue'
import AdminLogin from './components/AdminLogin.vue'
import AdminPanel from './components/AdminPanel.vue'

const session = ref(null)      // 前台会话 { user, token }
const adminSession = ref(null) // 后台会话 { user, token }
const view = ref('home')       // home | adminLogin | admin
const toast = ref('')          // 权限拦截提示文案

/**
 * 当前登录用户（含 permission）：
 *   1 = 默认权限（注册用户），0 = 管理员权限
 * 刷新页面时从本地缓存恢复，用于路由权限守卫。
 * 真正的权限校验始终以后端为准（/api/admin/* 会校验 permission==0，否则 403）。
 */
const me = ref(null)

function loadMe() {
  try {
    const raw = localStorage.getItem('boonie_user')
    me.value = raw ? JSON.parse(raw) : null
  } catch {
    me.value = null
  }
}

function saveMe(user) {
  me.value = user || null
  if (user) localStorage.setItem('boonie_user', JSON.stringify(user))
  else localStorage.removeItem('boonie_user')
}

const isAdmin = computed(() => Number(me.value?.permission) === 0)

let toastTimer = null
function showToast(msg) {
  toast.value = msg
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 2800)
}

/**
 * 轻量前端路由：把视图与地址栏同步（/ → 前台，/admin/login → 后台登录，/admin → 后台主页）
 * 项目未引入 vue-router，这里用 History API 保持地址可读且支持浏览器前进/后退。
 */
const ROUTES = [
  { path: '/', view: 'home' },
  { path: '/admin/login', view: 'adminLogin' },
  { path: '/admin', view: 'admin' }
]
const PATH_OF = { home: '/', adminLogin: '/admin/login', admin: '/admin' }

function go(path, viewName) {
  if (location.pathname !== path) {
    history.pushState({ view: viewName }, '', path)
  }
  view.value = viewName
}

function viewOfPath(path) {
  const hit = ROUTES.find((r) => r.path === path)
  return hit ? hit.view : 'home'
}

/**
 * 后台路由权限守卫：
 *   /admin       —— 必须持有后台令牌，否则回落 /admin/login
 *   /admin/login —— 默认权限用户（permission === 1）禁止进入，拦回首页并提示
 */
function guard(v) {
  if (v === 'admin') {
    return localStorage.getItem('boonie_admin_token') ? 'admin' : 'adminLogin'
  }
  if (v === 'adminLogin') {
    if (localStorage.getItem('boonie_admin_token')) return 'admin'
    // 已登录但非管理员 → 不允许进入后台登录
    if (me.value && Number(me.value.permission) !== 0) {
      showToast('当前账号为默认权限，无法进入后台管理系统')
      return 'home'
    }
    return 'adminLogin'
  }
  return v
}

function onPopState() {
  const v = viewOfPath(location.pathname)
  const g = guard(v)
  if (g !== v) {
    go(PATH_OF[g], g)
    return
  }
  view.value = v
}

function goHome() {
  go('/', 'home')
}

function onLogin(payload) {
  // payload.user.permission：1 = 默认权限，0 = 管理员
  session.value = payload
  saveMe(payload.user)

  // 若停留在后台路由，但当前账号不是管理员 → 拦回首页
  if (viewOfPath(location.pathname) !== 'home' && !isAdmin.value) {
    showToast('当前账号为默认权限，无法进入后台管理系统')
    go('/', 'home')
    return
  }
  if (location.pathname !== '/') go('/', 'home')
}

function onLogout() {
  localStorage.removeItem('boonie_token')
  localStorage.removeItem('boonie_admin_token')
  localStorage.removeItem('boonie_user')
  session.value = null
  adminSession.value = null
  me.value = null
  go('/', 'home')
}

/** 首页导航栏点「后台管理」齿轮：跳转 /admin/login（该按钮仅管理员可见） */
function enterAdminLogin() {
  if (!isAdmin.value) {
    showToast('当前账号为默认权限，无法进入后台管理系统')
    return
  }
  go('/admin/login', 'adminLogin')
}

/** 后台登录成功：必须是管理员才允许进入 /admin */
function onAdminLogin(payload) {
  if (Number(payload?.user?.permission) !== 0) {
    localStorage.removeItem('boonie_admin_token')
    showToast('权限不足：仅管理员可进入后台管理系统')
    go('/', 'home')
    return
  }
  adminSession.value = payload
  saveMe(payload.user)
  go('/admin', 'admin')
}

/** 退出后台，回到首页（保留前台会话） */
function exitAdmin() {
  localStorage.removeItem('boonie_admin_token')
  adminSession.value = null
  go('/', 'home')
}

onMounted(() => {
  loadMe()
  window.addEventListener('popstate', onPopState)
  onPopState()
})

onBeforeUnmount(() => {
  window.removeEventListener('popstate', onPopState)
  clearTimeout(toastTimer)
})
</script>

<style scoped>
.auth-page {
  display: flex;
  min-height: 100vh;
}

/* 权限拦截提示条 */
.app-toast {
  position: fixed;
  top: 18px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  max-width: 90vw;
  padding: 10px 18px;
  border-radius: 12px;
  background: rgba(24, 36, 29, 0.96);
  color: #f3d98a;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.3px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.28);
}
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-10px);
}

/* 左侧森林区在窄屏隐藏，宽屏显示 */
@media (min-width: 860px) {
  :deep(.forest) {
    display: flex;
  }
  /* 桌面优化：宽屏时给卡片区更舒展的内边距 */
  :deep(.panel) {
    padding: 32px 48px;
  }
}
</style>
