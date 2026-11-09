/**
 * 会员积分规则（与后端 MemberService 保持一致）：
 *   获取：实付 1 元 = 1 积分（向下取整）
 *   抵扣：1 积分 = 0.3 元，单笔最多抵扣订单金额的 50%
 *   等级：累计积分 <500 普通 / 500–1999 白银 / ≥2000 黄金
 */
export const POINT_RULES = { pointsPerYuan: 1, yuanPerPoint: 0.3, maxRatio: 0.5 }

export const LEVELS = [
  { name: '普通会员', at: 0, icon: '🐻', color: '#8a7a63' },
  { name: '白银会员', at: 500, icon: '🥈', color: '#7b8794' },
  { name: '黄金会员', at: 2000, icon: '👑', color: '#a9740a' }
]

export function levelOf(cumPoints = 0) {
  if (cumPoints >= 2000) return '黄金会员'
  if (cumPoints >= 500) return '白银会员'
  return '普通会员'
}

export function levelMeta(name) {
  return LEVELS.find((l) => l.name === name) || LEVELS[0]
}

/** 本单最多可抵扣多少积分：余额 与「金额 × 50% ÷ 0.3」取小 */
export function maxDeductPoints(points = 0, amount = 0) {
  const p = Number(points) || 0
  const a = Number(amount) || 0
  if (p <= 0 || a <= 0) return 0
  const byRatio = Math.floor((a * POINT_RULES.maxRatio) / POINT_RULES.yuanPerPoint + 1e-6)
  return Math.max(0, Math.min(p, byRatio))
}

export function discountOf(points = 0) {
  return Math.round((Number(points) || 0) * POINT_RULES.yuanPerPoint * 100) / 100
}

export function earnedOf(paid = 0) {
  return Math.floor(Math.max(0, Number(paid) || 0))
}

/** 补齐等级图标 / 进度等派生字段，供个人中心与结算页共用 */
export function decorate(member) {
  if (!member) return null
  const cum = Number(member.cumPoints) || 0
  const lv = levelOf(cum)
  const meta = levelMeta(lv)
  const nextAt = cum >= 2000 ? 2000 : cum >= 500 ? 2000 : 500
  return {
    ...member,
    totalPoints: Number(member.totalPoints) || 0,
    cumPoints: cum,
    levelName: lv,
    levelIcon: meta.icon,
    levelColor: meta.color,
    nextLevelName: cum >= 2000 ? '已达最高等级' : levelOf(nextAt),
    nextNeed: Math.max(0, nextAt - cum),
    progress: cum >= 2000 ? 100 : Math.round(((cum - meta.at) * 100) / Math.max(1, nextAt - meta.at)),
    createdAt: member.createdAt || ''
  }
}

/* ============ 本地兜底：后端不可达时也能演示会员积分流程 ============ */
const LS_KEY = 'boonie_member_v1'
export const MEMBER_PHONE_KEY = 'boonie_member_phone'

function readAll() {
  try {
    return JSON.parse(localStorage.getItem(LS_KEY) || '{}')
  } catch {
    return {}
  }
}

function writeAll(all) {
  try {
    localStorage.setItem(LS_KEY, JSON.stringify(all))
  } catch {
    /* 本地存储不可用时静默降级 */
  }
}

export function loadLocalMember(phone) {
  const all = readAll()
  return decorate(all[phone] || null)
}

/** 本地记一笔积分变动（正数获取、负数抵扣） */
export function localAddPoints(phone, { type, points, orderNo = '', remark = '', nickname = '' }) {
  if (!phone) return null
  const all = readAll()
  const now = new Date().toLocaleString('zh-CN', { hour12: false })
  let m = all[phone]
  if (!m) {
    m = {
      phone,
      nickname: nickname || '熊粉' + phone.slice(-4),
      avatar: '🐻',
      totalPoints: 0,
      cumPoints: 0,
      levelName: '普通会员',
      createdAt: now,
      logs: []
    }
  }
  if (nickname && (!m.nickname || m.nickname.startsWith('熊粉'))) m.nickname = nickname
  m.totalPoints = Math.max(0, (m.totalPoints || 0) + points)
  if (points > 0) m.cumPoints = (m.cumPoints || 0) + points
  m.levelName = levelOf(m.cumPoints || 0)
  m.logs = [{ type, points, balance: m.totalPoints, orderNo, remark, createdAt: now }, ...(m.logs || [])].slice(0, 60)
  all[phone] = m
  writeAll(all)
  return decorate(m)
}
