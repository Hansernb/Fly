/**
 * 统一请求封装（参考 vue3-element-admin 的 utils/request 思路）
 *
 * 目的：消灭各组件里散落的 fetch —— 统一 baseURL、Token 注入、JSON 解析、错误消息。
 * 约定：后端统一返回 { ok, data } 或 { message } 结构；HTTP 非 2xx 时抛 Error 并带 status。
 */
const BASE = '/api'

/**
 * @param {string} path    /attractions 这样的相对路径（会自动拼上 /api）
 * @param {object} options
 * @param {string} [options.method='GET']
 * @param {any}    [options.body]        普通对象自动 JSON 化；FormData 直接透传（文件上传）
 * @param {string} [options.token]       有值时注入 Authorization: Bearer <token>
 * @param {object} [options.headers]
 * @param {AbortSignal} [options.signal]
 */
export async function request(path, { method = 'GET', body, token, headers = {}, signal } = {}) {
  const opts = { method, headers: { ...headers }, signal }

  if (token) opts.headers.Authorization = `Bearer ${token}`

  if (body !== undefined && body !== null) {
    if (typeof FormData !== 'undefined' && body instanceof FormData) {
      opts.body = body // 交给浏览器自动设置 multipart 边界
    } else {
      opts.headers['Content-Type'] = 'application/json'
      opts.body = JSON.stringify(body)
    }
  }

  const res = await fetch(BASE + path, opts)

  // 兼容空响应体（如 204）与非 JSON 响应
  const text = await res.text()
  let json = null
  if (text) {
    try {
      json = JSON.parse(text)
    } catch {
      json = null
    }
  }

  if (!res.ok) {
    const err = new Error(json?.message || json?.error || `请求失败（HTTP ${res.status}）`)
    err.status = res.status
    err.data = json
    throw err
  }

  return json
}

export default request
