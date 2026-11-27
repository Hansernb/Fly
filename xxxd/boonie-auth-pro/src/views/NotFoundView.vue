<template>
  <NotFound @home="go('/')" @attractions="go('/attractions')" />
</template>

<script setup>
import NotFound from '../components/NotFound.vue'
import { useNav } from './useNav'

const { go } = useNav()
</script>
