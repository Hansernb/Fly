<template>
  <div class="admin-login">
    <div class="al-card">
      <div class="al-head">
        <div class="al-badge">⚙️</div>
        <h1 class="al-title">后台管理系统</h1>
        <p class="al-sub">仅管理员可登录 · 请使用管理员账号</p>
      </div>

      <form class="al-form" @submit.prevent="submit">
        <label class="al-field">
          <span class="al-label">管理员用户名</span>
          <input
            v-model.trim="username"
            class="al-input"
            type="text"
            placeholder="请输入管理员用户名"
            autocomplete="username"
            required
          />
        </label>

        <label class="al-field">
          <span class="al-label">密码</span>
          <input
            v-model="password"
            class="al-input"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
            required
          />
        </label>

        <p v-if="error" class="al-error">⚠️ {{ error }}</p>

        <button class="al-submit" type="submit" :disabled="loading">
          {{ loading ? '登录中…' : '登录后台' }}
        </button>
      </form>

      <button class="al-back" type="button" @click="$emit('back')">← 返回游乐园首页</button>

      <p class="al-tip">提示：普通用户（默认权限）无法登录后台，请联系管理员开通权限。</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['success', 'back'])

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  if (!username.value || !password.value) {
    error.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  try {
    const res = await fetch('/api/admin/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: username.value, password: password.value })
    })
    const data = await res.json().catch(() => ({}))

    if (!res.ok) {
      // 后端 BizException 会带 message 字段
      error.value = data.message || '登录失败，请检查用户名和密码'
      return
    }
    // 登录成功：把令牌存起来，并进入后台
    localStorage.setItem('boonie_token', data.token)
    localStorage.setItem('boonie_admin_token', data.token)
    emit('success', { user: data.user, token: data.token })
  } catch (e) {
    error.value = '网络异常，请确认后端已启动'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.admin-login {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(160deg, #eaf6ee 0%, #d8ecdf 55%, #cfe6d6 100%);
}

.al-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border: 3px solid #d9c7a7;
  border-radius: 18px;
  padding: 32px 30px 24px;
  box-shadow: 0 18px 44px rgba(47, 125, 79, 0.18);
}

.al-head {
  text-align: center;
  margin-bottom: 24px;
}

.al-badge {
  width: 58px;
  height: 58px;
  margin: 0 auto 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  border-radius: 16px;
  background: #eaf6ee;
  border: 2px solid #2f7d4f;
}

.al-title {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 800;
  color: #1f5d3a;
}

.al-sub {
  margin: 0;
  font-size: 13px;
  color: #6b7f72;
}

.al-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.al-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.al-label {
  font-size: 13px;
  font-weight: 700;
  color: #2f4a38;
}

.al-input {
  border: 2px solid #e3d9c6;
  border-radius: 10px;
  padding: 11px 14px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.15s ease;
}
.al-input:focus {
  border-color: #2f7d4f;
}

.al-error {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: #c0392b;
  background: #fdecea;
  border: 1px solid #f5c6c2;
  border-radius: 8px;
  padding: 8px 12px;
}

.al-submit {
  margin-top: 4px;
  border: none;
  border-radius: 10px;
  padding: 12px;
  font-size: 15px;
  font-weight: 800;
  color: #fff;
  background: #2f7d4f;
  cursor: pointer;
  transition: background 0.15s ease;
}
.al-submit:hover:not(:disabled) {
  background: #24603d;
}
.al-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.al-back {
  margin-top: 18px;
  width: 100%;
  border: 2px solid #d9c7a7;
  background: #fff;
  color: #2f4a38;
  font-size: 13px;
  font-weight: 700;
  padding: 9px;
  border-radius: 10px;
  cursor: pointer;
}
.al-back:hover {
  background: #f6f1e7;
}

.al-tip {
  margin: 14px 0 0;
  font-size: 12px;
  color: #8a9a90;
  text-align: center;
  line-height: 1.6;
}
</style>
