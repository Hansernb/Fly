<template>
  <div class="booking">
    <!-- 顶部栏：返回 + 标题 + 我的订单切换 -->
    <header class="bk-bar">
      <button class="bk-back" type="button" @click="$emit('back')">‹ 返回</button>
      <h1 class="bk-title">🎫 狗熊岭在线订票</h1>
      <button class="bk-orders-btn" type="button" @click="goOrders">
        🧾 我的订单
        <span v-if="orders.length" class="bk-orders-badge">{{ orders.length }}</span>
      </button>
    </header>

    <!-- ===================== 预订流程 ===================== -->
    <section v-if="mode === 'book'" class="bk-main">
      <!-- 步骤指示 -->
      <ol class="steps">
        <li
          v-for="(s, i) in steps"
          :key="s.key"
          class="step"
          :class="{ on: step === i + 1, done: step > i + 1 }"
        >
          <span class="step-dot">{{ step > i + 1 ? '✓' : i + 1 }}</span>
          <span class="step-label">{{ s.label }}</span>
        </li>
      </ol>

      <!-- 步骤卡片 -->
      <div class="bk-card reveal">
        <!-- 步骤 1：选票种 -->
        <div v-if="step === 1">
          <h2 class="bk-h2">① 选择票种</h2>
          <p class="bk-sub">请选择您要购买的门票类型</p>
          <div class="tk-grid">
            <button
              v-for="t in TICKETS"
              :key="t.id"
              type="button"
              class="tk"
              :class="{ on: selectedId === t.id, hot: t.tag === '热门' }"
              :style="{ '--tk': t.color }"
              @click="selectTicket(t.id)"
            >
              <span v-if="t.tag === '热门'" class="tk-hot">🔥 热门</span>
              <span v-else-if="t.discount" class="tk-disc">{{ t.discount }}</span>
              <span class="tk-emoji">{{ t.emoji }}</span>
              <span class="tk-name">{{ t.name }}</span>
              <span class="tk-price">
                ¥{{ t.price }}<small>/人</small>
                <s v-if="t.originalPrice > t.price" class="tk-origin">¥{{ t.originalPrice }}</s>
              </span>
              <span class="tk-desc">{{ t.desc }}</span>
            </button>
          </div>
        </div>

        <!-- 步骤 2：选日期 -->
        <div v-else-if="step === 2">
          <h2 class="bk-h2">② 选择游玩日期</h2>
          <p class="bk-sub">门票仅限所选日期当日使用，可提前 90 天预订</p>

          <!-- 未来 7 天快捷日期卡 -->
          <div class="day-strip">
            <button
              v-for="d in weekDays"
              :key="d.val"
              type="button"
              class="day-card"
              :class="{ on: visitDate === d.val }"
              @click="visitDate = d.val"
            >
              <span v-if="d.badge" class="day-badge">{{ d.badge }}</span>
              <span class="day-week">{{ d.week }}</span>
              <span class="day-num">{{ d.md }}</span>
            </button>
          </div>

          <!-- 其他日期 -->
          <div class="date-other">
            <span class="date-other-label">📆 其他日期</span>
            <input
              v-model="visitDate"
              class="date-input"
              type="date"
              :min="todayStr"
              :max="maxDateStr"
            />
          </div>

          <div class="date-selected">
            📅 已选：<b>{{ prettyDate }}</b>
            <span class="date-week">（{{ weekday }}）</span>
          </div>
        </div>

        <!-- 步骤 3：选数量 -->
        <div v-else-if="step === 3">
          <h2 class="bk-h2">③ 选择数量</h2>
          <p class="bk-sub">每张门票对应一位游客，单次最多购买 10 张</p>
          <div v-if="selectedTicket" class="qty-block">
            <div class="qty-tk">
              <span class="qty-emoji">{{ selectedTicket.emoji }}</span>
              <div>
                <div class="qty-name">{{ selectedTicket.name }}</div>
                <div class="qty-unit">
                  单价 ¥{{ selectedTicket.price }} / 人
                  <s v-if="selectedTicket.originalPrice > selectedTicket.price" class="qty-origin">¥{{ selectedTicket.originalPrice }}</s>
                </div>
              </div>
            </div>
            <div class="stepper">
              <button type="button" :disabled="quantity <= 1" @click="quantity--">−</button>
              <span class="stepper-num">{{ quantity }}</span>
              <button type="button" :disabled="quantity >= 10" @click="quantity++">＋</button>
            </div>
          </div>

          <div class="qty-quick">
            <button
              v-for="n in 5"
              :key="n"
              type="button"
              class="qty-chip"
              :class="{ on: quantity === n }"
              @click="quantity = n"
            >
              {{ n }} 张
            </button>
          </div>

          <div v-if="selectedTicket" class="qty-subtotal">
            <span>小计：{{ quantity }} 张 × ¥{{ selectedTicket.price }}
              <em v-if="saved > 0" class="qty-saved">已省 ¥{{ saved }}</em>
            </span>
            <b>¥{{ total }}</b>
          </div>
        </div>

        <!-- 步骤 4：确认 -->
        <div v-else-if="step === 4">
          <h2 class="bk-h2">④ 确认订单</h2>
          <p class="bk-sub">请核对信息并填写取票人</p>
          <div v-if="selectedTicket" class="confirm">
            <div class="confirm-row">
              <span class="confirm-k">票种</span>
              <span class="confirm-v">{{ selectedTicket.emoji }} {{ selectedTicket.name }}</span>
            </div>
            <div class="confirm-row">
              <span class="confirm-k">游玩日期</span>
              <span class="confirm-v">{{ prettyDate }}（{{ weekday }}）</span>
            </div>
            <div class="confirm-row">
              <span class="confirm-k">购买数量</span>
              <span class="confirm-v">{{ quantity }} 张</span>
            </div>
            <div class="confirm-row">
              <span class="confirm-k">单价</span>
              <span class="confirm-v">
                ¥{{ selectedTicket.price }}
                <s v-if="selectedTicket.originalPrice > selectedTicket.price" class="confirm-origin">¥{{ selectedTicket.originalPrice }}</s>
              </span>
            </div>
            <div v-if="saved > 0" class="confirm-row">
              <span class="confirm-k">已享优惠</span>
              <span class="confirm-v saved">−¥{{ saved }}</span>
            </div>
            <div class="confirm-row total">
              <span class="confirm-k">应付总价</span>
              <span class="confirm-v">¥{{ total }}</span>
            </div>
            <p class="confirm-note">🐻 提交后将生成电子订单，凭订单号在园区闸机核销入园。</p>
          </div>

          <!-- 取票人信息 -->
          <div class="buyer">
            <h3 class="buyer-title">取票人信息</h3>
            <div class="field">
              <label for="bk-name">姓名</label>
              <input
                id="bk-name"
                v-model.trim="buyerName"
                type="text"
                maxlength="12"
                placeholder="请输入取票人姓名"
              />
            </div>
            <div class="field">
              <label for="bk-phone">手机号</label>
              <input
                id="bk-phone"
                v-model.trim="buyerPhone"
                type="tel"
                maxlength="11"
                inputmode="numeric"
                placeholder="用于接收电子门票短信"
              />
            </div>
            <p v-if="buyerError" class="field-err">⚠️ {{ buyerError }}</p>
            <label class="agree">
              <input v-model="agreed" type="checkbox" />
              <span>我已阅读并同意《门票预订须知》，知晓门票当日有效、过期不退。</span>
            </label>
          </div>
        </div>

        <!-- 底部导航 -->
        <div class="bk-foot">
          <button v-if="step > 1" class="bk-btn ghost" type="button" @click="prev">‹ 上一步</button>
          <span class="bk-foot-spacer"></span>
          <button
            v-if="step < 4"
            class="bk-btn primary"
            type="button"
            :disabled="!canNext"
            @click="next"
          >
            下一步 ›
          </button>
          <button
            v-else
            class="bk-btn primary submit"
            type="button"
            :disabled="!canSubmit || submitting"
            @click="submit"
          >
            {{ submitting ? '⏳ 正在提交…' : `✅ 提交订单 · ¥${total}` }}
          </button>
        </div>
      </div>

      <!-- 实时价格条 -->
      <div class="price-bar">
        <div class="price-info">
          <span class="price-tk">{{ selectedTicket ? selectedTicket.emoji + ' ' + selectedTicket.name : '尚未选择票种' }}</span>
          <span v-if="selectedTicket" class="price-meta">
            {{ quantity }} 张 × ¥{{ selectedTicket.price }}
            <s v-if="selectedTicket.originalPrice > selectedTicket.price" class="price-origin">¥{{ selectedTicket.originalPrice }}</s>
            <em v-if="saved > 0" class="price-saved">已省 ¥{{ saved }}</em>
          </span>
        </div>
        <div class="price-total">¥{{ total }}</div>
      </div>
    </section>

    <!-- ===================== 订单提交成功 ===================== -->
    <section v-else-if="mode === 'success'" class="bk-main">
      <div class="success">
        <div class="success-badge">✅</div>
        <h2>预订成功！</h2>
        <p class="success-sub">您的电子门票已生成，请妥善保管订单号</p>
        <div class="success-order">
          <div class="so-row"><span>订单号</span><b class="so-no">{{ successOrder.id }}</b></div>
          <div class="so-row"><span>票种</span><b>{{ successOrder.emoji }} {{ successOrder.ticketName }}</b></div>
          <div class="so-row"><span>游玩日期</span><b>{{ successOrder.date }}</b></div>
          <div class="so-row"><span>数量</span><b>{{ successOrder.quantity }} 张</b></div>
          <div class="so-row"><span>取票人</span><b>{{ successOrder.buyerName }} · {{ successOrder.buyerPhone }}</b></div>
          <div class="so-row total"><span>实付</span><b>¥{{ successOrder.total }}</b></div>
        </div>
        <div class="success-actions">
          <button class="bk-btn primary" type="button" @click="goOrders">🧾 查看我的订单</button>
          <button class="bk-btn ghost" type="button" @click="goBook">🎫 继续预订</button>
        </div>
      </div>
    </section>

    <!-- ===================== 我的订单 ===================== -->
    <section v-else class="bk-main">
      <div class="orders-head">
        <h2 class="bk-h2">
          🧾 我的订单
          <span class="order-src" :class="ordersFromApi ? 'cloud' : 'local'">
            {{ ordersLoading ? '⏳ 同步中…' : ordersFromApi ? '☁️ 云端订单' : '💾 本地订单' }}
          </span>
        </h2>
        <button class="bk-btn ghost sm" type="button" @click="goBook">＋ 去订票</button>
      </div>

      <div v-if="orders.length" class="order-list">
        <article
          v-for="o in orders"
          :key="o.id"
          class="order"
          :class="{ cancelled: o.status === '已取消' }"
        >
          <div class="order-top">
            <span class="order-tk">{{ o.emoji }} {{ o.ticketName }}</span>
            <span class="order-status" :class="o.status === '已取消' ? 'st-cancel' : 'st-ok'">
              {{ o.status }}
            </span>
          </div>
          <div class="order-no">订单号：<b>{{ o.id }}</b><span class="order-buyer">取票人：{{ o.buyerName || '—' }}</span></div>
          <div class="order-grid">
            <div><span>游玩日期</span><b>{{ o.date }}</b></div>
            <div><span>数量</span><b>{{ o.quantity }} 张</b></div>
            <div><span>下单时间</span><b>{{ o.createdAt }}</b></div>
          </div>
          <div class="order-foot">
            <span class="order-total">合计 <b>¥{{ o.total }}</b></span>
            <div class="order-ops">
              <button
                v-if="o.status !== '已取消'"
                class="op-btn danger"
                type="button"
                @click="cancelOrder(o.id)"
              >
                取消订单
              </button>
              <button class="op-btn" type="button" @click="reorder(o)">再次预订</button>
            </div>
          </div>
        </article>
      </div>
      <div v-else class="order-empty">
        🍃 还没有订单，去挑一张门票开启森林之旅吧～
        <div class="order-empty-cta">
          <button class="bk-btn primary" type="button" @click="goBook">🎫 立即订票</button>
        </div>
      </div>
    </section>

    <!-- 轻提示 -->
    <transition name="toast">
      <div v-if="toast" class="bk-toast">{{ toast }}</div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { TICKETS, loadTickets } from '../data/tickets.js'

const props = defineProps({
  user: { type: Object, default: null },
  token: { type: String, default: '' },
  initialTicketId: { type: String, default: '' }
})
const emit = defineEmits(['back'])

const steps = [
  { key: 'type', label: '选票种' },
  { key: 'date', label: '选日期' },
  { key: 'qty', label: '选数量' },
  { key: 'confirm', label: '确认' }
]

/* ===================== 状态 ===================== */
const mode = ref('book') // book | success | orders
const step = ref(1)
const selectedId = ref('')
const visitDate = ref('')
const quantity = ref(1)
const buyerName = ref('')
const buyerPhone = ref('')
const agreed = ref(false)
const orders = ref([])
const successOrder = ref(null)
const toast = ref('')
let toastTimer = null
const submitting = ref(false)
const ordersFromApi = ref(false)
const ordersLoading = ref(false)

const LS_KEY = 'boonie_ticket_orders_v1'
const WEEK = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

/** 带令牌的请求头（订单接口需要登录态） */
function authHeaders(json = false) {
  const h = {}
  if (props.token) h.Authorization = 'Bearer ' + props.token
  if (json) h['Content-Type'] = 'application/json'
  return h
}

/* ===================== 日期工具 ===================== */
function fmt(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}
const today = new Date()
const todayStr = fmt(today)
const maxDate = new Date(today.getTime() + 90 * 86400000)
const maxDateStr = fmt(maxDate)

// 默认游玩日期为今天
visitDate.value = todayStr

// 未来 7 天：日期卡（含 今天 / 明天 角标）
const weekDays = computed(() => {
  const arr = []
  for (let i = 0; i < 7; i++) {
    const d = new Date(today.getTime() + i * 86400000)
    arr.push({
      val: fmt(d),
      week: i === 0 ? '今天' : i === 1 ? '明天' : WEEK[d.getDay()],
      md: `${d.getMonth() + 1}/${d.getDate()}`,
      badge: i === 0 ? '今天' : ''
    })
  }
  return arr
})

const prettyDate = computed(() => {
  if (!visitDate.value) return '—'
  const [y, m, d] = visitDate.value.split('-')
  return `${y}年${Number(m)}月${Number(d)}日`
})
const weekday = computed(() => {
  if (!visitDate.value) return ''
  return WEEK[new Date(visitDate.value).getDay()]
})

/* ===================== 计算 ===================== */
const selectedTicket = computed(() => TICKETS.find((t) => t.id === selectedId.value) || null)
const total = computed(() => (selectedTicket.value ? selectedTicket.value.price * quantity.value : 0))
// 优惠金额（原价 - 实付）× 数量；无优惠为 0
const saved = computed(() => {
  const t = selectedTicket.value
  if (!t) return 0
  return Math.max(0, t.originalPrice - t.price) * quantity.value
})
const canNext = computed(() => {
  if (step.value === 1) return !!selectedId.value
  if (step.value === 2) return !!visitDate.value
  if (step.value === 3) return quantity.value >= 1
  return true
})
// 手机号校验：11 位、1 开头
const phoneOk = computed(() => /^1\d{10}$/.test(buyerPhone.value))
const buyerError = computed(() => {
  if (buyerPhone.value && !phoneOk.value) return '请输入 11 位有效手机号'
  return ''
})
// 确认步骤可提交：票种 + 日期 + 姓名 + 手机号 + 已同意条款
const canSubmit = computed(
  () =>
    !!selectedTicket.value &&
    !!visitDate.value &&
    buyerName.value.length > 0 &&
    phoneOk.value &&
    agreed.value
)

/* ===================== 交互 ===================== */
function selectTicket(id) {
  selectedId.value = id
}
function next() {
  if (!canNext.value) return
  if (step.value < 4) step.value += 1
}
function prev() {
  if (step.value > 1) step.value -= 1
}

function genOrderNo() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  const rand = String(Math.floor(Math.random() * 9000) + 1000)
  return `BK${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}${pad(d.getHours())}${pad(d.getMinutes())}${rand}`
}

/** 后端订单字段 -> 前端展示字段 */
function normalizeOrder(o) {
  const price = Number(o.price) || 0
  const original = o.originalPrice == null ? price : Number(o.originalPrice)
  return {
    id: o.id,
    ticketId: o.ticketId,
    ticketName: o.ticketName,
    emoji: o.emoji || '🎫',
    price,
    originalPrice: original > 0 ? original : price,
    date: o.date,
    quantity: Number(o.quantity) || 1,
    total: Number(o.total) || 0,
    saved: Number(o.saved) || 0,
    buyerName: o.buyerName || '',
    buyerPhone: o.buyerPhone || '',
    status: o.status || '待使用',
    createdAt: o.createdAt || '',
    user: o.user || ''
  }
}

function loadLocalOrders() {
  try {
    orders.value = JSON.parse(localStorage.getItem(LS_KEY) || '[]')
  } catch {
    orders.value = []
  }
  ordersFromApi.value = false
}

function cacheOrders(orders_) {
  try {
    localStorage.setItem(LS_KEY, JSON.stringify(orders_))
  } catch {
    /* 本地缓存失败不影响主流程 */
  }
}

/** 我的订单：优先 GET /api/orders（MySQL），未登录或后端不可达时降级本地缓存 */
async function loadOrders() {
  if (!props.token) {
    loadLocalOrders()
    return
  }
  ordersLoading.value = true
  try {
    const res = await fetch('/api/orders', { headers: authHeaders() })
    if (res.status === 401) throw new Error('unauthorized')
    if (!res.ok) throw new Error('HTTP ' + res.status)
    const json = await res.json()
    const list = json && Array.isArray(json.data) ? json.data : []
    orders.value = list.map(normalizeOrder)
    ordersFromApi.value = true
    cacheOrders(orders.value)
  } catch {
    loadLocalOrders()
  } finally {
    ordersLoading.value = false
  }
}

/** 后端不可用时的本地兜底下单，保证演示流程不中断 */
function createLocalOrder() {
  const order = {
    id: genOrderNo(),
    ticketId: selectedTicket.value.id,
    ticketName: selectedTicket.value.name,
    emoji: selectedTicket.value.emoji,
    price: selectedTicket.value.price,
    originalPrice: selectedTicket.value.originalPrice,
    date: visitDate.value,
    quantity: quantity.value,
    total: total.value,
    saved: saved.value,
    buyerName: buyerName.value,
    buyerPhone: buyerPhone.value,
    status: '待使用',
    createdAt: new Date().toLocaleString('zh-CN', { hour12: false }),
    user: props.user?.name || '游客'
  }
  orders.value.unshift(order)
  cacheOrders(orders.value)
  successOrder.value = order
  mode.value = 'success'
}

async function submit() {
  if (!canSubmit.value || submitting.value) return
  const ticket = selectedTicket.value
  if (!ticket) return

  // 未登录：继续走本地模式
  if (!props.token) {
    createLocalOrder()
    showToast('⚠️ 未登录，订单已暂存在本机')
    return
  }

  submitting.value = true
  try {
    const res = await fetch('/api/orders', {
      method: 'POST',
      headers: authHeaders(true),
      body: JSON.stringify({
        ticketId: ticket.id,
        quantity: quantity.value,
        visitDate: visitDate.value,
        buyerName: buyerName.value,
        buyerPhone: buyerPhone.value
      })
    })
    const json = await res.json().catch(() => ({}))
    if (!res.ok) {
      // 4xx 属于业务校验失败（票种下架 / 日期非法等），直接提示，不做本地兜底
      if (res.status >= 400 && res.status < 500) {
        showToast('⚠️ ' + (json.message || '下单失败，请检查填写内容'))
        return
      }
      throw new Error('HTTP ' + res.status)
    }
    const order = normalizeOrder(json.data)
    orders.value.unshift(order)
    cacheOrders(orders.value)
    ordersFromApi.value = true
    successOrder.value = order
    mode.value = 'success'
    showToast('🎫 订单已生成并写入数据库')
  } catch {
    createLocalOrder()
    showToast('⚠️ 数据库暂时连不上，订单已暂存在本机')
  } finally {
    submitting.value = false
  }
}

async function cancelOrder(id) {
  const local = orders.value.find((x) => x.id === id)
  if (!local) return

  if (props.token && ordersFromApi.value) {
    try {
      const res = await fetch(`/api/orders/${encodeURIComponent(id)}/cancel`, {
        method: 'POST',
        headers: authHeaders()
      })
      const json = await res.json().catch(() => ({}))
      if (!res.ok) {
        showToast('⚠️ ' + (json.message || '取消失败'))
        return
      }
      local.status = '已取消'
      cacheOrders(orders.value)
      showToast('订单已取消')
      return
    } catch {
      /* 网络异常 -> 走本地取消 */
    }
  }
  local.status = '已取消'
  cacheOrders(orders.value)
  showToast('订单已取消（本地）')
}
function reorder(o) {
  selectedId.value = o.ticketId
  visitDate.value = o.date
  quantity.value = o.quantity
  buyerName.value = o.buyerName || ''
  buyerPhone.value = o.buyerPhone || ''
  agreed.value = !!o.buyerName
  step.value = 1
  mode.value = 'book'
}
function goOrders() {
  loadOrders()
  mode.value = 'orders'
}
function goBook() {
  mode.value = 'book'
}

function showToast(msg) {
  toast.value = msg
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 2600)
}

onMounted(async () => {
  loadOrders()
  // 从首页某张票卡「立即预订」进入时，预选该票种并直接到选日期
  if (props.initialTicketId) {
    selectedId.value = props.initialTicketId
    step.value = 2
  }
  // 票种以数据库为准（失败自动沿用内置票库）
  await loadTickets()
  if (selectedId.value && !TICKETS.some((t) => t.id === selectedId.value)) {
    selectedId.value = ''
    step.value = 1
  }
})
</script>

<style scoped>
.booking {
  max-width: 880px;
  margin: 0 auto;
  padding: 16px 18px 40px;
}

/* 顶部栏 */
.bk-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}
.bk-back {
  border: 2px solid var(--wood-light);
  background: #fff;
  color: var(--forest-deep);
  font-size: 14px;
  font-weight: 700;
  padding: 8px 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s ease;
  flex-shrink: 0;
}
.bk-back:hover {
  background: var(--forest);
  color: #fff;
  border-color: var(--forest);
}
.bk-title {
  flex: 1;
  font-size: 20px;
  font-weight: 900;
  color: var(--forest-deep);
  margin: 0;
}
.bk-orders-btn {
  position: relative;
  border: 2px solid var(--honey);
  background: linear-gradient(180deg, #ffd23f, var(--honey));
  color: #5a3d00;
  font-size: 13.5px;
  font-weight: 800;
  padding: 8px 14px;
  border-radius: 10px;
  cursor: pointer;
  box-shadow: 0 4px 0 #e09b00;
  transition: transform 0.08s ease, box-shadow 0.08s ease;
  flex-shrink: 0;
}
.bk-orders-btn:active {
  transform: translateY(3px);
  box-shadow: 0 1px 0 #e09b00;
}
.bk-orders-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  border-radius: 9px;
  background: var(--danger);
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
  border: 2px solid #fff;
}

/* 步骤指示 */
.steps {
  display: flex;
  list-style: none;
  margin: 0 0 18px;
  padding: 0;
}
.step {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  position: relative;
}
.step:not(:last-child)::after {
  content: '';
  position: absolute;
  top: 15px;
  left: 60%;
  width: 80%;
  height: 3px;
  background: var(--wood-light);
  border-radius: 2px;
}
.step.done:not(:last-child)::after {
  background: var(--leaf);
}
.step-dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 800;
  background: #fff;
  border: 3px solid var(--wood-light);
  color: var(--muted);
  z-index: 1;
  transition: all 0.2s ease;
}
.step.on .step-dot {
  background: linear-gradient(180deg, #ffd23f, var(--honey));
  border-color: var(--honey);
  color: #5a3d00;
  box-shadow: 0 4px 10px rgba(224, 155, 0, 0.4);
}
.step.done .step-dot {
  background: var(--leaf);
  border-color: var(--leaf);
  color: #fff;
}
.step-label {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--muted);
}
.step.on .step-label {
  color: var(--forest-deep);
}

/* 卡片 */
.bk-card {
  background: #fffdf7;
  border: 3px solid var(--wood-light);
  border-radius: var(--radius);
  padding: 22px 22px 18px;
  box-shadow: 0 14px 34px rgba(47, 125, 79, 0.12);
}
.bk-h2 {
  font-size: 19px;
  font-weight: 900;
  color: var(--forest-deep);
  margin: 0 0 4px;
}
.bk-sub {
  font-size: 13px;
  color: var(--muted);
  margin: 0 0 18px;
}

/* 票种网格 */
.tk-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.tk {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 16px 10px 14px;
  border: 3px solid var(--wood-light);
  border-radius: 16px;
  background: #fff;
  cursor: pointer;
  text-align: center;
  transition: transform 0.14s ease, box-shadow 0.14s ease, border-color 0.14s ease;
}
.tk:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 22px rgba(47, 125, 79, 0.16);
}
.tk.on {
  border-color: var(--tk);
  box-shadow: 0 8px 20px color-mix(in srgb, var(--tk) 30%, transparent);
  background: color-mix(in srgb, var(--tk) 8%, #fff);
}
.tk-hot {
  position: absolute;
  top: -10px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--danger);
  color: #fff;
  font-size: 11px;
  font-weight: 800;
  padding: 2px 10px;
  border-radius: 999px;
  white-space: nowrap;
}
/* 优惠角标（儿童 / 老人 / 套票） */
.tk-disc {
  position: absolute;
  top: -10px;
  right: -8px;
  background: linear-gradient(180deg, #ff6b3d, var(--danger));
  color: #fff;
  font-size: 11px;
  font-weight: 800;
  padding: 2px 9px;
  border-radius: 999px;
  white-space: nowrap;
  box-shadow: 0 3px 8px rgba(228, 87, 46, 0.45);
}
.tk-emoji {
  font-size: 34px;
  line-height: 1;
}
.tk-name {
  font-size: 15px;
  font-weight: 800;
  color: var(--forest-deep);
}
.tk-price {
  font-size: 18px;
  font-weight: 900;
  color: var(--tk);
  display: flex;
  align-items: baseline;
  gap: 5px;
}
.tk-price small {
  font-size: 11px;
  font-weight: 600;
  color: var(--muted);
}
.tk-origin {
  font-size: 12px;
  font-weight: 600;
  color: var(--muted);
  text-decoration: line-through;
}
.tk-desc {
  font-size: 11.5px;
  color: var(--muted);
  line-height: 1.4;
}

/* 日期 */
.date-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.date-input {
  width: 100%;
  height: 52px;
  font-size: 16px;
  padding: 0 16px;
  border: 2px solid var(--wood-light);
  border-radius: 14px;
  background: #fffdf8;
  color: var(--text);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.date-input:focus {
  outline: none;
  border-color: var(--forest);
  box-shadow: 0 0 0 3px rgba(47, 125, 79, 0.18);
}
.date-tip {
  font-size: 14px;
  color: var(--text);
}
.date-tip b {
  color: var(--forest-deep);
}
.date-week {
  color: var(--muted);
  font-size: 13px;
}
/* 未来 7 天日期卡 */
.day-strip {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
  margin-bottom: 18px;
}
.day-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 12px 4px 10px;
  border: 2px solid var(--wood-light);
  border-radius: 14px;
  background: #fff;
  cursor: pointer;
  transition: all 0.14s ease;
}
.day-card:hover {
  transform: translateY(-2px);
  border-color: var(--forest);
}
.day-card.on {
  background: rgba(47, 125, 79, 0.1);
  border-color: var(--forest);
  box-shadow: 0 6px 14px rgba(47, 125, 79, 0.18);
}
.day-week {
  font-size: 12px;
  font-weight: 700;
  color: var(--muted);
}
.day-card.on .day-week {
  color: var(--forest-deep);
}
.day-num {
  font-size: 16px;
  font-weight: 900;
  color: var(--text);
}
.day-card.on .day-num {
  color: var(--forest-deep);
}
.day-badge {
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--honey);
  color: #5a3d00;
  font-size: 10px;
  font-weight: 800;
  padding: 1px 7px;
  border-radius: 999px;
  white-space: nowrap;
}
/* 其他日期 */
.date-other {
  display: flex;
  align-items: center;
  gap: 12px;
}
.date-other-label {
  font-size: 13px;
  font-weight: 700;
  color: var(--muted);
  white-space: nowrap;
}
.date-selected {
  margin-top: 14px;
  padding: 12px 16px;
  background: #f6fbf3;
  border: 2px dashed var(--leaf);
  border-radius: 12px;
  font-size: 14px;
  color: var(--text);
}
.date-selected b {
  color: var(--forest-deep);
}

/* 数量 */
.qty-block {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: #f6fbf3;
  border: 2px solid var(--leaf);
  border-radius: 16px;
  padding: 18px 20px;
  flex-wrap: wrap;
}
.qty-tk {
  display: flex;
  align-items: center;
  gap: 12px;
}
.qty-emoji {
  font-size: 38px;
}
.qty-name {
  font-size: 16px;
  font-weight: 800;
  color: var(--forest-deep);
}
.qty-unit {
  font-size: 13px;
  color: var(--muted);
}
.stepper {
  display: flex;
  align-items: center;
  gap: 14px;
}
.stepper button {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: 2px solid var(--forest);
  background: #fff;
  color: var(--forest-deep);
  font-size: 22px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.14s ease;
  line-height: 1;
}
.stepper button:hover:not(:disabled) {
  background: var(--forest);
  color: #fff;
}
.stepper button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  border-color: var(--wood-light);
}
.stepper-num {
  min-width: 36px;
  text-align: center;
  font-size: 24px;
  font-weight: 900;
  color: var(--forest-deep);
}
/* 数量快选 */
.qty-quick {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}
.qty-chip {
  border: 2px solid var(--wood-light);
  background: #fffdf8;
  color: var(--muted);
  font-size: 13px;
  font-weight: 700;
  padding: 8px 16px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.14s ease;
}
.qty-chip:hover {
  border-color: var(--forest);
  color: var(--forest-deep);
}
.qty-chip.on {
  background: rgba(47, 125, 79, 0.12);
  border-color: var(--forest);
  color: var(--forest-deep);
}
/* 数量小计 */
.qty-subtotal {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding: 14px 18px;
  background: linear-gradient(180deg, #f6fbf3, #eef7ea);
  border: 2px solid var(--leaf);
  border-radius: 14px;
  font-size: 14px;
  color: var(--muted);
  flex-wrap: wrap;
  gap: 8px;
}
.qty-subtotal b {
  font-size: 22px;
  color: var(--danger);
}
.qty-saved {
  font-style: normal;
  font-weight: 800;
  color: var(--forest);
  margin-left: 6px;
}
.qty-origin {
  text-decoration: line-through;
  margin-left: 6px;
  color: var(--muted);
}

/* 取票人信息 */
.buyer {
  margin-top: 20px;
  padding-top: 4px;
}
.buyer-title {
  font-size: 15px;
  font-weight: 900;
  color: var(--forest-deep);
  margin: 0 0 12px;
}
.field {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.field label {
  width: 62px;
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 700;
  color: var(--muted);
  text-align: right;
}
.field input {
  flex: 1;
  height: 46px;
  padding: 0 14px;
  font-size: 15px;
  border: 2px solid var(--wood-light);
  border-radius: 12px;
  background: #fffdf8;
  color: var(--text);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.field input:focus {
  outline: none;
  border-color: var(--forest);
  box-shadow: 0 0 0 3px rgba(47, 125, 79, 0.16);
}
.field-err {
  margin: -4px 0 10px 74px;
  font-size: 12.5px;
  font-weight: 700;
  color: var(--danger);
}
.agree {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 6px;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--muted);
  cursor: pointer;
}
.agree input {
  margin-top: 2px;
  width: 16px;
  height: 16px;
  accent-color: var(--forest);
  flex-shrink: 0;
}

/* 确认 */
.confirm {
  border: 2px dashed var(--wood-light);
  border-radius: 16px;
  padding: 8px 18px;
}
.confirm-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 13px 0;
  border-bottom: 1px dashed var(--wood-light);
  font-size: 15px;
}
.confirm-row:last-of-type {
  border-bottom: none;
}
.confirm-k {
  color: var(--muted);
  font-weight: 600;
}
.confirm-v {
  font-weight: 800;
  color: var(--text);
}
.confirm-origin {
  font-size: 13px;
  font-weight: 600;
  color: var(--muted);
  text-decoration: line-through;
  margin-left: 6px;
}
.confirm-v.saved {
  color: var(--forest);
}
.confirm-row.total .confirm-v {
  font-size: 22px;
  color: var(--danger);
}
.confirm-note {
  margin: 14px 0 0;
  font-size: 12.5px;
  color: var(--muted);
  text-align: center;
}

/* 底部导航 */
.bk-foot {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
}
.bk-foot-spacer {
  flex: 1;
}
.bk-btn {
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 800;
  padding: 12px 24px;
  cursor: pointer;
  transition: transform 0.08s ease, box-shadow 0.08s ease, filter 0.15s ease;
}
.bk-btn.primary {
  background: linear-gradient(180deg, #ffd23f, var(--honey));
  color: #5a3d00;
  box-shadow: 0 5px 0 #e09b00;
}
.bk-btn.primary:active {
  transform: translateY(4px);
  box-shadow: 0 1px 0 #e09b00;
}
.bk-btn.primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: 0 5px 0 #e0c080;
}
.bk-btn.ghost {
  background: #fff;
  border: 2px solid var(--wood-light);
  color: var(--forest-deep);
}
.bk-btn.ghost:hover {
  background: var(--forest);
  color: #fff;
  border-color: var(--forest);
}
.bk-btn.sm {
  padding: 8px 14px;
  font-size: 13px;
}

/* 实时价格条 */
.price-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding: 14px 20px;
  background: linear-gradient(180deg, #2f7d4f, #1f5c39);
  border-radius: 16px;
  color: #fff;
  box-shadow: 0 10px 24px rgba(31, 92, 57, 0.32);
}
.price-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.price-tk {
  font-size: 15px;
  font-weight: 800;
}
.price-meta {
  font-size: 12px;
  opacity: 0.85;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.price-origin {
  text-decoration: line-through;
  opacity: 0.75;
}
.price-saved {
  font-style: normal;
  font-weight: 800;
  color: #ffd23f;
  background: rgba(255, 210, 63, 0.18);
  border-radius: 999px;
  padding: 1px 7px;
}
.price-total {
  font-size: 28px;
  font-weight: 900;
  color: #ffd23f;
}

/* 成功页 */
.success {
  background: #fffdf7;
  border: 3px solid var(--leaf);
  border-radius: var(--radius);
  padding: 34px 24px;
  text-align: center;
  box-shadow: 0 14px 34px rgba(47, 125, 79, 0.14);
}
.success-badge {
  font-size: 60px;
  line-height: 1;
  animation: pop 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}
@keyframes pop {
  from { transform: scale(0.4); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}
.success h2 {
  margin: 12px 0 4px;
  font-size: 24px;
  color: var(--forest-deep);
}
.success-sub {
  color: var(--muted);
  font-size: 14px;
  margin: 0 0 20px;
}
.success-order {
  max-width: 360px;
  margin: 0 auto 22px;
  border: 2px solid var(--wood-light);
  border-radius: 16px;
  padding: 6px 18px;
  text-align: left;
}
.so-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px dashed var(--wood-light);
  font-size: 14px;
}
.so-row:last-child {
  border-bottom: none;
}
.so-row span {
  color: var(--muted);
}
.so-row b {
  color: var(--text);
}
.so-row.total b {
  font-size: 20px;
  color: var(--danger);
}
.so-no {
  font-family: "Courier New", monospace;
  letter-spacing: 0.5px;
}
.success-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

/* 订单列表 */
.orders-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.order-src {
  display: inline-block;
  margin-left: 8px;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 9px;
  border-radius: 999px;
  vertical-align: middle;
}
.order-src.cloud {
  background: rgba(47, 125, 79, 0.12);
  color: var(--forest-deep);
}
.order-src.local {
  background: rgba(255, 183, 3, 0.18);
  color: #9a6c05;
}
.order-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.order {
  background: #fffdf7;
  border: 3px solid var(--wood-light);
  border-radius: 16px;
  padding: 16px 18px;
  box-shadow: 0 8px 20px rgba(47, 125, 79, 0.1);
}
.order.cancelled {
  opacity: 0.62;
}
.order-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.order-tk {
  font-size: 16px;
  font-weight: 800;
  color: var(--forest-deep);
}
.order-status {
  font-size: 12px;
  font-weight: 800;
  padding: 3px 10px;
  border-radius: 999px;
}
.st-ok {
  background: rgba(76, 175, 109, 0.18);
  color: var(--forest-deep);
}
.st-cancel {
  background: rgba(138, 122, 99, 0.18);
  color: var(--muted);
}
.order-no {
  font-size: 13px;
  color: var(--muted);
  margin-bottom: 12px;
}
.order-no b {
  font-family: "Courier New", monospace;
  color: var(--text);
}
.order-buyer {
  margin-left: 12px;
  color: var(--muted);
}
.order-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  padding: 12px 0;
  border-top: 1px dashed var(--wood-light);
  border-bottom: 1px dashed var(--wood-light);
}
.order-grid > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.order-grid span {
  font-size: 12px;
  color: var(--muted);
}
.order-grid b {
  font-size: 14px;
  color: var(--text);
}
.order-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  flex-wrap: wrap;
  gap: 10px;
}
.order-total {
  font-size: 14px;
  color: var(--muted);
}
.order-total b {
  font-size: 20px;
  color: var(--danger);
  margin-left: 4px;
}
.order-ops {
  display: flex;
  gap: 8px;
}
.op-btn {
  border: 2px solid var(--wood-light);
  background: #fff;
  color: var(--forest-deep);
  font-size: 13px;
  font-weight: 700;
  padding: 7px 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.14s ease;
}
.op-btn:hover {
  background: var(--forest);
  color: #fff;
  border-color: var(--forest);
}
.op-btn.danger {
  border-color: var(--danger);
  color: var(--danger);
}
.op-btn.danger:hover {
  background: var(--danger);
  color: #fff;
}

.order-empty {
  text-align: center;
  padding: 50px 20px;
  color: var(--muted);
  font-size: 15px;
}
.order-empty-cta {
  margin-top: 18px;
}

/* 轻提示 */
.bk-toast {
  position: fixed;
  top: 18px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  max-width: 90vw;
  padding: 10px 18px;
  border-radius: 12px;
  background: rgba(24, 36, 29, 0.96);
  color: #f3d98a;
  font-size: 13px;
  font-weight: 700;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.28);
}
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-10px);
}

/* 渐显 */
.reveal {
  opacity: 0;
  transform: translateY(20px);
  transition: opacity 0.5s ease, transform 0.5s ease;
}
.reveal.is-visible {
  opacity: 1;
  transform: none;
}

/* 响应式 */
@media (max-width: 620px) {
  .tk-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .order-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .bk-title {
    font-size: 17px;
  }
  .day-strip {
    grid-template-columns: repeat(4, 1fr);
  }
  .date-other {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
  .date-other-label {
    text-align: left;
  }
  .field label {
    width: 52px;
  }
  .field-err {
    margin-left: 64px;
  }
}
</style>
