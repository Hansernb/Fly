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
          <div v-for="s in grouped[p]" :key="s.id" class="show-row">
            <div class="show-main">
              <span class="show-emoji">{{ s.emoji }}</span>
              <div class="show-text">
                <div class="show-name">{{ s.name }}</div>
                <div class="show-sub">🕒 {{ s.time }} · 📍 {{ s.place }}</div>
              </div>
            </div>

            <div class="show-right">
              <span class="slots" :class="{ low: s.slots > 0 && s.slots <= 5, zero: s.slots === 0 }">
                {{ s.slots === 0 ? '已约满' : '余 ' + s.slots + ' 名额' }}
              </span>
              <button
                class="book-btn"
                type="button"
                :disabled="s.slots === 0"
                @click="book(s)"
              >{{ s.slots === 0 ? '已满' : '预约' }}</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed } from 'vue'

const periods = ['上午', '下午', '晚上']

const shows = ref([
  { id: 1, emoji: '🐻', name: '熊大熊二迎宾秀', period: '上午', time: '10:00', place: '中心广场舞台', slots: 32 },
  { id: 2, emoji: '🐵', name: '吉吉国王杂技团', period: '上午', time: '11:30', place: '欢乐马戏篷', slots: 8 },
  { id: 3, emoji: '🦊', name: '森林精灵舞会', period: '下午', time: '14:00', place: '湖畔木台', slots: 20 },
  { id: 4, emoji: '🌳', name: '光头强木工剧场', period: '下午', time: '16:00', place: '树屋剧场', slots: 0 },
  { id: 5, emoji: '🎆', name: '星空烟花大秀', period: '晚上', time: '19:30', place: '摩天轮广场', slots: 5 },
  { id: 6, emoji: '🔥', name: '篝火狂欢夜', period: '晚上', time: '20:30', place: '营火草坪', slots: 15 }
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
  if (s.slots > 0) s.slots -= 1
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
  gap: 10px;
}
.show-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px;
  border-radius: 14px;
  background: #fafdf8;
  border: 1px solid #e7f0e2;
}
.show-main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.show-emoji {
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  border-radius: 12px;
  background: #eaf4e4;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}
.show-text { min-width: 0; }
.show-name {
  font-weight: 800;
  color: var(--forest-deep);
  font-size: 15px;
}
.show-sub {
  font-size: 12px;
  color: var(--muted);
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.show-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  flex: 0 0 auto;
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
