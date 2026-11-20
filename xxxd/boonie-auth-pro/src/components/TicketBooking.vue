<template>
  <div class="booking">
    <!-- 顶部栏：返回 + 标题 + 我的订单切换 -->
    <header class="bk-bar">
      <button class="bk-back" type="button" @click="$emit('back')">‹ 返回上一页</button>
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
        <transition name="step-slide" mode="out-in">
          <div :key="step" class="step-pane">
        <!-- 步骤 1：选票种 -->
        <div v-if="step === 1">
          <h2 class="bk-h2">① 选择票种</h2>
          <p class="bk-sub">请选择您要购买的门票类型</p>
          <div class="tk-grid">
            <button
              v-for="(t, i) in TICKETS"
              :key="t.id"
              type="button"
              class="tk"
              :class="{ on: selectedId === t.id, hot: t.tag === '热门' }"
              :style="{ '--tk': t.color, '--i': i }"
              @click="selectTicket(t, $event)"
            >
              <span v-if="t.tag === '热门'" class="tk-hot">🔥 热门</span>
              <span v-else-if="t.discount" class="tk-disc">{{ t.discount }}</span>
              <span class="tk-check" :class="{ show: selectedId === t.id }">✓</span>
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
              <span class="stepper-num"><span :key="quantity" class="num-roll">{{ quantity }}</span></span>
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
            <b><span :key="total" class="num-roll">¥{{ total }}</span></b>
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
            <div v-if="pointsDiscountValue > 0" class="confirm-row">
              <span class="confirm-k">积分抵扣</span>
              <span class="confirm-v saved">−¥{{ pointsDiscountValue }}（{{ usePointsOn ? pointsToUse : 0 }} 积分）</span>
            </div>
            <div class="confirm-row total">
              <span class="confirm-k">{{ pointsDiscountValue > 0 ? '实付金额' : '应付总价' }}</span>
              <span class="confirm-v">
                ¥{{ payable }}
                <s v-if="pointsDiscountValue > 0" class="confirm-origin">¥{{ total }}</s>
              </span>
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

          <!-- 会员积分抵扣 -->
          <div class="member-panel" :class="{ on: usePointsOn }">
            <div class="mp-head">
              <span class="mp-icon">💎</span>
              <div class="mp-info">
                <b>森林会员 · 积分抵扣</b>
                <span v-if="memberLoading" class="mp-sub">正在查询会员…</span>
                <span v-else-if="memberInfo" class="mp-sub">
                  {{ memberInfo.levelName }} · 可用 <b class="mp-pts">{{ memberInfo.totalPoints }}</b> 积分
                  <em v-if="memberInfo.phone">（{{ maskPhone(memberInfo.phone) }}）</em>
                </span>
                <span v-else class="mp-sub">
                  {{ phoneOk ? '该手机号还未开通会员，下单后自动开通' : '填写手机号后自动识别会员，无需单独注册' }}
                </span>
              </div>
              <button
                v-if="maxPointsUsable > 0"
                class="mp-switch"
                :class="{ on: usePointsOn }"
                type="button"
                @click="toggleUsePoints"
              >
                {{ usePointsOn ? '✓ 已使用积分' : '使用积分抵扣' }}
              </button>
            </div>

            <div v-if="usePointsOn && maxPointsUsable > 0" class="mp-body">
              <div class="mp-scale">
                <span>0</span>
                <span>最多 {{ maxPointsUsable }} 积分（抵 ¥{{ maxDiscount }}）</span>
              </div>
              <input
                v-model.number="pointsToUse"
                class="mp-range"
                type="range"
                min="0"
                :max="maxPointsUsable"
                step="1"
              />
              <div class="mp-quick">
                <button
                  v-for="n in quickPoints"
                  :key="n"
                  type="button"
                  class="mp-chip"
                  :class="{ on: pointsToUse === n }"
                  @click="pointsToUse = n"
                >
                  {{ n }} 积分
                </button>
                <button type="button" class="mp-chip" :class="{ on: pointsToUse === maxPointsUsable }" @click="pointsToUse = maxPointsUsable">
                  全部抵扣
                </button>
              </div>
              <div class="mp-calc">
                <div class="mp-calc-row">
                  <span>积分抵扣（1 积分 = ¥0.3）</span>
                  <b class="mp-minus">−¥{{ pointsDiscountValue }}</b>
                </div>
                <div class="mp-calc-row">
                  <span>抵扣后应付</span>
                  <b class="mp-pay">¥{{ payable }}</b>
                </div>
                <div class="mp-calc-row small">
                  <span>支付后可得积分</span>
                  <b class="mp-gain">+{{ willEarn }}</b>
                </div>
              </div>
            </div>
            <p v-else class="mp-tip">
              1 积分抵 ¥0.3，单笔最多抵扣订单金额的 50%；支付成功后按实付金额 1 元 = 1 积分到账。
            </p>
          </div>
        </div>
          </div>
        </transition>

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
            {{ submitting ? '⏳ 正在提交…' : `✅ 提交订单 · ¥${payable}` }}
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
        <div class="price-total">
          <span v-if="pointsDiscountValue > 0" class="price-total-sub">积分抵扣</span>
          <span :key="payable" class="num-roll">¥{{ payable }}</span>
        </div>
      </div>
    </section>

    <!-- ===================== 订单提交成功 ===================== -->
    <section v-else-if="mode === 'success'" class="bk-main">
      <div class="success">
        <div class="success-badge">
          <svg class="ok-svg" viewBox="0 0 72 72" aria-hidden="true">
            <circle class="ok-circle" cx="36" cy="36" r="30" />
            <path class="ok-check" d="M22 37 L31 46 L50 27" />
          </svg>
        </div>
        <h2>购票成功！</h2>
        <p class="success-sub">您的电子门票已生成，请妥善保管订单号</p>
        <div class="success-order">
          <div class="so-row"><span>订单号</span><b class="so-no">{{ successOrder.id }}</b></div>
          <div class="so-row"><span>票种</span><b>{{ successOrder.emoji }} {{ successOrder.ticketName }}</b></div>
          <div class="so-row"><span>游玩日期</span><b>{{ successOrder.date }}</b></div>
          <div class="so-row">
            <span>数量</span><b>{{ successOrder.quantity }} 张</b>
          </div>
          <div class="so-row"><span>取票人</span><b>{{ successOrder.buyerName }} · {{ successOrder.buyerPhone }}</b></div>
          <div v-if="successOrder.pointsDiscount > 0" class="so-row">
            <span>积分抵扣</span><b class="so-minus">−¥{{ successOrder.pointsDiscount }}（{{ successOrder.pointsUsed }} 积分）</b>
          </div>
          <div class="so-row total"><span>实付</span><b>¥{{ successOrder.total }}</b></div>
        </div>

        <!-- 会员自动开通提示 -->
        <div v-if="successOrder.member" class="success-member">
          <div class="sm-top">
            <span class="sm-icon">{{ successOrder.member.levelIcon || '💎' }}</span>
            <div class="sm-txt">
              <b v-if="successOrder.isNewMember">🎉 已自动开通会员，获得 {{ successOrder.pointsEarned }} 积分</b>
              <b v-else>🎉 会员积分已到账 +{{ successOrder.pointsEarned }}</b>
              <span>
                {{ successOrder.member.nickname }} · {{ successOrder.member.levelName }}
                · 可用 {{ successOrder.member.totalPoints }} / 累计 {{ successOrder.member.cumPoints }} 积分
              </span>
            </div>
          </div>
          <div class="sm-bar">
            <div class="sm-bar-fill" :style="{ width: successOrder.member.progress + '%' }"></div>
          </div>
          <p class="sm-tip">
            再获得 {{ successOrder.member.nextNeed }} 积分可升级为
            <b>{{ successOrder.member.nextLevelName }}</b>；下次下单可用积分抵扣（1 积分 = ¥0.3）。
          </p>
        </div>
        <div class="success-actions">
          <button class="bk-btn primary" type="button" @click="goOrders">🧾 查看我的订单</button>
          <button v-if="successOrder.member" class="bk-btn honey" type="button" @click="$emit('member')">💎 我的会员卡</button>
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
            <span class="order-total">
              <span v-if="o.pointsDiscount > 0" class="order-pts">积分抵扣 −¥{{ o.pointsDiscount }}</span>
              合计 <b>¥{{ o.total }}</b>
            </span>
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

    <!-- 点选票卡：小票飞入底部价格条 -->
    <span
      v-if="fly"
      class="fly-ticket"
      :class="{ go: fly.go }"
      :style="{
        left: fly.x + 'px',
        top: fly.y + 'px',
        '--dx': fly.dx + 'px',
        '--dy': fly.dy + 'px'
      }"
      >🎟️</span
    >
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { TICKETS, loadTickets } from '../data/tickets.js'
import {
  POINT_RULES,
  MEMBER_PHONE_KEY,
  maxDeductPoints,
  discountOf,
  earnedOf,
  decorate,
  loadLocalMember,
  localAddPoints
} from '../data/member.js'

const props = defineProps({
  user: { type: Object, default: null },
  token: { type: String, default: '' },
  initialTicketId: { type: String, default: '' }
})
const emit = defineEmits(['back', 'member'])

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

/* ===================== 会员积分 ===================== */
const memberInfo = ref(null)
const memberLoading = ref(false)
const usePointsOn = ref(false)
const pointsToUse = ref(0)
let memberTimer = 0

/** 本单最多可用积分（受余额与 50% 上限双重约束） */
const maxPointsUsable = computed(() => maxDeductPoints(memberInfo.value?.totalPoints || 0, total.value))
const maxDiscount = computed(() => discountOf(maxPointsUsable.value))
const pointsDiscountValue = computed(() =>
  usePointsOn.value ? discountOf(Math.min(pointsToUse.value, maxPointsUsable.value)) : 0
)
/** 抵扣后实付 */
const payable = computed(() => Math.max(0, Math.round((total.value - pointsDiscountValue.value) * 100) / 100))
/** 本单支付后可得积分：1 元 = 1 积分 */
const willEarn = computed(() => earnedOf(payable.value))

const quickPoints = computed(() => {
  const max = maxPointsUsable.value
  if (max <= 0) return []
  return [...new Set([Math.floor(max / 2), max].filter((n) => n > 0 && n <= max))]
})

function maskPhone(p) {
  return p ? p.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : ''
}

function clampPoints() {
  if (pointsToUse.value > maxPointsUsable.value) pointsToUse.value = maxPointsUsable.value
  if (maxPointsUsable.value <= 0) {
    usePointsOn.value = false
    pointsToUse.value = 0
  }
}

function toggleUsePoints() {
  if (usePointsOn.value) {
    usePointsOn.value = false
    pointsToUse.value = 0
    return
  }
  usePointsOn.value = true
  pointsToUse.value = maxPointsUsable.value
}

/** 手机号 / 金额变化 → 查询会员与可抵扣额度（防抖）
 *  已登录：会员积分跟账号走，手机号没填也能查；
 *  未登录：仍按取票人手机号查（游客兜底） */
function scheduleMemberLookup() {
  clearTimeout(memberTimer)
  if (!props.token && !phoneOk.value) {
    memberInfo.value = null
    usePointsOn.value = false
    pointsToUse.value = 0
    return
  }
  memberTimer = setTimeout(fetchMember, 360)
}

async function fetchMember() {
  if (!props.token && !phoneOk.value) return
  memberLoading.value = true
  try {
    const res = await fetch(
      `/api/member/preview?phone=${encodeURIComponent(buyerPhone.value)}&amount=${total.value}`,
      { headers: authHeaders() }
    )
    if (!res.ok) throw new Error('HTTP ' + res.status)
    const json = await res.json()
    const m = json?.data?.member
    memberInfo.value = m ? decorate(m) : null
  } catch {
    // 后端不可达时用本地会员档案兜底，保证演示流程完整
    memberInfo.value = loadLocalMember(buyerPhone.value)
  } finally {
    memberLoading.value = false
    clampPoints()
  }
}

watch([buyerPhone, total], scheduleMemberLookup)

/* ===================== 交互 ===================== */
const fly = ref(null)
let flyTimer = 0

/** 点选票卡：小票从卡片位置飞入底部价格条 */
function selectTicket(t, e) {
  selectedId.value = t.id
  try {
    const card = e && e.currentTarget ? e.currentTarget.getBoundingClientRect() : null
    const target = document.querySelector('.booking .price-bar')?.getBoundingClientRect()
    if (card && target) {
      const startX = card.left + card.width / 2 - 16
      const startY = card.top + card.height / 2 - 16
      const dx = target.left + target.width / 2 - 16 - startX
      const dy = target.top + target.height / 2 - 16 - startY
      clearTimeout(flyTimer)
      fly.value = { x: startX, y: startY, dx, dy, go: false }
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          if (fly.value) fly.value = { ...fly.value, go: true }
        })
      })
      flyTimer = setTimeout(() => (fly.value = null), 460)
    }
  } catch {
    /* 飞票动画失败不影响选票 */
  }
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
    grossAmount: Number(o.grossAmount) || Number(o.total) || 0,
    pointsUsed: Number(o.pointsUsed) || 0,
    pointsDiscount: Number(o.pointsDiscount) || 0,
    pointsEarned: Number(o.pointsEarned) || 0,
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

/** 后端不可用时的本地兜底下单，保证演示流程不中断（含会员积分本地记账） */
function createLocalOrder() {
  const used = pointsDiscountValue.value > 0 ? Math.min(pointsToUse.value, maxPointsUsable.value) : 0
  const discount = used > 0 ? discountOf(used) : 0
  const paid = Math.max(0, Math.round((total.value - discount) * 100) / 100)
  const earned = earnedOf(paid)
  const existed = !!loadLocalMember(buyerPhone.value)

  const order = {
    id: genOrderNo(),
    ticketId: selectedTicket.value.id,
    ticketName: selectedTicket.value.name,
    emoji: selectedTicket.value.emoji,
    price: selectedTicket.value.price,
    originalPrice: selectedTicket.value.originalPrice,
    date: visitDate.value,
    quantity: quantity.value,
    grossAmount: total.value,
    total: paid,
    pointsUsed: used,
    pointsDiscount: discount,
    pointsEarned: earned,
    saved: saved.value,
    buyerName: buyerName.value,
    buyerPhone: buyerPhone.value,
    status: '待使用',
    createdAt: new Date().toLocaleString('zh-CN', { hour12: false }),
    user: props.user?.name || '游客'
  }

  // 本地会员积分：先抵扣、后赠送
  if (used > 0) {
    localAddPoints(buyerPhone.value, {
      type: '抵扣',
      points: -used,
      orderNo: order.id,
      remark: `订单抵扣 ¥${discount.toFixed(2)}`,
      nickname: buyerName.value
    })
  }
  const m = localAddPoints(buyerPhone.value, {
    type: '获取',
    points: earned,
    orderNo: order.id,
    remark: `购票实付 ¥${paid.toFixed(2)} 获得积分`,
    nickname: buyerName.value
  })
  order.member = m
  order.isNewMember = !existed

  try {
    localStorage.setItem(MEMBER_PHONE_KEY, buyerPhone.value)
  } catch {
    /* 忽略 */
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

  // 未登录也直接走后端：手机号下单即自动开通会员（后端支持游客订单）；
  // 后端不可达时降级本地，保证演示流程不中断
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
        buyerPhone: buyerPhone.value,
        usePoints: pointsDiscountValue.value > 0 ? Math.min(pointsToUse.value, maxPointsUsable.value) : 0
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
    order.member = json.member ? decorate(json.member) : null
    order.isNewMember = !!json.isNewMember
    order.pointsEarned = Number(json.pointsEarned) || order.pointsEarned
    order.pointsUsed = Number(json.pointsUsed) || order.pointsUsed
    order.pointsDiscount = Number(json.pointsDiscount) || order.pointsDiscount
    try {
      localStorage.setItem(MEMBER_PHONE_KEY, buyerPhone.value)
    } catch {
      /* 忽略 */
    }
    orders.value.unshift(order)
    cacheOrders(orders.value)
    ordersFromApi.value = true
    successOrder.value = order
    mode.value = 'success'
    showToast(json.message || '🎫 订单已生成并写入数据库')
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
  // 渐显卡片：订票页独立路由，没有首页的 IntersectionObserver 来触发，
  // 这里直接标记可见，否则 .reveal 会一直保持 opacity: 0（历史遗留 bug）
  nextTick(() => {
    document.querySelectorAll('.booking .reveal').forEach((el) => el.classList.add('is-visible'))
  })
  // 记住上次下单手机号：再次下单自动带出，等同「会员自动登录」
  try {
    const savedPhone = localStorage.getItem(MEMBER_PHONE_KEY)
    if (savedPhone && !buyerPhone.value) buyerPhone.value = savedPhone
  } catch {
    /* 忽略 */
  }
  loadOrders()
  // 从首页某张票卡「立即预订」进入时，预选该票种并直接到选日期
  if (props.initialTicketId) {
    selectedId.value = props.initialTicketId
    step.value = 2
  }
  // 已登录：进页就按账号拉会员卡（积分跟账号走，不依赖手机号）
  if (props.token) fetchMember()
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

/* ===================== 购票动画 ===================== */
/* ① 票卡依次从下往上淡入上滑 */
.tk {
  animation: tk-in 0.34s cubic-bezier(0.22, 1, 0.36, 1) backwards;
  animation-delay: calc(var(--i, 0) * 70ms);
}
@keyframes tk-in {
  from {
    opacity: 0;
    transform: translateY(26px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

/* ② 悬停上浮 + 阴影加深 */
.tk:hover {
  transform: translateY(-6px) scale(1.02);
  box-shadow: 0 16px 30px rgba(47, 125, 79, 0.24);
}

/* ③ 选中：亮绿描边 + 勾选标记弹入 */
.tk.on {
  border-color: var(--leaf);
  box-shadow: 0 0 0 3px rgba(76, 175, 109, 0.28), 0 10px 22px rgba(76, 175, 109, 0.22);
}
.tk-check {
  position: absolute;
  top: 6px;
  left: 6px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--leaf);
  color: #fff;
  font-size: 13px;
  font-weight: 900;
  line-height: 22px;
  text-align: center;
  transform: scale(0);
  transition: transform 0.22s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.tk-check.show {
  transform: scale(1);
}

/* ④ 按钮按压缩放回弹 */
.tk:active,
.bk-btn:active,
.stepper button:active,
.qty-chip:active,
.mp-chip:active,
.op-btn:active {
  transform: scale(0.94);
  transition-duration: 0.18s;
  transition-timing-function: cubic-bezier(0.34, 1.56, 0.64, 1);
}
.bk-btn.primary:active {
  transform: scale(0.94) !important;
}

/* ⑤ 点选票卡 → 小票飞入底部价格条 */
.fly-ticket {
  position: fixed;
  z-index: 9999;
  font-size: 30px;
  line-height: 1;
  pointer-events: none;
  filter: drop-shadow(0 4px 8px rgba(47, 125, 79, 0.4));
  transform: translate(0, 0) scale(1.15) rotate(-8deg);
  opacity: 1;
}
.fly-ticket.go {
  transform: translate(var(--dx, 0), var(--dy, 0)) scale(0.25) rotate(14deg);
  opacity: 0.25;
  transition: transform 0.42s cubic-bezier(0.5, -0.15, 0.65, 1), opacity 0.42s ease-in;
}

/* ⑥ 步骤面板右侧滑入展开 */
.step-slide-enter-active {
  transition: opacity 0.26s ease, transform 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}
.step-slide-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}
.step-slide-enter-from {
  opacity: 0;
  transform: translateX(36px) scale(0.985);
}
.step-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* ⑦ 数字滚动更新 */
.num-roll {
  display: inline-block;
  animation: num-roll 0.26s cubic-bezier(0.22, 1, 0.36, 1);
}
@keyframes num-roll {
  from {
    opacity: 0;
    transform: translateY(55%) scale(0.9);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

/* ⑧ 支付成功：绿色对勾描边动画 */
.success-badge {
  display: flex;
  justify-content: center;
}
.ok-svg {
  width: 88px;
  height: 88px;
}
.ok-circle {
  fill: rgba(76, 175, 109, 0.1);
  stroke: var(--leaf);
  stroke-width: 5;
  stroke-dasharray: 190;
  stroke-dashoffset: 190;
  animation: ok-draw 0.4s ease-out forwards;
  transform-origin: center;
  transform: rotate(-90deg);
}
.ok-check {
  fill: none;
  stroke: var(--leaf);
  stroke-width: 6;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-dasharray: 46;
  stroke-dashoffset: 46;
  animation: ok-draw 0.3s 0.38s ease-out forwards;
}
@keyframes ok-draw {
  to {
    stroke-dashoffset: 0;
  }
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

/* ===================== 会员积分（结算页） ===================== */
.member-panel {
  margin-top: 18px;
  background: linear-gradient(150deg, #f7fbef, #fffaf0 60%, #fff4dc);
  border: 2px dashed rgba(169, 116, 60, 0.5);
  border-radius: 18px;
  padding: 16px 18px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}
.member-panel.on {
  border-style: solid;
  border-color: var(--honey);
  box-shadow: 0 10px 26px -18px rgba(255, 183, 3, 0.9);
}
.mp-head {
  display: flex;
  align-items: center;
  gap: 12px;
}
.mp-icon {
  flex: 0 0 38px;
  height: 38px;
  display: grid;
  place-items: center;
  font-size: 20px;
  border-radius: 12px;
  background: linear-gradient(150deg, #ffe9a8, #ffcf42);
  box-shadow: 0 6px 14px -8px rgba(224, 155, 0, 0.9);
}
.mp-info {
  flex: 1;
  min-width: 0;
}
.mp-info b {
  display: block;
  font-size: 14px;
  color: var(--forest-deep);
}
.mp-sub {
  display: block;
  font-size: 12px;
  color: var(--muted);
  font-weight: 600;
  margin-top: 2px;
}
.mp-sub .mp-pts {
  color: #d98600;
  font-size: 14px;
}
.mp-sub em {
  font-style: normal;
  opacity: 0.75;
}
.mp-switch {
  flex: 0 0 auto;
  border: 2px solid var(--honey);
  background: #fff;
  color: #a9740a;
  font-size: 12px;
  font-weight: 800;
  padding: 8px 14px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.16s ease;
  white-space: nowrap;
}
.mp-switch:hover {
  background: #fff6dd;
}
.mp-switch.on {
  background: linear-gradient(180deg, #ffd23f, var(--honey));
  color: #5a3d00;
  box-shadow: 0 4px 0 #e09b00;
}
.mp-body {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 2px dashed rgba(200, 155, 99, 0.35);
}
.mp-scale {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  font-weight: 700;
  color: var(--muted);
  margin-bottom: 6px;
}
.mp-range {
  width: 100%;
  appearance: none;
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--honey), #ffd97a);
  outline: none;
  cursor: pointer;
}
.mp-range::-webkit-slider-thumb {
  appearance: none;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #fff;
  border: 3px solid var(--honey);
  box-shadow: 0 3px 8px rgba(169, 116, 10, 0.35);
  cursor: pointer;
}
.mp-quick {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0 12px;
}
.mp-chip {
  border: 2px solid rgba(47, 125, 79, 0.18);
  background: #fff;
  color: var(--forest-deep);
  font-size: 12px;
  font-weight: 800;
  padding: 6px 12px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.mp-chip:hover {
  border-color: var(--honey);
}
.mp-chip.on {
  background: #fff6dd;
  border-color: var(--honey);
  color: #a9740a;
}
.mp-calc {
  background: #fffdf6;
  border: 2px solid rgba(200, 155, 99, 0.28);
  border-radius: 14px;
  padding: 10px 14px;
}
.mp-calc-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 700;
  color: var(--muted);
  padding: 4px 0;
}
.mp-calc-row.small {
  font-size: 12px;
  border-top: 1px dashed rgba(200, 155, 99, 0.35);
  margin-top: 4px;
  padding-top: 8px;
}
.mp-minus {
  color: var(--danger);
}
.mp-pay {
  color: var(--forest-deep);
  font-size: 17px;
}
.mp-gain {
  color: #d98600;
}
.mp-tip {
  margin-top: 10px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--muted);
  font-weight: 600;
}

/* 成功页会员卡 */
.success-member {
  margin-top: 16px;
  text-align: left;
  background: linear-gradient(150deg, #fff8e4, #f4fbe9 65%, #fff);
  border: 2px solid var(--honey);
  border-radius: 18px;
  padding: 14px 16px 12px;
  box-shadow: 0 12px 26px -20px rgba(224, 155, 0, 0.9);
}
.sm-top {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.sm-icon {
  font-size: 26px;
  line-height: 1;
}
.sm-txt b {
  display: block;
  font-size: 14px;
  color: var(--forest-deep);
  margin-bottom: 4px;
}
.sm-txt span {
  font-size: 12px;
  color: var(--muted);
  font-weight: 700;
}
.sm-bar {
  margin: 10px 0 6px;
  height: 8px;
  border-radius: 999px;
  background: rgba(47, 125, 79, 0.12);
  overflow: hidden;
}
.sm-bar-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--honey), #ffe08a);
  transition: width 0.5s ease;
}
.sm-tip {
  font-size: 12px;
  color: var(--muted);
  font-weight: 600;
}
.sm-tip b {
  color: #a9740a;
}
.bk-btn.honey {
  background: linear-gradient(180deg, #5cc07c, var(--forest));
  color: #fff;
  box-shadow: 0 5px 0 #1d5c39;
}
.bk-btn.honey:active {
  transform: translateY(4px);
  box-shadow: 0 1px 0 #1d5c39;
}
.order-pts {
  display: inline-block;
  margin-right: 8px;
  font-size: 11px;
  font-weight: 800;
  color: #a9740a;
  background: rgba(255, 183, 3, 0.16);
  border-radius: 999px;
  padding: 3px 9px;
}
.price-total-sub {
  display: block;
  font-size: 10px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.75);
  letter-spacing: 0.5px;
}
.so-minus {
  color: var(--danger);
}

</style>
