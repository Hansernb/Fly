<template>
  <AdminPanel
    :user="store.adminSession?.user || store.me"
    :token="store.adminToken"
    @exit="exitAdmin"
  />
</template>

<script setup>
import AdminPanel from '../components/AdminPanel.vue'
import { useSessionStore } from '../stores/session'
import { useNav } from './useNav'

const store = useSessionStore()
const { exitAdmin } = useNav()
</script>
