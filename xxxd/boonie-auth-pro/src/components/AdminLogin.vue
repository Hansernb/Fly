<template>
  <div class="admin-login">
    <!-- 沉稳松墨绿背景 + 金色落叶飘落动效（区别于前台的明亮春日感） -->
    <div class="al-bg" aria-hidden="true">
      <span class="al-glow al-glow-a"></span>
      <span class="al-glow al-glow-b"></span>
      <div class="al-leaves">
        <span v-for="l in leaves" :key="l.id" class="al-leaf" :style="leafStyle(l)">
          <i class="al-leaf-shape" :class="l.shape" :style="shapeStyle(l)"></i>
        </span>
      </div>
      <div class="al-motes">
        <i v-for="m in motes" :key="'m' + m.id" :style="moteStyle(m)"></i>
      </div>
    </div>

    <!-- 居中圆角卡片：仅「管理员账号 / 密码」+「登录」 -->
    <div class="al-card" :class="{ shake: shake }" @animationend="onAnimEnd">
      <div class="al-head">
        <div class="al-badge">
          <svg viewBox="0 0 24 24" width="26" height="26" aria-hidden="true">
            <path
              fill="currentColor"
              d="M19.14 12.94a7.07 7.07 0 0 0 .06-.94 7.07 7.07 0 0 0-.06-.94l2.03-1.58a.5.5 0 0 0 .12-.64l-1.92-3.32a.5.5 0 0 0-.6-.22l-2.39.96a7.3 7.3 0 0 0-1.62-.94l-.36-2.54a.5.5 0 0 0-.5-.42h-3.84a.5.5 0 0 0-.5.42l-.36 2.54c-.59.24-1.13.56-1.62.94l-2.39-.96a.5.5 0 0 0-.6.22L2.65 8.84a.5.5 0 0 0 .12.64l2.03 1.58a7.07 7.07 0 0 0-.06.94c0 .32.02.63.06.94L2.77 14.5a.5.5 0 0 0-.12.64l1.92 3.32c.13.22.39.31.6.22l2.39-.96c.49.38 1.03.7 1.62.94l.36 2.54c.04.24.25.42.5.42h3.84c.25 0 .46-.18.5-.42l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.09.47 0 .6-.22l1.92-3.32a.5.5 0 0 0-.12-.64l-2.03-1.58ZM12 15.6A3.6 3.6 0 1 1 12 8.4a3.6 3.6 0 0 1 0 7.2Z"
            />
          </svg>
        </div>
        <h1 class="al-title">狗熊岭后台管理</h1>
        <p class="al-sub">仅限管理员登录 · 请出示你的钥匙</p>
      </div>

      <form class="al-form" @submit.prevent="submit">
        <label class="al-field">
          <span class="al-label">管理员账号</span>
          <input
            v-model.trim="username"
            class="al-input"
            type="text"
            placeholder="请输入管理员账号"
            autocomplete="username"
            required
          />
        </label>

        <label class="al-field">
          <span class="al-label">密码</span>
          <input
            v-model="password"
            class="al-input"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
            required
          />
        </label>

        <p v-if="error" class="al-error">⚠️ {{ error }}</p>

        <button class="al-submit" type="submit" :disabled="loading">
          {{ loading ? '登录中…' : '登 录' }}
        </button>
      </form>

      <button class="al-back" type="button" @click="$emit('back')">← 返回游乐园首页</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['success', 'back'])

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')
const shake = ref(false)

/* ---- 金色落叶：数量适中，慢速飘落，营造沉稳氛围 ---- */
const rand = (a, b) => Math.random() * (b - a) + a
const GOLDS = ['#e8c46a', '#f3d98a', '#c9a24a', '#f0dfa8']
const leaves = Array.from({ length: 28 }, (_, i) => ({
  id: i,
  left: +rand(-4, 104).toFixed(1),
  delay: +rand(0, 10).toFixed(2),
  dur: +rand(8, 16).toFixed(1), // 后台氛围更慢更沉稳
  swayDur: +rand(3, 6).toFixed(1),
  swayAmp: +rand(12, 40).toFixed(0),
  spinDur: +rand(4, 9).toFixed(1),
  size: +rand(10, 22).toFixed(1),
  color: GOLDS[i % GOLDS.length],
  shape: i % 3 === 0 ? 'willow' : 'leaf',
  opacity: +rand(0.45, 0.85).toFixed(2)
}))
const motes = Array.from({ length: 14 }, (_, i) => ({
  id: i,
  top: +rand(10, 92).toFixed(0),
  left: +rand(2, 98).toFixed(0),
  size: +rand(2.5, 5).toFixed(1),
  dur: +rand(5, 11).toFixed(1),
  delay: +rand(0, 7).toFixed(1)
}))

function leafStyle(l) {
  return { left: l.left + '%', animationDuration: l.dur + 's', animationDelay: l.delay + 's' }
}
function shapeStyle(l) {
  return {
    width: l.size + 'px',
    height: l.shape === 'willow' ? l.size * 0.45 + 'px' : l.size + 'px',
    backgroundColor: l.color,
    opacity: l.opacity,
    animationDuration: l.spinDur + 's',
    animationDelay: l.delay + 's',
    '--amp': l.swayAmp + 'px',
    '--sway': l.swayDur + 's'
  }
}
function moteStyle(m) {
  return {
    top: m.top + '%',
    left: m.left + '%',
    width: m.size + 'px',
    height: m.size + 'px',
    animationDuration: m.dur + 's',
    animationDelay: m.delay + 's'
  }
}

function triggerShake() {
  shake.value = false
  requestAnimationFrame(() => {
    shake.value = true
  })
}
function onAnimEnd(e) {
  if (e.animationName === 'alShake') shake.value = false
}

async function submit() {
  error.value = ''
  if (!username.value || !password.value) {
    error.value = '请输入管理员账号和密码'
    triggerShake()
    return
  }
  loading.value = true
  try {
    const res = await fetch('/api/admin/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: username.value, password: password.value })
    })
    const data = await res.json().catch(() => ({}))

    if (!res.ok) {
      // 后端 BizException 会带 message 字段（如「权限不足」）
      error.value = data.message || '账号或密码错误'
      triggerShake()
      return
    }
    localStorage.setItem('boonie_token', data.token)
    localStorage.setItem('boonie_admin_token', data.token)
    emit('success', { user: data.user, token: data.token })
  } catch (e) {
    error.value = '网络异常，请确认后端已启动'
    triggerShake()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.admin-login {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow: hidden;
  background: linear-gradient(165deg, #0f1e17 0%, #1b3325 45%, #142a1e 100%);
}

/* ---------- 背景：金色光晕 + 落叶 + 光粒 ---------- */
.al-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}
.al-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
}
.al-glow-a {
  width: 46vw;
  height: 46vw;
  top: -14vw;
  right: -8vw;
  background: radial-gradient(circle, rgba(232, 196, 106, 0.22), transparent 68%);
}
.al-glow-b {
  width: 38vw;
  height: 38vw;
  bottom: -12vw;
  left: -6vw;
  background: radial-gradient(circle, rgba(120, 170, 130, 0.18), transparent 70%);
}

.al-leaves {
  position: absolute;
  inset: 0;
}
.al-leaf {
  position: absolute;
  top: -10%;
  animation-name: alFall;
  animation-timing-function: linear;
  animation-iteration-count: infinite;
}
@keyframes alFall {
  from { transform: translateY(-12vh); }
  to { transform: translateY(118vh); }
}
.al-leaf-shape {
  display: block;
  animation-name: alSway, alSpin;
  animation-timing-function: ease-in-out, linear;
  animation-iteration-count: infinite, infinite;
  animation-direction: alternate, normal;
  animation-duration: var(--sway), 7s;
}
@keyframes alSway {
  from { transform: translateX(calc(var(--amp) * -1)); }
  to { transform: translateX(var(--amp)); }
}
@keyframes alSpin {
  from { rotate: 0deg; }
  to { rotate: 360deg; }
}
.al-leaf-shape.leaf { border-radius: 0 100% 0 100%; }
.al-leaf-shape.willow { border-radius: 100% 0 100% 0; }

.al-motes {
  position: absolute;
  inset: 0;
}
.al-motes i {
  position: absolute;
  border-radius: 50%;
  background: #f3d98a;
  box-shadow: 0 0 8px 2px rgba(243, 217, 138, 0.6);
  animation-name: alMote;
  animation-timing-function: ease-in-out;
  animation-iteration-count: infinite;
}
@keyframes alMote {
  0% { transform: translate(0, 0) scale(0.6); opacity: 0.1; }
  40% { opacity: 0.85; }
  100% { transform: translate(-12px, -46px) scale(0.6); opacity: 0.1; }
}

/* ---------- 居中卡片 ---------- */
.al-card {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 400px;
  background: linear-gradient(180deg, rgba(24, 44, 33, 0.94) 0%, rgba(17, 33, 24, 0.96) 100%);
  border: 1px solid rgba(232, 196, 106, 0.34);
  border-radius: 20px;
  padding: 34px 30px 26px;
  box-shadow: 0 26px 64px rgba(0, 0, 0, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.06);
  -webkit-backdrop-filter: blur(10px);
  backdrop-filter: blur(10px);
  animation: alCardIn 0.6s cubic-bezier(0.22, 1, 0.36, 1) both;
}
@keyframes alCardIn {
  from { opacity: 0; transform: translateY(22px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
/* 登录失败抖动 */
.al-card.shake {
  animation: alShake 0.45s;
}
@keyframes alShake {
  0%, 100% { transform: translateX(0); }
  18% { transform: translateX(-10px) rotate(-1deg); }
  36% { transform: translateX(10px) rotate(1deg); }
  54% { transform: translateX(-7px); }
  72% { transform: translateX(7px); }
  86% { transform: translateX(-3px); }
}

.al-head {
  text-align: center;
  margin-bottom: 24px;
}
.al-badge {
  width: 54px;
  height: 54px;
  margin: 0 auto 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #f3d98a;
  border-radius: 15px;
  background: rgba(232, 196, 106, 0.12);
  border: 1px solid rgba(232, 196, 106, 0.4);
  box-shadow: 0 0 22px rgba(232, 196, 106, 0.18);
}
.al-title {
  margin: 0 0 6px;
  font-size: 21px;
  font-weight: 800;
  color: #f3ecd8;
  letter-spacing: 0.02em;
}
.al-sub {
  margin: 0;
  font-size: 12.5px;
  color: #9fb3a5;
}

.al-form {
  display: flex;
  flex-direction: column;
  gap: 15px;
}
.al-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.al-label {
  font-size: 12.5px;
  font-weight: 700;
  color: #cbd9cd;
}
.al-input {
  border: 1px solid rgba(232, 196, 106, 0.28);
  background: rgba(10, 22, 16, 0.72);
  color: #f0ead9;
  border-radius: 11px;
  padding: 11px 14px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.al-input::placeholder {
  color: #7d9086;
}
.al-input:focus {
  border-color: #e8c46a;
  box-shadow: 0 0 0 3px rgba(232, 196, 106, 0.16);
}

.al-error {
  margin: 0;
  font-size: 12.5px;
  font-weight: 600;
  color: #ffb4a8;
  background: rgba(193, 64, 48, 0.16);
  border: 1px solid rgba(224, 106, 88, 0.42);
  border-radius: 9px;
  padding: 8px 12px;
}

/* 金色主按钮 */
.al-submit {
  margin-top: 4px;
  border: none;
  border-radius: 12px;
  padding: 12px;
  font-size: 15px;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: #3a2a08;
  background: linear-gradient(135deg, #f7dd9a 0%, #e0b455 55%, #c99b3f 100%);
  cursor: pointer;
  box-shadow: 0 8px 22px rgba(224, 180, 85, 0.28);
  transition: filter 0.15s ease, transform 0.08s ease;
}
.al-submit:hover:not(:disabled) {
  filter: brightness(1.06);
}
.al-submit:active:not(:disabled) {
  transform: translateY(2px);
}
.al-submit:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.al-back {
  margin-top: 18px;
  width: 100%;
  border: 1px solid rgba(232, 196, 106, 0.22);
  background: transparent;
  color: #9fb3a5;
  font-size: 12.5px;
  font-weight: 600;
  padding: 9px;
  border-radius: 10px;
  cursor: pointer;
  transition: color 0.15s ease, border-color 0.15s ease;
}
.al-back:hover {
  color: #f3d98a;
  border-color: rgba(232, 196, 106, 0.5);
}

@media (prefers-reduced-motion: reduce) {
  .al-leaf,
  .al-leaf-shape,
  .al-motes i,
  .al-card {
    animation: none !important;
  }
}
</style>
