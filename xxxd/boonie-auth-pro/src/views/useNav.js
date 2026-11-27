/**
 * 路由导航 composable（router-view 重构专用）
 *
 * 原 App.vue 用 v-if 链渲染组件，子组件通过 emit 把导航意图交回 App.vue，
 * 由 App.vue 的 go()/goHome()/enterXxx() 统一 router.push。
 * 重构为 <router-view> 后，各 view 适配层用本 composable 承接同样的映射，
 * 保证「返回优先后退」等交互与原逻辑逐字一致（UI 零变化）。
 */
import { useRouter } from 'vue-router'
import { useSessionStore } from '../stores/session'

/** 打开项目详情：回首页并广播事件（原 App.vue openAttractionDetail 逻辑） */
export function useNav() {
  const router = useRouter()
  const store = useSessionStore()

  /** 统一跳转 */
  function go(path) {
    router.push(path)
  }

  /** 各板块「返回」：优先浏览器后退，无历史才兜底回首页 */
  function goHome() {
    if (window.history.state?.back) {
      router.back()
      return
    }
    router.replace('/')
  }

  /** 带票种预选跳订票页 */
  function enterTickets(ticketId) {
    const id = typeof ticketId === 'string' ? ticketId : ''
    router.push(id ? { path: '/tickets', query: { ticketId: id } } : { path: '/tickets' })
  }

  /** 进入会员中心（订单 / 积分明细的唯一真实数据源） */
  function enterMember() {
    router.push('/member')
  }

  /** 个人中心点收藏/历史项目 → 回首页并广播打开详情弹窗 */
  function openAttractionDetail(id) {
    router.push('/')
    setTimeout(() => {
      window.dispatchEvent(new CustomEvent('boonie_open_attraction', { detail: { id } }))
    }, 400)
  }

  /** 首页齿轮 → 后台登录（仅管理员可见，仍做权限校验） */
  function enterAdminLogin() {
    if (!store.isAdmin) {
      store.showToast('当前账号为默认权限，无法进入后台管理系统')
      return
    }
    router.push('/admin/login')
  }

  /** 后台登录成功 → 校验管理员后进后台 */
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

  /** 后台退出 → 回首页（保留前台会话） */
  function exitAdmin() {
    store.exitAdmin()
    router.replace('/')
  }

  return {
    router,
    store,
    go,
    goHome,
    enterTickets,
    enterMember,
    openAttractionDetail,
    enterAdminLogin,
    onAdminLogin,
    exitAdmin
  }
}
