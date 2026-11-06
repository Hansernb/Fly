// 狗熊岭森林游乐园 —— 门票数据库（前端统一数据源）
//
// 首页「门票速览」与「在线订票」流程共用本文件，保证票种 / 价格 / 优惠一致。
//
// 数据来源优先级：
//   1) 后端 GET /api/tickets（MySQL boonie_ticket 表，后台可维护）
//   2) 接口不可达时降级为本文件内置的 FALLBACK 票库，保证页面始终有内容
//
// 字段说明：
//   id            票种编码（adult / child / senior / family / night，与库里 type_code 一致）
//   name          票种名称
//   emoji         图标
//   price         实际售价（元）
//   originalPrice 原价（元，用于展示优惠划线价；无优惠时等于 price）
//   discount      优惠标签（如「5 折」），无优惠为空串
//   tag           角标文案（标准 / 儿童优惠 / 长者优惠 / 热门 / 夜场）
//   hot           是否首页「热门」高亮
//   desc          一句话说明（订票流程用）
//   features      权益列表（首页门票速览用）
//   color         主题色
//
// 优惠规则：儿童票、老人票为成人票原价（¥199）的 5 折，即 ¥99。

import { reactive } from 'vue'

const FALLBACK = [
  {
    id: 'adult',
    name: '成人票',
    emoji: '🧑',
    price: 199,
    originalPrice: 199,
    discount: '',
    tag: '标准',
    hot: false,
    desc: '1.5m 以上游客全天畅玩',
    features: ['全天畅玩所有项目', '含 1 场剧场演出'],
    color: '#e4572e'
  },
  {
    id: 'child',
    name: '儿童票',
    emoji: '🧒',
    price: 99,
    originalPrice: 199,
    discount: '5 折',
    tag: '儿童优惠',
    hot: false,
    desc: '1.0m–1.5m 儿童 · 原价 ¥199 立减 100',
    features: ['1.0m–1.5m 儿童适用', '全天畅玩所有项目', '含 1 场剧场演出'],
    color: '#ffb703'
  },
  {
    id: 'senior',
    name: '老人票',
    emoji: '👴',
    price: 99,
    originalPrice: 199,
    discount: '5 折',
    tag: '长者优惠',
    hot: false,
    desc: '65 周岁及以上长者 · 原价 ¥199 立减 100',
    features: ['65 周岁及以上长者适用', '全天畅玩所有项目', '含 1 场剧场演出'],
    color: '#7c5cbf'
  },
  {
    id: 'family',
    name: '亲子套票',
    emoji: '👨‍👩‍👧',
    price: 399,
    originalPrice: 497,
    discount: '套票',
    tag: '热门',
    hot: true,
    desc: '2 大 1 小 全天畅玩 · 比单买省 ¥98',
    features: ['2 大 1 小全天畅玩', '含全部演出', '赠蜂蜜爆米花一份'],
    color: '#2f7d4f'
  },
  {
    id: 'night',
    name: '夜场票',
    emoji: '🌙',
    price: 99,
    originalPrice: 99,
    discount: '',
    tag: '夜场',
    hot: false,
    desc: '17:00 后入园 · 含星光巡游',
    features: ['17:00 后入园', '含星光巡游观赏位'],
    color: '#4caf6d'
  }
]

/**
 * 统一票库（响应式）：组件的 `v-for` / `computed` 会跟着接口返回自动更新，
 * 所以拿到数据库数据后只需 splice 替换内容，无需改动调用方。
 */
export const TICKETS = reactive(FALLBACK.map((t) => ({ ...t })))

/** 是否已从数据库加载成功 */
export const ticketsSource = reactive({ fromApi: false, loading: false, error: '' })

/** 后端票库字段 -> 前端票库字段 */
function normalize(t) {
  const price = Number(t.price) || 0
  const original = t.originalPrice == null ? price : Number(t.originalPrice)
  return {
    id: String(t.id || t.code || ''),
    dbId: t.dbId,
    name: t.name,
    emoji: t.emoji || '🎫',
    price,
    originalPrice: original > 0 ? original : price,
    discount: t.discount || '',
    tag: t.tag || '标准',
    hot: !!t.hot,
    desc: t.desc || '',
    features: Array.isArray(t.features) ? t.features.slice() : [],
    color: t.color || '#2f7d4f'
  }
}

let pending = null

/**
 * 从 MySQL 加载门票（GET /api/tickets）。
 * 失败时保留内置票库，并把错误信息写进 ticketsSource.error 供页面提示。
 */
export function loadTickets(force = false) {
  if (pending && !force) return pending
  ticketsSource.loading = true
  pending = (async () => {
    try {
      const res = await fetch('/api/tickets')
      if (!res.ok) throw new Error('HTTP ' + res.status)
      const json = await res.json()
      const list = json && Array.isArray(json.data) ? json.data : []
      if (!list.length) throw new Error('票库为空')
      TICKETS.splice(0, TICKETS.length, ...list.map(normalize))
      ticketsSource.fromApi = true
      ticketsSource.error = ''
    } catch (e) {
      ticketsSource.fromApi = false
      ticketsSource.error = '门票数据库暂时连不上，已使用内置票库'
    } finally {
      ticketsSource.loading = false
    }
    return TICKETS
  })()
  return pending
}

/** 按 id 取票种 */
export function getTicket(id) {
  return TICKETS.find((t) => t.id === id) || null
}
