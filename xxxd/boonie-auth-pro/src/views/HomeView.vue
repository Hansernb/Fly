<template>
  <!-- 未登录：森林登录 / 注册页（原 App.vue 的 auth-page 分支，UI 完全一致） -->
  <div v-if="!session" class="auth-page">
    <ForestPanel />
    <AuthCard @success="onLogin" />
  </div>

  <!-- 已登录：森林游乐园主页 -->
  <ParkHome
    v-else
    :user="session.user"
    :token="session.token"
    @logout="onLogout"
    @admin="enterAdminLogin"
    @attractions="go('/attractions')"
    @tickets="enterTickets"
    @games="go('/games')"
    @member="go('/member')"
    @profile="go('/profile')"
  />
</template>

<script setup>
import { storeToRefs } from 'pinia'
import ForestPanel from '../components/ForestPanel.vue'
import AuthCard from '../components/AuthCard.vue'
import ParkHome from '../components/ParkHome.vue'
import { useSessionStore } from '../stores/session'
import { useNav } from './useNav'

const store = useSessionStore()
const { session } = storeToRefs(store)
const { router, go, enterTickets, enterAdminLogin } = useNav()

// 原 App.vue onLogin：本 view 只在 / 渲染，登录后 setSession 即切到首页视图
function onLogin(payload) {
  store.setSession(payload)
}

// 原 App.vue onLogout：清会话后 replace 回首页
function onLogout() {
  store.logout()
  router.replace('/')
}
</script>

<style scoped>
/* 与 App.vue 原 auth-page 样式一致 */
.auth-page {
  display: flex;
  min-height: 100vh;
}

@media (min-width: 860px) {
  :deep(.forest) {
    display: flex;
  }
  :deep(.panel) {
    padding: 32px 48px;
  }
}
</style>
