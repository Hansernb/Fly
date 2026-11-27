<template>
  <div class="attr-page">
    <!-- 页头 -->
    <header class="attr-head">
      <button class="back-btn" type="button" @click="$emit('back')">‹ 返回上一页</button>
      <h1>🎠 游乐项目</h1>
      <p class="sub">狗熊岭森林游乐园 · 全部热门项目一目了然</p>
    </header>

    <!-- 分类筛选（后台分类字典，接口失败降级为内置分类） -->
    <div class="filters">
      <button
        v-for="c in categoryNames"
        :key="c"
        type="button"
        class="chip"
        :class="{ on: cat === c }"
        @click="cat = c"
      >{{ c }}</button>
    </div>

    <!-- 身高限制 / 排队时长筛选 + 价格排序 -->
    <div class="filter-bar">
      <div class="filter-row">
        <span class="f-label">📏 身高限制</span>
        <button
          v-for="h in heightOptions"
          :key="'h' + h.value"
          type="button"
          class="f-chip"
          :class="{ on: heightSel === h.value }"
          @click="heightSel = h.value"
        >{{ h.label }}</button>
      </div>
      <div class="filter-row">
        <span class="f-label">⏱ 排队时长</span>
        <button
          v-for="w in waitOptions"
          :key="'w' + w.value"
          type="button"
          class="f-chip"
          :class="{ on: waitSel === w.value }"
          @click="waitSel = w.value"
        >{{ w.label }}</button>
      </div>
      <div class="filter-row">
        <span class="f-label">↕️ 排序方式</span>
        <select v-model="sortBy" class="f-select">
          <option value="default">默认（推荐）</option>
          <option value="price-asc">价格从低到高</option>
          <option value="price-desc">价格从高到低</option>
          <option value="rating-desc">评分从高到低</option>
        </select>
        <span class="f-count">共 {{ filtered.length }} 个项目</span>
        <button
          v-if="heightSel > 0 || waitSel !== 'all' || sortBy !== 'default'"
          class="f-reset"
          type="button"
          @click="resetFilters"
        >重置筛选</button>
      </div>
    </div>

    <!-- 卡片网格 -->
    <div v-if="filtered.length" class="attr-grid">
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
          <span class="price-tag">¥{{ a.price || 0 }}</span>
        </div>
        <div class="body">
          <h3>{{ a.emoji }} {{ a.name }}</h3>
          <div class="meta">
            <span class="meta-item">📏 身高 {{ a.height }}</span>
            <span class="meta-item">⏱ 排队 {{ a.wait }} 分钟</span>
          </div>
          <div class="rate-row">
            <span class="rate">★ {{ a.rating.toFixed(1) }}</span>
            <span class="price">💰 ¥{{ a.price || 0 }}</span>
          </div>
        </div>
      </article>
    </div>
    <div v-else class="attr-empty">
      <div class="attr-empty-emoji">🍃</div>
      <p>没有符合筛选条件的项目，换个条件试试～</p>
      <button class="f-reset" type="button" @click="resetFilters">重置筛选</button>
    </div>

    <!-- 详情页（点击卡片进入） -->
    <AttractionDetail v-if="selectedId !== null" :id="selectedId" @back="close" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { attractionsDB } from '../data/attractions.js'
import { useContentStore } from '../stores/content'
import AttractionDetail from './AttractionDetail.vue'

const emit = defineEmits(['back'])

// 分类筛选从内容 store 读取（后台可维护，接口失败自动降级为内置分类）
const contentStore = useContentStore()
const { categoryNames } = storeToRefs(contentStore)
onMounted(() => contentStore.loadCategories())

const cat = ref('全部')
const selectedId = ref(null)

/* 身高限制：选项值为身高阈值(cm)，0 表示不限；项目身高线 ≤ 阈值即视为可玩 */
const heightOptions = [
  { value: 0, label: '不限' },
  { value: 90, label: '≥90cm' },
  { value: 100, label: '≥100cm' },
  { value: 110, label: '≥110cm' },
  { value: 120, label: '≥120cm' },
  { value: 130, label: '≥130cm' }
]
const heightSel = ref(0)

/* 排队时长筛选 */
const waitOptions = [
  { value: 'all', label: '不限' },
  { value: 'short', label: '15 分钟内' },
  { value: 'mid', label: '15 ~ 30 分钟' },
  { value: 'long', label: '30 分钟以上' }
]
const waitSel = ref('all')

/* 排序方式 */
const sortBy = ref('default')

const filtered = computed(() => {
  let list =
    cat.value === '全部' ? [...attractionsDB] : attractionsDB.filter((a) => a.category === cat.value)
  if (heightSel.value > 0) {
    list = list.filter((a) => parseInt(a.height) <= heightSel.value)
  }
  if (waitSel.value === 'short') list = list.filter((a) => a.wait <= 15)
  else if (waitSel.value === 'mid') list = list.filter((a) => a.wait > 15 && a.wait <= 30)
  else if (waitSel.value === 'long') list = list.filter((a) => a.wait > 30)
  if (sortBy.value === 'price-asc') list.sort((x, y) => (x.price || 0) - (y.price || 0))
  else if (sortBy.value === 'price-desc') list.sort((x, y) => (y.price || 0) - (x.price || 0))
  else if (sortBy.value === 'rating-desc') list.sort((x, y) => (y.rating || 0) - (x.rating || 0))
  return list
})

function resetFilters() {
  heightSel.value = 0
  waitSel.value = 'all'
  sortBy.value = 'default'
}

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

/* 高级筛选条：身高 / 排队 / 排序 */
.filter-bar {
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: #fff;
  border-radius: var(--radius);
  padding: 14px 18px;
  margin-bottom: 24px;
  box-shadow: 0 8px 22px rgba(47, 125, 79, 0.08);
}
.filter-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.f-label {
  flex: 0 0 84px;
  font-size: 13px;
  font-weight: 800;
  color: var(--forest-deep);
}
.f-chip {
  border: 1.5px solid var(--wood-light);
  background: #fafdf8;
  color: var(--text);
  font-size: 12.5px;
  font-weight: 700;
  padding: 5px 14px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.f-chip:hover { border-color: var(--forest); }
.f-chip.on {
  background: var(--forest);
  border-color: var(--forest);
  color: #fff;
}
.f-chip:active { transform: scale(0.94); }
.f-select {
  border: 1.5px solid var(--wood-light);
  background: #fafdf8;
  color: var(--text);
  font-size: 13px;
  font-weight: 700;
  padding: 5px 10px;
  border-radius: 10px;
  cursor: pointer;
}
.f-count {
  margin-left: auto;
  font-size: 12.5px;
  color: var(--muted);
}
.f-reset {
  border: none;
  background: rgba(228, 87, 46, 0.1);
  color: #c74a1e;
  font-size: 12.5px;
  font-weight: 800;
  padding: 5px 14px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.f-reset:hover { background: rgba(228, 87, 46, 0.18); }
.f-reset:active { transform: scale(0.94); }

/* 无结果提示 */
.attr-empty {
  text-align: center;
  padding: 60px 20px;
  color: var(--muted);
  font-size: 14px;
}
.attr-empty-emoji { font-size: 52px; margin-bottom: 10px; }
.attr-empty .f-reset { margin-top: 14px; }

/* 卡片价格角标 + 底部价格行 */
.price-tag {
  position: absolute;
  right: 12px;
  top: 12px;
  color: #fff;
  font-size: 13px;
  font-weight: 900;
  padding: 4px 12px;
  border-radius: 999px;
  background: linear-gradient(180deg, #ffc83d, var(--honey));
  color: #5a3d00;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
}
.rate-row {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.rate {
  font-weight: 800;
  color: var(--honey);
}
.price {
  font-weight: 900;
  color: #e09b00;
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
