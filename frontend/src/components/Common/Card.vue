<template>
  <div :class="['card', { hoverable, padded }]">
    <slot />
  </div>
</template>

<script setup lang="ts">
interface Props {
  hoverable?: boolean
  padded?: boolean
}
const props = withDefaults(defineProps<Props>(), {
  hoverable: false,
  padded: false,
})
</script>

<style scoped>
.card { background: #fff; border: 1px solid #e5e7eb; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
.card.padded { padding: 16px; }
.card.hoverable { transition: transform .15s ease, box-shadow .15s ease; }
.card.hoverable:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(0,0,0,0.1); }
</style>
