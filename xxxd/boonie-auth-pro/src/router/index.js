import { createRouter, createWebHistory } from 'vue-router'
import { useSessionStore } from '../stores/session'

/**
 * 路由表
 *
 * 2026-11-27 架构升级：App.vue 已从 v-if 链改为真正的 <router-view>，
 * 每条路由指向 src/views/ 下的薄适配层（承接原 App.vue 的 props 传递与 emit 导航），
 * 并改用 () => import() 懒加载分包，获得按需加载收益。路径与旧版完全一致。
 */
const routes = [
  { path: '/', name: 'home', component: () => import('../views/HomeView.vue'), meta: { title: '狗熊岭森林游乐园' } },
  { path: '/admin/login', name: 'adminLogin', component: () => import('../views/AdminLoginView.vue'), meta: { title: '后台登录' } },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('../admin/layout/AdminLayout.vue'),
    redirect: { name: 'adminModule', params: { module: 'projects' } },
    meta: { title: '后台管理', requiresAdmin: true },
    children: [
      {
        path: ':module',
        name: 'adminModule',
        component: () => import('../admin/views/CrudView.vue'),
        meta: { title: '后台管理' }
      }
    ]
  },
  { path: '/attractions', name: 'attractions', component: () => import('../views/AttractionsView.vue'), meta: { title: '游乐项目' } },
  { path: '/map', name: 'map', component: () => import('../views/MapView.vue'), meta: { title: '园区地图' } },
  { path: '/tickets', name: 'tickets', component: () => import('../views/TicketsView.vue'), meta: { title: '门票预订' } },
  { path: '/games', name: 'games', component: () => import('../views/GamesView.vue'), meta: { title: '森林小游戏' } },
  { path: '/member', name: 'member', component: () => import('../views/MemberView.vue'), meta: { title: '会员中心' } },
  { path: '/profile', name: 'profile', component: () => import('../views/ProfileView.vue'), meta: { title: '个人中心' } },
  {
    path: '/:pathMatch(.*)*',
    name: 'notfound',
    component: () => import('../views/NotFoundView.vue'),
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
