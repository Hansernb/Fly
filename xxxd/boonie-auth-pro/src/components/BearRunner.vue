<template>
  <div class="br-wrap">
    <canvas
      ref="cv"
      class="br-canvas"
      width="900"
      height="380"
      @pointerdown="jump"
    ></canvas>

    <!-- 开场 / 结束遮罩：自包含，父组件只同步数据 -->
    <div v-if="phase !== 'playing'" class="br-overlay" @click="jump">
      <div class="br-panel">
        <span class="br-panel-emoji">{{ phase === 'over' ? '💥' : '🐻' }}</span>
        <h3>{{ phase === 'over' ? '熊大撞树桩啦！' : '熊大快跑' }}</h3>
        <p v-if="phase === 'over'" class="br-final">
          本局 <b>{{ score }}</b> 分 · 收了 <b>{{ honey }}</b> 罐蜂蜜
        </p>
        <p v-if="phase === 'over' && isNewBest" class="br-newbest">🎉 新纪录！</p>
        <p class="br-tip">空格 / ↑ / 点击屏幕跳跃 · 躲开树桩 · 吃蜂蜜罐 +10 分</p>
        <button class="br-btn" type="button" @click.stop="jump">
          {{ phase === 'over' ? '再跑一次' : '开始奔跑' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 熊大快跑 —— 单文件原生 Canvas 跑酷
 *
 * 核心逻辑抽自 GitHub Topic `endless-runner` 的单文件跑酷
 * （CursedPrograms/endless-runner-html、amoldalwai/Chrome_Dino_Game）：
 *   rAF 游戏循环 + 重力跳跃 + 障碍数组 + AABB 碰撞 + 计分加速 + localStorage 最高分
 * 画风改为本项目的暖绿 + 暖黄（沿用 --forest / --honey 色系）。
 */
import { ref, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  /** 父级统一最高分（历史记录），与本组件 localStorage 取最大值 */
  best: { type: Number, default: 0 }
})
const emit = defineEmits(['score', 'honey', 'best', 'gameover'])

const cv = ref(null)
let ctx = null

/* ============ 常量 ============ */
const W = 900
const H = 380
const GROUND = 300 // 地面 y
const PX = 118 // 熊大固定 x
const PW = 40 // 碰撞宽
const GRAVITY = 0.72 // 每帧重力
const JUMP_V = 12.6 // 起跳速度
const START_SPEED = 5
const MAX_SPEED = 11
const STORE_KEY = 'boonie_bear_runner_best_v1'

/* ============ 状态 ============ */
const phase = ref('ready') // ready | playing | over
const score = ref(0)
const honey = ref(0)
const bestScore = ref(0)
const isNewBest = ref(false)

let playerY = 0 // 离地高度
let velocity = 0
let speed = START_SPEED
let dist = 0
let obstacles = []
let items = []
let particles = []
let clouds = []
let spawnIn = 60
let raf = 0
let lastTs = 0

/* ============ 最高分 ============ */
function readBest() {
  let local = 0
  try {
    local = Number(localStorage.getItem(STORE_KEY)) || 0
  } catch {
    /* 忽略隐私模式等存储异常 */
  }
  bestScore.value = Math.max(local, Number(props.best) || 0)
}
function writeBest(v) {
  bestScore.value = v
  try {
    localStorage.setItem(STORE_KEY, String(v))
  } catch {
    /* 忽略 */
  }
  emit('best', v)
}

/* ============ 跳跃 ============ */
function jump() {
  if (phase.value !== 'playing') {
    start()
    return
  }
  if (playerY === 0) {
    velocity = JUMP_V
    burst(PX + PW / 2, GROUND, '#fff3c4', 6)
  }
}
function onKey(e) {
  if (e.code === 'Space' || e.code === 'ArrowUp') {
    e.preventDefault()
    jump()
  }
}

/* ============ 开局 / 结束 ============ */
function start() {
  phase.value = 'playing'
  score.value = 0
  honey.value = 0
  isNewBest.value = false
  playerY = 0
  velocity = 0
  speed = START_SPEED
  dist = 0
  obstacles = []
  items = []
  particles = []
  spawnIn = 60
  if (!clouds.length) {
    clouds = Array.from({ length: 4 }, (_, i) => ({
      x: (i * 260) % W,
      y: 42 + (i % 3) * 30,
      s: 0.75 + (i % 3) * 0.22
    }))
  }
  lastTs = 0
  cancelAnimationFrame(raf)
  raf = requestAnimationFrame(frame)
}

function gameOver() {
  phase.value = 'over'
  cancelAnimationFrame(raf)
  raf = 0
  const final = score.value
  if (final > bestScore.value) {
    isNewBest.value = true
    writeBest(final)
  }
  emit('gameover', {
    score: final,
    honey: honey.value,
    best: bestScore.value,
    isNewBest: isNewBest.value
  })
}

/* ============ 障碍 / 收集物 ============ */
function spawn() {
  const h = 34 + Math.random() * 20
  const w = 32 + Math.random() * 10
  obstacles.push({ x: W + 30, w, h, passed: false })
  // 约 7 成概率在障碍后放置蜂蜜罐（跳跃轨迹高度）
  if (Math.random() < 0.7) {
    items.push({
      x: W + 130 + Math.random() * 70,
      y: GROUND - 64 - Math.random() * 40,
      taken: false
    })
  }
  // 速度越快，间隔越大，保证可反应
  spawnIn = Math.floor(52 + Math.random() * 46 + speed * 2.5)
}

function burst(x, y, color, n) {
  for (let i = 0; i < n; i++) {
    particles.push({
      x, y,
      vx: (Math.random() - 0.5) * 6,
      vy: -Math.random() * 5 - 1,
      r: 2 + Math.random() * 3,
      life: 0.5 + Math.random() * 0.4,
      color
    })
  }
}

/* AABB：熊大固定在 PX，跳起高度 playerY 超过障碍高度即安全 */
function hits(o) {
  return PX < o.x + o.w && PX + PW > o.x && playerY < o.h
}
function hitItem(it) {
  const cx = PX + PW / 2
  const cy = GROUND - playerY - 26
  return Math.abs(cx - it.x) < 26 && Math.abs(cy - it.y) < 28
}

/* ============ 游戏循环 ============ */
function frame(ts) {
  if (phase.value !== 'playing') return
  if (!lastTs) lastTs = ts
  const dt = Math.min(2.2, (ts - lastTs) / 16.667) || 1
  lastTs = ts

  // 速度随得分增长（参考 endless-runner：每过一个障碍加一点速度）
  speed = Math.min(MAX_SPEED, START_SPEED + score.value * 0.06)
  dist += speed * dt

  // 重力跳跃
  if (playerY > 0 || velocity > 0) {
    velocity -= GRAVITY * dt
    playerY = Math.max(0, playerY + velocity * dt)
    if (playerY === 0) velocity = 0
  }

  // 云视差
  for (const cl of clouds) {
    cl.x -= speed * 0.12 * dt
    if (cl.x < -70) cl.x += W + 140
  }

  // 生成：按时间步进（dt 归一化到 60fps），避免高刷新率下障碍爆发式生成
  spawnIn -= dt
  if (spawnIn <= 0) spawn()

  // 障碍移动 / 计分 / 碰撞
  for (const o of obstacles) {
    o.x -= speed * dt
    if (!o.passed && o.x + o.w < PX) {
      o.passed = true
      score.value += 1
      emit('score', score.value)
    }
    if (hits(o)) {
      burst(PX + PW / 2, GROUND - playerY - 14, '#ff8a65', 16)
      gameOver()
      return
    }
  }
  obstacles = obstacles.filter((o) => o.x + o.w > -40)

  // 蜂蜜罐
  for (const it of items) {
    it.x -= speed * dt
    if (!it.taken && hitItem(it)) {
      it.taken = true
      honey.value += 1
      score.value += 10
      emit('score', score.value)
      emit('honey', honey.value)
      burst(it.x, it.y, '#ffb703', 12)
    }
  }
  items = items.filter((it) => !it.taken && it.x > -40)

  // 粒子
  for (const p of particles) {
    p.x += p.vx * dt
    p.y += p.vy * dt
    p.vy += 0.16 * dt
    p.life -= 0.03 * dt
  }
  particles = particles.filter((p) => p.life > 0)

  draw()
  raf = requestAnimationFrame(frame)
}

/* ============ 绘制 ============ */
function rrect(c, x, y, w, h, r) {
  c.beginPath()
  c.moveTo(x + r, y)
  c.arcTo(x + w, y, x + w, y + h, r)
  c.arcTo(x + w, y + h, x, y + h, r)
  c.arcTo(x, y + h, x, y, r)
  c.arcTo(x, y, x + w, y, r)
  c.closePath()
}

function draw() {
  const c = ctx
  if (!c) return
  c.clearRect(0, 0, W, H)

  // 天空：暖黄渐变
  const sky = c.createLinearGradient(0, 0, 0, H)
  sky.addColorStop(0, '#fff1c9')
  sky.addColorStop(0.62, '#f7f3d2')
  sky.addColorStop(1, '#dff0c4')
  c.fillStyle = sky
  c.fillRect(0, 0, W, H)

  // 太阳
  c.fillStyle = 'rgba(255, 183, 3, 0.28)'
  c.beginPath()
  c.arc(W - 96, 62, 42, 0, Math.PI * 2)
  c.fill()
  c.fillStyle = 'rgba(255, 206, 84, 0.85)'
  c.beginPath()
  c.arc(W - 96, 62, 26, 0, Math.PI * 2)
  c.fill()

  // 云
  for (const cl of clouds) drawCloud(c, cl.x, cl.y, cl.s)

  // 远景树林（0.2x 视差）
  drawFarTrees(c, dist * 0.2)

  // 地面：暖绿 + 泥土
  c.fillStyle = '#7cc65b'
  c.fillRect(0, GROUND, W, H - GROUND)
  c.fillStyle = '#93d473'
  c.fillRect(0, GROUND, W, 8)
  c.fillStyle = '#c99a5f'
  c.fillRect(0, GROUND + 28, W, H - GROUND - 28)

  // 草簇随速度滚动
  const gsp = 40
  const gfrac = dist % gsp
  c.strokeStyle = '#4e9b3c'
  c.lineWidth = 3
  c.lineCap = 'round'
  for (let x = -gfrac; x < W + gsp; x += gsp) {
    c.beginPath()
    c.moveTo(x, GROUND + 2)
    c.quadraticCurveTo(x + 3, GROUND - 9, x + 7, GROUND - 12)
    c.moveTo(x + 6, GROUND + 2)
    c.quadraticCurveTo(x + 9, GROUND - 8, x + 12, GROUND - 10)
    c.stroke()
  }

  // 障碍 / 蜂蜜罐 / 熊大
  for (const o of obstacles) drawStump(c, o)
  for (const it of items) drawHoney(c, it.x, it.y)
  drawBear(c, PX, GROUND - playerY)

  // 粒子
  for (const p of particles) {
    c.globalAlpha = Math.max(0, Math.min(1, p.life * 2))
    c.fillStyle = p.color
    c.beginPath()
    c.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    c.fill()
  }
  c.globalAlpha = 1

  // 画布内计分
  c.save()
  c.font = '900 22px "Comic Sans MS","Microsoft YaHei",sans-serif'
  c.textAlign = 'left'
  c.lineWidth = 5
  c.strokeStyle = 'rgba(58, 46, 31, 0.45)'
  c.strokeText(`得分 ${score.value}`, 18, 36)
  c.fillStyle = '#fff8ec'
  c.fillText(`得分 ${score.value}`, 18, 36)
  c.restore()
}

function drawCloud(c, x, y, s) {
  c.fillStyle = 'rgba(255, 255, 255, 0.86)'
  c.beginPath()
  c.arc(x, y, 17 * s, 0, Math.PI * 2)
  c.arc(x + 19 * s, y - 9 * s, 13 * s, 0, Math.PI * 2)
  c.arc(x + 38 * s, y, 15 * s, 0, Math.PI * 2)
  c.fill()
}

function drawFarTrees(c, off) {
  const sp = 128
  const base = Math.floor(off / sp)
  const frac = off % sp
  for (let k = -1; k * sp - frac < W + sp; k++) {
    const idx = base + k
    const x = k * sp - frac + ((idx * 47 + 19) % 38)
    const h = 62 + ((idx * 31) % 42)
    c.fillStyle = 'rgba(122, 176, 118, 0.42)'
    c.beginPath()
    c.moveTo(x - h * 0.3, GROUND + 6)
    c.lineTo(x, GROUND + 6 - h)
    c.lineTo(x + h * 0.3, GROUND + 6)
    c.closePath()
    c.fill()
    c.beginPath()
    c.moveTo(x - h * 0.24, GROUND + 6 - h * 0.4)
    c.lineTo(x, GROUND + 6 - h * 1.12)
    c.lineTo(x + h * 0.24, GROUND + 6 - h * 0.4)
    c.closePath()
    c.fill()
  }
}

function drawStump(c, o) {
  const { x, w, h } = o
  const y = GROUND - h
  // 桩身
  c.fillStyle = '#8a5a2b'
  rrect(c, x, y + 6, w, h - 6, 6)
  c.fill()
  // 年轮顶面
  c.fillStyle = '#c89b63'
  c.beginPath()
  c.ellipse(x + w / 2, y + 7, w / 2, 7, 0, 0, Math.PI * 2)
  c.fill()
  c.strokeStyle = '#8a5a2b'
  c.lineWidth = 2
  c.beginPath()
  c.ellipse(x + w / 2, y + 7, w / 2 - 6, 3.6, 0, 0, Math.PI * 2)
  c.stroke()
  // 木纹
  c.strokeStyle = 'rgba(70, 45, 20, 0.32)'
  c.lineWidth = 2
  c.beginPath()
  c.moveTo(x + 7, y + 17)
  c.lineTo(x + 7, GROUND)
  c.moveTo(x + w - 7, y + 17)
  c.lineTo(x + w - 7, GROUND)
  c.stroke()
}

function drawHoney(c, x, y) {
  // 罐身
  c.fillStyle = '#ffb703'
  rrect(c, x - 13, y - 13, 26, 26, 7)
  c.fill()
  // 罐盖
  c.fillStyle = '#a9743c'
  rrect(c, x - 16, y - 18, 32, 7, 3)
  c.fill()
  // 高光
  c.fillStyle = 'rgba(255, 255, 255, 0.6)'
  rrect(c, x - 9, y - 7, 5, 13, 2)
  c.fill()
  // 蜜标
  c.fillStyle = '#fff8ec'
  c.font = 'bold 13px "Microsoft YaHei",sans-serif'
  c.textAlign = 'center'
  c.fillText('蜜', x, y + 5)
}

function drawBear(c, x, footY) {
  const legPhase = Math.sin(dist * 0.24) * 7
  // 腿（跑动）
  c.strokeStyle = '#7d5228'
  c.lineWidth = 9
  c.lineCap = 'round'
  c.beginPath()
  c.moveTo(x + 10, footY - 20)
  c.lineTo(x + 10 + legPhase, footY - 2)
  c.moveTo(x + 28, footY - 20)
  c.lineTo(x + 28 - legPhase, footY - 2)
  c.stroke()
  // 身体
  c.fillStyle = '#a9743c'
  c.beginPath()
  c.ellipse(x + 20, footY - 30, 20, 24, 0, 0, Math.PI * 2)
  c.fill()
  // 头
  c.beginPath()
  c.arc(x + 25, footY - 62, 18, 0, Math.PI * 2)
  c.fill()
  // 耳朵
  c.beginPath()
  c.arc(x + 12, footY - 74, 7, 0, Math.PI * 2)
  c.arc(x + 38, footY - 74, 7, 0, Math.PI * 2)
  c.fill()
  // 口鼻
  c.fillStyle = '#f0dcc0'
  c.beginPath()
  c.ellipse(x + 32, footY - 58, 10, 8, 0, 0, Math.PI * 2)
  c.fill()
  // 五官
  c.fillStyle = '#3a2e1f'
  c.beginPath()
  c.arc(x + 27, footY - 66, 2.2, 0, Math.PI * 2)
  c.arc(x + 38, footY - 60, 3, 0, Math.PI * 2)
  c.fill()
  // 暖绿围巾（呼应主题）
  c.fillStyle = '#2f7d4f'
  rrect(c, x + 5, footY - 47, 31, 7, 3)
  c.fill()
  c.beginPath()
  c.moveTo(x + 9, footY - 42)
  c.lineTo(x + 2, footY - 26)
  c.lineTo(x + 12, footY - 28)
  c.closePath()
  c.fill()
}

/* ============ 生命周期 ============ */
onMounted(() => {
  ctx = cv.value && cv.value.getContext('2d')
  readBest()
  window.addEventListener('keydown', onKey)
  // 首帧静态预览
  obstacles = [{ x: 640, w: 38, h: 46, passed: false }]
  items = [{ x: 790, y: GROUND - 90, taken: false }]
  clouds = Array.from({ length: 4 }, (_, i) => ({
    x: (i * 260) % W,
    y: 42 + (i % 3) * 30,
    s: 0.75 + (i % 3) * 0.22
  }))
  draw()
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  raf = 0
  window.removeEventListener('keydown', onKey)
})

defineExpose({ start, jump })
</script>

<style scoped>
.br-wrap {
  position: relative;
  width: 100%;
}
.br-canvas {
  display: block;
  width: 100%;
  height: auto;
  aspect-ratio: 900 / 380;
  border-radius: 18px;
  border: 2px solid rgba(200, 155, 99, 0.5);
  background: #fff1c9;
  cursor: pointer;
  touch-action: none;
}
.br-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  background: rgba(36, 62, 40, 0.45);
  backdrop-filter: blur(3px);
  cursor: pointer;
}
.br-panel {
  text-align: center;
  padding: 22px 34px;
  border-radius: 20px;
  background: linear-gradient(160deg, #fff8e7, #f2f7df);
  border: 2px solid #c89b63;
  box-shadow: 0 12px 34px rgba(47, 125, 79, 0.25);
  max-width: 88%;
}
.br-panel-emoji {
  font-size: 40px;
  display: block;
  line-height: 1.1;
}
.br-panel h3 {
  margin: 6px 0 8px;
  color: var(--forest-deep, #1f5c39);
  font-size: 24px;
}
.br-final {
  margin: 0 0 4px;
  color: var(--text, #3a2e1f);
  font-size: 15px;
}
.br-final b {
  color: #d48600;
  font-size: 19px;
}
.br-newbest {
  margin: 2px 0;
  color: #d48600;
  font-weight: 800;
}
.br-tip {
  margin: 10px 0 16px;
  color: var(--muted, #8a7a63);
  font-size: 13px;
  line-height: 1.6;
}
.br-btn {
  border: 0;
  border-radius: 999px;
  padding: 11px 30px;
  font-size: 15px;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(135deg, var(--forest, #2f7d4f), #4caf6d);
  box-shadow: 0 6px 16px rgba(47, 125, 79, 0.3);
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}
.br-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 9px 20px rgba(47, 125, 79, 0.38);
}
</style>
