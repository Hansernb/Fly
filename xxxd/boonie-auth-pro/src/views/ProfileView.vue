<template>
  <PersonalCenter
    :user="store.me"
    @back="goHome"
    @attraction="openAttractionDetail"
    @member="enterMember"
  />
</template>

<script setup>
import PersonalCenter from '../components/PersonalCenter.vue'
import { useSessionStore } from '../stores/session'
import { useNav } from './useNav'

const store = useSessionStore()
const { goHome, openAttractionDetail, enterMember } = useNav()
</script>
