<template>
  <div class="attr-page">
    <!-- 页头 -->
    <header class="attr-head">
      <button class="back-btn" type="button" @click="$emit('back')">‹ 返回首页</button>
      <h1>🎠 游乐项目</h1>
      <p class="sub">狗熊岭森林游乐园 · 全部热门项目一目了然</p>
    </header>

    <!-- 分类筛选（刺激 / 亲子 / 观光） -->
    <div class="filters">
      <button
        v-for="c in categories"
        :key="c"
        type="button"
        class="chip"
        :class="{ on: cat === c }"
        @click="cat = c"
      >{{ c }}</button>
    </div>

    <!-- 卡片网格 -->
    <div class="attr-grid">
      <article
        v-for="a in filtered"
        :key="a.id"
        class="attr-card"
        role="button"
        tabindex="0"
        @click="open(a.id)"
        @keydown.enter="open(a.id)"
      >
        <div class="cover" :style="{ background: a.color }">
          <img :src="a.img" :alt="a.name" loading="lazy" />
          <span class="badge" :class="catClass(a.category)">{{ a.category }}</span>
        </div>
        <div class="body">
          <h3>{{ a.emoji }} {{ a.name }}</h3>
          <div class="meta">
            <span class="meta-item">📏 身高 {{ a.height }}</span>
            <span class="meta-item">⏱ 排队 {{ a.wait }} 分钟</span>
          </div>
          <div class="rate">★ {{ a.rating.toFixed(1) }}</div>
        </div>
      </article>
    </div>

    <!-- 详情页（点击卡片进入） -->
    <AttractionDetail v-if="selectedId !== null" :id="selectedId" @back="close" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { attractionsDB } from '../data/attractions.js'
import AttractionDetail from './AttractionDetail.vue'

const emit = defineEmits(['back'])

const categories = ['全部', '刺激', '亲子', '观光']
const cat = ref('全部')
const selectedId = ref(null)

const filtered = computed(() =>
  cat.value === '全部'
    ? attractionsDB
    : attractionsDB.filter((a) => a.category === cat.value)
)

function catClass(c) {
  return { 刺激: 'c-thrill', 亲子: 'c-family', 观光: 'c-sight' }[c] || ''
}
function open(id) {
  selectedId.value = id
}
function close() {
  selectedId.value = null
}
</script>

<style scoped>
.attr-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 26px 24px 60px;
}
.attr-head {
  position: relative;
  text-align: center;
  margin-bottom: 22px;
}
.attr-head h1 {
  font-size: 30px;
  font-weight: 900;
  color: var(--forest-deep);
}
.attr-head .sub {
  margin-top: 6px;
  color: var(--muted);
  font-size: 14px;
}
.back-btn {
  position: absolute;
  left: 0;
  top: 6px;
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
.back-btn:hover {
  background: #fff;
}

/* 筛选标签 */
.filters {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 26px;
}
.chip {
  border: 2px solid var(--wood-light);
  background: #fff;
  color: var(--text);
  font-weight: 700;
  font-size: 14px;
  padding: 8px 20px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.18s ease;
}
.chip:hover {
  border-color: var(--forest);
}
.chip.on {
  background: var(--forest);
  border-color: var(--forest);
  color: #fff;
}

/* 卡片网格 */
.attr-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}
.attr-card {
  background: #fff;
  border-radius: var(--radius);
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 12px 28px rgba(47, 125, 79, 0.1);
  border: 2px solid transparent;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
  outline: none;
}
.attr-card:hover,
.attr-card:focus-visible {
  transform: translateY(-6px);
  box-shadow: 0 20px 40px rgba(47, 125, 79, 0.2);
  border-color: var(--leaf);
}
.cover {
  position: relative;
  height: 160px;
  overflow: hidden;
}
.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.badge {
  position: absolute;
  left: 12px;
  top: 12px;
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  padding: 4px 12px;
  border-radius: 999px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
}
.c-thrill { background: #e4572e; }
.c-family { background: #2f9e6f; }
.c-sight { background: #4a7bd1; }

.body {
  padding: 14px 16px 18px;
}
.body h3 {
  font-size: 17px;
  font-weight: 800;
  color: var(--forest-deep);
  margin-bottom: 10px;
}
.meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: var(--muted);
}
.rate {
  margin-top: 10px;
  font-weight: 800;
  color: var(--honey);
}
</style>
