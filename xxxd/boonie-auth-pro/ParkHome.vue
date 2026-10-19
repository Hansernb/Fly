<template>
  <div class="park">
    <!-- 全屏绿叶飘落 -->
    <div class="leaves" aria-hidden="true">
      <span v-for="l in leaves" :key="l.id" class="leaf" :style="l.style">{{ l.char }}</span>
    </div>

    <!-- 顶部导航 -->
    <header class="nav">
      <div class="nav-inner">
        <div class="logo">🌲 狗熊岭森林游乐园</div>
        <div class="nav-user">
          <span class="hello">🐻 你好，{{ user.name }}</span>
          <button class="logout" type="button" @click="$emit('logout')">🚪 退出登录</button>
        </div>
      </div>
    </header>

    <!-- 主视觉横幅 -->
    <section class="hero">
      <!-- 萤火虫 -->
      <span v-for="f in fireflies" :key="f.id" class="firefly" :style="f.style" aria-hidden="true"></span>
      <div class="hero-text">
        <h1>欢迎来到狗熊岭森林游乐园</h1>
        <p>和熊大、熊二一起，开启一整天的森林冒险之旅！</p>
        <div class="hero-tags">
          <span>🎢 6 大主题项目</span>
          <span>🎪 3 场森林演出</span>
          <span>🍯 蜂蜜美食街</span>
        </div>
      </div>
      <div class="hero-animals" aria-hidden="true">
        <span class="animal a1">🐻</span>
        <span class="animal a2">🐻</span>
        <span class="animal a3">🦉</span>
        <span class="animal a4">🐿️</span>
      </div>
      <!-- 林间小树 -->
      <div class="hero-trees" aria-hidden="true">
        <span v-for="(t, i) in heroTrees" :key="i" :style="{ animationDelay: i * 0.4 + 's' }">{{ t }}</span>
      </div>
    </section>

    <!-- 森林生态实时条 -->
    <section class="eco-bar">
      <div v-for="e in ecoStats" :key="e.label" class="eco-item">
        <span class="eco-emoji">{{ e.emoji }}</span>
        <div>
          <div class="eco-value">{{ e.value }}</div>
          <div class="eco-label">{{ e.label }}</div>
        </div>
      </div>
      <div class="eco-live"><span class="live-dot"></span>森林生态实时播报</div>
    </section>

    <!-- 游乐项目 -->
    <section class="section">
      <h2 class="section-title">🎡 热门游乐项目</h2>
      <div class="grid">
        <article v-for="a in attractions" :key="a.name" class="ride" :style="{ '--accent': a.color }">
          <div class="ride-emoji">{{ a.emoji }}</div>
          <h3>{{ a.name }}</h3>
          <p>{{ a.desc }}</p>
          <div class="ride-meta">
            <span class="tag">{{ a.tag }}</span>
            <span class="wait">⏱ 排队约 {{ a.wait }} 分钟</span>
          </div>
        </article>
      </div>
    </section>

    <!-- 森林栖息地 -->
    <section class="section">
      <h2 class="section-title">🦉 森林栖息地 · 和小动物们打个招呼</h2>
      <div class="grid">
        <article v-for="h in habitats" :key="h.name" class="habitat" :style="{ '--accent': h.color }">
          <div class="habitat-emoji">{{ h.emoji }}</div>
          <h3>{{ h.name }}</h3>
          <p>{{ h.desc }}</p>
          <div class="habitat-meta">
            <span class="resident">🏡 居民：{{ h.residents }}</span>
            <span class="mood">{{ h.mood }}</span>
          </div>
        </article>
      </div>
    </section>

    <!-- 演出时间表 -->
    <section class="section">
      <h2 class="section-title">🎪 今日森林演出</h2>
      <div class="shows">
        <div v-for="s in shows" :key="s.time" class="show">
          <div class="show-time">{{ s.time }}</div>
          <div class="show-info">
            <h3>{{ s.emoji }} {{ s.name }}</h3>
            <p>{{ s.place }}</p>
          </div>
          <span class="show-tag">{{ s.tag }}</span>
        </div>
      </div>
    </section>

    <!-- 门票 -->
    <section class="section">
      <h2 class="section-title">🎫 门票速览</h2>
      <div class="tickets">
        <div v-for="t in tickets" :key="t.name" class="ticket" :class="{ hot: t.hot }">
          <div v-if="t.hot" class="hot-badge">🔥 热门</div>
          <h3>{{ t.emoji }} {{ t.name }}</h3>
          <div class="price">¥{{ t.price }}<small>/人</small></div>
          <ul>
            <li v-for="(f, i) in t.features" :key="i">✅ {{ f }}</li>
          </ul>
        </div>
      </div>
    </section>

    <!-- 林缘树线 -->
    <div class="tree-line" aria-hidden="true">
      <span v-for="(t, i) in treeLine" :key="i" :style="{ animationDelay: i * 0.25 + 's', fontSize: t.size }">{{ t.char }}</span>
    </div>

    <footer class="footer">
      🌲 狗熊岭森林游乐园 · 和熊大熊二一起守护森林 · 营业时间 09:00 - 21:00
    </footer>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'

const props = defineProps({
  user: { type: Object, required: true },
  token: { type: String, required: true }
})
const emit = defineEmits(['logout'])

// 进入游乐园时向后端校验登录态（token 失效则退回登录页）
onMounted(async () => {
  try {
    const res = await fetch('/api/me', {
      headers: { Authorization: 'Bearer ' + props.token }
    })
    if (!res.ok) {
      localStorage.removeItem('boonie_token')
      emit('logout')
    }
  } catch {
    // 后端暂时不可达时保持页面展示，下次操作再校验
  }
})

// ---- 飘落的绿叶 ----
const leafChars = ['🍃', '🌿', '🍂', '🍀']
const leaves = Array.from({ length: 18 }, (_, i) => ({
  id: i,
  char: leafChars[i % leafChars.length],
  style: {
    left: Math.random() * 100 + '%',
    fontSize: 14 + Math.random() * 16 + 'px',
    animationDuration: 9 + Math.random() * 9 + 's',
    animationDelay: Math.random() * 12 + 's',
    '--sway': 30 + Math.random() * 70 + 'px'
  }
}))

// ---- 萤火虫 ----
const fireflies = Array.from({ length: 10 }, (_, i) => ({
  id: i,
  style: {
    left: 5 + Math.random() * 90 + '%',
    top: 10 + Math.random() * 80 + '%',
    animationDuration: 4 + Math.random() * 4 + 's',
    animationDelay: Math.random() * 4 + 's'
  }
}))

const heroTrees = ['🌲', '🌳', '🌲', '🌲', '🌳']

const treeLine = Array.from({ length: 14 }, (_, i) => ({
  char: i % 3 === 1 ? '🌳' : '🌲',
  size: 26 + Math.random() * 18 + 'px'
}))

const ecoStats = [
  { emoji: '🌲', value: '92%', label: '森林覆盖率' },
  { emoji: '🦌', value: '36 种', label: '栖息动物' },
  { emoji: '🍃', value: '5800个/cm³', label: '负氧离子' },
  { emoji: '🌡️', value: '22°C', label: '林间温度' }
]

const attractions = [
  { emoji: '🎢', name: '树洞过山车', desc: '穿梭在千年古树之间，俯冲、旋转，尖叫声响彻狗熊岭！', tag: '刺激', wait: 25, color: '#e4572e' },
  { emoji: '🎡', name: '星空摩天轮', desc: '升至森林之巅，俯瞰整片狗熊岭的日落与星空。', tag: '浪漫', wait: 15, color: '#7c5cbf' },
  { emoji: '🚂', name: '森林小火车', desc: '呜——哐当哐当，跟着吉吉国王环游五大主题区。', tag: '亲子', wait: 10, color: '#2f7d4f' },
  { emoji: '🍯', name: '蜂蜜漂流', desc: '乘上蜂蜜罐小船，顺着金色溪流来一场甜蜜漂流。', tag: '清凉', wait: 30, color: '#ffb703' },
  { emoji: '🏰', name: '光头强迷宫', desc: '在光头强的小木屋迷宫里寻宝，小心别被发现哦！', tag: '益智', wait: 8, color: '#a9743c' },
  { emoji: '💦', name: '激流勇士', desc: '从瀑布顶端一跃而下，夏日限定的透心凉体验。', tag: '刺激', wait: 40, color: '#2a9d8f' }
]

const habitats = [
  { emoji: '🦉', name: '猫头鹰树屋', desc: '百年老橡树上的圆圆树洞，夜幕降临后这里最热闹。', residents: '猫头鹰一家 · 夜间巡逻员', mood: '😴 白天打盹中', color: '#7c5cbf' },
  { emoji: '🐿️', name: '松鼠松果谷', desc: '满地松果的山谷，小松鼠们正忙着囤积冬天的口粮。', residents: '蹦蹦和它的表兄弟们', mood: '🌰 囤粮大作战', color: '#a9743c' },
  { emoji: '🦌', name: '小鹿溪谷', desc: '清澈溪流穿谷而过，清晨常能遇见来喝水的小鹿。', residents: '鹿妈妈与两只小鹿', mood: '💧 溪边饮水', color: '#2a9d8f' },
  { emoji: '🐇', name: '兔子草甸', desc: '开满野花的柔软草甸，是兔子们赛跑的天然运动场。', residents: '萝卜头田径队', mood: '🏃 晨练进行中', color: '#4caf6d' },
  { emoji: '🐝', name: '蜜蜂花园', desc: '蜂蜜美食街的秘密源头，万花丛中嗡嗡作响。', residents: '熊二的蜂蜜供应商', mood: '🍯 采蜜忙碌中', color: '#ffb703' },
  { emoji: '🦔', name: '刺猬灌木丛', desc: '浆果灌木深处的小窝，缩成球的小刺猬最可爱。', residents: '团团 & 圆圆', mood: '🍓 浆果品鉴会', color: '#e4572e' }
]

const shows = [
  { time: '10:30', emoji: '🎭', name: '熊出没剧场', place: '中央大舞台 · 熊大熊二爆笑登场', tag: '约 40 分钟' },
  { time: '14:00', emoji: '🦜', name: '百鸟森林音乐会', place: '鸟语林剧场 · 涂涂领衔主唱', tag: '约 30 分钟' },
  { time: '19:30', emoji: '🎆', name: '森林星光巡游', place: '主干道 · 花车 + 烟花压轴大秀', tag: '约 50 分钟' }
]

const tickets = [
  { emoji: '🎫', name: '成人票', price: 199, hot: false, features: ['全天畅玩所有项目', '含 1 场剧场演出'] },
  { emoji: '👨‍👩‍👧', name: '亲子套票', price: 399, hot: true, features: ['2 大 1 小全天畅玩', '含全部演出', '赠蜂蜜爆米花一份'] },
  { emoji: '🌙', name: '夜场票', price: 99, hot: false, features: ['17:00 后入园', '含星光巡游观赏位'] }
]
</script>

<style scoped>
.park {
  min-height: 100vh;
  max-width: 1280px;
  margin: 0 auto;
}

/* ---- 全屏绿叶飘落 ---- */
.leaves {
  position: fixed;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 40;
}
.leaf {
  position: absolute;
  top: -40px;
  opacity: 0;
  animation: leafFall linear infinite;
  filter: drop-shadow(0 2px 2px rgba(47, 125, 79, 0.2));
}
@keyframes leafFall {
  0% {
    transform: translate3d(0, -40px, 0) rotate(0deg);
    opacity: 0;
  }
  8% {
    opacity: 0.95;
  }
  25% {
    transform: translate3d(var(--sway, 50px), 25vh, 0) rotate(120deg);
  }
  50% {
    transform: translate3d(calc(var(--sway, 50px) * -0.6), 52vh, 0) rotate(200deg);
  }
  75% {
    transform: translate3d(var(--sway, 50px), 78vh, 0) rotate(300deg);
  }
  100% {
    transform: translate3d(calc(var(--sway, 50px) * -0.4), 108vh, 0) rotate(380deg);
    opacity: 0.5;
  }
}

/* 顶部导航 */
.nav {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 248, 236, 0.92);
  backdrop-filter: blur(8px);
  border-bottom: 2px solid var(--wood-light);
}
.nav-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
}
.logo {
  font-size: 18px;
  font-weight: 800;
  color: var(--forest-deep);
}
.nav-user {
  display: flex;
  align-items: center;
  gap: 14px;
}
.hello {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}
.logout {
  border: 2px solid var(--wood-light);
  background: #fff;
  color: var(--forest-deep);
  font-size: 13px;
  font-weight: 700;
  padding: 8px 16px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.logout:hover {
  background: var(--forest);
  border-color: var(--forest);
  color: #fff;
}

/* 主视觉 */
.hero {
  margin: 22px 24px 0;
  padding: 56px 40px 84px;
  border-radius: var(--radius);
  background:
    radial-gradient(circle at 85% 20%, rgba(255, 183, 3, 0.25) 0, transparent 40%),
    linear-gradient(120deg, var(--forest-deep) 0%, var(--forest) 60%, var(--leaf) 100%);
  color: #fff;
  position: relative;
  overflow: hidden;
  animation: cardIn 0.7s cubic-bezier(0.22, 1, 0.36, 1) both;
}
@keyframes cardIn {
  from { opacity: 0; transform: translateY(24px); }
  to { opacity: 1; transform: translateY(0); }
}
.hero h1 {
  font-size: 34px;
  font-weight: 900;
  margin-bottom: 12px;
}
.hero p {
  font-size: 16px;
  opacity: 0.92;
  margin-bottom: 20px;
}
.hero-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.hero-tags span {
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.35);
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
}
.hero-animals .animal {
  position: absolute;
  font-size: 44px;
  animation: float 4s ease-in-out infinite;
  filter: drop-shadow(0 6px 10px rgba(0, 0, 0, 0.2));
}
.a1 { right: 60px; top: 30px; }
.a2 { right: 140px; bottom: 66px; animation-delay: 0.8s; }
.a3 { right: 220px; top: 44px; font-size: 34px; animation-delay: 1.6s; }
.a4 { right: 90px; bottom: 100px; font-size: 30px; animation-delay: 2.4s; }
@keyframes float {
  0%, 100% { transform: translateY(0) rotate(-4deg); }
  50% { transform: translateY(-12px) rotate(4deg); }
}

/* 萤火虫 */
.firefly {
  position: absolute;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #ffe27a;
  box-shadow: 0 0 12px 4px rgba(255, 226, 122, 0.75);
  animation: fly ease-in-out infinite;
  pointer-events: none;
}
@keyframes fly {
  0%, 100% {
    transform: translate(0, 0) scale(1);
    opacity: 0.2;
  }
  25% {
    transform: translate(18px, -22px) scale(1.2);
    opacity: 1;
  }
  50% {
    transform: translate(-14px, -38px) scale(0.9);
    opacity: 0.5;
  }
  75% {
    transform: translate(22px, -12px) scale(1.1);
    opacity: 0.9;
  }
}

/* 主视觉底部小树 */
.hero-trees {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 8px;
  display: flex;
  justify-content: space-around;
  pointer-events: none;
}
.hero-trees span {
  font-size: 34px;
  display: inline-block;
  transform-origin: bottom center;
  animation: sway 3.6s ease-in-out infinite;
  opacity: 0.85;
}
@keyframes sway {
  0%, 100% { transform: rotate(-3deg); }
  50% { transform: rotate(3deg); }
}

/* 森林生态实时条 */
.eco-bar {
  margin: 18px 24px 0;
  background: #fff;
  border: 2px solid var(--wood-light);
  border-radius: var(--radius);
  padding: 16px 24px;
  display: flex;
  align-items: center;
  gap: 32px;
  flex-wrap: wrap;
}
.eco-item {
  display: flex;
  align-items: center;
  gap: 10px;
}
.eco-emoji {
  font-size: 26px;
  animation: bobY 2.6s ease-in-out infinite;
}
@keyframes bobY {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}
.eco-value {
  font-size: 17px;
  font-weight: 900;
  color: var(--forest-deep);
}
.eco-label {
  font-size: 12px;
  color: var(--muted);
}
.eco-live {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 700;
  color: var(--forest);
}
.live-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--leaf);
  animation: pulse 1.6s ease-out infinite;
}
@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(76, 175, 109, 0.6); }
  70% { box-shadow: 0 0 0 9px rgba(76, 175, 109, 0); }
  100% { box-shadow: 0 0 0 0 rgba(76, 175, 109, 0); }
}

/* 通用区块 */
.section {
  padding: 40px 24px 0;
}
.section-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--forest-deep);
  margin-bottom: 20px;
}

/* 卡片网格 */
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 18px;
}

/* 游乐项目卡片 */
.ride {
  background: #fff;
  border: 2px solid var(--wood-light);
  border-top: 5px solid var(--accent, var(--forest));
  border-radius: var(--radius);
  padding: 22px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.ride:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 36px rgba(47, 125, 79, 0.18);
}
.ride-emoji {
  font-size: 42px;
  margin-bottom: 10px;
  display: inline-block;
  transition: transform 0.25s ease;
}
.ride:hover .ride-emoji {
  transform: scale(1.18) rotate(-8deg);
}
.ride h3 {
  font-size: 17px;
  font-weight: 800;
  color: var(--text);
  margin-bottom: 8px;
}
.ride p {
  font-size: 13px;
  color: var(--muted);
  line-height: 1.6;
  margin-bottom: 14px;
}
.ride-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.tag {
  background: var(--accent, var(--forest));
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 12px;
  border-radius: 999px;
}
.wait {
  font-size: 12px;
  color: var(--muted);
}

/* 栖息地卡片 */
.habitat {
  background: linear-gradient(160deg, #ffffff 0%, #f2f9ef 100%);
  border: 2px solid var(--wood-light);
  border-radius: var(--radius);
  padding: 22px;
  position: relative;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.habitat::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 5px;
  background: linear-gradient(90deg, var(--accent, var(--forest)), transparent);
}
.habitat:hover {
  transform: translateY(-6px) rotate(-0.5deg);
  box-shadow: 0 16px 36px rgba(47, 125, 79, 0.18);
}
.habitat-emoji {
  font-size: 42px;
  margin-bottom: 10px;
  display: inline-block;
  animation: bobY 3s ease-in-out infinite;
}
.habitat:hover .habitat-emoji {
  animation: wiggle 0.5s ease-in-out infinite;
}
@keyframes wiggle {
  0%, 100% { transform: rotate(-8deg) scale(1.15); }
  50% { transform: rotate(8deg) scale(1.15); }
}
.habitat h3 {
  font-size: 17px;
  font-weight: 800;
  color: var(--text);
  margin-bottom: 8px;
}
.habitat p {
  font-size: 13px;
  color: var(--muted);
  line-height: 1.6;
  margin-bottom: 14px;
}
.habitat-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}
.resident {
  font-size: 12px;
  color: var(--forest-deep);
  font-weight: 600;
}
.mood {
  font-size: 12px;
  font-weight: 700;
  color: var(--accent, var(--forest));
  background: rgba(47, 125, 79, 0.08);
  padding: 4px 10px;
  border-radius: 999px;
  white-space: nowrap;
}

/* 演出 */
.shows {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.show {
  display: flex;
  align-items: center;
  gap: 18px;
  background: #fff;
  border: 2px solid var(--wood-light);
  border-radius: var(--radius);
  padding: 16px 22px;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.show:hover {
  transform: translateX(6px);
  box-shadow: 0 10px 26px rgba(47, 125, 79, 0.14);
}
.show-time {
  font-size: 20px;
  font-weight: 900;
  color: var(--forest);
  min-width: 64px;
}
.show-info {
  flex: 1;
}
.show-info h3 {
  font-size: 15px;
  font-weight: 800;
  margin-bottom: 4px;
}
.show-info p {
  font-size: 13px;
  color: var(--muted);
}
.show-tag {
  font-size: 12px;
  color: var(--wood);
  font-weight: 700;
  white-space: nowrap;
}

/* 门票 */
.tickets {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 18px;
}
.ticket {
  position: relative;
  background: #fff;
  border: 2px solid var(--wood-light);
  border-radius: var(--radius);
  padding: 26px 22px;
  text-align: center;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.ticket:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 36px rgba(47, 125, 79, 0.18);
}
.ticket.hot {
  border-color: var(--honey);
  box-shadow: 0 10px 30px rgba(255, 183, 3, 0.22);
}
.hot-badge {
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  background: linear-gradient(180deg, #ffc83d, var(--honey));
  color: #5a3d00;
  font-size: 12px;
  font-weight: 800;
  padding: 4px 14px;
  border-radius: 999px;
  box-shadow: 0 4px 10px rgba(255, 183, 3, 0.4);
}
.ticket h3 {
  font-size: 16px;
  font-weight: 800;
  margin-bottom: 10px;
}
.price {
  font-size: 34px;
  font-weight: 900;
  color: var(--forest-deep);
  margin-bottom: 14px;
}
.price small {
  font-size: 13px;
  font-weight: 600;
  color: var(--muted);
}
.ticket ul {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.ticket li {
  font-size: 13px;
  color: var(--muted);
}

/* 林缘树线 */
.tree-line {
  margin-top: 44px;
  padding: 0 16px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  pointer-events: none;
}
.tree-line span {
  display: inline-block;
  transform-origin: bottom center;
  animation: sway 4s ease-in-out infinite;
  filter: drop-shadow(0 3px 4px rgba(47, 125, 79, 0.25));
}

/* 页脚 */
.footer {
  margin-top: 8px;
  padding: 20px 24px 34px;
  text-align: center;
  font-size: 13px;
  color: var(--muted);
  border-top: 2px dashed var(--wood-light);
}

/* 窄屏适配 */
@media (max-width: 640px) {
  .hero { padding: 36px 22px 74px; }
  .hero h1 { font-size: 24px; }
  .hero-animals { display: none; }
  .show { flex-wrap: wrap; }
  .eco-bar { gap: 18px; }
  .eco-live { margin-left: 0; }
}
</style>
