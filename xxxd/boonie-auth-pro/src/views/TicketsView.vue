<template>
  <TicketBooking
    :user="store.me"
    :token="store.token"
    :initial-ticket-id="ticketId"
    @back="goHome"
    @member="go('/member')"
  />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import TicketBooking from '../components/TicketBooking.vue'
import { useSessionStore } from '../stores/session'
import { useNav } from './useNav'

const route = useRoute()
const store = useSessionStore()
const { me } = storeToRefs(store)
const { go, goHome } = useNav()

// 原 App.vue 的 presetTicketId：从 ParkHome emit('tickets', id) 写入，
// 重构后由首页 → query.ticketId 传入，组件契约不变。
const ticketId = computed(() => (typeof route.query.ticketId === 'string' ? route.query.ticketId : ''))
</script>
