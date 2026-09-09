<template>
  <header class="page-header-component">
    <div class="page-header-copy"><slot name="breadcrumb" /><span v-if="eyebrow" class="eyebrow">{{ eyebrow }}</span><h1>{{ title }}</h1><p v-if="description">{{ description }}</p></div>
    <div v-if="$slots.actions" class="page-header-actions"><slot name="actions" /></div>
  </header>
</template>
<script setup>
defineProps({ title: { type: String, required: true }, description: { type: String, default: '' }, eyebrow: { type: String, default: '' } })
</script>
<style scoped>
.page-header-component { display: flex; align-items: flex-end; justify-content: space-between; gap: 28px; margin-bottom: 26px; }
.page-header-copy { min-width: 0; }
.eyebrow { display: block; margin-bottom: 6px; color: var(--brand); font-size: 11px; font-weight: 700; letter-spacing: .04em; }
h1 { margin: 0; color: var(--text-primary); font-size: clamp(25px, 2.5vw, 34px); line-height: 1.18; letter-spacing: -.035em; }
p { margin: 8px 0 0; color: var(--text-tertiary); font-size: 14px; }
.page-header-actions { display: flex; align-items: center; justify-content: flex-end; gap: 10px; flex-wrap: wrap; }
@media (max-width: 640px) {
  .page-header-component { align-items: flex-start; flex-direction: column; gap: 18px; margin-bottom: 22px; }
  .page-header-actions { width: 100%; justify-content: flex-start; }
  .page-header-actions :deep(.el-button) { margin-left: 0; }
}
</style>
