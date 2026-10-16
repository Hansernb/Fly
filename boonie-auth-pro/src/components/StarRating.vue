<template>
  <span class="stars" :class="{ editable }" :style="{ fontSize: size + 'px' }">
    <span class="layer base">{{ starStr }}</span>
    <span class="layer fill" :style="{ width: fillWidth }">{{ starStr }}</span>
    <span v-if="editable" class="input">
      <button
        v-for="i in max"
        :key="i"
        type="button"
        :aria-label="i + ' 星'"
        @mouseenter="hover = i"
        @mouseleave="hover = 0"
        @click="$emit('update:modelValue', i)"
      ></button>
    </span>
  </span>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  score: { type: Number, default: 0 }, // 只读展示用的分值
  modelValue: { type: Number, default: 0 }, // 可编辑时双向绑定
  max: { type: Number, default: 5 },
  editable: { type: Boolean, default: false },
  size: { type: Number, default: 18 }
})
const emit = defineEmits(['update:modelValue'])

const hover = ref(0)
const shown = computed(() => (props.editable ? hover.value || props.modelValue : props.score))
const fillWidth = computed(() => Math.max(0, Math.min(100, (shown.value / props.max) * 100)) + '%')
const starStr = computed(() => '★'.repeat(props.max))
</script>

<style scoped>
.stars {
  position: relative;
  display: inline-block;
  line-height: 1;
  white-space: nowrap;
  vertical-align: middle;
}
.layer {
  letter-spacing: 2px;
}
.layer.base {
  color: #e3d8c4;
}
.layer.fill {
  position: absolute;
  top: 0;
  left: 0;
  color: var(--honey, #ffb703);
  overflow: hidden;
  width: 0;
}
.input {
  position: absolute;
  inset: 0;
  display: flex;
}
.input button {
  flex: 1;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
}
.editable {
  cursor: pointer;
}
</style>
