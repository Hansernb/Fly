<template>
  <!-- 已登录：进入森林游乐园主页 -->
  <ParkHome v-if="session" :user="session.user" :token="session.token" @logout="onLogout" />

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

const session = ref(null) // { user, token }

function onLogin(payload) {
  session.value = payload
}

function onLogout() {
  localStorage.removeItem('boonie_token')
  session.value = null
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
