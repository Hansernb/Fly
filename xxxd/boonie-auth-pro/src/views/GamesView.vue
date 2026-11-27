<template>
  <ForestGames :token="store.token" :user="store.me" @back="goHome" />
</template>

<script setup>
import ForestGames from '../components/ForestGames.vue'
import { useSessionStore } from '../stores/session'
import { useNav } from './useNav'

const store = useSessionStore()
const { goHome } = useNav()
</script>
