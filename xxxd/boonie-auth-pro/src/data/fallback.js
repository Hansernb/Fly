/**
 * 首页兜底常量（后端不可达时保证页面仍有内容）
 *
 * 迁移说明：这些常量原来定义在 ParkHome.vue 内部，
 * 现在抽出来供 content store 与组件共用，避免两处各存一份导致不一致。
 */

/** 项目/栖息地缺省配图 */
export const FALLBACK_IMG = '/img/background.png'

/** 轮播兜底（后台 /api/banners 为空或不可达时使用） */
export const FALLBACK_BANNERS = [
  { img: '/img/ferris.jpeg', tag: '🎡 招牌项目', title: '森林摩天轮', desc: '360° 俯瞰整片狗熊岭林海，黄昏时最浪漫。' },
  { img: '/img/coaster.gif', tag: '🎢 尖叫首选', title: '林海过山车', desc: '穿梭树冠间的极速轨道，熊二都直呼过瘾！' },
  { img: '/img/rafting.jpeg', tag: '🛶 清凉一夏', title: '激流勇进漂流', desc: '顺溪而下穿越原始森林，全程水花四溅。' },
  { img: '/img/train.jpeg', tag: '🚂 全家同游', title: '小熊观光火车', desc: '慢悠悠绕园一圈，熊大熊二沿途陪你打招呼。' }
]

/** 顶部滚动公告兜底 */
export const FALLBACK_NOTICES = [
  '🕘 开园时间：09:00 - 21:00（20:30 停止入园）',
  '☀️ 今日天气：晴 12~22°C，紫外线偏强，请注意防晒',
  '📌 临时通知：「树洞过山车」11月23日例行检修，暂停开放一天，敬请谅解'
]
