import { request } from '../utils/request'

/**
 * 内容类接口（首页 / 详情页用）
 *
 * 迁移说明：原来这些路径以 fetch('/api/xxx') 的形式散落在 ParkHome.vue 里，
 * 现统一收口到本文件。函数签名保留 token 可选参数，便于逐步替换。
 */

/** 游乐项目列表（含各自评价） */
export const getAttractions = (token) => request('/attractions', { token })

/** 提交游客评价（默认进入待审核，审核通过后才在前台展示） */
export const postReview = (payload, token) => request('/reviews', { method: 'POST', body: payload, token })

/** 首页轮播图（后台可维护） */
export const getBanners = () => request('/banners')

/** 顶部滚动公告（后台可维护） */
export const getNotices = () => request('/notices')

/** 门票列表（MySQL） */
export const getTickets = () => request('/tickets')

/** 栖息地 */
export const getHabitats = () => request('/habitats')

/** 演出场次 */
export const getShows = () => request('/shows')

/* ===================== 账号 / 头像 ===================== */

/** 当前登录用户（用于校验 token 是否仍有效） */
export const getMe = (token) => request('/me', { token })

/** 上传头像文件（FormData） */
export const uploadAvatar = (formData, token) =>
  request('/avatar/upload', { method: 'POST', body: formData, token })

/** 更新当前用户头像地址 */
export const updateMyAvatar = (avatar, token) => request('/me/avatar', { method: 'PUT', body: { avatar }, token })
