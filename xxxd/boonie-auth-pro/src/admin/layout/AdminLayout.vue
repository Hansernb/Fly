<template>
  <div class="al" :class="{ collapsed: collapsed }">
    <!-- ============ 左侧菜单 ============ -->
    <aside class="al-side">
      <div class="al-brand">
        <span class="al-brand-ico">🐻</span>
        <div v-if="!collapsed" class="al-brand-txt">
          <strong>狗熊岭中后台</strong>
          <span>森林游乐园管理系统</span>
        </div>
      </div>

      <nav class="al-menu" aria-label="后台菜单">
        <button
          v-for="m in menus"
          :key="m.key"
          type="button"
          class="al-menu-item"
          :class="{ active: isActive(m.key) }"
          :title="m.name"
          @click="go(m.key)"
        >
          <span class="al-menu-ico">{{ m.icon }}</span>
          <span v-if="!collapsed" class="al-menu-label">{{ m.name }}</span>
        </button>
      </nav>

      <div class="al-side-foot">
        <button class="al-collapse" type="button" @click="collapsed = !collapsed">
          {{ collapsed ? '»' : '« 收起菜单' }}
        </button>
      </div>
    </aside>

    <!-- ============ 右侧：顶部栏 + 内容区 ============ -->
    <div class="al-body">
      <header class="al-topbar">
        <nav class="al-crumb" aria-label="面包屑">
          <span class="al-crumb-home">后台管理</span>
          <span class="al-crumb-sep">/</span>
          <span class="al-crumb-current">{{ current?.name || '—' }}</span>
        </nav>
        <div class="al-top-right">
          <img class="al-avatar" :src="avatar" alt="管理员头像" @error="onAvatarErr" />
          <div class="al-who">
            <strong>{{ user?.name || '管理员' }}</strong>
            <span>超级管理员</span>
          </div>
          <button class="al-exit" type="button" @click="exit">退出登录</button>
        </div>
      </header>

      <main class="al-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MODULES } from '../config/modules.js'
import { useSessionStore } from '../../stores/session'

const route = useRoute()
const router = useRouter()
const store = useSessionStore()

const menus = MODULES
const collapsed = ref(false)
const current = computed(() => MODULES.find((m) => m.key === route.params.module) || null)
const user = computed(() => store.adminSession?.user || store.me)
const avatar = computed(() => user.value?.avatar || '/img/avatar.jpg')

const DEFAULT_AVATAR = '/img/avatar.jpg'
function onAvatarErr(e) {
  e.target.src = DEFAULT_AVATAR
}

function isActive(key) {
  return route.params.module === key
}

function go(key) {
  router.push({ name: 'adminModule', params: { module: key } })
}

function exit() {
  store.exitAdmin()
  router.replace({ name: 'adminLogin' })
}
</script>

<style scoped>
.al {
  display: flex;
  min-height: 100vh;
  background: #f5f7f3;
}
/* ---------- 左侧 ---------- */
.al-side {
  width: 216px;
  flex: 0 0 216px;
  background: var(--forest-deep);
  color: #eaf3ea;
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease, flex-basis 0.2s ease;
}
.al.collapsed .al-side {
  width: 68px;
  flex-basis: 68px;
}
.al-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  min-height: 72px;
}
.al-brand-ico {
  font-size: 26px;
  flex: 0 0 auto;
}
.al-brand-txt strong {
  display: block;
  font-size: 14px;
  font-weight: 900;
  color: #fff;
}
.al-brand-txt span {
  font-size: 10px;
  color: rgba(234, 243, 234, 0.65);
}
.al-menu {
  flex: 1;
  padding: 12px 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
}
.al-menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 11px 12px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: rgba(234, 243, 234, 0.82);
  font-size: 13px;
  font-weight: 800;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: background 0.16s ease;
}
.al-menu-item:hover {
  background: rgba(255, 255, 255, 0.08);
}
.al-menu-item.active {
  background: var(--honey);
  color: #4a3208;
}
.al-menu-ico {
  font-size: 17px;
  flex: 0 0 auto;
  width: 22px;
  text-align: center;
}
.al.collapsed .al-menu-item {
  justify-content: center;
}
.al-side-foot {
  padding: 12px 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
}
.al-collapse {
  width: 100%;
  padding: 9px;
  border: none;
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(234, 243, 234, 0.85);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
/* ---------- 右侧 ---------- */
.al-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.al-topbar {
  height: 64px;
  flex: 0 0 64px;
  background: #fffdf7;
  border-bottom: 2px solid var(--wood-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 26px;
}
.al-crumb {
  font-size: 13px;
  font-weight: 800;
  color: var(--muted);
}
.al-crumb-home {
  color: var(--muted);
}
.al-crumb-sep {
  margin: 0 8px;
  color: var(--wood-light);
}
.al-crumb-current {
  color: var(--forest-deep);
}
.al-top-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.al-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 2px solid var(--wood-light);
  object-fit: cover;
}
.al-who strong {
  display: block;
  font-size: 12px;
  font-weight: 900;
  color: var(--text);
}
.al-who span {
  font-size: 10px;
  font-weight: 700;
  color: var(--muted);
}
.al-exit {
  margin-left: 8px;
  padding: 8px 16px;
  border: 2px solid var(--wood-light);
  border-radius: 999px;
  background: transparent;
  color: var(--text);
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
  transition: all 0.16s ease;
}
.al-exit:hover {
  border-color: #c74a1e;
  color: #c74a1e;
}
.al-content {
  flex: 1;
  padding: 22px 26px 32px;
  overflow-y: auto;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.16s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 760px) {
  .al-side {
    width: 62px;
    flex-basis: 62px;
  }
  .al-brand-txt,
  .al-menu-label {
    display: none;
  }
  .al-menu-item {
    justify-content: center;
  }
  .al-content {
    padding: 16px;
  }
}
</style>
