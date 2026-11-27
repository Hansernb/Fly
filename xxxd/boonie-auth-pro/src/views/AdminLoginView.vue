<template>
  <AdminLogin @success="onAdminLogin" @back="goHome" />
</template>

<script setup>
import AdminLogin from '../components/AdminLogin.vue'
import { useNav } from './useNav'

const { goHome, onAdminLogin } = useNav()
</script>
