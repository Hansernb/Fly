<template>
  <AttractionList @back="goHome" />
</template>

<script setup>
import AttractionList from '../components/AttractionList.vue'
import { useNav } from './useNav'

const { goHome } = useNav()
</script>
