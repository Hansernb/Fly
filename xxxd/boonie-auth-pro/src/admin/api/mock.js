/**
 * 中后台 Mock 数据层（仿 vue3-element-admin 的 api 分层）
 *
 * 统一接口：listModule / createRow / updateRow / deleteRow / toggleRow
 * 全部返回 Promise，模拟 220ms 网络延迟；数据落在 localStorage，刷新不丢。
 *
 * 后续接 Springboot：只要把本文件里的函数体换成 request('/admin/xxx') 即可，
 * 页面与组件无需改动（CrudView 只依赖这套函数签名）。
 */
import { MODULES } from '../config/modules.js'

const LS_PREFIX = 'boonie_admin_mock_'
const DELAY = 220

function sleep(ms = DELAY) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function keyOf(moduleKey) {
  return LS_PREFIX + moduleKey
}

function seedOf(moduleKey) {
  const mod = MODULES.find((m) => m.key === moduleKey)
  return mod ? JSON.parse(JSON.stringify(mod.seed)) : []
}

/** 读取模块数据；首次访问写入种子 */
function readRows(moduleKey) {
  try {
    const raw = localStorage.getItem(keyOf(moduleKey))
    if (raw) {
      const arr = JSON.parse(raw)
      if (Array.isArray(arr)) return arr
    }
  } catch {
    /* 存储不可用时降级为内存种子 */
  }
  const seed = seedOf(moduleKey)
  writeRows(moduleKey, seed)
  return seed
}

function writeRows(moduleKey, rows) {
  try {
    localStorage.setItem(keyOf(moduleKey), JSON.stringify(rows))
  } catch {
    /* ignore */
  }
}

function nextId(rows) {
  return rows.reduce((max, r) => Math.max(max, Number(r.id) || 0), 0) + 1
}

/**
 * 分页列表
 * @param {string} moduleKey
 * @param {{page:number, pageSize:number, keyword?:string}} params
 * @returns {Promise<{list:Array, total:number}>}
 */
export async function listModule(moduleKey, { page = 1, pageSize = 10, keyword = '' } = {}) {
  await sleep()
  let rows = readRows(moduleKey)
  const kw = String(keyword || '').trim().toLowerCase()
  if (kw) {
    rows = rows.filter((r) =>
      Object.values(r).some((v) => String(v ?? '').toLowerCase().includes(kw))
    )
  }
  const total = rows.length
  const start = (page - 1) * pageSize
  return { list: rows.slice(start, start + pageSize), total }
}

/** 新增；自动生成 id 与创建时间 */
export async function createRow(moduleKey, form) {
  await sleep()
  const rows = readRows(moduleKey)
  const row = { ...form, id: nextId(rows) }
  if (!row.createdAt) row.createdAt = new Date().toISOString().slice(0, 10)
  rows.unshift(row)
  writeRows(moduleKey, rows)
  return row
}

/** 编辑 */
export async function updateRow(moduleKey, id, form) {
  await sleep()
  const rows = readRows(moduleKey)
  const idx = rows.findIndex((r) => r.id === id)
  if (idx < 0) throw new Error('记录不存在')
  rows[idx] = { ...rows[idx], ...form, id }
  writeRows(moduleKey, rows)
  return rows[idx]
}

/** 删除（支持批量） */
export async function deleteRow(moduleKey, ids) {
  await sleep()
  const arr = Array.isArray(ids) ? ids : [ids]
  const rows = readRows(moduleKey).filter((r) => !arr.includes(r.id))
  writeRows(moduleKey, rows)
  return { removed: arr.length }
}

/** 切换状态（表格行内 switch）：在「上架/下架」「启用/禁用」「状态值」间循环 */
export async function toggleRow(moduleKey, id) {
  await sleep(120)
  const rows = readRows(moduleKey)
  const row = rows.find((r) => r.id === id)
  if (!row) throw new Error('记录不存在')
  if ('enabled' in row) row.enabled = !row.enabled
  else if ('status' in row) {
    const opts = ['上架', '下架', '待审核', '已通过', '已驳回', '待使用', '已使用', '已取消']
    const i = opts.indexOf(row.status)
    row.status = opts[(i + 1) % opts.length]
  }
  writeRows(moduleKey, rows)
  return row
}

/** 重置某模块为种子数据（调试用） */
export async function resetModule(moduleKey) {
  await sleep(120)
  const seed = seedOf(moduleKey)
  writeRows(moduleKey, seed)
  return seed
}
