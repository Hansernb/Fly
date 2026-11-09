<template>
  <div class="mc-page">
    <header class="mc-nav">
      <button class="mc-back" type="button" @click="emit('back')">← 返回首页</button>
      <div class="mc-brand">💎 森林会员中心</div>
      <form class="mc-search" @submit.prevent="lookup">
        <input
          v-model.trim="phoneInput"
          type="tel"
          maxlength="11"
          inputmode="numeric"
          placeholder="输入手机号查询会员"
        />
        <button type="submit" :disabled="loading || !phoneInputOk">
          {{ loading ? '查询中…' : '查询' }}
        </button>
      </form>
    </header>

    <main class="mc-main">
      <!-- 会员卡 -->
      <section v-if="member" class="mc-card" :class="'lv-' + levelKey">
        <div class="mc-card-top">
          <div class="mc-avatar" @click="avatarOpen = !avatarOpen">
            <span class="mc-avatar-emoji">{{ member.avatar || '🐻' }}</span>
            <span class="mc-avatar-edit">✎</span>
          </div>
          <div class="mc-id">
            <div class="mc-name-row">
              <b class="mc-name">{{ member.nickname }}</b>
              <span class="mc-level">{{ member.levelIcon }} {{ member.levelName }}</span>
            </div>
            <span class="mc-phone">{{ maskPhone(member.phone) }} · 注册于 {{ member.createdAt.slice(0, 10) || '—' }}</span>
          </div>
          <div class="mc-points">
            <b>{{ member.totalPoints }}</b>
            <span>可用积分</span>
          </div>
        </div>

        <!-- 头像选择 -->
        <div v-if="avatarOpen" class="mc-avatar-pick">
          <button
            v-for="e in AVATARS"
            :key="e"
            type="button"
            class="mc-avatar-opt"
            :class="{ on: member.avatar === e }"
            @click="pickAvatar(e)"
          >
            {{ e }}
          </button>
          <span class="mc-avatar-hint">选一个熊出没头像</span>
        </div>

        <!-- 等级进度 -->
        <div class="mc-progress">
          <div class="mc-progress-top">
            <span>{{ member.levelName }} 累计 {{ member.cumPoints }} 积分</span>
            <span v-if="member.nextNeed > 0">距 {{ member.nextLevelName }} 还差 {{ member.nextNeed }} 积分</span>
            <span v-else>已达最高等级 👑</span>
          </div>
          <div class="mc-progress-bar">
            <div class="mc-progress-fill" :style="{ width: member.progress + '%' }"></div>
          </div>
          <div class="mc-levels">
            <span v-for="l in LEVELS" :key="l.name" class="mc-level-dot" :class="{ on: member.levelName === l.name }">
              {{ l.icon }} {{ l.name }} · {{ l.at }}
            </span>
          </div>
        </div>

        <div class="mc-rename">
          <input v-model.trim="nicknameDraft" maxlength="12" placeholder="修改会员昵称（最多 12 字）" />
          <button type="button" :disabled="saving || !nicknameDirty" @click="saveNickname">
            {{ saving ? '保存中…' : '保存昵称' }}
          </button>
        </div>
      </section>

      <!-- 会员规则 -->
      <section class="mc-rules">
        <div class="mc-rule"><b>1 元 = 1 积分</b><span>门票实付金额自动累计</span></div>
        <div class="mc-rule"><b>1 积分抵 ¥0.3</b><span>下次下单结算页直接抵扣</span></div>
        <div class="mc-rule"><b>最多抵 50%</b><span>单笔订单抵扣上限</span></div>
        <div class="mc-rule"><b>自动升级</b><span>500 白银 / 2000 黄金</span></div>
      </section>

      <!-- 积分明细 -->
      <section v-if="member" class="mc-logs">
        <div class="mc-logs-head">
          <h2>积分明细</h2>
          <span class="mc-logs-count">{{ logs.length }} 条记录</span>
        </div>
        <ul v-if="logs.length" class="mc-log-list">
          <li v-for="l in logs" :key="l.id" class="mc-log" :class="l.points > 0 ? 'in' : 'out'">
            <span class="mc-log-icon">{{ logIcon(l.type) }}</span>
            <div class="mc-log-mid">
              <b>{{ l.type }} · {{ l.remark || logDefault(l.type) }}</b>
              <span>
                {{ l.createdAt }}
                <em v-if="l.orderNo">· 订单 {{ l.orderNo }}</em>
              </span>
            </div>
            <div class="mc-log-right">
              <b class="mc-log-p">{{ l.points > 0 ? '+' + l.points : l.points }}</b>
              <span>余额 {{ l.balance }}</span>
            </div>
          </li>
        </ul>
        <div v-else class="mc-empty">
          🍯 还没有积分记录，买一张门票就能自动开通会员并攒积分啦～
          <button class="mc-cta sm" type="button" @click="emit('tickets')">🎫 去订票</button>
        </div>
      </section>

      <!-- 未开通会员 -->
      <section v-else-if="looked && !loading" class="mc-none">
        <div class="mc-none-emoji">🪹</div>
        <h2>这个手机号还没有会员档案</h2>
        <p>无需注册：首次下单填这个手机号，系统会自动开通会员并送出积分。</p>
        <button class="mc-cta" type="button" @click="emit('tickets')">🎫 去订票开通会员</button>
      </section>

      <!-- 初始引导 -->
      <section v-else-if="!loading" class="mc-none">
        <div class="mc-none-emoji">💎</div>
        <h2>输入手机号查看会员卡</h2>
        <p>会员与购票手机号绑定。买票时填过的手机号，这里输入就能自动登录并查看积分。</p>
      </section>

      <div v-else class="mc-none">
        <div class="mc-none-emoji">⏳</div>
        <h2>正在查询会员…</h2>
      </div>
    </main>

    <transition name="mc-toast">
      <div v-if="toast" class="mc-toast">{{ toast }}</div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  LEVELS,
  MEMBER_PHONE_KEY,
  decorate,
  loadLocalMember
} from '../data/member.js'

const props = defineProps({
  token: { type: String, default: '' },
  user: { type: Object, default: null }
})
const emit = defineEmits(['back', 'tickets'])

const AVATARS = ['🐻', '🐼', '🦊', '🐿️', '🦉', '🦌', '🐰', '🍯', '🌲', '⛺']

const phoneInput = ref('')
const member = ref(null)
const logs = ref([])
const loading = ref(false)
const looked = ref(false)
const avatarOpen = ref(false)
const nicknameDraft = ref('')
const saving = ref(false)
const toast = ref('')
let toastTimer = null

const phoneInputOk = computed(() => /^1\d{10}$/.test(phoneInput.value))
const levelKey = computed(() => {
  const n = member.value?.levelName || ''
  if (n.includes('黄金')) return 'gold'
  if (n.includes('白银')) return 'silver'
  return 'normal'
})
const nicknameDirty = computed(
  () => !!member.value && nicknameDraft.value && nicknameDraft.value !== member.value.nickname
)

function showToast(msg) {
  toast.value = msg
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 2600)
}

function maskPhone(p) {
  return p ? String(p).replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : ''
}

function logIcon(type) {
  if (type === '获取') return '🍯'
  if (type === '抵扣') return '🎫'
  if (type === '退款') return '↩️'
  return '🧾'
}

function logDefault(type) {
  if (type === '获取') return '购票获得积分'
  if (type === '抵扣') return '积分抵扣'
  if (type === '退款') return '积分退回'
  return '积分调整'
}

function applyMember(m) {
  member.value = m ? decorate(m) : null
  nicknameDraft.value = member.value?.nickname || ''
}

async function lookup() {
  if (!phoneInputOk.value) {
    showToast('请输入 11 位手机号')
    return
  }
  loading.value = true
  looked.value = true
  avatarOpen.value = false
  try {
    const res = await fetch(`/api/member?phone=${phoneInput.value}`)
    const json = await res.json().catch(() => ({}))
    if (res.status === 404) {
      applyMember(null)
      logs.value = []
      return
    }
    if (!res.ok) throw new Error(json.message || 'HTTP ' + res.status)
    applyMember(json?.data?.member)
    logs.value = Array.isArray(json?.data?.logs) ? json.data.logs : []
  } catch {
    // 后端不可达 → 本地会员档案兜底
    const local = loadLocalMember(phoneInput.value)
    const all = JSON.parse(localStorage.getItem('boonie_member_v1') || '{}')
    applyMember(local)
    logs.value = local ? all[phoneInput.value]?.logs || [] : []
  } finally {
    loading.value = false
  }
}

function pickAvatar(e) {
  if (!member.value) return
  member.value.avatar = e
  avatarOpen.value = false
  saveProfile({ avatar: e })
}

async function saveNickname() {
  if (!nicknameDirty.value || !member.value) return
  saving.value = true
  const ok = await saveProfile({ nickname: nicknameDraft.value })
  saving.value = false
  if (!ok) return
  member.value.nickname = nicknameDraft.value
  showToast('昵称已更新')
}

/** 修改资料：优先走后端，失败则只更新本地档案 */
async function saveProfile(patch) {
  if (!member.value) return false
  try {
    const res = await fetch('/api/member/profile', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone: member.value.phone, ...patch })
    })
    if (res.ok) {
      const json = await res.json()
      applyMember(json?.data || member.value)
      return true
    }
    throw new Error('HTTP ' + res.status)
  } catch {
    try {
      const all = JSON.parse(localStorage.getItem('boonie_member_v1') || '{}')
      const m = all[member.value.phone]
      if (m) {
        Object.assign(m, patch)
        localStorage.setItem('boonie_member_v1', JSON.stringify(all))
      }
      member.value = decorate({ ...member.value, ...patch })
      return true
    } catch {
      showToast('保存失败，请稍后再试')
      return false
    }
  }
}

onMounted(() => {
  try {
    const saved = localStorage.getItem(MEMBER_PHONE_KEY)
    if (saved) {
      phoneInput.value = saved
      lookup()
    }
  } catch {
    /* 忽略 */
  }
})
</script>

<style scoped>
.mc-page {
  min-height: 100vh;
  padding: 18px 20px 60px;
  position: relative;
}

/* 顶栏 */
.mc-nav {
  max-width: 900px;
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  background: rgba(255, 251, 240, 0.94);
  border: 2px solid var(--wood-light);
  border-radius: 999px;
  padding: 10px 18px;
  box-shadow: 0 10px 26px -14px rgba(31, 92, 57, 0.5);
}
.mc-back {
  border: 2px solid var(--wood-light);
  background: #fffdf8;
  color: var(--forest-deep);
  font-size: 13px;
  font-weight: 800;
  padding: 8px 16px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.16s ease;
  white-space: nowrap;
}
.mc-back:hover {
  background: var(--forest);
  border-color: var(--forest);
  color: #fff;
  transform: translateX(-2px);
}
.mc-brand {
  flex: 1;
  font-size: 16px;
  font-weight: 900;
  color: var(--forest-deep);
}
.mc-search {
  display: flex;
  gap: 8px;
}
.mc-search input {
  width: 175px;
  border: 2px solid rgba(47, 125, 79, 0.18);
  border-radius: 999px;
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 700;
  color: var(--text);
  background: #fff;
  outline: none;
}
.mc-search input:focus {
  border-color: var(--forest);
}
.mc-search button {
  border: none;
  background: var(--honey);
  color: #4a3208;
  font-size: 13px;
  font-weight: 900;
  padding: 8px 18px;
  border-radius: 999px;
  cursor: pointer;
  box-shadow: 0 5px 12px -6px rgba(255, 183, 3, 0.9);
}
.mc-search button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.mc-main {
  max-width: 900px;
  margin: 0 auto;
}

/* 会员卡 */
.mc-card {
  position: relative;
  border-radius: 24px;
  padding: 22px 24px 20px;
  border: 2px solid var(--wood-light);
  background: linear-gradient(140deg, #fffdf6, #f4fbe9 60%, #eaf6dc);
  box-shadow: 0 20px 44px -28px rgba(31, 92, 57, 0.8);
  overflow: hidden;
}
.mc-card::after {
  content: "";
  position: absolute;
  right: -60px;
  top: -70px;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 183, 3, 0.22), transparent 68%);
  pointer-events: none;
}
.mc-card.lv-silver {
  background: linear-gradient(140deg, #fdfefe, #eef3f8 60%, #e2e9f1);
}
.mc-card.lv-gold {
  background: linear-gradient(140deg, #fffdf3, #fff4d6 55%, #ffe6ac);
  border-color: var(--honey);
}
.mc-card-top {
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  z-index: 1;
}
.mc-avatar {
  position: relative;
  width: 64px;
  height: 64px;
  border-radius: 22px;
  background: linear-gradient(150deg, #ffe9a8, #ffcf42);
  display: grid;
  place-items: center;
  cursor: pointer;
  box-shadow: 0 10px 20px -12px rgba(224, 155, 0, 0.95);
  flex: 0 0 auto;
}
.mc-avatar-emoji {
  font-size: 32px;
  line-height: 1;
}
.mc-avatar-edit {
  position: absolute;
  right: -4px;
  bottom: -4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fff;
  color: var(--wood);
  font-size: 11px;
  display: grid;
  place-items: center;
  border: 2px solid var(--wood-light);
}
.mc-id {
  flex: 1;
  min-width: 0;
}
.mc-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.mc-name {
  font-size: 20px;
  font-weight: 900;
  color: var(--forest-deep);
}
.mc-level {
  font-size: 12px;
  font-weight: 800;
  color: #a9740a;
  background: rgba(255, 183, 3, 0.18);
  border: 1px solid rgba(255, 183, 3, 0.45);
  padding: 3px 10px;
  border-radius: 999px;
}
.mc-card.lv-silver .mc-level {
  color: #5b6b7d;
  background: rgba(140, 160, 180, 0.18);
  border-color: rgba(140, 160, 180, 0.45);
}
.mc-card.lv-gold .mc-level {
  color: #7a5200;
  background: rgba(255, 183, 3, 0.28);
}
.mc-phone {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  font-weight: 700;
  color: var(--muted);
}
.mc-points {
  text-align: right;
  flex: 0 0 auto;
}
.mc-points b {
  display: block;
  font-size: 34px;
  font-weight: 900;
  color: #d98600;
  line-height: 1.1;
}
.mc-points span {
  font-size: 11px;
  font-weight: 800;
  color: var(--muted);
  letter-spacing: 0.5px;
}

.mc-avatar-pick {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  background: #fffdf6;
  border: 2px dashed rgba(200, 155, 99, 0.45);
  border-radius: 14px;
  padding: 10px 12px;
}
.mc-avatar-opt {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  border: 2px solid transparent;
  background: #fff;
  font-size: 18px;
  cursor: pointer;
  transition: all 0.14s ease;
}
.mc-avatar-opt:hover {
  transform: translateY(-2px);
}
.mc-avatar-opt.on {
  border-color: var(--honey);
  background: #fff6dd;
}
.mc-avatar-hint {
  font-size: 11px;
  font-weight: 700;
  color: var(--muted);
}

.mc-progress {
  margin-top: 18px;
  position: relative;
  z-index: 1;
}
.mc-progress-top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 12px;
  font-weight: 800;
  color: var(--forest-deep);
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.mc-progress-bar {
  height: 10px;
  border-radius: 999px;
  background: rgba(47, 125, 79, 0.12);
  overflow: hidden;
}
.mc-progress-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--honey), #ffe08a);
  transition: width 0.6s ease;
}
.mc-levels {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}
.mc-level-dot {
  font-size: 11px;
  font-weight: 800;
  color: var(--muted);
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(200, 155, 99, 0.4);
  border-radius: 999px;
  padding: 3px 10px;
}
.mc-level-dot.on {
  color: #7a5200;
  background: #fff3cf;
  border-color: var(--honey);
}
.mc-rename {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  position: relative;
  z-index: 1;
}
.mc-rename input {
  flex: 1;
  border: 2px solid rgba(47, 125, 79, 0.18);
  border-radius: 12px;
  padding: 9px 14px;
  font-size: 13px;
  font-weight: 700;
  color: var(--text);
  background: #fff;
  outline: none;
}
.mc-rename input:focus {
  border-color: var(--forest);
}
.mc-rename button {
  border: 2px solid var(--wood-light);
  background: #fff;
  color: var(--forest-deep);
  font-size: 12px;
  font-weight: 900;
  padding: 9px 16px;
  border-radius: 12px;
  cursor: pointer;
}
.mc-rename button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 规则 */
.mc-rules {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
  margin: 18px 0;
}
.mc-rule {
  background: rgba(255, 251, 240, 0.95);
  border: 2px solid rgba(200, 155, 99, 0.45);
  border-radius: 16px;
  padding: 12px 14px;
}
.mc-rule b {
  display: block;
  font-size: 14px;
  color: var(--forest-deep);
}
.mc-rule span {
  font-size: 11px;
  font-weight: 700;
  color: var(--muted);
}

/* 明细 */
.mc-logs {
  background: rgba(255, 251, 240, 0.97);
  border: 2px solid var(--wood-light);
  border-radius: 22px;
  padding: 16px 18px 18px;
  box-shadow: 0 16px 36px -26px rgba(31, 92, 57, 0.7);
}
.mc-logs-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.mc-logs-head h2 {
  font-size: 16px;
  font-weight: 900;
  color: var(--forest-deep);
}
.mc-logs-count {
  font-size: 11px;
  font-weight: 800;
  color: var(--muted);
}
.mc-log-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 460px;
  overflow-y: auto;
}
.mc-log {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fffdf7;
  border: 2px solid rgba(200, 155, 99, 0.28);
  border-radius: 14px;
  padding: 10px 14px;
}
.mc-log-icon {
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  display: grid;
  place-items: center;
  font-size: 16px;
  border-radius: 11px;
  background: rgba(47, 125, 79, 0.08);
}
.mc-log.in .mc-log-icon {
  background: rgba(76, 175, 109, 0.16);
}
.mc-log.out .mc-log-icon {
  background: rgba(255, 183, 3, 0.18);
}
.mc-log-mid {
  flex: 1;
  min-width: 0;
}
.mc-log-mid b {
  display: block;
  font-size: 13px;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.mc-log-mid span {
  font-size: 11px;
  font-weight: 700;
  color: var(--muted);
}
.mc-log-mid em {
  font-style: normal;
  opacity: 0.85;
}
.mc-log-right {
  text-align: right;
  flex: 0 0 auto;
}
.mc-log-p {
  display: block;
  font-size: 16px;
  font-weight: 900;
}
.mc-log.in .mc-log-p {
  color: var(--forest);
}
.mc-log.out .mc-log-p {
  color: #d98600;
}
.mc-log-right span {
  font-size: 10px;
  font-weight: 700;
  color: var(--muted);
}

/* 空态 */
.mc-none {
  background: rgba(255, 251, 240, 0.95);
  border: 2px dashed var(--wood-light);
  border-radius: 22px;
  padding: 40px 24px;
  text-align: center;
}
.mc-none-emoji {
  font-size: 46px;
  line-height: 1;
}
.mc-none h2 {
  font-size: 17px;
  font-weight: 900;
  color: var(--forest-deep);
  margin: 10px 0 6px;
}
.mc-none p {
  font-size: 13px;
  font-weight: 600;
  color: var(--muted);
  margin-bottom: 16px;
}
.mc-empty {
  text-align: center;
  font-size: 13px;
  font-weight: 700;
  color: var(--muted);
  padding: 26px 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.mc-cta {
  border: none;
  background: var(--honey);
  color: #4a3208;
  font-size: 14px;
  font-weight: 900;
  padding: 11px 24px;
  border-radius: 999px;
  cursor: pointer;
  box-shadow: 0 8px 18px -10px rgba(255, 183, 3, 0.95);
  transition: all 0.16s ease;
}
.mc-cta:hover {
  background: #ffc733;
  transform: translateY(-2px);
}
.mc-cta.sm {
  padding: 8px 18px;
  font-size: 12px;
}

.mc-toast {
  position: fixed;
  left: 50%;
  bottom: 34px;
  transform: translateX(-50%);
  background: rgba(24, 36, 29, 0.94);
  color: #f3d98a;
  font-size: 13px;
  font-weight: 800;
  padding: 10px 20px;
  border-radius: 999px;
  z-index: 20;
}
.mc-toast-enter-active,
.mc-toast-leave-active {
  transition: all 0.24s ease;
}
.mc-toast-enter-from,
.mc-toast-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(10px);
}

@media (max-width: 720px) {
  .mc-page {
    padding: 12px 12px 44px;
  }
  .mc-nav {
    border-radius: 20px;
    gap: 8px;
  }
  .mc-search {
    width: 100%;
  }
  .mc-search input {
    flex: 1;
    width: auto;
  }
  .mc-card {
    padding: 18px 16px 16px;
  }
  .mc-card-top {
    flex-wrap: wrap;
  }
  .mc-points {
    width: 100%;
    text-align: left;
    padding-top: 8px;
    border-top: 1px dashed rgba(200, 155, 99, 0.45);
  }
  .mc-points b {
    display: inline;
    font-size: 26px;
    margin-right: 8px;
  }
}
</style>
