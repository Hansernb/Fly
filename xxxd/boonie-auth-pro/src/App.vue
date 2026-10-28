<template>
  <!-- 后台管理系统（已通过后台登录） -->
  <AdminPanel
    v-if="view === 'admin'"
    :user="adminSession.user"
    :token="adminSession.token"
    @exit="exitAdmin"
  />

  <!-- 后台登录页（仅登录，无注册） -->
  <AdminLogin v-else-if="view === 'adminLogin'" @success="onAdminLogin" @back="view = 'home'" />

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
import { ref } from 'vue'
import ForestPanel from './components/ForestPanel.vue'
import AuthCard from './components/AuthCard.vue'
import ParkHome from './components/ParkHome.vue'
import AdminLogin from './components/AdminLogin.vue'
import AdminPanel from './components/AdminPanel.vue'

const session = ref(null)      // 前台会话 { user, token }
const adminSession = ref(null) // 后台会话 { user, token }
const view = ref('home')       // home | adminLogin | admin

function onLogin(payload) {
  // payload.user.permission：1 = 默认权限，0 = 管理员
  session.value = payload
}

function onLogout() {
  localStorage.removeItem('boonie_token')
  localStorage.removeItem('boonie_admin_token')
  session.value = null
  adminSession.value = null
  view.value = 'home'
}

/** 首页导航栏点「后台管理系统」：先去后台登录页 */
function enterAdminLogin() {
  view.value = 'adminLogin'
}

/** 后台登录成功 */
function onAdminLogin(payload) {
  adminSession.value = payload
  view.value = 'admin'
}

/** 退出后台，回到首页 */
function exitAdmin() {
  adminSession.value = null
  view.value = session.value ? 'home' : 'home'
}
</script>

<style scoped>
.auth-page {
  display: flex;
  min-height: 100vh;
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
