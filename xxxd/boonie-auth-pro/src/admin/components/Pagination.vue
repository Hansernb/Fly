<template>
  <div class="pg">
    <span class="pg-info">共 {{ total }} 条 · 第 {{ page }}/{{ pageCount }} 页</span>
    <div class="pg-btns">
      <button type="button" :disabled="page <= 1" @click="go(page - 1)" aria-label="上一页">‹</button>
      <button
        v-for="n in visiblePages"
        :key="n"
        type="button"
        :class="{ on: n === page }"
        @click="go(n)"
      >{{ n }}</button>
      <button type="button" :disabled="page >= pageCount" @click="go(page + 1)" aria-label="下一页">›</button>
    </div>
    <select v-model.number="pageSizeLocal" class="pg-size" aria-label="每页条数">
      <option :value="5">5 条/页</option>
      <option :value="10">10 条/页</option>
      <option :value="20">20 条/页</option>
    </select>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  total: { type: Number, default: 0 }
})
const emit = defineEmits(['update:page', 'update:pageSize', 'change'])

const pageSizeLocal = ref(props.pageSize)
watch(pageSizeLocal, (v) => {
  emit('update:pageSize', v)
  emit('update:page', 1)
  emit('change', 1, v)
})

const pageCount = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))
const visiblePages = computed(() => {
  const count = pageCount.value
  if (count <= 7) return Array.from({ length: count }, (_, i) => i + 1)
  const start = Math.max(1, Math.min(props.page - 3, count - 6))
  return Array.from({ length: 7 }, (_, i) => start + i)
})

function go(n) {
  if (n < 1 || n > pageCount.value || n === props.page) return
  emit('update:page', n)
  emit('change', n, props.pageSize)
}
</script>

<style scoped>
.pg {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
  flex-wrap: wrap;
  padding: 14px 2px 2px;
}
.pg-info {
  font-size: 12px;
  font-weight: 700;
  color: var(--muted);
}
.pg-btns {
  display: flex;
  gap: 5px;
}
.pg-btns button {
  min-width: 30px;
  height: 30px;
  border: 2px solid var(--wood-light);
  border-radius: 8px;
  background: #fff;
  color: var(--text);
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
  padding: 0 7px;
}
.pg-btns button.on {
  background: var(--forest);
  border-color: var(--forest);
  color: #fff;
}
.pg-btns button:disabled {
  opacity: 0.4;
  cursor: default;
}
.pg-size {
  border: 2px solid var(--wood-light);
  border-radius: 8px;
  padding: 6px 8px;
  font-size: 12px;
  font-weight: 700;
  color: var(--text);
  background: #fff;
  cursor: pointer;
}
</style>
