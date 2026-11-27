<template>
  <div class="cv">
    <div class="cv-head">
      <div>
        <h1 class="cv-title">{{ module.icon }} {{ module.name }}</h1>
        <p class="cv-desc">{{ module.desc }}</p>
      </div>
      <div class="cv-tools">
        <span v-if="msg" class="cv-msg" :class="{ err: msgType === 'err' }">{{ msg }}</span>
        <button class="cv-btn ghost" type="button" @click="load">🔄 刷新</button>
        <button
          v-if="selected.length"
          class="cv-btn danger"
          type="button"
          :disabled="busy"
          @click="removeSelected"
        >🗑 删除选中 ({{ selected.length }})</button>
        <button class="cv-btn primary" type="button" @click="openCreate">＋ 新增</button>
      </div>
    </div>

    <div class="cv-card">
      <!-- 搜索栏 -->
      <div class="cv-search">
        <input
          v-model.trim="keyword"
          type="text"
          placeholder="输入关键字搜索…"
          @keyup.enter="onSearch"
        />
        <button class="cv-btn primary" type="button" @click="onSearch">🔍 搜索</button>
        <button class="cv-btn ghost" type="button" @click="resetSearch">重置</button>
        <span class="cv-search-tip">💡 数据为本地 Mock，刷新页面不会丢失</span>
      </div>

      <!-- 配置化表格 -->
      <ProTable
        :columns="module.columns"
        :data="rows"
        selectable
        :selected="selected"
        :loading="loading"
        @update:selected="selected = $event"
        @toggle="toggleStatus"
      >
        <template #actions="{ row }">
          <button class="cv-link" type="button" @click="openEdit(row)">编辑</button>
          <button class="cv-link danger" type="button" @click="removeOne(row)">删除</button>
        </template>
      </ProTable>

      <!-- 分页 -->
      <Pagination
        v-model:page="page"
        v-model:page-size="pageSize"
        :total="total"
        @change="load"
      />
    </div>

    <!-- 配置化弹窗表单 -->
    <FormDialog
      v-model="dialogOpen"
      :title="editing ? '编辑' : '新增'"
      :fields="module.fields"
      :initial="form"
      :saving="saving"
      @submit="save"
    />
  </div>
</template>

<script setup>
import { computed, ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { findModule } from '../config/modules.js'
import * as api from '../api/mock.js'
import ProTable from '../components/ProTable.vue'
import FormDialog from '../components/FormDialog.vue'
import Pagination from '../components/Pagination.vue'

const route = useRoute()
const moduleKey = computed(() => route.params.module)
const module = computed(() => findModule(moduleKey.value) || { name: '未知模块', icon: '❓', desc: '', columns: [], fields: [] })

const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const selected = ref([])
const loading = ref(false)
const busy = ref(false)

const dialogOpen = ref(false)
const editing = ref(null)
const form = ref({})
const saving = ref(false)

const msg = ref('')
const msgType = ref('')
let msgTimer = null

function showMsg(text, err = false) {
  msg.value = text
  msgType.value = err ? 'err' : ''
  clearTimeout(msgTimer)
  msgTimer = setTimeout(() => (msg.value = ''), 2600)
}

async function load() {
  loading.value = true
  try {
    const res = await api.listModule(moduleKey.value, {
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value
    })
    rows.value = res.list
    total.value = res.total
    selected.value = []
  } catch (e) {
    showMsg(e?.message || '加载失败', true)
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  load()
}

function resetSearch() {
  keyword.value = ''
  page.value = 1
  load()
}

function openCreate() {
  editing.value = null
  form.value = {}
  dialogOpen.value = true
}

function openEdit(row) {
  editing.value = row
  form.value = { ...row }
  dialogOpen.value = true
}

async function save(payload) {
  saving.value = true
  try {
    if (editing.value) {
      await api.updateRow(moduleKey.value, editing.value.id, payload)
      showMsg('✓ 已保存修改')
    } else {
      await api.createRow(moduleKey.value, payload)
      showMsg('✓ 新增成功')
    }
    dialogOpen.value = false
    await load()
  } catch (e) {
    showMsg(e?.message || '保存失败', true)
  } finally {
    saving.value = false
  }
}

async function removeOne(row) {
  if (!window.confirm(`确定删除「${row.name || row.title || row.orderNo || '这条记录'}」吗？`)) return
  busy.value = true
  try {
    await api.deleteRow(moduleKey.value, row.id)
    showMsg('✓ 已删除')
    await load()
  } catch (e) {
    showMsg(e?.message || '删除失败', true)
  } finally {
    busy.value = false
  }
}

async function removeSelected() {
  if (!window.confirm(`确定删除选中的 ${selected.value.length} 条记录吗？`)) return
  busy.value = true
  try {
    await api.deleteRow(moduleKey.value, selected.value)
    showMsg('✓ 批量删除成功')
    await load()
  } catch (e) {
    showMsg(e?.message || '删除失败', true)
  } finally {
    busy.value = false
  }
}

async function toggleStatus(row) {
  try {
    await api.toggleRow(moduleKey.value, row.id)
    showMsg('✓ 状态已切换')
    await load()
  } catch (e) {
    showMsg(e?.message || '操作失败', true)
  }
}

watch(moduleKey, () => {
  page.value = 1
  keyword.value = ''
  selected.value = []
  load()
})
onMounted(load)
</script>

<style scoped>
.cv-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}
.cv-title {
  font-size: 21px;
  font-weight: 900;
  color: var(--forest-deep);
  margin: 0 0 5px;
}
.cv-desc {
  font-size: 12px;
  font-weight: 700;
  color: var(--muted);
  margin: 0;
}
.cv-tools {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.cv-msg {
  font-size: 12px;
  font-weight: 800;
  color: var(--forest);
  background: rgba(76, 175, 109, 0.14);
  padding: 6px 12px;
  border-radius: 999px;
}
.cv-msg.err {
  color: #c74a1e;
  background: rgba(228, 87, 46, 0.12);
}
.cv-btn {
  border: 2px solid var(--wood-light);
  background: transparent;
  color: var(--text);
  font-size: 12px;
  font-weight: 900;
  padding: 8px 16px;
  border-radius: 999px;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.16s ease;
}
.cv-btn.primary {
  background: var(--forest);
  border-color: var(--forest);
  color: #fff;
}
.cv-btn.danger {
  border-color: #c74a1e;
  color: #c74a1e;
}
.cv-btn:disabled {
  opacity: 0.55;
  cursor: default;
}
.cv-card {
  background: #fffdf7;
  border: 2px solid var(--wood-light);
  border-radius: 18px;
  padding: 16px 18px;
  box-shadow: 0 16px 36px -26px rgba(31, 92, 57, 0.7);
}
.cv-search {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.cv-search input {
  flex: 1;
  min-width: 180px;
  max-width: 320px;
  padding: 8px 12px;
  border: 2px solid var(--wood-light);
  border-radius: 10px;
  font-size: 13px;
  font-family: inherit;
  color: var(--text);
  background: #fff;
  outline: none;
}
.cv-search input:focus {
  border-color: var(--forest);
}
.cv-search-tip {
  margin-left: auto;
  font-size: 11px;
  font-weight: 700;
  color: var(--muted);
}
.cv-link {
  border: none;
  background: transparent;
  color: var(--forest);
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
  padding: 4px 8px;
  font-family: inherit;
}
.cv-link.danger {
  color: #c74a1e;
}
</style>
