<template>
  <div class="pt-wrap">
    <table class="pt-table">
      <thead>
        <tr>
          <th v-if="selectable" class="pt-check">
            <input
              type="checkbox"
              :checked="allSelected"
              :indeterminate="someSelected"
              aria-label="全选"
              @change="toggleAll"
            />
          </th>
          <th v-for="c in columns" :key="c.key" :style="c.width ? { width: c.width } : {}">
            {{ c.label }}
          </th>
          <th v-if="$slots.actions" class="pt-actions">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="row in data"
          :key="row.id"
          :class="{ 'is-selected': selected.includes(row.id) }"
        >
          <td v-if="selectable" class="pt-check">
            <input
              type="checkbox"
              :checked="selected.includes(row.id)"
              aria-label="选择该行"
              @change="toggleOne(row.id)"
            />
          </td>
          <td v-for="c in columns" :key="c.key">
            <!-- 自定义单元格槽位优先 -->
            <slot :name="'cell-' + c.key" :row="row" :value="row[c.key]">
              <span v-if="c.type === 'status'" class="pt-tag" :class="statusClass(row[c.key])">
                {{ row[c.key] }}
              </span>
              <span v-else-if="c.type === 'statusBool'" class="pt-tag" :class="row[c.key] ? 'ok' : 'off'">
                {{ row[c.key] ? '启用' : '禁用' }}
              </span>
              <i v-else-if="c.type === 'color'" class="pt-color" :style="{ background: row[c.key] }" :title="row[c.key]"></i>
              <template v-else-if="c.type === 'switch'">
                <button
                  class="pt-switch"
                  :class="{ on: row[c.key] }"
                  type="button"
                  :aria-label="row[c.key] ? '点击关闭' : '点击启用'"
                  @click="$emit('toggle', row)"
                ><i></i></button>
              </template>
              <span v-else>{{ c.format ? c.format(row[c.key], row) : (row[c.key] ?? '—') }}</span>
            </slot>
          </td>
          <td v-if="$slots.actions" class="pt-actions">
            <slot name="actions" :row="row"></slot>
          </td>
        </tr>
        <tr v-if="!data.length">
          <td class="pt-empty" :colspan="columns.length + (selectable ? 1 : 0) + ($slots.actions ? 1 : 0)">
            🍂 {{ emptyText }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  columns: { type: Array, default: () => [] },
  data: { type: Array, default: () => [] },
  selectable: { type: Boolean, default: false },
  selected: { type: Array, default: () => [] },
  emptyText: { type: String, default: '暂无数据' }
})
const emit = defineEmits(['update:selected', 'toggle'])

const allSelected = computed(
  () => props.data.length > 0 && props.data.every((r) => props.selected.includes(r.id))
)
const someSelected = computed(
  () => props.selected.length > 0 && !allSelected.value
)

function statusClass(s) {
  const v = String(s ?? '')
  if (['上架', '已通过', '已使用', '待使用'].includes(v)) return 'ok'
  if (['下架', '已驳回', '已取消', '禁用'].includes(v)) return 'off'
  if (['待审核'].includes(v)) return 'warn'
  return ''
}

function toggleAll() {
  const ids = props.data.map((r) => r.id)
  if (allSelected.value) {
    emit('update:selected', props.selected.filter((id) => !ids.includes(id)))
  } else {
    emit('update:selected', [...new Set([...props.selected, ...ids])])
  }
}

function toggleOne(id) {
  emit(
    'update:selected',
    props.selected.includes(id)
      ? props.selected.filter((x) => x !== id)
      : [...props.selected, id]
  )
}
</script>

<style scoped>
.pt-wrap {
  overflow-x: auto;
  border-radius: 12px;
}
.pt-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.pt-table thead th {
  text-align: left;
  padding: 12px 14px;
  background: #f4f7f2;
  color: var(--forest-deep);
  font-weight: 900;
  font-size: 12px;
  white-space: nowrap;
  border-bottom: 2px solid var(--wood-light);
}
.pt-table tbody td {
  padding: 12px 14px;
  border-bottom: 1px solid #eef2ec;
  color: var(--text);
  vertical-align: middle;
}
.pt-table tbody tr:hover {
  background: #fbfdf9;
}
.pt-table tbody tr.is-selected {
  background: #f0f8f1;
}
.pt-check {
  width: 40px;
  text-align: center !important;
}
.pt-check input {
  width: 15px;
  height: 15px;
  accent-color: var(--forest);
  cursor: pointer;
}
.pt-actions {
  text-align: right;
  white-space: nowrap;
}
.pt-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 900;
  background: #eef2ec;
  color: var(--muted);
}
.pt-tag.ok {
  background: rgba(76, 175, 109, 0.18);
  color: var(--forest);
}
.pt-tag.off {
  background: rgba(140, 160, 180, 0.2);
  color: #6b7785;
}
.pt-tag.warn {
  background: rgba(255, 183, 3, 0.22);
  color: #a9740a;
}
.pt-color {
  display: inline-block;
  width: 22px;
  height: 22px;
  border-radius: 7px;
  border: 2px solid #fff;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.08);
  vertical-align: middle;
}
.pt-switch {
  position: relative;
  width: 38px;
  height: 20px;
  border: none;
  border-radius: 999px;
  background: #cfd6cd;
  cursor: pointer;
  padding: 0;
  transition: background 0.18s ease;
}
.pt-switch.on {
  background: var(--forest);
}
.pt-switch i {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.18s ease;
}
.pt-switch.on i {
  transform: translateX(18px);
}
.pt-empty {
  text-align: center;
  padding: 48px 12px !important;
  color: var(--muted);
  font-weight: 700;
}
</style>
