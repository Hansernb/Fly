<template>
  <MemberCenter
    :token="store.token"
    :user="store.me"
    @back="goHome"
    @tickets="enterTickets"
  />
</template>

<script setup>
import MemberCenter from '../components/MemberCenter.vue'
import { useSessionStore } from '../stores/session'
import { useNav } from './useNav'

const store = useSessionStore()
const { goHome, enterTickets } = useNav()
</script>
