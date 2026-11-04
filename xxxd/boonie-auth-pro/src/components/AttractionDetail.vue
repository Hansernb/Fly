<template>
  <div class="detail-mask" @click.self="$emit('back')">
    <div class="detail" v-if="a">
      <button class="back-btn" type="button" @click="$emit('back')">‹ 返回列表</button>

      <div class="hero" :style="{ background: a.color }">
        <img :src="a.img" :alt="a.name" />
      </div>

      <div class="info">
        <span class="badge" :class="catClass(a.category)">{{ a.category }}</span>
        <h1>{{ a.emoji }} {{ a.name }}</h1>

        <div class="stats">
          <div class="stat">
            <span class="stat-label">📏 身高限制</span>
            <strong>{{ a.height }}</strong>
          </div>
          <div class="stat">
            <span class="stat-label">⏱ 排队时长</span>
            <strong>约 {{ a.wait }} 分钟</strong>
          </div>
          <div class="stat">
            <span class="stat-label">★ 综合评分</span>
            <strong>{{ a.rating.toFixed(1) }}</strong>
          </div>
        </div>

        <p class="desc">{{ a.desc }}</p>

        <h2 class="rev-title">游客评价</h2>
        <ul class="reviews">
          <li v-for="r in a.reviews" :key="r.id">
            <span class="ava">{{ r.avatar }}</span>
            <div class="rev-body">
              <div class="r-top">
                <b>{{ r.name }}</b>
                <span class="r-date">{{ r.date }}</span>
              </div>
              <div class="r-stars">{{ '★'.repeat(r.rating) }}{{ '☆'.repeat(5 - r.rating) }}</div>
              <p>{{ r.text }}</p>
            </div>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { attractionsDB } from '../data/attractions.js'

const props = defineProps({
  id: { type: Number, required: true }
})
defineEmits(['back'])

const a = computed(() => attractionsDB.find((x) => x.id === props.id) || null)

function catClass(c) {
  return { 刺激: 'c-thrill', 亲子: 'c-family', 观光: 'c-sight' }[c] || ''
}
</script>

<style scoped>
.detail-mask {
  position: fixed;
  inset: 0;
  z-index: 500;
  background: rgba(20, 36, 26, 0.55);
  backdrop-filter: blur(3px);
  overflow-y: auto;
  padding: 30px 16px 50px;
  animation: pop 0.22s ease;
}
@keyframes pop {
  from { opacity: 0; transform: scale(0.98); }
  to { opacity: 1; transform: scale(1); }
}
.detail {
  max-width: 720px;
  margin: 0 auto;
  background: var(--cream);
  border-radius: 22px;
  overflow: hidden;
  box-shadow: 0 30px 70px rgba(0, 0, 0, 0.35);
  position: relative;
}
.back-btn {
  position: absolute;
  left: 16px;
  top: 16px;
  z-index: 3;
  border: none;
  background: rgba(255, 255, 255, 0.9);
  color: var(--forest-deep);
  font-weight: 800;
  font-size: 14px;
  padding: 8px 14px;
  border-radius: 999px;
  cursor: pointer;
}
.hero {
  height: 240px;
}
.hero img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.info {
  padding: 22px 26px 30px;
}
.badge {
  display: inline-block;
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  padding: 4px 14px;
  border-radius: 999px;
  margin-bottom: 10px;
}
.c-thrill { background: #e4572e; }
.c-family { background: #2f9e6f; }
.c-sight { background: #4a7bd1; }

.info h1 {
  font-size: 26px;
  font-weight: 900;
  color: var(--forest-deep);
  margin-bottom: 16px;
}
.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 18px;
}
.stat {
  background: #fff;
  border-radius: 14px;
  padding: 14px 10px;
  text-align: center;
  box-shadow: 0 6px 16px rgba(47, 125, 79, 0.08);
}
.stat-label {
  display: block;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 6px;
}
.stat strong {
  font-size: 16px;
  color: var(--forest-deep);
}
.desc {
  font-size: 15px;
  line-height: 1.7;
  color: var(--text);
  margin-bottom: 8px;
}
.rev-title {
  font-size: 18px;
  color: var(--forest-deep);
  margin: 22px 0 12px;
  border-left: 5px solid var(--leaf);
  padding-left: 10px;
}
.reviews {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.reviews li {
  display: flex;
  gap: 12px;
  background: #fff;
  border-radius: 14px;
  padding: 14px 16px;
  box-shadow: 0 4px 12px rgba(47, 125, 79, 0.06);
}
.ava {
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  border-radius: 50%;
  background: #eef6ea;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}
.rev-body { flex: 1; }
.r-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}
.r-top b { color: var(--forest-deep); }
.r-date { font-size: 12px; color: var(--muted); }
.r-stars { color: var(--honey); font-size: 14px; margin: 4px 0; }
.rev-body p { font-size: 14px; line-height: 1.6; color: var(--text); }

@media (max-width: 560px) {
  .stats { grid-template-columns: 1fr; }
}
</style>
