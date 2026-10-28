<template>
  <div class="admin">
    <!-- 顶部栏 -->
    <header class="ad-nav">
      <div class="ad-nav-inner">
        <div class="ad-logo">⚙️ 后台管理系统</div>
        <div class="ad-user">
          <span class="ad-who">管理员：{{ user.name }}</span>
          <button class="ad-exit" type="button" @click="$emit('exit')">← 返回首页</button>
        </div>
      </div>
    </header>

    <!-- 标签页 -->
    <div class="ad-tabs">
      <button
        v-for="t in tabs"
        :key="t.key"
        class="ad-tab"
        :class="{ active: active === t.key }"
        type="button"
        @click="active = t.key"
      >
        {{ t.icon }} {{ t.label }}
      </button>
      <button class="ad-refresh" type="button" @click="loadAll">🔄 刷新数据</button>
    </div>

    <div class="ad-body">
      <p v-if="msg" class="ad-msg" :class="{ err: msgType === 'err' }">{{ msg }}</p>

      <!-- ============ 游乐项目管理 ============ -->
      <section v-if="active === 'attractions'">
        <div class="ad-panel">
          <h2 class="ad-h2">{{ editingId ? '编辑项目' : '新增项目' }}</h2>
          <div class="ad-form">
            <input v-model="form.name" class="ad-input" placeholder="项目名称 *" />
            <input v-model="form.emoji" class="ad-input ad-input-sm" placeholder="图标 🎡" />
            <input v-model="form.tag" class="ad-input ad-input-sm" placeholder="标签（刺激/浪漫/亲子/清凉/益智）" />
            <input v-model.number="form.waitMinutes" class="ad-input ad-input-sm" type="number" min="0" placeholder="排队分钟" />
            <input v-model="form.color" class="ad-input ad-input-sm" type="color" title="主题色" />
            <textarea v-model="form.description" class="ad-input ad-textarea" placeholder="项目具体内容 / 玩法说明"></textarea>
            <div class="ad-form-actions">
              <button class="ad-btn primary" type="button" @click="saveAttraction">
                {{ editingId ? '保存修改' : '新增项目' }}
              </button>
              <button v-if="editingId" class="ad-btn" type="button" @click="resetForm">取消编辑</button>
            </div>
          </div>
        </div>

        <div class="ad-panel">
          <h2 class="ad-h2">项目列表（{{ attractions.length }}）</h2>
          <div v-if="!attractions.length" class="ad-empty">暂无项目数据</div>
          <div v-else class="ad-list">
            <div v-for="a in attractions" :key="a.id" class="ad-item">
              <div class="ad-item-main">
                <span class="ad-emoji" :style="{ background: a.color }">{{ a.emoji }}</span>
                <div class="ad-item-text">
                  <div class="ad-item-title">
                    {{ a.name }}
                    <span class="ad-tag">{{ a.tag }}</span>
                    <span class="ad-wait">排队 {{ a.waitMinutes }} 分钟</span>
                  </div>
                  <div class="ad-item-desc">{{ a.description }}</div>
                </div>
              </div>
              <div class="ad-item-ops">
                <button class="ad-btn sm" type="button" @click="startEdit(a)">编辑</button>
                <button class="ad-btn sm danger" type="button" @click="removeAttraction(a)">删除</button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ============ 游客评价管理 ============ -->
      <section v-if="active === 'reviews'">
        <div class="ad-panel">
          <h2 class="ad-h2">游客评价（{{ reviews.length }}）</h2>
          <div v-if="!reviews.length" class="ad-empty">暂无评价数据</div>
          <div v-else class="ad-list">
            <div v-for="r in reviews" :key="r.id" class="ad-item">
              <div class="ad-item-main">
                <span class="ad-emoji">{{ r.avatar || '🐻' }}</span>
                <div class="ad-item-text">
                  <div class="ad-item-title">
                    {{ r.name }}
                    <span class="ad-tag">{{ r.attraction }}</span>
                    <span class="ad-wait">⭐ {{ r.rating }}</span>
                    <span class="ad-wait">{{ r.date }}</span>
                  </div>
                  <div class="ad-item-desc">{{ r.text }}</div>
                </div>
              </div>
              <div class="ad-item-ops">
                <button class="ad-btn sm danger" type="button" @click="removeReview(r)">删除</button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ============ 用户与权限管理 ============ -->
      <section v-if="active === 'users'">
        <div class="ad-panel">
          <h2 class="ad-h2">用户管理（{{ users.length }}）</h2>
          <p class="ad-hint">权限说明：<b>0 = 管理员</b>（可见并登录后台）；<b>1 = 默认权限</b>（普通注册用户，看不到后台入口也无法登录）。</p>
          <div v-if="!users.length" class="ad-empty">暂无用户</div>
          <div v-else class="ad-list">
            <div v-for="u in users" :key="u.id" class="ad-item">
              <div class="ad-item-main">
                <span class="ad-emoji">{{ u.permission === 0 ? '👑' : '🐻' }}</span>
                <div class="ad-item-text">
                  <div class="ad-item-title">
                    {{ u.name }}
                    <span class="ad-wait">{{ u.email }}</span>
                    <span class="ad-perm" :class="{ admin: u.permission === 0 }">
                      {{ u.permission === 0 ? '管理员(0)' : '默认权限(1)' }}
                    </span>
                  </div>
                  <div class="ad-item-desc">注册时间：{{ u.createdAt || '-' }}</div>
                </div>
              </div>
              <div class="ad-item-ops">
                <button
                  class="ad-btn sm"
                  type="button"
                  @click="togglePermission(u)"
                >
                  {{ u.permission === 0 ? '降为默认' : '设为管理员' }}
                </button>
                <button class="ad-btn sm" type="button" @click="openReset(u)">重置密码</button>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- 重置密码弹窗 -->
    <div v-if="resetUser" class="ad-mask" @click="resetUser = null">
      <div class="ad-modal" @click.stop>
        <h3 class="ad-h2">重置「{{ resetUser.name }}」的密码</h3>
        <p class="ad-hint">新密码以明文保存到数据库（按项目约定不加密），至少 6 位。</p>
        <input v-model="newPwd" class="ad-input" placeholder="输入新密码（至少 6 位）" />
        <div class="ad-form-actions">
          <button class="ad-btn primary" type="button" @click="doReset">确认重置</button>
          <button class="ad-btn" type="button" @click="resetUser = null">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const props = defineProps({
  user: { type: Object, required: true },
  token: { type: String, required: true }
})
const emit = defineEmits(['exit'])

const tabs = [
  { key: 'attractions', label: '游乐项目管理', icon: '🎡' },
  { key: 'reviews', label: '游客评价管理', icon: '💬' },
  { key: 'users', label: '用户与权限', icon: '👥' }
]
const active = ref('attractions')

const attractions = ref([])
const reviews = ref([])
const users = ref([])
const msg = ref('')
const msgType = ref('ok')

const editingId = ref(null)
const form = ref(blankForm())
const resetUser = ref(null)
const newPwd = ref('')

function blankForm() {
  return { name: '', emoji: '🎡', description: '', tag: '亲子', waitMinutes: 0, color: '#2f7d4f' }
}

function authHeaders() {
  return {
    'Content-Type': 'application/json',
    Authorization: 'Bearer ' + props.token
  }
}

function flash(text, type = 'ok') {
  msg.value = text
  msgType.value = type
  setTimeout(() => (msg.value = ''), 3000)
}

/** 统一请求：非 2xx 时抛出后端返回的 message */
async function adminFetch(url, options = {}) {
  const res = await fetch(url, {
    ...options,
    headers: { ...authHeaders(), ...(options.headers || {}) }
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    const err = new Error(data.message || '操作失败')
    err.status = res.status
    throw err
  }
  return data
}

async function loadAttractions() {
  const d = await adminFetch('/api/admin/attractions')
  attractions.value = d.data || []
}
async function loadReviews() {
  const d = await adminFetch('/api/admin/reviews')
  reviews.value = d.data || []
}
async function loadUsers() {
  const d = await adminFetch('/api/admin/users')
  users.value = d.data || []
}

async function loadAll() {
  try {
    await Promise.all([loadAttractions(), loadReviews(), loadUsers()])
    flash('数据已刷新')
  } catch (e) {
    flash(e.message, 'err')
  }
}

function startEdit(a) {
  editingId.value = a.id
  form.value = {
    name: a.name,
    emoji: a.emoji,
    description: a.description || '',
    tag: a.tag,
    waitMinutes: a.waitMinutes,
    color: a.color
  }
}
function resetForm() {
  editingId.value = null
  form.value = blankForm()
}

async function saveAttraction() {
  if (!form.value.name || !form.value.name.trim()) {
    flash('项目名称不能为空', 'err')
    return
  }
  try {
    if (editingId.value) {
      await adminFetch('/api/admin/attractions/' + editingId.value, {
        method: 'PUT',
        body: JSON.stringify(form.value)
      })
      flash('项目已更新')
    } else {
      await adminFetch('/api/admin/attractions', {
        method: 'POST',
        body: JSON.stringify(form.value)
      })
      flash('项目已新增')
    }
    resetForm()
    await loadAttractions()
  } catch (e) {
    flash(e.message, 'err')
  }
}

async function removeAttraction(a) {
  if (!confirm(`确定删除项目「${a.name}」吗？该项目下的评价也会一并删除。`)) return
  try {
    await adminFetch('/api/admin/attractions/' + a.id, { method: 'DELETE' })
    flash('项目已删除')
    await loadAttractions()
    await loadReviews()
  } catch (e) {
    flash(e.message, 'err')
  }
}

async function removeReview(r) {
  if (!confirm(`确定删除「${r.name}」的评价吗？`)) return
  try {
    await adminFetch('/api/admin/reviews/' + r.id, { method: 'DELETE' })
    flash('评价已删除')
    await loadReviews()
  } catch (e) {
    flash(e.message, 'err')
  }
}

async function togglePermission(u) {
  const next = u.permission === 0 ? 1 : 0
  const tip = next === 0 ? '设为管理员（可登录后台）' : '降为默认权限（无法登录后台）'
  if (!confirm(`确定把「${u.name}」${tip}吗？`)) return
  try {
    await adminFetch('/api/admin/users/' + u.id, {
      method: 'PUT',
      body: JSON.stringify({ permission: next })
    })
    flash('权限已更新')
    await loadUsers()
  } catch (e) {
    flash(e.message, 'err')
  }
}

function openReset(u) {
  resetUser.value = u
  newPwd.value = ''
}
async function doReset() {
  if (!newPwd.value || newPwd.value.length < 6) {
    flash('新密码至少 6 位', 'err')
    return
  }
  try {
    await adminFetch('/api/admin/users/' + resetUser.value.id, {
      method: 'PUT',
      body: JSON.stringify({ password: newPwd.value })
    })
    flash('密码已重置（明文保存）')
    resetUser.value = null
  } catch (e) {
    flash(e.message, 'err')
  }
}

onMounted(loadAll)
</script>

<style scoped>
.admin {
  min-height: 100vh;
  background: #f4f7f5;
  padding-bottom: 40px;
}

.ad-nav {
  background: #2f7d4f;
  color: #fff;
}
.ad-nav-inner {
  max-width: 1080px;
  margin: 0 auto;
  padding: 14px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.ad-logo {
  font-size: 18px;
  font-weight: 800;
}
.ad-user {
  display: flex;
  align-items: center;
  gap: 12px;
}
.ad-who {
  font-size: 13px;
  opacity: 0.9;
}
.ad-exit {
  border: 2px solid #fff;
  background: transparent;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  padding: 7px 14px;
  border-radius: 9px;
  cursor: pointer;
}
.ad-exit:hover {
  background: #fff;
  color: #2f7d4f;
}

.ad-tabs {
  max-width: 1080px;
  margin: 0 auto;
  padding: 18px 24px 0;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}
.ad-tab {
  border: 2px solid #d9e2dc;
  background: #fff;
  color: #2f4a38;
  font-size: 13px;
  font-weight: 700;
  padding: 9px 16px;
  border-radius: 10px;
  cursor: pointer;
}
.ad-tab:hover {
  border-color: #2f7d4f;
}
.ad-tab.active {
  background: #2f7d4f;
  border-color: #2f7d4f;
  color: #fff;
}
.ad-refresh {
  margin-left: auto;
  border: 2px solid #d9e2dc;
  background: #fff;
  color: #2f4a38;
  font-size: 13px;
  font-weight: 700;
  padding: 9px 16px;
  border-radius: 10px;
  cursor: pointer;
}

.ad-body {
  max-width: 1080px;
  margin: 0 auto;
  padding: 18px 24px 0;
}

.ad-msg {
  margin: 0 0 14px;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  background: #eaf6ee;
  color: #24603d;
  border: 1px solid #bfe0c9;
}
.ad-msg.err {
  background: #fdecea;
  color: #c0392b;
  border-color: #f5c6c2;
}

.ad-panel {
  background: #fff;
  border: 1px solid #e3e9e5;
  border-radius: 14px;
  padding: 18px 20px;
  margin-bottom: 18px;
  box-shadow: 0 6px 18px rgba(47, 125, 79, 0.06);
}
.ad-h2 {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 800;
  color: #1f5d3a;
}
.ad-hint {
  margin: -6px 0 14px;
  font-size: 12px;
  color: #7b8b81;
  line-height: 1.6;
}

.ad-form {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 10px;
}
.ad-input {
  border: 2px solid #e3e9e5;
  border-radius: 9px;
  padding: 10px 12px;
  font-size: 14px;
  outline: none;
  font-family: inherit;
}
.ad-input:focus {
  border-color: #2f7d4f;
}
.ad-input-sm {
  min-width: 120px;
}
.ad-textarea {
  grid-column: 1 / -1;
  min-height: 78px;
  resize: vertical;
}
.ad-form-actions {
  grid-column: 1 / -1;
  display: flex;
  gap: 10px;
}

.ad-btn {
  border: 2px solid #cfdad3;
  background: #fff;
  color: #2f4a38;
  font-size: 13px;
  font-weight: 700;
  padding: 9px 16px;
  border-radius: 9px;
  cursor: pointer;
}
.ad-btn:hover {
  border-color: #2f7d4f;
}
.ad-btn.primary {
  background: #2f7d4f;
  border-color: #2f7d4f;
  color: #fff;
}
.ad-btn.primary:hover {
  background: #24603d;
}
.ad-btn.danger {
  color: #c0392b;
  border-color: #f0c9c4;
}
.ad-btn.danger:hover {
  background: #c0392b;
  border-color: #c0392b;
  color: #fff;
}
.ad-btn.sm {
  padding: 6px 12px;
  font-size: 12px;
}

.ad-empty {
  font-size: 13px;
  color: #93a49a;
  padding: 18px 0;
  text-align: center;
}

.ad-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.ad-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  border: 1px solid #e9efeb;
  border-radius: 12px;
  padding: 12px 14px;
  background: #fbfdfc;
}
.ad-item-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.ad-emoji {
  width: 40px;
  height: 40px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  border-radius: 11px;
  background: #eaf6ee;
}
.ad-item-text {
  min-width: 0;
}
.ad-item-title {
  font-size: 14px;
  font-weight: 800;
  color: #24402f;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.ad-item-desc {
  font-size: 12px;
  color: #7b8b81;
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}
.ad-tag,
.ad-wait {
  font-size: 11px;
  font-weight: 600;
  color: #5c7365;
  background: #eef4f0;
  border-radius: 6px;
  padding: 2px 7px;
}
.ad-perm {
  font-size: 11px;
  font-weight: 800;
  border-radius: 6px;
  padding: 2px 8px;
  background: #eef4f0;
  color: #5c7365;
}
.ad-perm.admin {
  background: #fff3d6;
  color: #a9743c;
}
.ad-item-ops {
  display: flex;
  gap: 8px;
  flex: none;
}

.ad-mask {
  position: fixed;
  inset: 0;
  background: rgba(20, 40, 28, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  z-index: 50;
}
.ad-modal {
  width: 100%;
  max-width: 380px;
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.25);
}
.ad-modal .ad-input {
  width: 100%;
  box-sizing: border-box;
  margin-bottom: 12px;
}
</style>
