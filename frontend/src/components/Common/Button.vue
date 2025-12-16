<template>
  <button :class="['btn', `btn-${variant}`, `btn-${size}`]" :disabled="disabled" v-bind="$attrs">
    <slot />
  </button>
</template>

<script setup lang="ts">
interface Props {
  variant?: 'primary' | 'secondary' | 'ghost'
  size?: 'sm' | 'md' | 'lg'
  disabled?: boolean
}
const props = withDefaults(defineProps<Props>(), {
  variant: 'primary',
  size: 'md',
  disabled: false,
})
</script>

<style scoped>
.btn { border: none; border-radius: 10px; padding: 10px 14px; cursor: pointer; transition: all .2s ease; }
.btn:disabled { opacity: .5; cursor: not-allowed; }
.btn-primary { background: linear-gradient(135deg, #2563eb, #1d4ed8); color: #fff; box-shadow: 0 6px 16px rgba(29,78,216,0.25); }
.btn-primary:hover { filter: brightness(1.06); }
.btn-secondary { background: #f3f4f6; color: #111827; border: 1px solid #e5e7eb; }
.btn-secondary:hover { background: #e5e7eb; }
.btn-ghost { background: transparent; color: #2563eb; }
.btn-sm { padding: 8px 10px; font-size: 13px; }
.btn-md { padding: 10px 14px; font-size: 14px; }
.btn-lg { padding: 12px 18px; font-size: 16px; }
</style>
