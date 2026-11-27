<template>
  <ParkMap @back="goHome" />
</template>

<script setup>
import ParkMap from '../components/ParkMap.vue'
import { useNav } from './useNav'

const { goHome } = useNav()
</script>
