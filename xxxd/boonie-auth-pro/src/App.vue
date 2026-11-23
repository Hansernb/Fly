<template>
  <!-- 熊出没角色合影氛围背景（高斯模糊 + 压暗40% + 半透明浅绿蒙版，纯底纹不参与交互） -->
  <div class="bg-scene" aria-hidden="true"></div>

  <!-- 权限拦截提示（如：默认权限用户尝试进入后台） -->
  <Transition name="toast">
    <div v-if="toast" class="app-toast" role="status">{{ toast }}</div>
  </Transition>

  <!-- 后台管理系统（已通过后台登录） -->
  <AdminPanel
    v-if="view === 'admin'"
    :user="adminSession?.user"
    :token="adminSession?.token || ''"
    @exit="exitAdmin"
  />

  <!-- 后台登录页（仅登录，无注册） -->
  <AdminLogin v-else-if="view === 'adminLogin'" @success="onAdminLogin" @back="goHome" />

  <!-- 游乐项目列表页（独立路由） -->
  <AttractionList v-else-if="view === 'attractions'" @back="goHome" />

  <!-- 园区手绘地图页（独立路由） -->
  <ParkMap v-else-if="view === 'map'" @back="goHome" />

  <!-- 门票预订流程页（独立路由） -->
  <TicketBooking
    v-else-if="view === 'tickets'"
    :user="session?.user || null"
    :token="session?.token || ''"
    :initial-ticket-id="presetTicketId"
    @back="goHome"
    @member="enterMember"
  />

  <!-- 森林会员中心页（独立路由） -->
  <MemberCenter
    v-else-if="view === 'member'"
    :user="session?.user || null"
    :token="session?.token || ''"
    @back="goHome"
    @tickets="enterTickets"
  />

  <!-- 个人中心页（独立路由） -->
  <PersonalCenter
    v-else-if="view === 'profile'"
    :user="session?.user || null"
    @back="goHome"
    @attraction="openAttractionDetail"
  />

  <!-- 森林小游戏中心页（独立路由） -->
  <ForestGames
    v-else-if="view === 'games'"
    :token="session?.token || ''"
    :user="session?.user || null"
    @back="goHome"
  />

  <!-- 404 页面：未匹配任何路由 -->
  <NotFound v-else-if="view === 'notfound'" @home="go('/')" @attractions="enterAttractions" />

  <!-- 已登录：进入森林游乐园主页 -->
  <ParkHome
    v-else-if="session"
    :user="session.user"
    :token="session.token"
    @logout="onLogout"
    @admin="enterAdminLogin"
    @attractions="enterAttractions"
    @tickets="enterTickets"
    @games="enterGames"
    @member="enterMember"
    @profile="enterProfile"
  />

  <!-- 未登录：森林登录 / 注册页 -->
  <div v-else class="auth-page">
    <ForestPanel />
    <AuthCard @success="onLogin" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useSessionStore } from './stores/session'
import ForestPanel from './components/ForestPanel.vue'
import AuthCard from './components/AuthCard.vue'
import ParkHome from './components/ParkHome.vue'
import AdminLogin from './components/AdminLogin.vue'
import AdminPanel from './components/AdminPanel.vue'
import AttractionList from './components/AttractionList.vue'
import ParkMap from './components/ParkMap.vue'
import TicketBooking from './components/TicketBooking.vue'
import ForestGames from './components/ForestGames.vue'
import MemberCenter from './components/MemberCenter.vue'
import PersonalCenter from './components/PersonalCenter.vue'
import NotFound from './components/NotFound.vue'

/* ===================== 基础设施：vue-router + Pinia =====================
 * 改造前：App.vue 用 ref('view') + ROUTES 数组 + popstate 手写路由，
 *         会话状态用 ref + 手写 localStorage。
 * 改造后：路由交给 vue-router（见 src/router/index.js，含全局守卫），
 *         会话交给 Pinia（见 src/stores/session.js）。
 * 本组件的模板结构、事件契约、样式保持不变 —— 页面外观与交互零变化。
 */
const route = useRoute()
const router = useRouter()
const store = useSessionStore()
const { session, adminSession, toast } = storeToRefs(store)

/** 当前视图名 = 路由名，模板里的 v-if 链沿用原来的判断方式 */
const view = computed(() => route.name || 'notfound')

/** 从首页票卡直接预订时携带的票种 id */
const presetTicketId = ref('')

/** 统一导航入口（保留原 go(path) 的调用习惯，第二参数已不再需要） */
function go(path) {
  router.push(path)
}

/**
 * 各板块「返回」按钮：优先浏览器后退，回到上一个页面（如 游乐项目 → 首页）；
 * 只有在没有任何站内历史可退时（如新标签直达子页），才兜底回首页。
 */
function goHome() {
  if (window.history.state?.back) {
    router.back()
    return
  }
  router.replace('/')
}

function onLogin(payload) {
  // payload.user.permission：1 = 默认权限，0 = 管理员
  store.setSession(payload)

  // 若停留在后台路由，但当前账号不是管理员 → 拦回首页
  if ((route.path === '/admin' || route.path === '/admin/login') && !store.isAdmin) {
    store.showToast('当前账号为默认权限，无法进入后台管理系统')
    router.replace('/')
    return
  }
  if (route.path !== '/') router.replace('/')
}

function onLogout() {
  store.logout()
  router.replace('/')
}

/** 首页导航栏点「后台管理」齿轮：跳转 /admin/login（该按钮仅管理员可见） */
function enterAdminLogin() {
  if (!store.isAdmin) {
    store.showToast('当前账号为默认权限，无法进入后台管理系统')
    return
  }
  router.push('/admin/login')
}

/** 后台登录成功：必须是管理员才允许进入 /admin */
function onAdminLogin(payload) {
  if (Number(payload?.user?.permission) !== 0) {
    localStorage.removeItem('boonie_admin_token')
    store.showToast('权限不足：仅管理员可进入后台管理系统')
    router.replace('/')
    return
  }
  store.setAdminSession(payload)
  router.replace('/admin')
}

/** 首页导航「项目大全」→ 进入游乐项目列表页 */
function enterAttractions() {
  router.push('/attractions')
}

/** 首页导航「园区地图」→ 进入手绘地图页 */
function enterMap() {
  router.push('/map')
}

/** 首页导航「门票预订」→ 进入在线订票流程页（可携带预选票种） */
function enterTickets(ticketId) {
  presetTicketId.value = typeof ticketId === 'string' ? ticketId : ''
  router.push('/tickets')
}

/** 首页「森林小游戏」入口 → 进入小游戏中心页 */
function enterGames() {
  router.push('/games')
}

/** 会员中心：结算成功页「我的会员卡」/ 首页导航「会员中心」 */
function enterMember() {
  router.push('/member')
}

/** 个人中心：订单 / 收藏 / 浏览历史 / 积分 / 优惠券 / 徽章 */
function enterProfile() {
  router.push('/profile')
}

/** 个人中心点收藏/历史项目 → 回首页并广播打开该项目详情弹窗 */
function openAttractionDetail(id) {
  router.push('/')
  setTimeout(() => {
    window.dispatchEvent(new CustomEvent('boonie_open_attraction', { detail: { id } }))
  }, 400)
}

/** 退出后台，回到首页（保留前台会话） */
function exitAdmin() {
  store.exitAdmin()
  router.replace('/')
}

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
