<template>
  <transition name="fd">
    <div v-if="modelValue" class="fd-mask" @click.self="close">
      <div class="fd-dialog" role="dialog" aria-modal="true">
        <header class="fd-head">
          <h2>{{ title }}</h2>
          <button class="fd-x" type="button" aria-label="关闭" @click="close">✕</button>
        </header>

        <form class="fd-body" @submit.prevent="submit">
          <label v-for="f in fields" :key="f.key" class="fd-field">
            <span class="fd-label">
              {{ f.label }}
              <em v-if="f.required">*</em>
            </span>

            <select
              v-if="f.type === 'select'"
              v-model="draft[f.key]"
              class="fd-input"
            >
              <option v-for="o in normalizedOptions(f)" :key="o.value" :value="o.value">
                {{ o.label }}
              </option>
            </select>

            <textarea
              v-else-if="f.type === 'textarea'"
              v-model="draft[f.key]"
              class="fd-input fd-textarea"
              :placeholder="f.placeholder || ''"
              rows="3"
            ></textarea>

            <input
              v-else-if="f.type === 'number'"
              v-model.number="draft[f.key]"
              class="fd-input"
              type="number"
              :placeholder="f.placeholder || ''"
            />

            <input
              v-else-if="f.type === 'date'"
              v-model="draft[f.key]"
              class="fd-input"
              type="date"
            />

            <input
              v-else-if="f.type === 'color'"
              v-model="draft[f.key]"
              class="fd-color"
              type="color"
            />

            <label v-else-if="f.type === 'switch'" class="fd-switch-line">
              <button
                class="fd-switch"
                :class="{ on: draft[f.key] }"
                type="button"
                @click="draft[f.key] = !draft[f.key]"
              ><i></i></button>
              <span>{{ draft[f.key] ? '启用' : '禁用' }}</span>
            </label>

            <input
              v-else
              v-model="draft[f.key]"
              class="fd-input"
              type="text"
              :placeholder="f.placeholder || ''"
            />

            <small v-if="errors[f.key]" class="fd-err">{{ errors[f.key] }}</small>
          </label>
        </form>

        <footer class="fd-foot">
          <button class="fd-btn ghost" type="button" @click="close">取消</button>
          <button class="fd-btn primary" type="button" :disabled="saving" @click="submit">
            {{ saving ? '保存中…' : '确定保存' }}
          </button>
        </footer>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { reactive, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '编辑' },
  fields: { type: Array, default: () => [] },
  initial: { type: Object, default: () => ({}) },
  saving: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'submit'])

const draft = reactive({})
const errors = reactive({})

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    Object.keys(draft).forEach((k) => delete draft[k])
    Object.keys(errors).forEach((k) => delete errors[k])
    props.fields.forEach((f) => {
      draft[f.key] = props.initial[f.key] ?? (f.type === 'switch' ? false : '')
    })
  },
  { immediate: true }
)

function normalizedOptions(f) {
  return (f.options || []).map((o) =>
    typeof o === 'object' ? o : { value: o, label: f.optionLabels?.[o] ?? String(o) }
  )
}

function close() {
  emit('update:modelValue', false)
}

function submit() {
  Object.keys(errors).forEach((k) => delete errors[k])
  let ok = true
  for (const f of props.fields) {
    const v = draft[f.key]
    if (f.required && (v === '' || v === null || v === undefined)) {
      errors[f.key] = `请填写${f.label}`
      ok = false
    }
    if (f.type === 'number' && v !== '' && v !== null && Number.isNaN(Number(v))) {
      errors[f.key] = '请输入数字'
      ok = false
    }
  }
  if (!ok) return
  emit('submit', { ...draft })
}
</script>

<style scoped>
.fd-mask {
  position: fixed;
  inset: 0;
  background: rgba(24, 36, 29, 0.5);
  display: grid;
  place-items: center;
  z-index: 2000;
  padding: 20px;
}
.fd-dialog {
  width: min(560px, 100%);
  max-height: 86vh;
  display: flex;
  flex-direction: column;
  background: #fffdf7;
  border: 2px solid var(--wood-light);
  border-radius: 20px;
  box-shadow: 0 24px 64px -20px rgba(24, 36, 29, 0.5);
  overflow: hidden;
}
.fd-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 22px;
  border-bottom: 2px solid #eef2ec;
}
.fd-head h2 {
  font-size: 16px;
  font-weight: 900;
  color: var(--forest-deep);
}
.fd-x {
  border: none;
  background: transparent;
  font-size: 16px;
  color: var(--muted);
  cursor: pointer;
}
.fd-body {
  padding: 18px 22px;
  overflow-y: auto;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.fd-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.fd-field:has(.fd-textarea) {
  grid-column: 1 / -1;
}
.fd-label {
  font-size: 12px;
  font-weight: 900;
  color: var(--text);
}
.fd-label em {
  color: #d9534f;
  font-style: normal;
}
.fd-input {
  width: 100%;
  box-sizing: border-box;
  padding: 9px 12px;
  border: 2px solid var(--wood-light);
  border-radius: 10px;
  background: #fff;
  color: var(--text);
  font-size: 13px;
  font-family: inherit;
  outline: none;
  transition: border-color 0.16s ease;
}
.fd-input:focus {
  border-color: var(--forest);
}
.fd-textarea {
  resize: vertical;
}
.fd-color {
  width: 56px;
  height: 36px;
  border: 2px solid var(--wood-light);
  border-radius: 10px;
  padding: 2px;
  cursor: pointer;
}
.fd-switch-line {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 39px;
  font-size: 13px;
  font-weight: 700;
  color: var(--muted);
}
.fd-switch {
  position: relative;
  width: 40px;
  height: 22px;
  border: none;
  border-radius: 999px;
  background: #cfd6cd;
  cursor: pointer;
  padding: 0;
  transition: background 0.18s ease;
}
.fd-switch.on {
  background: var(--forest);
}
.fd-switch i {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.18s ease;
}
.fd-switch.on i {
  transform: translateX(18px);
}
.fd-err {
  color: #d9534f;
  font-size: 11px;
  font-weight: 700;
}
.fd-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 22px;
  border-top: 2px solid #eef2ec;
}
.fd-btn {
  border-radius: 999px;
  padding: 9px 22px;
  font-size: 13px;
  font-weight: 900;
  cursor: pointer;
  border: 2px solid var(--wood-light);
  background: transparent;
  color: var(--text);
}
.fd-btn.primary {
  background: var(--forest);
  border-color: var(--forest);
  color: #fff;
}
.fd-btn:disabled {
  opacity: 0.6;
  cursor: default;
}
.fd-enter-active,
.fd-leave-active {
  transition: opacity 0.18s ease;
}
.fd-enter-from,
.fd-leave-to {
  opacity: 0;
}

@media (max-width: 640px) {
  .fd-body {
    grid-template-columns: 1fr;
  }
}
</style>
