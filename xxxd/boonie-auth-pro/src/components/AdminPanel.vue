<template>
  <div class="admin">
    <!-- ============ 左侧深色侧边栏 ============ -->
    <aside class="side">
      <div class="side-logo">
        <svg class="logo-leaf" viewBox="0 0 24 24" aria-hidden="true">
          <path
            fill="currentColor"
            d="M20 3C10 4 4 9 4 15c0 2 .7 3.7 1.8 5C7 15 11 11 17 9c-4 2.6-7.4 6.2-9.2 10.6C10 21 13.6 22 17.5 22 20.7 22 22 19.6 22 16.5 22 10 21.5 5.5 20 3Z"
          />
        </svg>
        <div class="logo-text">
          <strong>狗熊岭后台</strong>
          <span>森林游乐园管理系统</span>
        </div>
      </div>

      <nav class="side-menu" aria-label="后台菜单">
        <button
          v-for="s in SECTIONS"
          :key="s.key"
          type="button"
          class="side-item"
          :class="{ active: activeKey === s.key }"
          @click="selectSection(s.key)"
        >
          <span class="side-icon">{{ s.icon }}</span>
          <span class="side-name">{{ s.name }}</span>
        </button>
      </nav>

      <div class="side-foot">
        <svg class="foot-leaf" viewBox="0 0 24 24" aria-hidden="true">
          <path
            fill="currentColor"
            d="M20 3C10 4 4 9 4 15c0 2 .7 3.7 1.8 5C7 15 11 11 17 9c-4 2.6-7.4 6.2-9.2 10.6C10 21 13.6 22 17.5 22 20.7 22 22 19.6 22 16.5 22 10 21.5 5.5 20 3Z"
          />
        </svg>
        <span>数据实时同步前台</span>
      </div>
    </aside>

    <!-- ============ 右侧浅色内容区 ============ -->
    <main class="main">
      <!-- 顶部：面包屑 + 管理员头像 + 退出 -->
      <header class="topbar">
        <nav class="crumb" aria-label="面包屑">
          <span class="crumb-item">后台管理</span>
          <span class="crumb-sep">/</span>
          <span class="crumb-item current">{{ current.name }}</span>
        </nav>

        <div class="top-right">
          <img class="admin-avatar" src="/img/avatar.jpg" alt="管理员头像" />
          <div class="admin-who">
            <strong>{{ user?.name || '管理员' }}</strong>
            <span>超级管理员</span>
          </div>
          <button class="btn-exit" type="button" @click="onExit">退出</button>
        </div>
      </header>

      <div class="content">
        <div class="content-head">
          <div>
            <h1 class="page-title">{{ current.icon }} {{ current.name }}</h1>
            <p class="page-desc">{{ current.desc }}</p>
          </div>
          <div class="head-actions">
            <span v-if="msg" class="msg" :class="{ err: msgType === 'err' }">{{ msg }}</span>
            <button class="btn-ghost" type="button" @click="load">🔄 刷新</button>
            <button class="btn-primary" type="button" @click="openCreate">＋ 新增</button>
          </div>
        </div>

        <!-- 数据表格 -->
        <div class="table-card">
          <table class="tbl">
            <thead>
              <tr>
                <th class="col-drag" title="拖拽排序"></th>
                <th class="col-idx">#</th>
                <th v-for="c in current.columns" :key="c.key">{{ c.label }}</th>
                <th class="col-switch">上下架</th>
                <th class="col-ops">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(row, i) in rows"
                :key="row.id ?? row._k"
                class="tbl-row"
                :class="{ dragging: dragIndex === i, off: !row.enabled }"
                draggable="true"
                @dragstart="onDragStart(i)"
                @dragover.prevent="onDragOver(i)"
                @drop.prevent="onDrop(i)"
                @dragend="onDragEnd"
              >
                <td class="col-drag"><span class="drag-handle" title="按住拖拽排序">⋮⋮</span></td>
                <td class="col-idx">{{ i + 1 }}</td>
                <td v-for="c in current.columns" :key="c.key">
                  <span v-if="c.type === 'color'" class="dot" :style="{ background: row[c.key] }"></span>
                  <span v-else>{{ display(row, c) }}</span>
                </td>
                <td class="col-switch">
                  <button
                    type="button"
                    class="sw"
                    :class="{ on: row.enabled !== false }"
                    :aria-pressed="row.enabled !== false"
                    @click="toggleEnabled(row)"
                  >
                    <span class="sw-dot"></span>
                  </button>
                </td>
                <td class="col-ops">
                  <button class="op" type="button" @click="openEdit(row)">编辑</button>
                  <button class="op danger" type="button" @click="removeRow(row)">删除</button>
                </td>
              </tr>
              <tr v-if="!rows.length">
                <td class="empty" :colspan="current.columns.length + 4">暂无数据，点击右上角「＋ 新增」添加</td>
              </tr>
            </tbody>
          </table>
        </div>

        <p class="tip">
          💡 拖拽行首的 ⋮⋮ 可调整顺序；「上下架」控制前台是否展示。
          <template v-if="current.api">本模块已对接后端接口，保存后前台首页实时生效。</template>
          <template v-else>本模块为本地配置（后端暂未开放接口），数据保存在浏览器本地。</template>
        </p>
      </div>
    </main>

    <!-- ============ 新增 / 编辑弹窗 ============ -->
    <div v-if="editing" class="mask" @click.self="closeForm">
      <div class="modal">
        <h2 class="modal-title">{{ editing.id ? '编辑' : '新增' }} · {{ current.name }}</h2>
        <div class="modal-body">
          <label v-for="c in current.columns" :key="c.key" class="fld">
            <span class="fld-label">{{ c.label }}</span>

            <select v-if="c.type === 'select'" v-model="form[c.key]" class="fld-input">
              <option v-for="o in c.options" :key="o.value ?? o" :value="o.value ?? o">
                {{ o.label ?? o }}
              </option>
            </select>

            <textarea
              v-else-if="c.type === 'textarea'"
              v-model="form[c.key]"
              class="fld-input"
              rows="3"
            ></textarea>

            <input v-else-if="c.type === 'color'" v-model="form[c.key]" class="fld-color" type="color" />

            <input
              v-else
              v-model="form[c.key]"
              class="fld-input"
              :type="c.type === 'number' ? 'number' : 'text'"
              :placeholder="'请输入' + c.label"
            />
          </label>

          <div class="fld fld-inline">
            <span class="fld-label">上架状态</span>
            <button type="button" class="sw" :class="{ on: form.enabled !== false }" @click="toggleFormEnabled">
              <span class="sw-dot"></span>
            </button>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn-ghost" type="button" @click="closeForm">取消</button>
          <button class="btn-primary" type="button" :disabled="saving" @click="saveForm">
            {{ saving ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'

const props = defineProps({
  user: { type: Object, default: null },
  token: { type: String, default: '' }
})
const emit = defineEmits(['exit'])

/* ===================== 模块配置（9 个菜单） ===================== */
const SECTIONS = [
  {
    key: 'banner',
    name: '首页 Banner 管理',
    icon: '🖼️',
    desc: '配置前台首页顶部轮播横幅的内容与跳转',
    columns: [
      { key: 'title', label: '标题', type: 'text' },
      { key: 'subtitle', label: '副标题', type: 'text' },
      { key: 'link', label: '跳转板块', type: 'select', options: ['游乐项目', '栖息地', '演出', '门票', '风景'] },
      { key: 'color', label: '主色', type: 'color' }
    ],
    seed: [
      { title: '春日森林季', subtitle: '樱花飘落的日子，来狗熊岭散步吧', link: '游乐项目', color: '#f2a0bd' },
      { title: '熊出没剧场', subtitle: '每天四场，熊大熊二等你来', link: '演出', color: '#f5a623' },
      { title: '亲子探险路线', subtitle: '一条线玩遍十大打卡点', link: '风景', color: '#4c9be8' }
    ]
  },
  {
    key: 'projects',
    name: '热门项目管理',
    icon: '🎡',
    desc: '管理前台「游乐项目」卡片，保存后前台首页实时生效',
    api: '/api/admin/attractions',
    columns: [
      { key: 'name', label: '项目名称', type: 'text' },
      { key: 'emoji', label: '图标', type: 'text' },
      { key: 'tag', label: '标签', type: 'select', options: ['刺激', '浪漫', '亲子', '清凉', '益智'] },
      { key: 'waitMinutes', label: '排队(分钟)', type: 'number' },
      { key: 'color', label: '主题色', type: 'color' },
      { key: 'description', label: '玩法说明', type: 'textarea' }
    ],
    seed: []
  },
  {
    key: 'shows',
    name: '演出时刻表',
    icon: '🎪',
    desc: '维护各剧场的演出时间与地点',
    columns: [
      { key: 'name', label: '演出名称', type: 'text' },
      { key: 'time', label: '时间', type: 'text' },
      { key: 'place', label: '地点', type: 'text' },
      { key: 'duration', label: '时长(分钟)', type: 'number' }
    ],
    seed: [
      { name: '熊大熊二迎宾秀', time: '09:30', place: '中心广场', duration: 20 },
      { name: '森林杂技表演', time: '11:00', place: '松林剧场', duration: 35 },
      { name: '光头强伐木大赛', time: '14:30', place: '木屋舞台', duration: 25 },
      { name: '暮光萤火音乐会', time: '19:00', place: '湖畔草地', duration: 45 }
    ]
  },
  {
    key: 'routes',
    name: '游园路线',
    icon: '🗺️',
    desc: '推荐游玩路线与强度分级',
    columns: [
      { key: 'name', label: '路线名称', type: 'text' },
      { key: 'stops', label: '途经点数', type: 'number' },
      { key: 'duration', label: '建议时长(小时)', type: 'number' },
      { key: 'level', label: '强度', type: 'select', options: ['轻松', '适中', '挑战'] }
    ],
    seed: [
      { name: '亲子半日游', stops: 6, duration: 4, level: '轻松' },
      { name: '刺激一整天', stops: 10, duration: 8, level: '挑战' },
      { name: '浪漫黄昏线', stops: 5, duration: 3, level: '适中' }
    ]
  },
  {
    key: 'spots',
    name: '打卡圣地',
    icon: '📸',
    desc: '前台热门拍照点推荐',
    columns: [
      { key: 'name', label: '打卡点', type: 'text' },
      { key: 'tag', label: '风格', type: 'select', options: ['花海', '林间', '湖畔', '木屋', '云端'] },
      { key: 'hot', label: '热度', type: 'number' }
    ],
    seed: [
      { name: '樱花大道', tag: '花海', hot: 98 },
      { name: '古树树洞', tag: '林间', hot: 91 },
      { name: '萤火湖畔', tag: '湖畔', hot: 87 }
    ]
  },
  {
    key: 'realtime',
    name: '实时数据配置',
    icon: '📊',
    desc: '配置前台实时数据看板的指标项',
    columns: [
      { key: 'label', label: '指标名称', type: 'text' },
      { key: 'value', label: '当前值', type: 'text' },
      { key: 'unit', label: '单位', type: 'text' },
      { key: 'refresh', label: '刷新间隔(秒)', type: 'number' }
    ],
    seed: [
      { label: '在园游客', value: '3,280', unit: '人', refresh: 30 },
      { label: '平均排队', value: '18', unit: '分钟', refresh: 60 },
      { label: '今日演出', value: '6', unit: '场', refresh: 120 }
    ]
  },
  {
    key: 'weather',
    name: '天气 / 客流',
    icon: '🌤️',
    desc: '维护天气与客流预测，供前台展示',
    columns: [
      { key: 'date', label: '日期', type: 'text' },
      { key: 'weather', label: '天气', type: 'select', options: ['晴', '多云', '阴', '小雨', '阵雨'] },
      { key: 'temp', label: '气温(℃)', type: 'number' },
      { key: 'flow', label: '预计客流', type: 'number' }
    ],
    seed: [
      { date: '今天', weather: '晴', temp: 24, flow: 3200 },
      { date: '明天', weather: '多云', temp: 22, flow: 2800 },
      { date: '后天', weather: '小雨', temp: 19, flow: 1500 }
    ]
  },
  {
    key: 'coupon',
    name: '优惠券 / 积分',
    icon: '🎟️',
    desc: '配置可发放的优惠券与积分规则',
    columns: [
      { key: 'name', label: '名称', type: 'text' },
      { key: 'type', label: '类型', type: 'select', options: ['优惠券', '积分'] },
      { key: 'amount', label: '面额(元)', type: 'number' },
      { key: 'points', label: '所需积分', type: 'number' }
    ],
    seed: [
      { name: '春日踏青券', type: '优惠券', amount: 20, points: 0 },
      { name: '森林积分兑换', type: '积分', amount: 10, points: 200 },
      { name: '亲子套票立减', type: '优惠券', amount: 50, points: 0 }
    ]
  },
  {
    key: 'settings',
    name: '管理员设置',
    icon: '👤',
    desc: '查看并调整账号权限（对接后端用户接口）',
    api: '/api/admin/users',
    columns: [
      { key: 'name', label: '账号', type: 'text' },
      { key: 'email', label: '邮箱', type: 'text' },
      {
        key: 'permission',
        label: '权限',
        type: 'select',
        options: [
          { value: 0, label: '管理员' },
          { value: 1, label: '普通用户' }
        ]
      }
    ],
    seed: []
  }
]

const activeKey = ref(SECTIONS[0].key)
const current = computed(() => SECTIONS.find((s) => s.key === activeKey.value))
const rows = ref([])
const msg = ref('')
const msgType = ref('ok')
const editing = ref(null)
const form = reactive({})
const saving = ref(false)
const dragIndex = ref(-1)

/* ===================== 本地持久化（后端暂无接口的模块） ===================== */
const LS = (k) => 'boonie_admin_rows_' + k
const LS_ORDER = (k) => 'boonie_admin_order_' + k
const LS_ENABLED = (k) => 'boonie_admin_enabled_' + k

function readJSON(key, fallback) {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : fallback
  } catch {
    return fallback
  }
}
function writeJSON(key, val) {
  localStorage.setItem(key, JSON.stringify(val))
}

function persistLocal() {
  const key = activeKey.value
  writeJSON(LS(key), rows.value)
}

/** 上下架 / 排序覆盖（后端暂无 sort、enabled 字段，落本地保证刷新后仍生效） */
function persistOverlay() {
  const key = activeKey.value
  const en = {}
  rows.value.forEach((r) => (en[r.id ?? r._k] = r.enabled !== false))
  writeJSON(LS_ENABLED(key), en)
  writeJSON(LS_ORDER(key), rows.value.map((r) => r.id ?? r._k))
}

/* ===================== 数据加载 ===================== */
async function load() {
  const sec = current.value
  rows.value = []
  if (sec.api) {
    try {
      const res = await fetch(sec.api, { headers: authHeaders() })
      const data = await res.json()
      let list = data.data ?? data.attractions ?? data.users ?? data.list ?? []
      const en = readJSON(LS_ENABLED(sec.key), {})
      list = list.map((r) => ({ ...r, enabled: en[r.id] !== undefined ? en[r.id] : true }))
      const order = readJSON(LS_ORDER(sec.key), null)
      if (order && order.length) {
        list.sort((a, b) => order.indexOf(a.id) - order.indexOf(b.id))
      }
      rows.value = list
      return
    } catch {
      flash('加载失败，请确认后端已启动', 'err')
      return
    }
  }
  const local = readJSON(LS(sec.key), null)
  rows.value = local && local.length ? local : sec.seed.map((s, i) => ({ ...s, _k: i + 1, enabled: true }))
}

function authHeaders() {
  return props.token
    ? { Authorization: 'Bearer ' + props.token, 'Content-Type': 'application/json' }
    : { 'Content-Type': 'application/json' }
}

function selectSection(key) {
  activeKey.value = key
  msg.value = ''
  load()
}

/* ===================== 增删改 ===================== */
function blankForm() {
  const f = { enabled: true }
  current.value.columns.forEach((c) => {
    if (c.type === 'number') f[c.key] = 0
    else if (c.type === 'color') f[c.key] = '#4caf50'
    else if (c.type === 'select') f[c.key] = c.options[0].value ?? c.options[0]
    else f[c.key] = ''
  })
  return f
}

function openCreate() {
  editing.value = { id: null }
  Object.assign(form, blankForm())
}
function openEdit(row) {
  editing.value = { id: row.id ?? row._k }
  Object.assign(form, blankForm(), JSON.parse(JSON.stringify(row)))
}
function closeForm() {
  editing.value = null
}
function toggleFormEnabled() {
  form.enabled = form.enabled === false
}

async function saveForm() {
  const sec = current.value
  saving.value = true
  try {
    if (sec.api) {
      const payload = {}
      sec.columns.forEach((c) => (payload[c.key] = form[c.key]))
      const isEdit = !!(editing.value && editing.value.id)
      const res = await fetch(sec.api + (isEdit ? '/' + editing.value.id : ''), {
        method: isEdit ? 'PUT' : 'POST',
        headers: authHeaders(),
        body: JSON.stringify(payload)
      })
      if (!res.ok) {
        const d = await res.json().catch(() => ({}))
        flash(d.message || '保存失败', 'err')
        return
      }
      flash('已保存，前台首页已同步 ✓')
    } else {
      if (editing.value && editing.value.id) {
        const row = rows.value.find((r) => (r.id ?? r._k) === editing.value.id)
        if (row) Object.assign(row, JSON.parse(JSON.stringify(form)))
      } else {
        rows.value.push({ ...JSON.parse(JSON.stringify(form)), _k: Date.now() })
      }
      persistLocal()
      flash('已保存（本地配置）')
    }
    closeForm()
    await load()
  } catch {
    flash('网络异常，请确认后端已启动', 'err')
  } finally {
    saving.value = false
  }
}

async function removeRow(row) {
  if (!confirm('确定删除这条记录吗？')) return
  const sec = current.value
  const id = row.id ?? row._k
  if (sec.api && row.id) {
    try {
      const res = await fetch(sec.api + '/' + row.id, { method: 'DELETE', headers: authHeaders() })
      if (!res.ok) {
        flash('删除失败', 'err')
        return
      }
      flash('已删除，前台首页已同步 ✓')
    } catch {
      flash('网络异常，请确认后端已启动', 'err')
      return
    }
  } else {
    rows.value = rows.value.filter((r) => (r.id ?? r._k) !== id)
    persistLocal()
    flash('已删除')
  }
  await load()
}

function toggleEnabled(row) {
  row.enabled = row.enabled === false
  if (current.value.api) persistOverlay()
  else persistLocal()
  flash(row.enabled ? '已上架' : '已下架')
}

/* ===================== 拖拽排序 ===================== */
function onDragStart(i) {
  dragIndex.value = i
}
function onDragOver() {
  /* 允许放置 */
}
function onDrop(i) {
  const from = dragIndex.value
  if (from < 0 || from === i) return
  const list = rows.value.slice()
  const moved = list.splice(from, 1)[0]
  list.splice(i, 0, moved)
  rows.value = list
  if (current.value.api) persistOverlay()
  else persistLocal()
  flash('顺序已更新')
}
function onDragEnd() {
  dragIndex.value = -1
}

/* ===================== 其它 ===================== */
let msgTimer = null
function flash(text, type = 'ok') {
  msg.value = text
  msgType.value = type
  clearTimeout(msgTimer)
  msgTimer = setTimeout(() => (msg.value = ''), 3000)
}

function display(row, c) {
  const v = row[c.key]
  if (c.key === 'permission') return Number(v) === 0 ? '管理员' : '普通用户'
  if (v === undefined || v === null || v === '') return '—'
  const s = String(v)
  return s.length > 22 ? s.slice(0, 22) + '…' : s
}

function onExit() {
  emit('exit')
}

onMounted(load)
</script>

<style scoped>
.admin {
  display: flex;
  min-height: 100vh;
  background: #f5f7f4;
}

/* ================= 侧边栏（深色 + 金色点缀） ================= */
.side {
  width: 232px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #16291f 0%, #101f17 100%);
  color: #cfded2;
}
.side-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 18px;
  border-bottom: 1px solid rgba(232, 196, 106, 0.14);
}
.logo-leaf {
  width: 26px;
  height: 26px;
  color: #e8c46a;
  flex-shrink: 0;
}
.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}
.logo-text strong {
  font-size: 15px;
  color: #f3ecd8;
  font-weight: 800;
}
.logo-text span {
  font-size: 11px;
  color: #8ea596;
}

.side-menu {
  flex: 1;
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  overflow-y: auto;
}
.side-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  border: none;
  background: transparent;
  color: #b3c6b9;
  font-size: 13.5px;
  font-weight: 600;
  text-align: left;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}
.side-item:hover {
  background: rgba(232, 196, 106, 0.1);
  color: #f3ecd8;
}
.side-item.active {
  background: linear-gradient(90deg, rgba(232, 196, 106, 0.2), rgba(232, 196, 106, 0.04));
  color: #f7dd9a;
  box-shadow: inset 3px 0 0 #e8c46a;
}
.side-icon {
  font-size: 15px;
  width: 20px;
  text-align: center;
}

.side-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 18px;
  font-size: 11.5px;
  color: #7d9086;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}
.foot-leaf {
  width: 16px;
  height: 16px;
  color: rgba(232, 196, 106, 0.55);
}

/* ================= 内容区 ================= */
.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 26px;
  background: #fff;
  border-bottom: 1px solid #e6ebe6;
}
.crumb {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
}
.crumb-item {
  color: #8b9a90;
}
.crumb-sep {
  color: #c3cec5;
}
.crumb-item.current {
  color: #2f4a38;
  font-weight: 700;
}
.top-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.admin-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #e8c46a;
}
.admin-who {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}
.admin-who strong {
  font-size: 13px;
  color: #2f4a38;
}
.admin-who span {
  font-size: 11px;
  color: #8b9a90;
}
.btn-exit {
  border: 1px solid #dfe5df;
  background: #fff;
  color: #6b7c71;
  font-size: 12.5px;
  font-weight: 700;
  padding: 7px 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-exit:hover {
  border-color: #e0a52a;
  color: #b5811a;
  background: #fffaf0;
}

.content {
  flex: 1;
  padding: 22px 26px 30px;
  overflow-y: auto;
}
.content-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.page-title {
  margin: 0 0 4px;
  font-size: 19px;
  font-weight: 800;
  color: #24382c;
}
.page-desc {
  margin: 0;
  font-size: 12.5px;
  color: #7d8c82;
}
.head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.msg {
  font-size: 12.5px;
  font-weight: 700;
  color: #2f7d4f;
  background: #eaf6ee;
  border: 1px solid #bfe3c8;
  padding: 6px 12px;
  border-radius: 8px;
}
.msg.err {
  color: #c0392b;
  background: #fdecea;
  border-color: #f5c6c2;
}
.btn-primary {
  border: none;
  background: linear-gradient(135deg, #f7dd9a, #e0b455);
  color: #3a2a08;
  font-size: 13px;
  font-weight: 800;
  padding: 9px 16px;
  border-radius: 9px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(224, 180, 85, 0.3);
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-ghost {
  border: 1px solid #dfe5df;
  background: #fff;
  color: #5f7066;
  font-size: 13px;
  font-weight: 700;
  padding: 9px 14px;
  border-radius: 9px;
  cursor: pointer;
}
.btn-ghost:hover {
  border-color: #c3cec5;
}

/* ================= 表格 ================= */
.table-card {
  background: #fff;
  border: 1px solid #e6ebe6;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(30, 50, 38, 0.05);
}
.tbl {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.tbl thead th {
  background: #f6f9f7;
  color: #6b7c71;
  font-weight: 700;
  text-align: left;
  padding: 11px 12px;
  border-bottom: 1px solid #e6ebe6;
  white-space: nowrap;
}
.tbl tbody td {
  padding: 10px 12px;
  border-bottom: 1px solid #f0f4f1;
  color: #3d4f44;
  vertical-align: middle;
}
.tbl-row:hover {
  background: #fafcfb;
}
.tbl-row.dragging {
  opacity: 0.45;
  background: #fff8e8;
}
.tbl-row.off td {
  color: #a9b5ac;
}
.col-drag {
  width: 40px;
  text-align: center;
}
.drag-handle {
  cursor: grab;
  color: #b6c2b9;
  font-size: 14px;
  user-select: none;
}
.drag-handle:active {
  cursor: grabbing;
}
.col-idx {
  width: 42px;
  color: #a9b5ac;
}
.col-switch {
  width: 92px;
  text-align: center;
}
.col-ops {
  width: 130px;
}
.op {
  border: 1px solid #dfe5df;
  background: #fff;
  color: #5f7066;
  font-size: 12px;
  font-weight: 700;
  padding: 5px 11px;
  border-radius: 7px;
  cursor: pointer;
  margin-right: 6px;
}
.op:hover {
  border-color: #e8c46a;
  color: #b5811a;
}
.op.danger:hover {
  border-color: #e08a7a;
  color: #c0392b;
}
.dot {
  display: inline-block;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.1);
  vertical-align: middle;
}
.empty {
  text-align: center;
  padding: 34px 0;
  color: #a1aea5;
}

/* 上下架开关 */
.sw {
  position: relative;
  width: 42px;
  height: 22px;
  border: none;
  border-radius: 999px;
  background: #d5ddd7;
  cursor: pointer;
  transition: background 0.18s ease;
  padding: 0;
}
.sw.on {
  background: linear-gradient(135deg, #6fcf97, #3f9c68);
}
.sw-dot {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  transition: transform 0.18s ease;
}
.sw.on .sw-dot {
  transform: translateX(20px);
}

.tip {
  margin: 12px 0 0;
  font-size: 12px;
  color: #8b9a90;
  line-height: 1.7;
}

/* ================= 弹窗 ================= */
.mask {
  position: fixed;
  inset: 0;
  background: rgba(16, 31, 23, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 60;
}
.modal {
  width: 100%;
  max-width: 460px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.28);
  overflow: hidden;
}
.modal-title {
  margin: 0;
  padding: 16px 20px;
  font-size: 15px;
  font-weight: 800;
  color: #24382c;
  border-bottom: 1px solid #eef2ef;
}
.modal-body {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 13px;
  max-height: 56vh;
  overflow-y: auto;
}
.fld {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.fld-inline {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}
.fld-label {
  font-size: 12.5px;
  font-weight: 700;
  color: #5f7066;
}
.fld-input {
  border: 1px solid #dfe5df;
  border-radius: 9px;
  padding: 9px 12px;
  font-size: 13.5px;
  color: #3d4f44;
  outline: none;
  font-family: inherit;
  resize: vertical;
}
.fld-input:focus {
  border-color: #e8c46a;
  box-shadow: 0 0 0 3px rgba(232, 196, 106, 0.15);
}
.fld-color {
  width: 52px;
  height: 34px;
  border: 1px solid #dfe5df;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  padding: 2px;
}
.modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  background: #fafcfb;
  border-top: 1px solid #eef2ef;
}

@media (max-width: 900px) {
  .side {
    width: 74px;
  }
  .side-name,
  .logo-text,
  .side-foot span {
    display: none;
  }
  .side-logo {
    justify-content: center;
  }
  .side-item {
    justify-content: center;
  }
}
</style>
