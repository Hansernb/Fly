<template>
  <div class="pc">
    <!-- 顶栏 -->
    <header class="pc-bar">
      <button class="pc-back" type="button" @click="$emit('back')">‹ 返回上一页</button>
      <h1 class="pc-title">👤 个人中心</h1>
      <span class="pc-user">
        <img class="pc-avatar" :src="myAvatar" alt="我的头像" @error="onAvatarErr" />
        {{ user?.name || '游客' }} 的森林小窝
      </span>
    </header>

    <!-- 数据概览 -->
    <div class="pc-stats">
      <div class="pc-stat"><b>{{ orders.length }}</b><span>订单</span></div>
      <div class="pc-stat"><b>{{ favorites.length }}</b><span>收藏</span></div>
      <div class="pc-stat"><b>{{ points }}</b><span>积分</span></div>
      <div class="pc-stat"><b>{{ unlockedCount }}/{{ badges.length }}</b><span>徽章</span></div>
    </div>

    <!-- 标签导航 -->
    <nav class="pc-tabs">
      <button
        v-for="t in tabs"
        :key="t.key"
        type="button"
        class="pc-tab"
        :class="{ on: tab === t.key }"
        @click="tab = t.key"
      >{{ t.icon }} {{ t.label }}</button>
    </nav>

    <!-- ============ 我的订单 ============ -->
    <section v-if="tab === 'orders'" class="pc-panel">
      <div v-if="orders.length" class="pc-list">
        <article v-for="o in orders" :key="o.orderNo" class="pc-order">
          <div class="pc-order-head">
            <span class="pc-order-no">🧾 订单号 {{ o.orderNo }}</span>
            <span class="pc-order-time">{{ o.createdAt }}</span>
          </div>
          <div class="pc-order-body">
            <span class="pc-order-name">{{ o.ticket?.emoji || '🎫' }} {{ o.ticket?.name || o.ticketName || '门票' }}</span>
            <span class="pc-order-meta">{{ o.quantity }} 张 · {{ o.date }}</span>
            <b class="pc-order-pay">¥{{ o.payable ?? o.total }}</b>
          </div>
        </article>
      </div>
      <div v-else class="pc-empty">🧾 还没有订单，去挑一张门票开启森林之旅吧～</div>
    </section>

    <!-- ============ 我的收藏 ============ -->
    <section v-else-if="tab === 'favorites'" class="pc-panel">
      <div v-if="favorites.length" class="pc-cards">
        <article v-for="f in favorites" :key="f.id" class="pc-card" @click="$emit('attraction', f.id)">
          <img :src="f.img || '/img/background.png'" :alt="f.name" loading="lazy" />
          <div class="pc-card-body">
            <b>{{ f.emoji }} {{ f.name }}</b>
            <span>{{ f.tag }} · ★ {{ Number(f.rating || 0).toFixed(1) }} · ¥{{ f.price || 0 }}</span>
          </div>
          <button class="pc-fav-btn on" type="button" title="取消收藏" @click.stop="removeFavorite(f.id)">❤️</button>
        </article>
      </div>
      <div v-else class="pc-empty">❤️ 还没有收藏，逛逛热门项目，点详情里的 🤍 收藏一下吧～</div>
    </section>

    <!-- ============ 浏览历史 ============ -->
    <section v-else-if="tab === 'history'" class="pc-panel">
      <div class="pc-panel-head">
        <span>最近浏览 {{ history.length }} 条</span>
        <button v-if="history.length" class="pc-mini-btn" type="button" @click="clearHistory">🗑 清空历史</button>
      </div>
      <div v-if="history.length" class="pc-history">
        <button v-for="h in history" :key="h.id" type="button" class="pc-hist" @click="$emit('attraction', h.id)">
          <span class="pc-hist-ico">{{ h.emoji }}</span>
          <span class="pc-hist-name">{{ h.name }}</span>
          <span class="pc-hist-time">{{ h.time }}</span>
        </button>
      </div>
      <div v-else class="pc-empty">👣 还没有浏览记录，先去「热门项目推荐」看看吧～</div>
    </section>

    <!-- ============ 积分明细 ============ -->
    <section v-else-if="tab === 'points'" class="pc-panel">
      <template v-if="logs.length">
        <div class="pc-panel-head"><span>共 {{ logs.length }} 条记录 · 当前可用 {{ points }} 积分</span></div>
        <ul class="pc-logs">
          <li v-for="(l, i) in logs" :key="i" class="pc-log" :class="l.points > 0 ? 'in' : 'out'">
            <span class="pc-log-dot">{{ l.points > 0 ? '↑' : '↓' }}</span>
            <div class="pc-log-mid">
              <b>{{ l.type }} · {{ l.remark || '积分变动' }}</b>
              <span>{{ l.createdAt }}<em v-if="l.orderNo"> · 订单 {{ l.orderNo }}</em></span>
            </div>
            <div class="pc-log-right">
              <b>{{ l.points > 0 ? '+' + l.points : l.points }}</b>
              <span>余额 {{ l.balance }}</span>
            </div>
          </li>
        </ul>
      </template>
      <div v-else class="pc-empty">🍯 还没有积分记录，购票即可自动开通会员并攒积分（1 元 = 1 积分）～</div>
    </section>

    <!-- ============ 优惠券 ============ -->
    <section v-else-if="tab === 'coupons'" class="pc-panel">
      <div class="pc-coupons">
        <article v-for="c in coupons" :key="c.id" class="pc-coupon" :class="{ claimed: isClaimed(c.id) }">
          <div class="pc-coupon-val">
            <b>¥{{ c.amount }}</b>
            <span>{{ c.cond }}</span>
          </div>
          <div class="pc-coupon-info">
            <b>{{ c.icon }} {{ c.name }}</b>
            <span>有效期至 {{ c.valid }}</span>
          </div>
          <button
            class="pc-coupon-btn"
            type="button"
            :disabled="isClaimed(c.id)"
            @click="claimCoupon(c)"
          >{{ isClaimed(c.id) ? '✓ 已领取' : '领取' }}</button>
        </article>
      </div>
      <div class="pc-coupon-tip">💡 优惠券在订票结算时自动抵扣，积分也能叠加使用（1 积分 = ¥0.3）。</div>
    </section>

    <!-- ============ 打卡成就徽章 ============ -->
    <section v-else class="pc-panel">
      <div class="pc-panel-head"><span>已解锁 {{ unlockedCount }} / {{ badges.length }} 枚徽章</span></div>
      <div class="pc-badges">
        <div
          v-for="b in badges"
          :key="b.id"
          class="pc-badge"
          :class="{ on: b.unlocked }"
          :title="b.desc"
        >
          <span class="pc-badge-ico">{{ b.unlocked ? b.icon : '🔒' }}</span>
          <b class="pc-badge-name">{{ b.name }}</b>
          <span class="pc-badge-desc">{{ b.desc }}</span>
          <span class="pc-badge-state">{{ b.unlocked ? '已达成' : '未达成' }}</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const props = defineProps({
  user: { type: Object, default: null }
})
const emit = defineEmits(['back', 'attraction'])

/* ===== 用户头像：与首页导航栏头像保持一致 =====
   首页换头像时会写入 localStorage('boonie_user').avatar，
   这里每次进入页面都从 localStorage 读取最新值，保证两边同步。 */
const DEFAULT_AVATAR = '/img/avatar.jpg'
const localAvatar = ref('')
try {
  const u = JSON.parse(localStorage.getItem('boonie_user') || 'null')
  if (u && u.avatar) localAvatar.value = u.avatar
} catch {
  /* 本地存储不可用时忽略 */
}
const myAvatar = computed(() => localAvatar.value || props.user?.avatar || DEFAULT_AVATAR)
function onAvatarErr(e) {
  e.target.src = DEFAULT_AVATAR
}

const tabs = [
  { key: 'orders', icon: '🧾', label: '我的订单' },
  { key: 'favorites', icon: '❤️', label: '我的收藏' },
  { key: 'history', icon: '👣', label: '浏览历史' },
  { key: 'points', icon: '🍯', label: '积分明细' },
  { key: 'coupons', icon: '🎟️', label: '优惠券' },
  { key: 'badges', icon: '🏅', label: '成就徽章' }
]
const tab = ref('orders')

/* ---------- 通用本地存取 ---------- */
function loadList(key) {
  try {
    const a = JSON.parse(localStorage.getItem(key) || '[]')
    return Array.isArray(a) ? a : []
  } catch {
    return []
  }
}
function saveList(key, list) {
  try {
    localStorage.setItem(key, JSON.stringify(list))
  } catch {
    /* ignore */
  }
}

/* ---------- 我的订单（复用订票页的本地订单库） ---------- */
const orders = ref(loadList('boonie_ticket_orders_v1'))

/* ---------- 我的收藏 ---------- */
const LS_FAV = 'boonie_favorites_v1'
const favorites = ref(loadList(LS_FAV))
function removeFavorite(id) {
  favorites.value = favorites.value.filter((f) => f.id !== id)
  saveList(LS_FAV, favorites.value)
}

/* ---------- 浏览历史 ---------- */
const LS_VIEW = 'boonie_view_history_v1'
const history = ref(loadList(LS_VIEW))
function clearHistory() {
  history.value = []
  saveList(LS_VIEW, history.value)
}

/* ---------- 积分明细（来自本地会员数据） ---------- */
const logs = ref([])
const points = ref(0)
onMounted(() => {
  try {
    const all = JSON.parse(localStorage.getItem('boonie_member_v1') || '{}')
    const phone = localStorage.getItem('boonie_member_phone') || ''
    const m = (phone && all[phone]) || Object.values(all)[0]
    if (m) {
      logs.value = (m.logs || []).slice(0, 60)
      points.value = Number(m.totalPoints) || 0
    }
  } catch {
    /* ignore */
  }
})

/* ---------- 优惠券 ---------- */
const COUPONS = [
  { id: 'c1', icon: '🎟️', name: '新人立减券', amount: 10, cond: '满 99 可用', valid: '2026-12-31' },
  { id: 'c2', icon: '🍯', name: '蜂蜜日特惠券', amount: 15, cond: '满 199 可用', valid: '2026-12-31' },
  { id: 'c3', icon: '🎪', name: '演出季折扣券', amount: 20, cond: '满 299 可用', valid: '2026-11-30' },
  { id: 'c4', icon: '💎', name: '会员专享券', amount: 30, cond: '满 399 可用', valid: '2026-12-31' }
]
const LS_COUPON = 'boonie_coupons_v1'
const claimedCoupons = ref(loadList(LS_COUPON))
const coupons = COUPONS
function isClaimed(id) {
  return claimedCoupons.value.includes(id)
}
function claimCoupon(c) {
  if (isClaimed(c.id)) return
  claimedCoupons.value = [...claimedCoupons.value, c.id]
  saveList(LS_COUPON, claimedCoupons.value)
}

/* ---------- 打卡成就徽章 ---------- */
const badges = ref([])
const unlockedCount = computed(() => badges.value.filter((b) => b.unlocked).length)
onMounted(() => {
  // 访问计数
  let visits = 0
  try {
    visits = Number(localStorage.getItem('boonie_visit_count_v1') || 0) + 1
    localStorage.setItem('boonie_visit_count_v1', String(visits))
  } catch {
    /* ignore */
  }
  let uploads = 0
  try {
    uploads = loadList('boonie_scenery_uploads').length
  } catch {
    /* ignore */
  }
  let reviews = 0
  try {
    reviews = Number(localStorage.getItem('boonie_review_count_v1') || 0)
  } catch {
    /* ignore */
  }
  const s = {
    visits,
    orders: orders.value.length,
    favs: favorites.value.length,
    uploads,
    reviews,
    points: points.value
  }
  const defs = [
    { id: 'b1', icon: '🌱', name: '初来乍到', desc: '首次访问狗熊岭', check: (x) => x.visits >= 1 },
    { id: 'b2', icon: '🌳', name: '森林常客', desc: '累计访问 5 次', check: (x) => x.visits >= 5 },
    { id: 'b3', icon: '🎫', name: '首单达成', desc: '完成第一次购票', check: (x) => x.orders >= 1 },
    { id: 'b4', icon: '🧾', name: '囤票达人', desc: '累计 3 张订单', check: (x) => x.orders >= 3 },
    { id: 'b5', icon: '❤️', name: '心有所属', desc: '收藏 1 个游乐项目', check: (x) => x.favs >= 1 },
    { id: 'b6', icon: '📸', name: '风景记录者', desc: '上传 1 张风景照', check: (x) => x.uploads >= 1 },
    { id: 'b7', icon: '🗣️', name: '点评新星', desc: '发表 1 条评价', check: (x) => x.reviews >= 1 },
    { id: 'b8', icon: '👑', name: '狗熊岭传奇', desc: '会员积分 ≥ 100', check: (x) => x.points >= 100 }
  ]
  badges.value = defs.map((d) => ({ ...d, unlocked: d.check(s) }))
})
</script>

<style scoped>
.pc {
  max-width: 960px;
  margin: 0 auto;
  padding: 26px 20px 60px;
}
.pc-bar {
  position: relative;
  text-align: center;
  margin-bottom: 18px;
}
.pc-back {
  position: absolute;
  left: 0;
  top: 4px;
  border: none;
  background: rgba(255, 255, 255, 0.85);
  color: var(--forest-deep);
  font-weight: 700;
  font-size: 14px;
  padding: 8px 14px;
  border-radius: 999px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(47, 125, 79, 0.12);
}
.pc-back:hover { background: #fff; }
.pc-title { font-size: 28px; font-weight: 900; color: var(--forest-deep); }
.pc-user {
  margin-top: 4px;
  font-size: 13px;
  color: var(--muted);
  display: inline-flex;
  align-items: center;
  gap: 7px;
}
/* 用户头像：与首页导航栏头像同源同步 */
.pc-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--leaf, #7cc46b);
  background: #fff;
  box-shadow: 0 2px 8px rgba(47, 125, 79, 0.18);
}

/* 概览 */
.pc-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 18px;
}
.pc-stat {
  background: #fff;
  border-radius: 16px;
  text-align: center;
  padding: 14px 8px;
  box-shadow: 0 8px 22px rgba(47, 125, 79, 0.1);
}
.pc-stat b { display: block; font-size: 22px; font-weight: 900; color: var(--forest-deep); }
.pc-stat span { font-size: 12px; color: var(--muted); }

/* 标签 */
.pc-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}
.pc-tab {
  border: 2px solid var(--wood-light);
  background: #fff;
  color: var(--text);
  font-weight: 800;
  font-size: 13.5px;
  padding: 8px 16px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.18s ease;
}
.pc-tab:hover { border-color: var(--forest); }
.pc-tab.on {
  background: var(--forest);
  border-color: var(--forest);
  color: #fff;
}
.pc-tab:active { transform: scale(0.94); }

/* 面板 */
.pc-panel {
  background: #fff;
  border-radius: var(--radius);
  padding: 18px;
  box-shadow: 0 10px 26px rgba(47, 125, 79, 0.1);
  min-height: 220px;
}
.pc-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: var(--muted);
  margin-bottom: 12px;
}
.pc-empty {
  text-align: center;
  color: var(--muted);
  font-size: 14px;
  padding: 50px 16px;
}
.pc-mini-btn {
  border: none;
  background: rgba(228, 87, 46, 0.1);
  color: #c74a1e;
  font-size: 12.5px;
  font-weight: 800;
  padding: 5px 14px;
  border-radius: 999px;
  cursor: pointer;
}
.pc-mini-btn:active { transform: scale(0.94); }

/* 订单 */
.pc-list { display: flex; flex-direction: column; gap: 12px; }
.pc-order {
  border: 1.5px solid #e7f0e2;
  background: #fafdf8;
  border-radius: 14px;
  padding: 12px 14px;
}
.pc-order-head {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 8px;
}
.pc-order-no { font-weight: 700; color: var(--forest-deep); }
.pc-order-body {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.pc-order-name { font-weight: 800; color: var(--forest-deep); }
.pc-order-meta { font-size: 12.5px; color: var(--muted); }
.pc-order-pay { margin-left: auto; color: #e09b00; font-size: 16px; }

/* 收藏卡片 */
.pc-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 14px;
}
.pc-card {
  position: relative;
  border-radius: 14px;
  overflow: hidden;
  border: 1.5px solid #e7f0e2;
  background: #fafdf8;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}
.pc-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(47, 125, 79, 0.16);
}
.pc-card img { width: 100%; height: 110px; object-fit: cover; display: block; }
.pc-card-body { padding: 10px 12px 12px; display: flex; flex-direction: column; gap: 4px; }
.pc-card-body b { font-size: 14px; color: var(--forest-deep); }
.pc-card-body span { font-size: 12px; color: var(--muted); }
.pc-fav-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  border: none;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  width: 30px;
  height: 30px;
  font-size: 14px;
  cursor: pointer;
  transition: transform 0.15s ease;
}
.pc-fav-btn:active { transform: scale(0.85); }

/* 浏览历史 */
.pc-history { display: flex; flex-direction: column; gap: 8px; }
.pc-hist {
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1.5px solid #e7f0e2;
  background: #fafdf8;
  border-radius: 12px;
  padding: 10px 14px;
  cursor: pointer;
  transition: background 0.15s ease;
}
.pc-hist:hover { background: #f0f6ec; }
.pc-hist-ico { font-size: 20px; }
.pc-hist-name { flex: 1; text-align: left; font-weight: 800; color: var(--forest-deep); font-size: 14px; }
.pc-hist-time { font-size: 12px; color: var(--muted); }

/* 积分明细 */
.pc-logs { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
.pc-log {
  display: flex;
  align-items: center;
  gap: 12px;
  border-radius: 12px;
  padding: 10px 14px;
  background: #fafdf8;
  border: 1.5px solid #e7f0e2;
}
.pc-log-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  font-size: 14px;
  flex: 0 0 28px;
}
.pc-log.in .pc-log-dot { background: rgba(76, 175, 109, 0.15); color: var(--forest); }
.pc-log.out .pc-log-dot { background: rgba(228, 87, 46, 0.12); color: #c74a1e; }
.pc-log-mid { flex: 1; min-width: 0; }
.pc-log-mid b { display: block; font-size: 13.5px; color: var(--forest-deep); }
.pc-log-mid span { font-size: 11.5px; color: var(--muted); }
.pc-log-right { text-align: right; }
.pc-log-right b { display: block; font-size: 15px; }
.pc-log.in .pc-log-right b { color: var(--forest); }
.pc-log.out .pc-log-right b { color: #c74a1e; }
.pc-log-right span { font-size: 11px; color: var(--muted); }

/* 优惠券 */
.pc-coupons { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 12px; }
.pc-coupon {
  display: flex;
  align-items: center;
  gap: 12px;
  border-radius: 14px;
  padding: 12px 14px;
  background: linear-gradient(120deg, #fff7df, #fffdf5);
  border: 1.5px dashed var(--honey);
}
.pc-coupon.claimed { opacity: 0.62; }
.pc-coupon-val { text-align: center; padding-right: 12px; border-right: 1.5px dashed var(--honey); }
.pc-coupon-val b { display: block; font-size: 22px; color: #e09b00; font-weight: 900; }
.pc-coupon-val span { font-size: 11px; color: var(--muted); }
.pc-coupon-info { flex: 1; min-width: 0; }
.pc-coupon-info b { display: block; font-size: 14px; color: #5a3d00; }
.pc-coupon-info span { font-size: 11.5px; color: var(--muted); }
.pc-coupon-btn {
  border: none;
  background: linear-gradient(180deg, #ffc83d, var(--honey));
  color: #5a3d00;
  font-weight: 800;
  font-size: 13px;
  padding: 7px 16px;
  border-radius: 999px;
  cursor: pointer;
  transition: transform 0.15s ease;
}
.pc-coupon-btn:active { transform: scale(0.92); }
.pc-coupon-btn:disabled { background: #cfdec9; color: #fff; cursor: not-allowed; }
.pc-coupon-tip { margin-top: 14px; font-size: 12.5px; color: var(--muted); }

/* 徽章 */
.pc-badges {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
}
.pc-badge {
  text-align: center;
  border-radius: 16px;
  padding: 16px 10px 14px;
  background: #fafdf8;
  border: 2px solid #e7f0e2;
  transition: transform 0.18s ease;
}
.pc-badge:hover { transform: translateY(-4px); }
.pc-badge.on {
  background: linear-gradient(160deg, #f2fbef, #e6f6df);
  border-color: var(--leaf);
  box-shadow: 0 8px 20px rgba(76, 175, 109, 0.18);
}
.pc-badge-ico { font-size: 34px; display: block; margin-bottom: 6px; filter: grayscale(1); }
.pc-badge.on .pc-badge-ico { filter: none; }
.pc-badge-name { display: block; font-size: 14px; color: var(--forest-deep); }
.pc-badge-desc { display: block; font-size: 11px; color: var(--muted); margin-top: 2px; }
.pc-badge-state {
  display: inline-block;
  margin-top: 8px;
  font-size: 11px;
  font-weight: 800;
  padding: 2px 10px;
  border-radius: 999px;
  background: #eee;
  color: var(--muted);
}
.pc-badge.on .pc-badge-state { background: var(--leaf); color: #fff; }

@media (max-width: 640px) {
  .pc-stats { grid-template-columns: repeat(2, 1fr); }
}
</style>
