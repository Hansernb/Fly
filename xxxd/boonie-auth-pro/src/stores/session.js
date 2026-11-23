import { defineStore } from 'pinia'

/**
 * 会话状态（前台 session / 后台 adminSession / 当前用户 me）
 *
 * 迁移说明：
 *  - 原来这些状态散落在 App.vue 的 ref + localStorage 里；
 *  - 现在收敛到 Pinia，localStorage 的 key 保持完全不变（boonie_token / boonie_admin_token / boonie_user），
 *    因此老的登录态在升级后依然有效，无需用户重新登录。
 */
const TOKEN_KEY = 'boonie_token'
const ADMIN_TOKEN_KEY = 'boonie_admin_token'
const USER_KEY = 'boonie_user'

function readJSON(key) {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export const useSessionStore = defineStore('session', {
  state: () => ({
    /** 前台会话 { user, token } */
    session: null,
    /** 后台会话 { user, token } */
    adminSession: null,
    /** 当前用户（含 permission：0=管理员，1=默认权限），用于前端权限判断 */
    me: null,
    /** 全局轻提示文案（权限拦截等） */
    toast: ''
  }),

  getters: {
    /** 是否管理员（仅用于前端显隐；真正校验以后端 403 为准） */
    isAdmin: (s) => Number(s.me?.permission) === 0,
    /** 前台是否已登录 */
    isLogin: (s) => !!s.session,
    /** 后台令牌 */
    adminToken: (s) => s.adminSession?.token || localStorage.getItem(ADMIN_TOKEN_KEY) || '',
    /** 前台令牌 */
    token: (s) => s.session?.token || localStorage.getItem(TOKEN_KEY) || ''
  },

  actions: {
    /** 启动时从 localStorage 恢复会话（老数据兼容） */
    restore() {
      this.me = readJSON(USER_KEY)
      const user = this.me
      let token = localStorage.getItem(TOKEN_KEY)
      let adminToken = localStorage.getItem(ADMIN_TOKEN_KEY)

      // 无用户信息时令牌无效（避免「有 token 无 user」的半残态导致页面取 user 报错）
      if (!user) {
        token = null
        adminToken = null
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem(ADMIN_TOKEN_KEY)
      }

      // 前台：有 token + user 就视为已登录（刷新不再掉登录态）
      if (user && token) this.session = { user, token }
      // 后台：修复原实现「刷新 /admin 时 adminSession 为 null」导致的空指针
      if (user && adminToken) this.adminSession = { user, token: adminToken }
    },

    /** 保存当前用户到本地（与原 saveMe 行为一致） */
    saveMe(user) {
      this.me = user || null
      if (user) localStorage.setItem(USER_KEY, JSON.stringify(user))
      else localStorage.removeItem(USER_KEY)
    },

    /** 前台登录成功 */
    setSession(payload) {
      this.session = payload
      if (payload?.token) localStorage.setItem(TOKEN_KEY, payload.token)
      this.saveMe(payload?.user)
    },

    /** 后台登录成功 */
    setAdminSession(payload) {
      this.adminSession = payload
      if (payload?.token) localStorage.setItem(ADMIN_TOKEN_KEY, payload.token)
      this.saveMe(payload?.user)
    },

    /** 退出后台，保留前台会话 */
    exitAdmin() {
      localStorage.removeItem(ADMIN_TOKEN_KEY)
      this.adminSession = null
    },

    /** 退出登录，清空全部会话 */
    logout() {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(ADMIN_TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      this.session = null
      this.adminSession = null
      this.me = null
    },

    /** 显示轻提示（自动 2.8s 消失，计时器放在 action 内避免组件各自管理） */
    showToast(msg) {
      this.toast = msg
      clearTimeout(this._toastTimer)
      this._toastTimer = setTimeout(() => (this.toast = ''), 2800)
    }
  }
})
