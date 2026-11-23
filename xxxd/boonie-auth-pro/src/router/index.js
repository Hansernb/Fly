import { createRouter, createWebHistory } from 'vue-router'
import { useSessionStore } from '../stores/session'

/**
 * 路由表
 *
 * 迁移说明：原 App.vue 用 ROUTES 数组 + viewOfPath() + popstate 手写路由，
 * 现改为 vue-router；路径与原来完全一致，旧链接/收藏夹不会失效。
 *
 * 注：这里用同步 import 而非 () => import()，原因是当前阶段由 App.vue 的 v-if 链
 * 负责实际渲染（渐进式迁移，保证 UI 零变化），组件已被 App.vue 静态引入，
 * 再用懒加载只会产生「同时被静态与动态引入」的分包警告。待第二步把 App.vue
 * 改为真正的 router-view 架构时，再把这里换成懒加载以获得分包收益。
 */
import ParkHome from '../components/ParkHome.vue'
import AdminLogin from '../components/AdminLogin.vue'
import AdminPanel from '../components/AdminPanel.vue'
import AttractionList from '../components/AttractionList.vue'
import ParkMap from '../components/ParkMap.vue'
import TicketBooking from '../components/TicketBooking.vue'
import ForestGames from '../components/ForestGames.vue'
import MemberCenter from '../components/MemberCenter.vue'
import PersonalCenter from '../components/PersonalCenter.vue'
import NotFound from '../components/NotFound.vue'

const routes = [
  { path: '/', name: 'home', component: ParkHome, meta: { title: '狗熊岭森林游乐园' } },
  { path: '/admin/login', name: 'adminLogin', component: AdminLogin, meta: { title: '后台登录' } },
  {
    path: '/admin',
    name: 'admin',
    component: AdminPanel,
    meta: { title: '后台管理', requiresAdmin: true }
  },
  { path: '/attractions', name: 'attractions', component: AttractionList, meta: { title: '游乐项目' } },
  { path: '/map', name: 'map', component: ParkMap, meta: { title: '园区地图' } },
  { path: '/tickets', name: 'tickets', component: TicketBooking, meta: { title: '门票预订' } },
  { path: '/games', name: 'games', component: ForestGames, meta: { title: '森林小游戏' } },
  { path: '/member', name: 'member', component: MemberCenter, meta: { title: '会员中心' } },
  { path: '/profile', name: 'profile', component: PersonalCenter, meta: { title: '个人中心' } },
  {
    path: '/:pathMatch(.*)*',
    name: 'notfound',
    component: NotFound,
    meta: { title: '页面走丢了' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  // 切换页面回到顶部（原手写路由没有这行为，属体验改进）
  scrollBehavior: () => ({ top: 0 })
})

/**
 * 全局前置守卫（对应 element-admin 的 permission.js）
 *   /admin       —— 无后台令牌 → 回登录页
 *   /admin/login —— 已登录管理员 → 直接进后台；普通权限用户 → 拦回首页并提示
 */
router.beforeEach((to) => {
  const store = useSessionStore()

  if (to.meta?.requiresAdmin) {
    if (!store.adminToken) return { name: 'adminLogin', replace: true }
  }

  if (to.name === 'adminLogin') {
    if (store.adminToken) return { name: 'admin', replace: true }
    if (store.me && Number(store.me.permission) !== 0) {
      store.showToast('当前账号为默认权限，无法进入后台管理系统')
      return { name: 'home', replace: true }
    }
  }

  return true
})

// 标题跟随路由
router.afterEach((to) => {
  if (to.meta?.title) document.title = `${to.meta.title} · 狗熊岭森林游乐园`
})

export default router
