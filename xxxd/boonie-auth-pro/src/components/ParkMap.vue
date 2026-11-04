<template>
  <div class="map-page" :class="{ embedded }">
    <!-- 独立页顶部栏（内嵌首页时隐藏，由首页 section-title 提供标题） -->
    <div v-if="!embedded" class="map-top">
      <button class="back-btn" type="button" @click="$emit('back')">← 返回首页</button>
      <h1 class="map-title">🗺️ 狗熊岭手绘游园地图</h1>
      <p class="map-sub">点击地图上的标记点，查看项目 / 餐厅 / 卫生间 / 停车场位置与简介</p>
    </div>

    <!-- 图例筛选 -->
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
        <span class="dot">{{ cat.emoji }}</span>{{ cat.label }}
      </button>
    </div>

    <!-- 手绘地图区 -->
    <div class="map-stage" @click.self="selected = null">
      <!-- 手绘底图（SVG 有机块面：湖泊、林地、小路、入口） -->
      <svg class="map-base" viewBox="0 0 1000 680" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
        <defs>
          <linearGradient id="grass" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stop-color="#eaf7e3" />
            <stop offset="1" stop-color="#d6efcd" />
          </linearGradient>
        </defs>
        <rect x="0" y="0" width="1000" height="680" fill="url(#grass)" />
        <path d="M60 80 Q160 30 250 90 Q300 150 220 210 Q120 250 50 190 Q10 130 60 80 Z" fill="#bfe6b3" opacity="0.85" />
        <path d="M760 40 Q900 20 960 120 Q980 220 880 250 Q780 240 740 150 Q720 80 760 40 Z" fill="#bfe6b3" opacity="0.8" />
        <path d="M700 420 Q840 380 920 470 Q960 560 850 600 Q740 620 690 540 Q660 470 700 420 Z" fill="#bfe6b3" opacity="0.82" />
        <path d="M360 430 Q470 380 560 440 Q620 500 540 560 Q440 600 370 550 Q310 500 360 430 Z" fill="#bfe4f2" stroke="#8fd0e6" stroke-width="3" />
        <path d="M500 650 Q480 560 520 500 Q560 440 500 360 Q450 280 500 200 Q540 130 500 70" fill="none" stroke="#c9a36b" stroke-width="6" stroke-linecap="round" stroke-dasharray="2 16" />
        <path d="M500 360 Q620 350 700 420" fill="none" stroke="#c9a36b" stroke-width="5" stroke-linecap="round" stroke-dasharray="2 14" />
        <path d="M500 360 Q380 350 300 300" fill="none" stroke="#c9a36b" stroke-width="5" stroke-linecap="round" stroke-dasharray="2 14" />
        <path d="M470 650 Q500 632 530 650" fill="none" stroke="#9bbf8a" stroke-width="5" stroke-linecap="round" />
        <text x="500" y="672" text-anchor="middle" font-size="20" fill="#5a7d4f" font-weight="700">南大门入口</text>
      </svg>

      <!-- 标记点 -->
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
        <span class="pin-emoji">{{ m.emoji }}</span>
      </button>

      <!-- 简介弹层 -->
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
            <span class="pop-emoji">{{ selected.emoji }}</span>
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

// 游乐项目在地图上的坐标（基于地图比例百分比），按 attractions.js 的 id 对应
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
  id: 'ride-' + a.id,
  type: 'ride',
  emoji: a.emoji,
  x: (RIDE_POS[a.id] || { x: 50, y: 50 }).x,
  y: (RIDE_POS[a.id] || { x: 50, y: 50 }).y,
  name: a.name,
  desc: a.desc,
  extra: `⏱ 平均排队 ${a.wait} 分钟 · 📏 身高 ${a.height}`
}))

// 餐厅 / 卫生间 / 停车场（项目暂无对应数据源，内置）
const staticMarkers = [
  { id: 'f1', type: 'food', emoji: '🍯', x: 44, y: 34, name: '蜂蜜餐厅', desc: '熊二最爱的蜂蜜烤翅与森林轻食，露天座位可看林景。', extra: '🍽️ 营业 11:00–21:00 · 人均 ¥68' },
  { id: 'f2', type: 'food', emoji: '🍦', x: 78, y: 56, name: '林间小食站', desc: '冰淇淋、烤肠、鲜榨果汁，快速补充能量。', extra: '⚡ 即买即走 · 人均 ¥25' },
  { id: 'w1', type: 'wc', emoji: '🚻', x: 40, y: 54, name: '中心卫生间', desc: '园区中部主卫生间，含母婴室与无障碍设施。', extra: '🛁 含母婴室 · ♿ 无障碍' },
  { id: 'w2', type: 'wc', emoji: '🚻', x: 84, y: 32, name: '东区卫生间', desc: '过山车与摩天轮附近，演出散场后较繁忙。', extra: '🧼 24h 热水洗手' },
  { id: 'p1', type: 'park', emoji: '🅿️', x: 50, y: 92, name: '南门停车场', desc: '主停车场，P1–P3 区共 1200 车位，新能源车有充电桩。', extra: '🔌 含充电桩 · 首小时免费' }
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
.legend-item .dot { font-size: 15px; }
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
  background: #eaf7e3;
}
.map-base {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

/* 标记点 */
.pin {
  position: absolute;
  transform: translate(-50%, -50%);
  width: 42px;
  height: 42px;
  border-radius: 50% 50% 50% 0;
  rotate: -45deg;
  background: var(--c);
  border: 3px solid #fff;
  box-shadow: 0 5px 12px rgba(0, 0, 0, 0.28);
  cursor: pointer;
  padding: 0;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.pin:hover { transform: translate(-50%, -50%) scale(1.12); }
.pin.active { transform: translate(-50%, -50%) scale(1.2); box-shadow: 0 8px 18px rgba(0, 0, 0, 0.35); }
.pin-emoji {
  display: block;
  rotate: 45deg;
  font-size: 20px;
  line-height: 36px;
  text-align: center;
}

/* 简介弹层 */
.popover {
  position: absolute;
  transform: translate(-50%, calc(-100% - 28px));
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
.pop-emoji {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: var(--c);
  display: grid;
  place-items: center;
  font-size: 22px;
  flex: none;
}
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
.pop-enter-from, .pop-leave-to { opacity: 0; transform: translate(-50%, calc(-100% - 18px)); }

@media (max-width: 560px) {
  .pin { width: 34px; height: 34px; }
  .pin-emoji { font-size: 16px; line-height: 28px; }
  .popover { width: 200px; }
  .map-title { font-size: 20px; }
}
</style>
