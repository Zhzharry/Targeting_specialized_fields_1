<template>
  <div class="section-header">
    <div class="texts">
      <h2 class="title"><slot /></h2>
      <p v-if="subtitle" class="subtitle">{{ subtitle }}</p>
    </div>
    <div class="actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props { subtitle?: string }
const props = defineProps<Props>()
</script>

<style scoped>
.section-header { display: flex; align-items: center; justify-content: space-between; padding: 4px 2px 12px; }
.title { margin: 0; font-size: 18px; font-weight: 700; color: #111827; }
.subtitle { margin: 4px 0 0; color: #6b7280; font-size: 13px; }
.actions { display: flex; align-items: center; gap: 8px; }
</style>
