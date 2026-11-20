<template>
  <section id="shows" class="section schedule reveal">
    <h2 class="section-title">🎪 演出时间表</h2>
    <p class="schedule-tip">按上午 / 下午 / 晚上分场次 · 点击「预约」锁定你的专属座位</p>

    <div class="periods">
      <div v-for="p in periods" :key="p" class="period">
        <div class="period-head">
          <span class="period-icon">{{ periodIcon(p) }}</span>
          <h3>{{ p }}</h3>
          <span class="period-count">{{ grouped[p].length }} 场</span>
        </div>

        <div class="show-list">
          <article v-for="s in grouped[p]" :key="s.id" class="show-card">
            <div class="show-cover">
              <img :src="s.img" :alt="s.name" loading="lazy" />
              <span class="show-when">📅 {{ s.date }} {{ s.time }}</span>
            </div>
            <div class="show-info">
              <div class="show-name">{{ s.emoji }} {{ s.name }}</div>
              <div class="show-sub">📍 {{ s.place }} · ⏱ {{ s.dur }}</div>
              <div class="show-foot">
                <span class="slots" :class="{ low: s.slots > 0 && s.slots <= 5, zero: s.slots === 0 }">
                  {{ s.slots === 0 ? '已约满' : '余 ' + s.slots + ' 名额' }}
                </span>
                <button
                  class="book-btn"
                  type="button"
                  :class="{ booked: s.booked }"
                  :disabled="s.slots === 0"
                  @click="book(s)"
                >{{ s.booked ? '✓ 已预约' : s.slots === 0 ? '已满' : '🔔 预约' }}</button>
              </div>
            </div>
          </article>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed } from 'vue'

const periods = ['上午', '下午', '晚上']

const shows = ref([
  { id: 1, emoji: '🐻', name: '熊大熊二迎宾秀', period: '上午', time: '10:00', place: '中心广场舞台', slots: 32, date: '11月22日 · 周六', dur: '约 25 分钟', img: '/scenery/scenery4.png' },
  { id: 2, emoji: '🐵', name: '吉吉国王杂技团', period: '上午', time: '11:30', place: '欢乐马戏篷', slots: 8, date: '11月22日 · 周六', dur: '约 40 分钟', img: '/scenery/scenery6.png' },
  { id: 3, emoji: '🦊', name: '森林精灵舞会', period: '下午', time: '14:00', place: '湖畔木台', slots: 20, date: '11月22日 · 周六', dur: '约 30 分钟', img: '/scenery/scenery2.png' },
  { id: 4, emoji: '🌳', name: '光头强木工剧场', period: '下午', time: '16:00', place: '树屋剧场', slots: 0, date: '11月22日 · 周六', dur: '约 45 分钟', img: '/scenery/scenery1.png' },
  { id: 5, emoji: '🎆', name: '星空烟花大秀', period: '晚上', time: '19:30', place: '摩天轮广场', slots: 5, date: '11月23日 · 周日', dur: '约 20 分钟', img: '/scenery/scenery5.png' },
  { id: 6, emoji: '🔥', name: '篝火狂欢夜', period: '晚上', time: '20:30', place: '营火草坪', slots: 15, date: '11月23日 · 周日', dur: '约 60 分钟', img: '/scenery/scenery3.png' }
])

const grouped = computed(() => {
  const g = { 上午: [], 下午: [], 晚上: [] }
  for (const s of shows.value) g[s.period].push(s)
  return g
})

function periodIcon(p) {
  return { 上午: '🌅', 下午: '☀️', 晚上: '🌙' }[p] || '🎪'
}

function book(s) {
  // 本地预约：剩余名额 -1（接后端时替换为 POST /api/shows/:id/reserve）
  if (s.slots > 0 && !s.booked) {
    s.slots -= 1
    s.booked = true
  }
}
</script>

<style scoped>
.schedule-tip {
  color: var(--muted);
  font-size: 14px;
  margin: -6px 0 18px;
}
.periods {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}
.period {
  background: #fff;
  border-radius: var(--radius);
  padding: 16px 16px 18px;
  box-shadow: 0 10px 26px rgba(47, 125, 79, 0.1);
  border: 2px solid transparent;
}
.period:nth-child(1) { border-color: #ffd98a; }
.period:nth-child(2) { border-color: #ffe08a; }
.period:nth-child(3) { border-color: #b9a7e0; }
.period-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.period-icon { font-size: 22px; }
.period-head h3 {
  font-size: 17px;
  font-weight: 800;
  color: var(--forest-deep);
}
.period-count {
  margin-left: auto;
  font-size: 12px;
  color: var(--muted);
  background: #f0f6ec;
  padding: 2px 10px;
  border-radius: 999px;
}
.show-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
/* 演出预告卡片：封面图 + 信息 */
.show-card {
  display: flex;
  gap: 12px;
  padding: 10px;
  border-radius: 14px;
  background: #fafdf8;
  border: 1px solid #e7f0e2;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.show-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 22px rgba(47, 125, 79, 0.14);
}
.show-cover {
  position: relative;
  flex: 0 0 96px;
  height: 72px;
  border-radius: 10px;
  overflow: hidden;
}
.show-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.show-when {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 2px 6px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(180deg, transparent, rgba(20, 60, 35, 0.75));
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.show-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.show-name {
  font-weight: 800;
  color: var(--forest-deep);
  font-size: 15px;
}
.show-sub {
  font-size: 12px;
  color: var(--muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.show-foot {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.book-btn.booked {
  background: linear-gradient(180deg, #7ed49b, var(--leaf));
  color: #fff;
  box-shadow: 0 4px 10px rgba(76, 175, 109, 0.35);
}
.slots {
  font-size: 12px;
  font-weight: 700;
  color: var(--forest);
}
.slots.low { color: var(--honey); }
.slots.zero { color: var(--danger); }
.book-btn {
  border: none;
  background: linear-gradient(180deg, #ffc83d 0%, var(--honey) 100%);
  color: #5a3d00;
  font-weight: 800;
  font-size: 13px;
  padding: 6px 16px;
  border-radius: 999px;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
  box-shadow: 0 4px 10px rgba(255, 183, 3, 0.35);
}
.book-btn:hover:not(:disabled) {
  transform: translateY(-1px);
}
.book-btn:disabled {
  background: #dcdbd5;
  color: #fff;
  cursor: not-allowed;
  box-shadow: none;
}

@media (max-width: 900px) {
  .periods { grid-template-columns: 1fr; }
}
</style>
