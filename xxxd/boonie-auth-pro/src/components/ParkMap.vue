<template>
  <div class="map-page" :class="{ embedded }">
    <!-- 独立页顶部栏（内嵌首页时隐藏，由首页 section-title 提供标题） -->
    <div v-if="!embedded" class="map-top">
      <button class="back-btn" type="button" @click="$emit('back')">← 返回首页</button>
      <h1 class="map-title">🗺️ 狗熊岭手绘游园地图</h1>
      <p class="map-sub">点击地图上的标记点，查看项目 / 餐厅 / 卫生间 / 停车场位置与简介</p>
    </div>

    <!-- 图例筛选（保留顶部分类筛选） -->
    <div class="legend">
      <button
        v-for="cat in categories"
        :key="cat.key"
        type="button"
        class="legend-item"
        :class="{ off: !active[cat.key] }"
        :style="{ '--c': cat.color }"
        @click="toggle(cat.key)"
      >
        <span class="dot" v-html="legendIcon(cat)"></span>{{ cat.label }}
      </button>
    </div>

    <!-- 手绘地图区 -->
    <div class="map-stage" @click.self="selected = null">
      <!-- 手绘底图（SVG 有机块面 + 卡通松树 / 湖面 / 石板路 / 指北针 / 点缀） -->
      <svg class="map-base" viewBox="0 0 1000 680" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
        <defs>
          <linearGradient id="grass" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stop-color="#eef3cf" />
            <stop offset="1" stop-color="#dfedb4" />
          </linearGradient>
          <radialGradient id="lake" cx="50%" cy="40%" r="65%">
            <stop offset="0" stop-color="#cdeef4" />
            <stop offset="1" stop-color="#9fd6e6" />
          </radialGradient>
          <linearGradient id="wood" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stop-color="#d79a5a" />
            <stop offset="1" stop-color="#b3753a" />
          </linearGradient>
          <!-- 手绘抖动滤镜：让规整路径产生自然的手绘感 -->
          <filter id="rough" x="-5%" y="-5%" width="110%" height="110%">
            <feTurbulence type="fractalNoise" baseFrequency="0.013" numOctaves="2" seed="7" result="n" />
            <feDisplacementMap in="SourceGraphic" in2="n" scale="3.2" xChannelSelector="R" yChannelSelector="G" />
          </filter>
          <!-- 纸张颗粒纹理 -->
          <filter id="paper">
            <feTurbulence type="fractalNoise" baseFrequency="0.9" numOctaves="2" result="n" />
            <feColorMatrix in="n" type="matrix"
              values="0 0 0 0 0  0 0 0 0 0  0 0 0 0 0  0 0 0 0.05 0" />
          </filter>

          <!-- 成簇卡通松树 -->
          <g id="pine">
            <ellipse cx="0" cy="2" rx="16" ry="4" fill="#00000018" />
            <rect x="-3.5" y="-16" width="7" height="18" rx="3" fill="#9c6b3f" />
            <polygon points="0,-44 16,-12 -16,-12" fill="#4f9a4f" />
            <polygon points="0,-56 12,-26 -12,-26" fill="#62b062" />
            <polygon points="0,-66 9,-38 -9,-38" fill="#76c576" />
            <polygon points="0,-44 7,-21 -7,-21" fill="#8ad18a" opacity="0.65" />
          </g>

          <!-- 路灯 -->
          <g id="lamp">
            <rect x="-2.5" y="-30" width="5" height="34" rx="2.5" fill="#8a5a32" />
            <circle cx="0" cy="-32" r="10" fill="#ffe27a" opacity="0.3" />
            <circle cx="0" cy="-32" r="5.5" fill="#ffe27a" stroke="#caa24a" stroke-width="1.5" />
            <rect x="-7" y="2" width="14" height="3.5" rx="1.5" fill="#7a4e2a" />
          </g>

          <!-- 长椅 -->
          <g id="bench">
            <rect x="-17" y="-7" width="34" height="5" rx="2" fill="#b07b43" />
            <rect x="-17" y="-1" width="34" height="4" rx="2" fill="#9c6b3f" />
            <rect x="-15" y="3" width="4" height="7" fill="#8a5a32" />
            <rect x="11" y="3" width="4" height="7" fill="#8a5a32" />
          </g>

          <!-- 小花（粉 / 黄） -->
          <g id="flowerP">
            <g fill="#f48fb1">
              <circle cx="0" cy="-5" r="3.2" /><circle cx="4.8" cy="-1.5" r="3.2" />
              <circle cx="3" cy="4" r="3.2" /><circle cx="-3" cy="4" r="3.2" /><circle cx="-4.8" cy="-1.5" r="3.2" />
            </g>
            <circle r="2.8" fill="#ffd23f" />
          </g>
          <g id="flowerY">
            <g fill="#ffd23f">
              <circle cx="0" cy="-5" r="3.2" /><circle cx="4.8" cy="-1.5" r="3.2" />
              <circle cx="3" cy="4" r="3.2" /><circle cx="-3" cy="4" r="3.2" /><circle cx="-4.8" cy="-1.5" r="3.2" />
            </g>
            <circle r="2.8" fill="#e8893a" />
          </g>
        </defs>

        <!-- 草地 -->
        <rect x="0" y="0" width="1000" height="680" fill="url(#grass)" />
        <!-- 暖黄草甸斑块 -->
        <path d="M620 200 Q700 178 762 232 Q718 274 636 252 Z" fill="#f6e7a8" opacity="0.55" filter="url(#rough)" />
        <path d="M196 418 Q280 398 332 452 Q278 492 206 470 Z" fill="#f6e7a8" opacity="0.5" filter="url(#rough)" />
        <path d="M120 300 Q180 286 214 322 Q170 352 122 338 Z" fill="#f6e7a8" opacity="0.45" filter="url(#rough)" />

        <!-- 树林：成簇卡通松树（替换原色块） -->
        <g filter="url(#rough)">
          <use
            v-for="(p, i) in pines"
            :key="'pine' + i"
            href="#pine"
            :transform="`translate(${p.x} ${p.y}) scale(${p.s}) rotate(${p.r})`"
          />
        </g>

        <!-- 湖面：波纹 + 木栈道 -->
        <g filter="url(#rough)">
          <path d="M360 440 Q338 502 382 548 Q432 598 512 576 Q582 560 576 500 Q570 448 520 430 Q438 410 360 440 Z"
            fill="url(#lake)" stroke="#7fc4d6" stroke-width="3" />
          <!-- 波纹 -->
          <path d="M398 470 q42 18 92 4" fill="none" stroke="#ffffff" stroke-width="2.4" stroke-linecap="round" opacity="0.55" />
          <path d="M412 502 q54 20 116 2" fill="none" stroke="#ffffff" stroke-width="2.4" stroke-linecap="round" opacity="0.5" />
          <path d="M392 532 q44 16 84 1" fill="none" stroke="#ffffff" stroke-width="2.2" stroke-linecap="round" opacity="0.45" />
          <path d="M470 552 q40 12 78 0" fill="none" stroke="#ffffff" stroke-width="2" stroke-linecap="round" opacity="0.4" />
          <!-- 木栈道 -->
          <g transform="translate(468 514) rotate(-6)">
            <rect x="-98" y="-13" width="196" height="26" rx="5" fill="url(#wood)" stroke="#8a5a32" stroke-width="2" />
            <rect x="-98" y="-15" width="196" height="3.5" fill="#e3b079" />
            <g stroke="#8a5a32" stroke-width="1.6" opacity="0.8">
              <line x1="-72" y1="-13" x2="-72" y2="13" />
              <line x1="-46" y1="-13" x2="-46" y2="13" />
              <line x1="-20" y1="-13" x2="-20" y2="13" />
              <line x1="6" y1="-13" x2="6" y2="13" />
              <line x1="32" y1="-13" x2="32" y2="13" />
              <line x1="58" y1="-13" x2="58" y2="13" />
              <line x1="84" y1="-13" x2="84" y2="13" />
            </g>
            <rect x="-90" y="10" width="6" height="14" rx="2" fill="#7a4e2a" />
            <rect x="84" y="10" width="6" height="14" rx="2" fill="#7a4e2a" />
          </g>
        </g>

        <!-- 石板小路（替换原虚线）：沙土路基 + 踏石 -->
        <g>
          <path
            v-for="(d, i) in roadPaths"
            :key="'road' + i"
            :d="d"
            fill="none"
            stroke="#ead9b0"
            stroke-width="32"
            stroke-linecap="round"
            stroke-linejoin="round"
            opacity="0.85"
          />
          <g filter="url(#rough)">
            <g v-for="(s, i) in stones" :key="'stone' + i">
              <ellipse
                :cx="s.x" :cy="s.y" :rx="15 * s.sx" :ry="11 * s.sy"
                :transform="`rotate(${s.rot} ${s.x} ${s.y})`"
                :fill="s.k > 0.5 ? '#ece0c4' : '#e3d4b0'"
                stroke="#cdbb95" stroke-width="1.4"
              />
              <ellipse
                :cx="s.x - 3" :cy="s.y - 2.5" :rx="7 * s.sx" :ry="4 * s.sy"
                :transform="`rotate(${s.rot} ${s.x} ${s.y})`"
                fill="#fff7e6" opacity="0.55"
              />
            </g>
          </g>
        </g>

        <!-- 小路小花点缀 -->
        <g filter="url(#rough)">
          <use
            v-for="(f, i) in flowers"
            :key="'fl' + i"
            :href="f.c === 'y' ? '#flowerY' : '#flowerP'"
            :transform="`translate(${f.x} ${f.y}) scale(${f.s})`"
          />
        </g>

        <!-- 长椅 / 路灯点缀 -->
        <g filter="url(#rough)">
          <use v-for="(b, i) in benches" :key="'bn' + i" href="#bench" :transform="`translate(${b.x} ${b.y})`" />
          <use v-for="(l, i) in lamps" :key="'lp' + i" href="#lamp" :transform="`translate(${l.x} ${l.y})`" />
        </g>

        <!-- 南大门入口 -->
        <g transform="translate(500 632)">
          <rect x="-36" y="-20" width="7" height="24" fill="#8a5a32" />
          <rect x="29" y="-20" width="7" height="24" fill="#8a5a32" />
          <path d="M-36 -20 Q0 -38 36 -20 L36 -11 Q0 -27 -36 -11 Z" fill="#e4572e" />
          <circle cx="0" cy="-30" r="2.6" fill="#ffe27a" />
        </g>
        <text x="500" y="668" text-anchor="middle" font-size="18" fill="#5a7d4f" font-weight="700"
          font-family="'PingFang SC','Microsoft YaHei',sans-serif">南大门入口</text>

        <!-- 指北针 -->
        <g transform="translate(132 606)">
          <circle r="34" fill="#fff8e6" stroke="#b98a4b" stroke-width="3" />
          <circle r="34" fill="none" stroke="#e8c98a" stroke-width="1" stroke-dasharray="2 4" />
          <polygon points="0,-27 9,2 0,7 -9,2" fill="#e4572e" />
          <polygon points="0,27 9,-2 0,-7 -9,-2" fill="#5a7d4f" />
          <circle r="4.5" fill="#b98a4b" />
          <text x="0" y="-32" font-size="13" font-weight="bold" fill="#e4572e" text-anchor="middle">N</text>
          <text x="0" y="46" font-size="11" font-weight="bold" fill="#5a7d4f" text-anchor="middle">S</text>
        </g>

        <!-- 纸张颗粒 -->
        <rect x="0" y="0" width="1000" height="680" filter="url(#paper)" opacity="0.6" />
      </svg>

      <!-- 立体卡通图钉（带小建筑插画） -->
      <button
        v-for="m in markers"
        v-show="active[m.type]"
        :key="m.id"
        type="button"
        class="pin"
        :class="['cat-' + m.type, { active: selected && selected.id === m.id }]"
        :style="{ left: m.x + '%', top: m.y + '%', '--c': catOf(m.type).color }"
        @click.stop="select(m)"
      >
        <span class="pin-shadow"></span>
        <span class="pin-body">
          <span class="pin-disc" v-html="artOf(m)"></span>
        </span>
      </button>

      <!-- 简介弹层（保留点击弹窗） -->
      <Transition name="pop">
        <div
          v-if="selected"
          class="popover"
          :class="'cat-' + selected.type"
          :style="{ left: selected.x + '%', top: selected.y + '%', '--c': catOf(selected.type).color }"
          @click.stop
        >
          <button class="pop-close" type="button" @click="selected = null">×</button>
          <div class="pop-head">
            <span class="pop-art" v-html="artOf(selected)"></span>
            <div>
              <div class="pop-name">{{ selected.name }}</div>
              <div class="pop-tag">{{ catOf(selected.type).label }}</div>
            </div>
          </div>
          <p class="pop-desc">{{ selected.desc }}</p>
          <div v-if="selected.extra" class="pop-extra">{{ selected.extra }}</div>
        </div>
      </Transition>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { attractionsDB } from '../data/attractions.js'

const props = defineProps({
  // 内嵌首页时隐藏返回按钮与标题（首页 section 自带标题）
  embedded: { type: Boolean, default: false }
})
const emit = defineEmits(['back'])

const categories = [
  { key: 'ride', label: '游乐项目', emoji: '🎡', color: '#e4572e' },
  { key: 'food', label: '餐厅', emoji: '🍔', color: '#e8a13a' },
  { key: 'wc', label: '卫生间', emoji: '🚻', color: '#4a90d9' },
  { key: 'park', label: '停车场', emoji: '🅿️', color: '#6fbf73' }
]
const catMap = Object.fromEntries(categories.map((c) => [c.key, c]))
function catOf(k) {
  return catMap[k] || { label: '', color: '#888' }
}

// 图例小图标（复用建筑插画，让筛选条也“手绘化”）
function legendIcon(cat) {
  const key = cat.key === 'ride' ? 'coaster' : cat.key
  return buildingSvg(key)
}

// ---- 小建筑插画（立体卡通图钉 / 弹窗 / 图例共用）----
const RIDE_ART = { 1: 'coaster', 2: 'ferris', 3: 'train', 4: 'raft', 5: 'maze', 6: 'slide' }
function artOf(m) {
  const key = m.type === 'ride' ? (RIDE_ART[m.id] || 'coaster') : m.type
  return buildingSvg(key)
}
function buildingSvg(key) {
  const s = {
    coaster: `<svg viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg"><path d="M3 30 Q11 10 20 22 Q27 31 34 12" fill="none" stroke="#7c5cbf" stroke-width="3" stroke-linecap="round"/><circle cx="20" cy="22" r="5.5" fill="none" stroke="#7c5cbf" stroke-width="3"/><rect x="16.5" y="25" width="7" height="6" rx="2" fill="#e4572e"/></svg>`,
    ferris: `<svg viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg"><line x1="20" y1="19" x2="8" y2="35" stroke="#8a5a32" stroke-width="2"/><line x1="20" y1="19" x2="32" y2="35" stroke="#8a5a32" stroke-width="2"/><circle cx="20" cy="18" r="13" fill="none" stroke="#e8a13a" stroke-width="2.4"/><g stroke="#e8a13a" stroke-width="1.3"><line x1="20" y1="18" x2="20" y2="5"/><line x1="20" y1="18" x2="33" y2="18"/><line x1="20" y1="18" x2="20" y2="31"/><line x1="20" y1="18" x2="7" y2="18"/><line x1="20" y1="18" x2="29" y2="9"/><line x1="20" y1="18" x2="29" y2="27"/><line x1="20" y1="18" x2="11" y2="27"/><line x1="20" y1="18" x2="11" y2="9"/></g><circle cx="20" cy="18" r="2.4" fill="#e4572e"/><g fill="#e8a13a"><circle cx="20" cy="5" r="2"/><circle cx="33" cy="18" r="2"/><circle cx="20" cy="31" r="2"/><circle cx="7" cy="18" r="2"/></g></svg>`,
    train: `<svg viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg"><rect x="6" y="34" width="28" height="2.5" fill="#9c6b3f"/><rect x="9" y="18" width="18" height="12" rx="3" fill="#2f7d4f"/><rect x="22" y="11" width="9" height="10" rx="2" fill="#2f7d4f"/><rect x="25" y="5" width="4" height="7" rx="1" fill="#2f7d4f"/><rect x="11" y="21" width="4" height="4" rx="1" fill="#ffe27a"/><rect x="17" y="21" width="4" height="4" rx="1" fill="#ffe27a"/><circle cx="14" cy="32" r="3.2" fill="#3a3a3a"/><circle cx="24" cy="32" r="3.2" fill="#3a3a3a"/></svg>`,
    raft: `<svg viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg"><ellipse cx="20" cy="22" rx="13" ry="4" fill="#bfe6ef"/><path d="M6 23 Q20 33 34 23 L31 29 Q20 37 9 29 Z" fill="#c98a4b"/><path d="M10 19 q4 -4 8 0 M18 19 q4 -4 8 0" stroke="#fff" stroke-width="1.6" fill="none" stroke-linecap="round"/><circle cx="20" cy="20" r="2.6" fill="#ffb703"/></svg>`,
    maze: `<svg viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg"><rect x="7" y="9" width="26" height="24" rx="2" fill="#6fb86f"/><path d="M11 13 H29 V19 H17 V25 H27 V31 H13" fill="none" stroke="#3f7d3f" stroke-width="2.6" stroke-linecap="round"/><rect x="7" y="9" width="26" height="24" rx="2" fill="none" stroke="#4f9a4f" stroke-width="1.5"/></svg>`,
    slide: `<svg viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg"><path d="M9 7 Q31 9 31 22 Q31 34 11 34" fill="none" stroke="#2a9d8f" stroke-width="6" stroke-linecap="round"/><path d="M9 7 Q31 9 31 22 Q31 34 11 34" fill="none" stroke="#bfe6ef" stroke-width="2.4" stroke-linecap="round"/><circle cx="11" cy="34" r="3" fill="#e4572e"/><path d="M8 30 q3 4 6 1" stroke="#2a9d8f" stroke-width="2" fill="none" stroke-linecap="round"/></svg>`,
    house: `<svg viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg"><rect x="8" y="18" width="24" height="16" rx="1.5" fill="#f3e2c0"/><path d="M6 18 L20 8 L34 18 Z" fill="#e4572e"/><rect x="16" y="22" width="8" height="12" rx="1" fill="#9c6b3f"/><rect x="10" y="21" width="4" height="4" rx="1" fill="#2f7d4f"/><rect x="26" y="21" width="4" height="4" rx="1" fill="#2f7d4f"/></svg>`,
    wc: `<svg viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg"><rect x="10" y="11" width="20" height="21" rx="2" fill="#4a90d9"/><rect x="13" y="15" width="14" height="10" rx="1.5" fill="#fff"/><text x="20" y="23" font-size="8" font-weight="bold" fill="#4a90d9" text-anchor="middle" font-family="sans-serif">WC</text><rect x="17" y="27" width="6" height="5" rx="1" fill="#2f6fae"/></svg>`,
    park: `<svg viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg"><rect x="13" y="5" width="14" height="13" rx="2" fill="#6fbf73"/><text x="20" y="15.5" font-size="10" font-weight="bold" fill="#fff" text-anchor="middle" font-family="sans-serif">P</text><rect x="18.5" y="18" width="3" height="16" rx="1" fill="#8a5a32"/><rect x="9" y="34" width="22" height="3.5" rx="1.5" fill="#8a5a32"/></svg>`
  }
  return s[key] || s.coaster
}

// ---- 游乐项目在地图上的坐标（基于地图比例百分比），按 attractions.js 的 id 对应 ----
const RIDE_POS = {
  1: { x: 72, y: 22 }, // 树洞过山车
  2: { x: 50, y: 12 }, // 星空摩天轮
  3: { x: 30, y: 46 }, // 森林小火车
  4: { x: 60, y: 62 }, // 蜂蜜漂流
  5: { x: 26, y: 20 }, // 光头强迷宫
  6: { x: 80, y: 44 }  // 激流勇士
}

// 游乐项目标记点 —— 直接由项目数据生成（名称 / emoji / 排队 / 身高均来自 attractions.js）
const rideMarkers = attractionsDB.map((a) => ({
  id: a.id,
  type: 'ride',
  x: (RIDE_POS[a.id] || { x: 50, y: 50 }).x,
  y: (RIDE_POS[a.id] || { x: 50, y: 50 }).y,
  name: a.name,
  desc: a.desc,
  extra: `⏱ 平均排队 ${a.wait} 分钟 · 📏 身高 ${a.height}`
}))

// 餐厅 / 卫生间 / 停车场（项目暂无对应数据源，内置）
const staticMarkers = [
  { id: 'f1', type: 'food', x: 44, y: 34, name: '蜂蜜餐厅', desc: '熊二最爱的蜂蜜烤翅与森林轻食，露天座位可看林景。', extra: '🍽️ 营业 11:00–21:00 · 人均 ¥68' },
  { id: 'f2', type: 'food', x: 78, y: 56, name: '林间小食站', desc: '冰淇淋、烤肠、鲜榨果汁，快速补充能量。', extra: '⚡ 即买即走 · 人均 ¥25' },
  { id: 'w1', type: 'wc', x: 40, y: 54, name: '中心卫生间', desc: '园区中部主卫生间，含母婴室与无障碍设施。', extra: '🛁 含母婴室 · ♿ 无障碍' },
  { id: 'w2', type: 'wc', x: 84, y: 32, name: '东区卫生间', desc: '过山车与摩天轮附近，演出散场后较繁忙。', extra: '🧼 24h 热水洗手' },
  { id: 'p1', type: 'park', x: 50, y: 92, name: '南门停车场', desc: '主停车场，P1–P3 区共 1200 车位，新能源车有充电桩。', extra: '🔌 含充电桩 · 首小时免费' }
]

const markers = [...rideMarkers, ...staticMarkers]

const active = reactive({ ride: true, food: true, wc: true, park: true })
const selected = ref(null)

function toggle(key) {
  active[key] = !active[key]
  if (selected.value && !active[selected.value.type]) selected.value = null
}
function select(m) {
  selected.value = m
}

// ===================== 手绘底图：树林 / 石板路 / 小花 =====================
// 成簇卡通松树（4 片林 + 边角点缀）
const pineClusters = [
  // 左上林
  [[120, 172, 1, 0], [152, 150, 0.8, -6], [94, 150, 0.7, 5], [162, 188, 1.1, 3], [108, 202, 0.9, -4]],
  // 右上林
  [[862, 150, 1, 0], [892, 128, 0.8, -5], [836, 134, 0.75, 6], [906, 176, 1.1, 4], [872, 196, 0.9, -3]],
  // 右下林
  [[820, 522, 1, 0], [856, 500, 0.85, -6], [794, 506, 0.75, 5], [882, 548, 1.05, 3], [840, 562, 0.9, -4]],
  // 边角点缀
  [[62, 382, 0.8, 4], [952, 360, 0.8, -4], [70, 250, 0.7, 3], [945, 250, 0.7, -3]]
]
const pines = pineClusters.flat().map((t) => ({ x: t[0], y: t[1], s: t[2], r: t[3] }))

// 石板小路：折线 + 踏石采样
const PATH_LINES = [
  [[500, 628], [492, 560], [505, 480], [500, 400], [512, 320], [500, 250]], // 主路
  [[500, 400], [400, 360], [320, 310], [270, 230]],                          // 左支
  [[500, 400], [610, 372], [690, 418], [760, 346]],                          // 右支
  [[500, 480], [560, 520], [470, 560]]                                       // 向湖/漂流
]
const roadPaths = PATH_LINES.map((pts) => 'M' + pts.map((p) => p.join(' ')).join(' L '))

function buildStones() {
  const out = []
  PATH_LINES.forEach((seg) => {
    for (let i = 0; i < seg.length - 1; i++) {
      const [x1, y1] = seg[i]
      const [x2, y2] = seg[i + 1]
      const d = Math.hypot(x2 - x1, y2 - y1)
      const n = Math.max(2, Math.round(d / 30))
      for (let j = 0; j <= n; j++) {
        const t = j / n
        const x = x1 + (x2 - x1) * t
        const y = y1 + (y2 - y1) * t
        const ang = (Math.atan2(y2 - y1, x2 - x1) * 180) / Math.PI
        out.push({
          x, y,
          rot: ang + (Math.random() * 22 - 11),
          sx: 1 + Math.random() * 0.3,
          sy: 1 + Math.random() * 0.25,
          k: Math.random()
        })
      }
    }
  })
  return out
}
const stones = buildStones()

// 小路小花：从踏石中挑出部分，沿垂直方向偏移摆放
const flowers = stones
  .filter((_, i) => i % 4 === 0)
  .map((s, i) => {
    const a = (s.rot * Math.PI) / 180
    const ox = Math.cos(a + Math.PI / 2) * 20
    const oy = Math.sin(a + Math.PI / 2) * 20
    return { x: s.x + ox, y: s.y + oy, s: 0.85 + (i % 3) * 0.12, c: i % 2 ? 'y' : 'p' }
  })

// 长椅 / 路灯点缀
const benches = [{ x: 470, y: 560 }, { x: 760, y: 344 }]
const lamps = [{ x: 505, y: 478 }, { x: 610, y: 372 }, { x: 400, y: 360 }]
</script>

<style scoped>
.map-page { width: 100%; }
.map-page.embedded { max-width: 1080px; margin: 0 auto; padding: 0; }

.map-top {
  text-align: center;
  margin-bottom: 14px;
}
.back-btn {
  float: left;
  border: none;
  background: rgba(255, 255, 255, 0.7);
  color: #2f5d3a;
  font-weight: 700;
  padding: 7px 14px;
  border-radius: 10px;
  cursor: pointer;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.1);
}
.back-btn:hover { background: #fff; }
.map-title {
  font-size: 24px;
  color: #2f5d3a;
  margin: 4px 0 2px;
  text-shadow: 0 2px 0 rgba(255, 255, 255, 0.6);
}
.map-sub {
  font-size: 13px;
  color: #4a6b52;
  margin: 0;
}

/* 图例 */
.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  margin: 14px 0 16px;
}
.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 2px solid var(--c);
  background: #fff;
  color: #333;
  font-weight: 700;
  font-size: 13px;
  padding: 6px 13px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.18s ease;
}
.legend-item .dot {
  width: 20px;
  height: 20px;
  display: inline-grid;
  place-items: center;
}
.legend-item .dot :deep(svg) { width: 20px; height: 20px; display: block; }
.legend-item.off {
  opacity: 0.4;
  border-style: dashed;
  filter: grayscale(0.6);
}

/* 地图舞台 */
.map-stage {
  position: relative;
  width: 100%;
  aspect-ratio: 1000 / 680;
  border-radius: 22px;
  overflow: hidden;
  box-shadow: 0 16px 40px rgba(40, 80, 50, 0.22);
  border: 4px solid #fff;
  background: #eef3cf;
}
.map-base {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

/* 立体卡通图钉 */
.pin {
  position: absolute;
  transform: translate(-50%, -100%);
  transform-origin: 50% 100%;
  width: 46px;
  height: 46px;
  border: none;
  background: none;
  padding: 0;
  cursor: pointer;
  transition: transform 0.15s ease, filter 0.15s ease;
}
.pin:hover { transform: translate(-50%, -100%) scale(1.12); }
.pin.active { transform: translate(-50%, -100%) scale(1.22); filter: drop-shadow(0 6px 8px rgba(0, 0, 0, 0.3)); }
.pin-shadow {
  position: absolute;
  left: 50%;
  top: 44px;
  width: 30px;
  height: 10px;
  transform: translateX(-50%);
  background: rgba(40, 60, 40, 0.28);
  filter: blur(3px);
  border-radius: 50%;
}
.pin-body {
  position: absolute;
  inset: 0;
  transform: rotate(-45deg);
  border-radius: 50% 50% 50% 0;
  background:
    radial-gradient(circle at 32% 28%, rgba(255, 255, 255, 0.6), transparent 55%),
    var(--c);
  border: 3px solid #fff;
  box-shadow:
    0 6px 12px rgba(0, 0, 0, 0.3),
    inset -3px -3px 7px rgba(0, 0, 0, 0.18),
    inset 3px 3px 7px rgba(255, 255, 255, 0.45);
}
.pin-disc {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 30px;
  height: 30px;
  transform: translate(-50%, -50%) rotate(45deg);
  display: grid;
  place-items: center;
}
.pin-disc :deep(svg) { width: 30px; height: 30px; display: block; }

/* 简介弹层 */
.popover {
  position: absolute;
  transform: translate(-50%, calc(-100% - 30px));
  width: 240px;
  background: #fff;
  border-radius: 16px;
  border-top: 5px solid var(--c);
  padding: 14px 14px 12px;
  box-shadow: 0 14px 34px rgba(0, 0, 0, 0.28);
  z-index: 5;
}
.pop-close {
  position: absolute;
  top: 6px;
  right: 8px;
  border: none;
  background: transparent;
  font-size: 18px;
  color: #999;
  cursor: pointer;
  line-height: 1;
}
.pop-head {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 8px;
}
.pop-art {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: #fff;
  border: 2px solid var(--c);
  display: grid;
  place-items: center;
  flex: none;
}
.pop-art :deep(svg) { width: 34px; height: 34px; display: block; }
.pop-name { font-weight: 800; color: #2f3a30; font-size: 15px; }
.pop-tag {
  display: inline-block;
  margin-top: 2px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: var(--c);
  padding: 1px 8px;
  border-radius: 10px;
}
.pop-desc {
  font-size: 13px;
  line-height: 1.55;
  color: #4a5a4e;
  margin: 0 0 8px;
}
.pop-extra {
  font-size: 12px;
  color: #2f5d3a;
  background: #f1f8ec;
  border-radius: 10px;
  padding: 6px 9px;
  font-weight: 600;
}

.pop-enter-active, .pop-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.pop-enter-from, .pop-leave-to { opacity: 0; transform: translate(-50%, calc(-100% - 20px)); }

@media (max-width: 560px) {
  .pin { width: 38px; height: 38px; }
  .pin-disc { width: 24px; height: 24px; }
  .pin-disc :deep(svg) { width: 24px; height: 24px; }
  .popover { width: 200px; }
  .map-title { font-size: 20px; }
}
</style>
