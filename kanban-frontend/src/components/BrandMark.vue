<template>
  <span class="brand-lockup" :class="{ compact }">
    <svg class="brand-symbol" viewBox="0 0 32 32" aria-hidden="true">
      <rect x="3" y="4" width="7" height="24" rx="3" fill="currentColor" />
      <rect x="12" y="4" width="17" height="8" rx="3" fill="currentColor" />
      <rect x="12" y="15" width="12" height="7" rx="3" fill="currentColor" opacity=".72" />
    </svg>
    <strong v-if="!compact">SmartPM</strong>
  </span>
</template>

<script setup>
defineProps({ compact: { type: Boolean, default: false } })
</script>

<style scoped>
.brand-lockup { display: inline-flex; align-items: center; gap: 10px; min-width: 0; color: var(--brand); }
.brand-symbol { width: 28px; height: 28px; flex: 0 0 auto; }
strong { color: var(--text-primary); font-size: 17px; letter-spacing: -.02em; }
.compact .brand-symbol { width: 26px; height: 26px; }
</style>
