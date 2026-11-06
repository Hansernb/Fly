<template>
  <div class="games-page">
    <!-- 飘落树叶氛围 -->
    <div class="gp-leaves" aria-hidden="true">
      <span v-for="i in 10" :key="i" class="gp-leaf" :style="leafStyle(i)">🍃</span>
    </div>

    <header class="gp-nav">
      <button class="gp-back" type="button" @click="emit('back')">← 返回首页</button>
      <div class="gp-brand">🎮 森林小游戏中心</div>
      <div class="gp-record">最高分：{{ best.honey }} / {{ best.mole }} / {{ best.match }} 步</div>
    </header>

    <main class="gp-main">
      <!-- ============ 大厅 ============ -->
      <section v-if="!active" class="lobby">
        <div class="lobby-hero">
          <span class="lh-badge">🍯 狗熊岭限定</span>
          <h1>森林小游戏中心</h1>
          <p>三款手绘小游戏，随手玩一局，看看你能拿多少分～</p>
          <div class="lh-chips">
            <span class="lh-chip">🎯 最佳 接蜂蜜 {{ best.honey }} 分</span>
            <span class="lh-chip">🐿️ 最佳 打松鼠 {{ best.mole }} 分</span>
            <span class="lh-chip">🃏 最少 {{ best.match || '—' }} 步配对</span>
          </div>
        </div>

        <div class="game-grid">
          <article
            v-for="g in GAMES"
            :key="g.id"
            class="game-card"
            :style="{ '--gc': g.color }"
          >
            <div class="gc-cover" :class="'cov-' + g.id">
              <span class="gc-icon">{{ g.icon }}</span>
              <span v-if="best[g.id]" class="gc-best">最佳 {{ best[g.id] }}{{ g.unit }}</span>
            </div>
            <div class="gc-body">
              <h3>{{ g.name }}</h3>
              <p>{{ g.desc }}</p>
              <div class="gc-foot">
                <span class="gc-time">⏱ {{ g.time }}</span>
                <button class="gc-play" type="button" @click="startGame(g.id)">▶ 开始游戏</button>
              </div>
            </div>
          </article>
        </div>

        <p class="lobby-tip">🐻 小提示：接蜂蜜可以用鼠标拖动，也能用键盘 ← → 控制小熊篮子。</p>
      </section>

      <!-- ============ 游戏中 ============ -->
      <section v-else class="stage">
        <div class="stage-bar">
          <div class="stage-title">
            <span class="stage-icon">{{ activeGame.icon }}</span>
            <div>
              <b>{{ activeGame.name }}</b>
              <span class="stage-sub">{{ activeGame.id === 'match' ? '翻出所有成对卡片' : '30 秒挑战' }}</span>
            </div>
          </div>
          <div class="stage-hud">
            <div class="hud-item">
              <span class="hud-k">{{ active === 'match' ? '步数' : '得分' }}</span>
              <span class="hud-v">{{ hudScore }}</span>
            </div>
            <div class="hud-item">
              <span class="hud-k">{{ active === 'match' ? '用时' : '剩余' }}</span>
              <span class="hud-v">{{ hudTime }}</span>
            </div>
            <div class="hud-item best">
              <span class="hud-k">最佳</span>
              <span class="hud-v">{{ best[active] || '—' }}{{ activeGame.unit }}</span>
            </div>
          </div>
          <div class="stage-ops">
            <button class="btn ghost sm" type="button" @click="exitGame">返回大厅</button>
            <button class="btn primary sm" type="button" @click="restart">↻ 重玩</button>
          </div>
        </div>

        <!-- 接蜂蜜：Canvas -->
        <div v-if="active === 'honey'" class="board-wrap">
          <canvas
            ref="honeyCanvas"
            class="honey-canvas"
            @pointermove="onHoneyMove"
            @pointerdown="onHoneyMove"
          ></canvas>
          <p class="stage-hint">拖动画布或按 ← → 移动小熊；接住蜂蜜罐 +10，碰到蜜蜂 −8。</p>
        </div>

        <!-- 打松鼠 -->
        <div v-else-if="active === 'mole'" class="board-wrap">
          <div class="mole-grid">
            <button
              v-for="(cell, i) in moleCells"
              :key="i"
              class="mole-hole"
              :class="{ up: cell.up, hit: cell.hit }"
              type="button"
              @click="whack(i)"
            >
              <span class="hole-grass"></span>
              <span class="mole-emoji">{{ cell.hit ? '💥' : '🐿️' }}</span>
              <span class="hole-dirt"></span>
            </button>
          </div>
          <p class="stage-hint">小松鼠从树洞里探头时点它 +10 分，漏掉不扣分，手速要快～</p>
        </div>

        <!-- 森林翻牌 -->
        <div v-else class="board-wrap">
          <div class="match-grid">
            <button
              v-for="c in matchCards"
              :key="c.uid"
              class="mcard"
              :class="{ flip: c.flipped || c.matched, done: c.matched }"
              type="button"
              @click="flipCard(c)"
            >
              <span class="mcard-inner">
                <span class="mcard-face mcard-back">🌲</span>
                <span class="mcard-face mcard-front">{{ c.icon }}</span>
              </span>
            </button>
          </div>
          <p class="stage-hint">翻开两张相同的小动物即可配对，8 秒内看清位置哦～</p>
        </div>

        <!-- 结算 -->
        <transition name="pop">
          <div v-if="result" class="result-mask">
            <div class="result-card">
              <div class="result-emoji">{{ result.emoji }}</div>
              <h3>{{ result.title }}</h3>
              <p class="result-score">{{ result.score }}</p>
              <p class="result-sub">{{ result.sub }}</p>
              <div class="result-btns">
                <button class="btn ghost" type="button" @click="exitGame">返回大厅</button>
                <button class="btn primary" type="button" @click="restart">再玩一次</button>
              </div>
            </div>
          </div>
        </transition>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  token: { type: String, default: '' },
  user: { type: Object, default: null }
})
const emit = defineEmits(['back'])

/* ===================== 基础配置 ===================== */
const STORAGE_KEY = 'boonie_game_best_v1'

const GAMES = [
  {
    id: 'honey',
    icon: '🍯',
    name: '接蜂蜜',
    color: '#ffb703',
    unit: '分',
    time: '30 秒',
    desc: '移动小熊篮子接住掉落的蜂蜜罐，小心别接到蜜蜂！'
  },
  {
    id: 'mole',
    icon: '🐿️',
    name: '打松鼠',
    color: '#4caf6d',
    unit: '分',
    time: '30 秒',
    desc: '小松鼠从树洞里探头，快点它，看看谁反应最快。'
  },
  {
    id: 'match',
    icon: '🃏',
    name: '森林翻牌',
    color: '#8a6fd1',
    unit: '步',
    time: '不限时',
    desc: '翻开卡片找出成对的森林伙伴，用最少步数全部配对。'
  }
]

const active = ref('')
const best = reactive({ honey: 0, mole: 0, match: 0 })
const result = ref(null)
const activeGame = computed(() => GAMES.find((g) => g.id === active.value) || GAMES[0])

try {
  const raw = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
  ;['honey', 'mole', 'match'].forEach((k) => {
    if (typeof raw[k] === 'number') best[k] = raw[k]
  })
} catch {
  /* 忽略本地存储异常 */
}

function saveBest() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ ...best }))
  } catch {
    /* 忽略 */
  }
}

/* ===================== 成绩落库（MySQL boonie_game_score） ===================== */
/** 登录后拉取自己的历史最佳成绩，覆盖本地缓存 */
async function loadScores() {
  if (!props.token) return
  try {
    const res = await fetch('/api/games/scores', {
      headers: { Authorization: 'Bearer ' + props.token }
    })
    if (!res.ok) return
    const json = await res.json()
    const list = json && Array.isArray(json.data) ? json.data : []
    list.forEach((s) => {
      if (s && best[s.gameId] !== undefined) best[s.gameId] = Number(s.bestScore) || 0
    })
    saveBest()
  } catch {
    /* 后端不可达时沿用本地成绩 */
  }
}

/** 上报一局成绩：后端只在破纪录时更新 best_score */
async function submitScore(gameId, score) {
  if (!props.token) return
  try {
    const res = await fetch('/api/games/scores', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer ' + props.token
      },
      body: JSON.stringify({ gameId, score })
    })
    if (!res.ok) return
    const json = await res.json()
    const serverBest = json && json.data ? Number(json.data.bestScore) : null
    if (serverBest != null && !Number.isNaN(serverBest)) {
      best[gameId] = serverBest
      saveBest()
    }
  } catch {
    /* 断网时仅保留本地记录 */
  }
}

onMounted(loadScores)

/* ===================== 接蜂蜜（Canvas） ===================== */
const honeyCanvas = ref(null)
const honeyScore = ref(0)
const honeyTime = ref(30)

let hCtx = null
let hW = 0
let hH = 0
let hItems = []
let hBasketX = 0
let hPointerX = null
let hRaf = 0
let hLast = 0
let hSpawn = 0
let hLeft = 30
let hRunning = false
const hKeys = { left: false, right: false }

function startHoney() {
  const c = honeyCanvas.value
  if (!c) return
  const rect = c.getBoundingClientRect()
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  hW = Math.max(280, Math.round(rect.width))
  hH = Math.max(240, Math.round(rect.height))
  c.width = Math.round(hW * dpr)
  c.height = Math.round(hH * dpr)
  hCtx = c.getContext('2d')
  hCtx.setTransform(dpr, 0, 0, dpr, 0, 0)

  hItems = []
  hBasketX = hW / 2
  hPointerX = null
  hSpawn = 0
  hLast = 0
  hLeft = 30
  honeyScore.value = 0
  honeyTime.value = 30
  hRunning = true
  hKeys.left = false
  hKeys.right = false
  window.addEventListener('keydown', onHoneyKey)
  window.addEventListener('keyup', onHoneyKey)
  hRaf = requestAnimationFrame(honeyLoop)
}

function onHoneyMove(e) {
  const c = honeyCanvas.value
  if (!c || !hRunning) return
  const r = c.getBoundingClientRect()
  hPointerX = e.clientX - r.left
}

function onHoneyKey(e) {
  if (!hRunning) return
  const k = e.key
  if (k === 'ArrowLeft' || k === 'a' || k === 'A') hKeys.left = e.type === 'keydown'
  if (k === 'ArrowRight' || k === 'd' || k === 'D') hKeys.right = e.type === 'keydown'
  if (k === 'ArrowLeft' || k === 'ArrowRight') e.preventDefault()
}

function honeyLoop(ts) {
  if (!hRunning) return
  if (!hLast) hLast = ts
  const dt = Math.min((ts - hLast) / 1000, 0.05)
  hLast = ts

  hLeft -= dt
  // 只在整秒变化时更新响应式数据，避免每帧重渲染
  const nowSec = Math.max(0, Math.ceil(hLeft))
  if (nowSec !== honeyTime.value) honeyTime.value = nowSec
  if (hLeft <= 0) {
    endHoney()
    return
  }

  // 随时间加快掉落频率
  hSpawn += dt
  const gap = 0.62 - Math.min(0.3, ((30 - hLeft) / 30) * 0.32)
  if (hSpawn >= gap) {
    hSpawn = 0
    hItems.push({
      x: 26 + Math.random() * (hW - 52),
      y: -20,
      vy: 120 + Math.random() * 110,
      r: 14,
      bee: Math.random() < 0.28
    })
  }

  // 篮子移动
  if (hKeys.left) {
    hPointerX = null
    hBasketX -= 340 * dt
  }
  if (hKeys.right) {
    hPointerX = null
    hBasketX += 340 * dt
  }
  if (hPointerX !== null) hBasketX += (hPointerX - hBasketX) * Math.min(1, dt * 16)
  hBasketX = Math.max(46, Math.min(hW - 46, hBasketX))

  // 掉落与接取
  const catchY = hH - 58
  for (let i = hItems.length - 1; i >= 0; i--) {
    const it = hItems[i]
    it.y += it.vy * dt
    if (
      it.y + it.r >= catchY &&
      it.y - it.r < catchY + 30 &&
      Math.abs(it.x - hBasketX) < 46
    ) {
      honeyScore.value = Math.max(0, honeyScore.value + (it.bee ? -8 : 10))
      hItems.splice(i, 1)
      continue
    }
    if (it.y - it.r > hH) hItems.splice(i, 1)
  }

  drawHoney()
  hRaf = requestAnimationFrame(honeyLoop)
}

function drawHoney() {
  const c = hCtx
  if (!c) return

  const bg = c.createLinearGradient(0, 0, 0, hH)
  bg.addColorStop(0, '#eef9e2')
  bg.addColorStop(0.6, '#f8f3da')
  bg.addColorStop(1, '#dcefc2')
  c.fillStyle = bg
  c.fillRect(0, 0, hW, hH)

  // 太阳
  c.fillStyle = 'rgba(255, 206, 84, 0.55)'
  c.beginPath()
  c.arc(hW - 52, 46, 28, 0, Math.PI * 2)
  c.fill()

  // 远景松树
  for (let x = -20; x < hW + 40; x += 78) {
    const th = 52 + ((x * 7) % 23)
    c.fillStyle = 'rgba(120, 180, 110, 0.32)'
    c.beginPath()
    c.moveTo(x, hH - 40)
    c.lineTo(x + 30, hH - 40 - th)
    c.lineTo(x + 60, hH - 40)
    c.closePath()
    c.fill()
  }

  // 草地
  c.fillStyle = '#bfe3a2'
  c.beginPath()
  c.moveTo(0, hH - 40)
  c.quadraticCurveTo(hW * 0.5, hH - 62, hW, hH - 40)
  c.lineTo(hW, hH)
  c.lineTo(0, hH)
  c.closePath()
  c.fill()

  hItems.forEach(drawHoneyItem)
  drawBasket(hBasketX, hH - 42)
}

function drawHoneyItem(it) {
  const c = hCtx
  c.save()
  c.translate(it.x, it.y)

  if (it.bee) {
    c.fillStyle = 'rgba(255, 255, 255, 0.85)'
    c.beginPath()
    c.ellipse(-7, -9, 7, 5, -0.6, 0, Math.PI * 2)
    c.fill()
    c.beginPath()
    c.ellipse(7, -9, 7, 5, 0.6, 0, Math.PI * 2)
    c.fill()
    c.fillStyle = '#ffc93c'
    c.beginPath()
    c.arc(0, 0, 13, 0, Math.PI * 2)
    c.fill()
    c.strokeStyle = '#3a2e1f'
    c.lineWidth = 3
    for (let i = -1; i <= 1; i++) {
      c.beginPath()
      c.arc(0, 0, 13, -0.55 + i * 0.6, 0.15 + i * 0.6)
      c.stroke()
    }
    c.fillStyle = '#3a2e1f'
    c.beginPath()
    c.arc(-4, -4, 1.8, 0, Math.PI * 2)
    c.arc(4, -4, 1.8, 0, Math.PI * 2)
    c.fill()
    c.restore()
    return
  }

  // 蜂蜜罐
  c.fillStyle = '#ffb703'
  c.beginPath()
  c.moveTo(-11, -4)
  c.lineTo(11, -4)
  c.lineTo(8, 12)
  c.quadraticCurveTo(0, 16, -8, 12)
  c.closePath()
  c.fill()
  c.fillStyle = '#f39c12'
  c.fillRect(-12, -10, 24, 7)
  c.fillStyle = '#fff7e0'
  c.beginPath()
  c.arc(0, 5, 4, 0, Math.PI * 2)
  c.fill()
  c.restore()
}

function drawBasket(x, y) {
  const c = hCtx
  c.save()
  c.translate(x, y)

  // 耳朵
  c.fillStyle = '#a9743c'
  c.beginPath()
  c.arc(-26, -34, 10, 0, Math.PI * 2)
  c.arc(26, -34, 10, 0, Math.PI * 2)
  c.fill()

  // 熊头
  c.fillStyle = '#c89b63'
  c.beginPath()
  c.arc(0, -30, 25, 0, Math.PI * 2)
  c.fill()
  c.fillStyle = '#f0dcc0'
  c.beginPath()
  c.ellipse(0, -21, 13, 10, 0, 0, Math.PI * 2)
  c.fill()
  c.fillStyle = '#3a2e1f'
  c.beginPath()
  c.arc(-9, -35, 3, 0, Math.PI * 2)
  c.arc(9, -35, 3, 0, Math.PI * 2)
  c.fill()
  c.beginPath()
  c.ellipse(0, -24, 3.6, 2.8, 0, 0, Math.PI * 2)
  c.fill()

  // 篮子
  c.fillStyle = '#d8a24a'
  c.beginPath()
  c.moveTo(-42, -8)
  c.lineTo(42, -8)
  c.lineTo(32, 24)
  c.quadraticCurveTo(0, 34, -32, 24)
  c.closePath()
  c.fill()
  c.strokeStyle = 'rgba(160, 110, 50, 0.45)'
  c.lineWidth = 2
  for (let i = -3; i <= 3; i++) {
    c.beginPath()
    c.moveTo(i * 11, -8)
    c.lineTo(i * 8, 26)
    c.stroke()
  }
  c.restore()
}

function endHoney() {
  hRunning = false
  if (hRaf) cancelAnimationFrame(hRaf)
  hRaf = 0
  window.removeEventListener('keydown', onHoneyKey)
  window.removeEventListener('keyup', onHoneyKey)

  const score = honeyScore.value
  const isNew = score > best.honey
  if (isNew) {
    best.honey = score
    saveBest()
  }
  submitScore('honey', score)
  result.value = {
    emoji: '🍯',
    title: '时间到！',
    score: `${score} 分`,
    sub: isNew ? '🎉 新纪录！小熊篮子满载而归' : `历史最佳 ${best.honey} 分，再试一次？`
  }
}

/* ===================== 打松鼠 ===================== */
const moleCells = ref(Array.from({ length: 9 }, () => ({ up: false, hit: false })))
const moleScore = ref(0)
const moleTime = ref(30)
let moleEpoch = 0
let moleSpawnTimer = 0
let moleTickTimer = 0

function startMole() {
  moleEpoch++
  const epoch = moleEpoch
  moleCells.value = Array.from({ length: 9 }, () => ({ up: false, hit: false }))
  moleScore.value = 0
  moleTime.value = 30

  moleTickTimer = window.setInterval(() => {
    if (epoch !== moleEpoch) return
    moleTime.value -= 1
    if (moleTime.value <= 0) endMole()
  }, 1000)

  const loopSpawn = () => {
    if (epoch !== moleEpoch) return
    const free = moleCells.value
      .map((c, i) => (!c.up ? i : -1))
      .filter((i) => i >= 0)
    if (free.length) {
      const idx = free[Math.floor(Math.random() * free.length)]
      const cell = moleCells.value[idx]
      cell.up = true
      cell.hit = false
      const stay = 760 + Math.random() * 520
      window.setTimeout(() => {
        if (epoch !== moleEpoch) return
        cell.up = false
        cell.hit = false
      }, stay)
    }
    moleSpawnTimer = window.setTimeout(loopSpawn, 520 + Math.random() * 380)
  }
  loopSpawn()
}

function whack(i) {
  if (result.value) return
  const cell = moleCells.value[i]
  if (!cell || !cell.up || cell.hit) return
  cell.hit = true
  moleScore.value += 10
  const epoch = moleEpoch
  window.setTimeout(() => {
    if (epoch !== moleEpoch) return
    cell.up = false
    cell.hit = false
  }, 240)
}

function endMole() {
  moleEpoch++
  clearInterval(moleTickTimer)
  clearTimeout(moleSpawnTimer)
  moleCells.value.forEach((c) => {
    c.up = false
    c.hit = false
  })
  const score = moleScore.value
  const isNew = score > best.mole
  if (isNew) {
    best.mole = score
    saveBest()
  }
  submitScore('mole', score)
  result.value = {
    emoji: '🐿️',
    title: '时间到！',
    score: `${score} 分`,
    sub: isNew ? '🎉 新纪录！松鼠都跑不过你' : `历史最佳 ${best.mole} 分，再来一局？`
  }
}

/* ===================== 森林翻牌 ===================== */
const ICONS = ['🦌', '🦉', '🐻', '🦊', '🐰', '🐿️']
const matchCards = ref([])
const matchMoves = ref(0)
const matchSeconds = ref(0)
let matchLock = false
let matchTimer = 0
let matchFound = 0

function shuffle(arr) {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[arr[i], arr[j]] = [arr[j], arr[i]]
  }
  return arr
}

function startMatch() {
  const deck = shuffle(
    ICONS.flatMap((icon, i) => [
      { uid: `${i}-a`, icon, flipped: false, matched: false },
      { uid: `${i}-b`, icon, flipped: false, matched: false }
    ])
  )
  matchCards.value = deck
  matchMoves.value = 0
  matchSeconds.value = 0
  matchFound = 0
  matchLock = false
  clearInterval(matchTimer)
  matchTimer = window.setInterval(() => {
    matchSeconds.value += 1
  }, 1000)
}

function flipCard(card) {
  if (matchLock || card.matched || card.flipped) return
  card.flipped = true
  const open = matchCards.value.filter((c) => c.flipped && !c.matched)
  if (open.length < 2) return

  matchMoves.value += 1
  const [a, b] = open
  if (a.icon === b.icon) {
    a.matched = true
    b.matched = true
    matchFound += 1
    if (matchFound === ICONS.length) finishMatch()
    return
  }

  matchLock = true
  window.setTimeout(() => {
    a.flipped = false
    b.flipped = false
    matchLock = false
  }, 720)
}

function finishMatch() {
  clearInterval(matchTimer)
  const moves = matchMoves.value
  const isNew = !best.match || moves < best.match
  if (isNew) {
    best.match = moves
    saveBest()
  }
  submitScore('match', moves)
  window.setTimeout(() => {
    result.value = {
      emoji: '🃏',
      title: '全部配对完成！',
      score: `${moves} 步`,
      sub: isNew
        ? `🎉 新纪录！用时 ${matchSeconds.value} 秒`
        : `历史最佳 ${best.match} 步，用时 ${matchSeconds.value} 秒`
    }
  }, 420)
}

/* ===================== HUD 与生命周期 ===================== */
const hudScore = computed(() => {
  if (active.value === 'honey') return honeyScore.value
  if (active.value === 'mole') return moleScore.value
  return matchMoves.value
})

const hudTime = computed(() => {
  if (active.value === 'honey') return `${honeyTime.value}s`
  if (active.value === 'mole') return `${moleTime.value}s`
  return `${matchSeconds.value}s`
})

function startGame(id) {
  stopAll()
  active.value = id
  result.value = null
  if (id === 'honey') nextTick(startHoney)
  else if (id === 'mole') startMole()
  else startMatch()
}

function restart() {
  startGame(active.value)
}

function exitGame() {
  stopAll()
  result.value = null
  active.value = ''
}

function stopAll() {
  // 接蜂蜜
  hRunning = false
  if (hRaf) cancelAnimationFrame(hRaf)
  hRaf = 0
  window.removeEventListener('keydown', onHoneyKey)
  window.removeEventListener('keyup', onHoneyKey)
  // 打松鼠
  moleEpoch++
  clearInterval(moleTickTimer)
  clearTimeout(moleSpawnTimer)
  // 翻牌
  clearInterval(matchTimer)
  matchLock = false
}

onBeforeUnmount(stopAll)

function leafStyle(i) {
  return {
    left: `${(i * 9 + 4) % 96}%`,
    animationDelay: `${(i % 7) * 0.9}s`,
    animationDuration: `${9 + (i % 5) * 1.6}s`,
    fontSize: `${11 + (i % 4) * 3}px`
  }
}
</script>

<style scoped>
.games-page {
  min-height: 100vh;
  padding: 18px 20px 60px;
  position: relative;
  overflow: hidden;
}

/* 飘叶 */
.gp-leaves {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}
.gp-leaf {
  position: absolute;
  top: -30px;
  opacity: 0.45;
  animation: gpFall linear infinite;
}
@keyframes gpFall {
  0% {
    transform: translateY(-20px) rotate(0deg);
  }
  100% {
    transform: translateY(104vh) rotate(320deg);
  }
}

/* 顶栏 */
.gp-nav {
  position: relative;
  z-index: 1;
  max-width: 1080px;
  margin: 0 auto 22px;
  display: flex;
  align-items: center;
  gap: 14px;
  background: rgba(255, 251, 240, 0.94);
  border: 2px solid var(--wood-light);
  border-radius: 999px;
  padding: 10px 18px;
  box-shadow: 0 10px 26px -14px rgba(31, 92, 57, 0.5);
}
.gp-back {
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
.gp-back:hover {
  background: var(--forest);
  border-color: var(--forest);
  color: #fff;
  transform: translateX(-2px);
}
.gp-brand {
  font-size: 16px;
  font-weight: 900;
  color: var(--forest-deep);
  flex: 1;
}
.gp-record {
  font-size: 12px;
  font-weight: 700;
  color: var(--muted);
  letter-spacing: 0.2px;
}

.gp-main {
  position: relative;
  z-index: 1;
  max-width: 1080px;
  margin: 0 auto;
}

/* ============ 大厅 ============ */
.lobby-hero {
  text-align: center;
  background: linear-gradient(160deg, #fdf6e3, #eef8e4 70%, #e3f2d5);
  border: 2px dashed var(--wood-light);
  border-radius: 24px;
  padding: 30px 22px 26px;
  margin-bottom: 22px;
  position: relative;
  overflow: hidden;
}
.lobby-hero::before {
  content: "";
  position: absolute;
  inset: -40px -40px auto auto;
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, rgba(255, 183, 3, 0.28), transparent 65%);
}
.lh-badge {
  display: inline-block;
  background: rgba(255, 183, 3, 0.2);
  color: #a9740a;
  font-size: 12px;
  font-weight: 800;
  padding: 4px 14px;
  border-radius: 999px;
  margin-bottom: 10px;
}
.lobby-hero h1 {
  font-size: 27px;
  color: var(--forest-deep);
  letter-spacing: 1px;
  margin-bottom: 8px;
}
.lobby-hero p {
  font-size: 14px;
  color: var(--muted);
  margin-bottom: 16px;
}
.lh-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
}
.lh-chip {
  background: #fff;
  border: 2px solid rgba(47, 125, 79, 0.16);
  border-radius: 999px;
  padding: 7px 14px;
  font-size: 12px;
  font-weight: 700;
  color: var(--forest-deep);
  box-shadow: 0 4px 10px -6px rgba(47, 125, 79, 0.4);
}

.game-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(270px, 1fr));
  gap: 20px;
}

.game-card {
  background: #fff;
  border: 2px solid var(--wood-light);
  border-radius: var(--radius);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 22px -12px rgba(47, 125, 79, 0.5);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}
.game-card:hover {
  transform: translateY(-7px);
  border-color: var(--gc);
  box-shadow: 0 24px 40px -20px var(--gc);
}
.gc-cover {
  position: relative;
  height: 138px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at 32% 26%, rgba(255, 255, 255, 0.5), transparent 55%),
    linear-gradient(135deg, var(--gc), color-mix(in srgb, var(--gc) 45%, #2f7d4f));
  overflow: hidden;
}
.gc-cover::after {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 15% 85%, rgba(255, 255, 255, 0.22) 0 26%, transparent 27%),
    radial-gradient(circle at 88% 78%, rgba(255, 255, 255, 0.16) 0 18%, transparent 19%);
}
.gc-icon {
  font-size: 62px;
  line-height: 1;
  filter: drop-shadow(0 8px 12px rgba(0, 0, 0, 0.22));
  transition: transform 0.3s ease;
  z-index: 1;
}
.game-card:hover .gc-icon {
  transform: scale(1.14) rotate(-8deg);
}
.gc-best {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 2;
  background: rgba(255, 255, 255, 0.92);
  color: var(--forest-deep);
  font-size: 11px;
  font-weight: 800;
  padding: 4px 10px;
  border-radius: 999px;
}
.gc-body {
  padding: 16px 18px 18px;
  display: flex;
  flex-direction: column;
  flex: 1;
}
.gc-body h3 {
  font-size: 17px;
  font-weight: 900;
  color: var(--text);
  margin-bottom: 7px;
}
.gc-body p {
  font-size: 13px;
  line-height: 1.6;
  color: var(--muted);
  flex: 1;
  margin-bottom: 14px;
}
.gc-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.gc-time {
  font-size: 12px;
  font-weight: 700;
  color: var(--muted);
}
.gc-play {
  border: none;
  background: var(--honey);
  color: #4a3208;
  font-size: 13px;
  font-weight: 900;
  padding: 9px 18px;
  border-radius: 999px;
  cursor: pointer;
  box-shadow: 0 6px 14px -6px rgba(255, 183, 3, 0.9);
  transition: all 0.16s ease;
}
.gc-play:hover {
  background: #ffc733;
  transform: translateY(-2px);
  box-shadow: 0 10px 20px -8px rgba(255, 183, 3, 0.95);
}
.lobby-tip {
  margin-top: 18px;
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
}

/* ============ 游戏舞台 ============ */
.stage {
  background: rgba(255, 251, 240, 0.97);
  border: 2px solid var(--wood-light);
  border-radius: 24px;
  padding: 16px;
  box-shadow: 0 18px 40px -22px rgba(31, 92, 57, 0.7);
  position: relative;
}
.stage-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  padding: 4px 6px 14px;
  border-bottom: 2px dashed rgba(200, 155, 99, 0.45);
  margin-bottom: 14px;
}
.stage-title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 150px;
}
.stage-icon {
  font-size: 30px;
}
.stage-title b {
  display: block;
  font-size: 16px;
  color: var(--forest-deep);
}
.stage-sub {
  font-size: 11px;
  color: var(--muted);
  font-weight: 700;
}
.stage-hud {
  display: flex;
  gap: 8px;
}
.hud-item {
  background: rgba(47, 125, 79, 0.07);
  border: 2px solid rgba(47, 125, 79, 0.14);
  border-radius: 14px;
  padding: 6px 14px;
  min-width: 66px;
  text-align: center;
}
.hud-item.best {
  background: rgba(255, 183, 3, 0.14);
  border-color: rgba(255, 183, 3, 0.4);
}
.hud-k {
  display: block;
  font-size: 10px;
  font-weight: 700;
  color: var(--muted);
}
.hud-v {
  display: block;
  font-size: 18px;
  font-weight: 900;
  color: var(--forest-deep);
  line-height: 1.2;
}
.hud-item.best .hud-v {
  color: #a9740a;
}
.stage-ops {
  display: flex;
  gap: 8px;
}

.btn {
  border: none;
  font-weight: 900;
  border-radius: 999px;
  padding: 11px 22px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.16s ease;
}
.btn.sm {
  padding: 9px 16px;
  font-size: 12px;
}
.btn.primary {
  background: var(--honey);
  color: #4a3208;
  box-shadow: 0 8px 18px -8px rgba(255, 183, 3, 0.95);
}
.btn.primary:hover {
  background: #ffc733;
  transform: translateY(-2px);
}
.btn.ghost {
  background: #fff;
  color: var(--forest-deep);
  border: 2px solid var(--wood-light);
}
.btn.ghost:hover {
  border-color: var(--forest);
  color: var(--forest);
}

.board-wrap {
  position: relative;
}
.stage-hint {
  margin-top: 12px;
  text-align: center;
  font-size: 12px;
  color: var(--muted);
  font-weight: 600;
}

/* 接蜂蜜 */
.honey-canvas {
  display: block;
  width: 100%;
  height: 380px;
  border-radius: 18px;
  border: 2px solid rgba(200, 155, 99, 0.5);
  cursor: crosshair;
  touch-action: none;
}

/* 打松鼠 */
.mole-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  max-width: 560px;
  margin: 6px auto 0;
}
.mole-hole {
  position: relative;
  aspect-ratio: 1 / 0.86;
  border: 2px solid var(--wood-light);
  border-radius: 18px;
  cursor: pointer;
  overflow: hidden;
  background: linear-gradient(180deg, #eaf7d9, #d5ecbb);
  padding: 0;
  transition: transform 0.12s ease, border-color 0.15s ease;
}
.mole-hole:active {
  transform: scale(0.97);
}
.hole-grass {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 42%;
  background: radial-gradient(circle at 50% 120%, rgba(76, 175, 109, 0.35), transparent 70%);
}
.hole-dirt {
  position: absolute;
  left: 50%;
  bottom: 12%;
  width: 62%;
  height: 26%;
  transform: translateX(-50%);
  background: radial-gradient(ellipse at 50% 30%, #6b4a25, #4a3218);
  border-radius: 50%;
  box-shadow: inset 0 6px 10px rgba(0, 0, 0, 0.45);
}
.mole-emoji {
  position: absolute;
  left: 50%;
  bottom: -18%;
  transform: translate(-50%, 0) scale(0.9);
  font-size: 46px;
  line-height: 1;
  opacity: 0;
  transition: all 0.16s cubic-bezier(0.34, 1.56, 0.64, 1);
  z-index: 2;
}
.mole-hole.up .mole-emoji {
  opacity: 1;
  bottom: 8%;
  transform: translate(-50%, 0) scale(1);
}
.mole-hole.up {
  border-color: var(--forest);
  background: linear-gradient(180deg, #f3fbe6, #d5ecbb);
}
.mole-hole.hit .mole-emoji {
  transform: translate(-50%, 0) scale(1.2) rotate(-8deg);
}

/* 翻牌 */
.match-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  max-width: 620px;
  margin: 6px auto 0;
}
.mcard {
  perspective: 700px;
  border: none;
  background: none;
  padding: 0;
  aspect-ratio: 1 / 1.06;
  cursor: pointer;
}
.mcard-inner {
  position: relative;
  display: block;
  width: 100%;
  height: 100%;
  transform-style: preserve-3d;
  transition: transform 0.42s cubic-bezier(0.4, 0.1, 0.2, 1);
}
.mcard.flip .mcard-inner {
  transform: rotateY(180deg);
}
.mcard-face {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  backface-visibility: hidden;
  font-size: 34px;
  border: 2px solid var(--wood-light);
}
.mcard-back {
  background: linear-gradient(150deg, #2f7d4f, #4caf6d);
  color: #eaf7d9;
  box-shadow: inset 0 0 0 4px rgba(255, 255, 255, 0.22);
}
.mcard-front {
  background: #fffdf5;
  transform: rotateY(180deg);
}
.mcard.done .mcard-front {
  background: #eef9e2;
  border-color: var(--forest);
  box-shadow: 0 0 0 3px rgba(76, 175, 109, 0.22);
}
.mcard.done {
  cursor: default;
}

/* 结算 */
.result-mask {
  position: absolute;
  inset: 0;
  background: rgba(31, 92, 57, 0.32);
  backdrop-filter: blur(3px);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 5;
}
.result-card {
  background: #fffdf5;
  border: 3px solid var(--honey);
  border-radius: 24px;
  padding: 26px 34px 24px;
  text-align: center;
  box-shadow: 0 24px 50px -18px rgba(0, 0, 0, 0.35);
  max-width: 320px;
}
.result-emoji {
  font-size: 46px;
  line-height: 1;
}
.result-card h3 {
  font-size: 17px;
  color: var(--forest-deep);
  margin: 8px 0 4px;
}
.result-score {
  font-size: 32px;
  font-weight: 900;
  color: var(--danger);
  margin-bottom: 6px;
}
.result-sub {
  font-size: 12px;
  color: var(--muted);
  font-weight: 700;
  margin-bottom: 18px;
}
.result-btns {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.pop-enter-active,
.pop-leave-active {
  transition: all 0.22s ease;
}
.pop-enter-from,
.pop-leave-to {
  opacity: 0;
  transform: scale(0.94);
}

@media (max-width: 720px) {
  .games-page {
    padding: 12px 12px 40px;
  }
  .gp-nav {
    flex-wrap: wrap;
    border-radius: 20px;
    gap: 8px;
  }
  .gp-record {
    display: none;
  }
  .lobby-hero {
    padding: 24px 16px 20px;
  }
  .lobby-hero h1 {
    font-size: 22px;
  }
  .stage-bar {
    gap: 10px;
  }
  .stage-hud {
    order: 3;
    width: 100%;
  }
  .hud-item {
    flex: 1;
  }
  .stage-ops {
    margin-left: auto;
  }
  .honey-canvas {
    height: 300px;
  }
  .mole-grid {
    gap: 8px;
  }
  .mole-emoji {
    font-size: 34px;
  }
  .match-grid {
    gap: 8px;
  }
  .mcard-face {
    font-size: 26px;
  }
}
</style>
