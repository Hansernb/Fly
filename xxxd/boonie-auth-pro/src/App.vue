<template>
  <!-- 熊出没角色合影氛围背景（高斯模糊 + 压暗40% + 半透明浅绿蒙版，纯底纹不参与交互） -->
  <div class="bg-scene" aria-hidden="true"></div>

  <!-- 权限拦截提示（如：默认权限用户尝试进入后台） -->
  <Transition name="toast">
    <div v-if="toast" class="app-toast" role="status">{{ toast }}</div>
  </Transition>

  <!-- 真正的路由出口：各 view 适配层承接原 v-if 链的 props/emit 映射 -->
  <router-view />
</template>

<script setup>
import { onMounted, onBeforeUnmount } from 'vue'
import { storeToRefs } from 'pinia'
import { useSessionStore } from './stores/session'

/*
 * 2026-11-27 架构升级：
 * 改造前本组件用 v-if 链渲染 10 个业务组件，vue-router 只做路由表；
 * 现在改为真正的 <router-view>，各路由指向 src/views/*View.vue 薄适配层，
 * 由适配层承接原先在本组件里的 props 传递与 emit→router.push 映射（见 views/useNav.js）。
 * 全局背景、toast 提示、恢复登录态等横切逻辑仍保留在应用壳。
 */
const store = useSessionStore()
const { toast } = storeToRefs(store)

onMounted(() => {
  // 兜底：若 main.js 的恢复逻辑未执行（如 HMR 热更新），这里再恢复一次
  if (!store.me && !store.session) store.restore()
})

onBeforeUnmount(() => {
  clearTimeout(store._toastTimer)
})
</script>

<style scoped>
/* 全屏氛围底纹：熊出没角色合影
   —— 高斯模糊 16px + brightness(0.6) 压暗40% + 浅绿半透明蒙版，
      放大 6% 遮住模糊边缘；z-index 压到最底层，前景卡片/文字不受影响 */
.bg-scene {
  position: fixed;
  inset: 0;
  z-index: -2;
  background: url("/img/boonie-cast.webp") center center / cover no-repeat;
  filter: blur(6px) brightness(0.6) saturate(1.08);
  transform: scale(1.06);
  pointer-events: none;
}
/* 半透明浅绿蒙版：压住照片杂色，统一成卡通游乐园的清新绿基调 */
.bg-scene::after {
  content: "";
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(140, 226, 168, 0.22) 0%, rgba(96, 200, 130, 0.3) 60%, rgba(60, 160, 100, 0.34) 100%);
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
</style>
