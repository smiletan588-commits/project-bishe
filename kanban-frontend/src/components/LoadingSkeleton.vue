<template><div class="loading-skeleton" role="status" aria-label="正在加载"><span v-for="index in rows" :key="index" :style="{ width: `${96 - (index % 3) * 12}%` }" /></div></template>
<script setup>
defineProps({ rows: { type: Number, default: 4 } })
</script>
<style scoped>
.loading-skeleton { display: grid; gap: 13px; width: 100%; padding: 20px 0; }
.loading-skeleton span { display: block; height: 44px; border-radius: 8px; background: linear-gradient(90deg, var(--surface-strong) 20%, var(--surface) 45%, var(--surface-strong) 70%); background-size: 220% 100%; }
@media (prefers-reduced-motion: no-preference) { .loading-skeleton span { animation: skeleton-shift 1.4s ease infinite; } }
@keyframes skeleton-shift { to { background-position: -220% 0; } }
</style>
