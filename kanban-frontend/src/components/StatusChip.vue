<template><span class="status-chip" :class="`is-${tone}`"><slot>{{ label }}</slot></span></template>
<script setup>
defineProps({ label: { type: String, default: '' }, tone: { type: String, default: 'neutral' } })
</script>
<style scoped>
.status-chip { display: inline-flex; align-items: center; min-height: 24px; padding: 2px 9px; border-radius: 999px; color: var(--text-secondary); background: var(--surface-strong); font-size: 11px; font-weight: 650; }
.is-todo,.is-warning { color: var(--todo); background: var(--todo-bg); }
.is-progress,.is-primary { color: var(--progress); background: var(--progress-bg); }
.is-done,.is-success { color: var(--done); background: var(--done-bg); }
.is-danger { color: var(--danger); background: var(--danger-bg); }
</style>
