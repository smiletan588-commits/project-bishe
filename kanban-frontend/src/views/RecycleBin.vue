<template>
  <div class="recycle-page">
    <header class="topbar"><div class="brand"><span class="brand-mark">S</span><strong>SmartPM</strong><span>回收站</span></div><button @click="router.push('/dashboard')">返回项目</button></header>
    <main class="content">
      <section class="heading"><div><p>RECOVERY DESK</p><h1>找回误删内容</h1><small>删除的数据会保留在这里。恢复后将回到原来的项目和位置。</small></div><el-button :loading="loading" @click="fetchItems">刷新</el-button></section>
      <section class="notice"><strong>恢复规则</strong><span>项目管理员可恢复本项目数据；系统管理员可永久删除。永久删除后无法撤销。</span></section>
      <section class="bin" v-loading="loading">
        <div class="bin-head"><strong>已删除项目、任务与文档</strong><small>{{ items.length }} 项</small></div>
        <el-table :data="items" class="bin-table" empty-text="回收站为空">
          <el-table-column label="内容" min-width="210"><template #default="{ row }"><div class="item-title"><span :class="['type', row.type.toLowerCase()]">{{ typeLabel(row.type) }}</span><strong>{{ row.title }}</strong></div></template></el-table-column>
          <el-table-column label="所属项目" min-width="160"><template #default="{ row }"><span class="muted">{{ row.projectName }}</span></template></el-table-column>
          <el-table-column label="删除人" width="120"><template #default="{ row }"><span class="muted">{{ row.deletedByName }}</span></template></el-table-column>
          <el-table-column label="删除时间" width="165"><template #default="{ row }"><span class="muted">{{ formatTime(row.deletedAt) }}</span></template></el-table-column>
          <el-table-column label="操作" width="210" fixed="right"><template #default="{ row }"><el-button text type="primary" :loading="busy === keyOf(row)" @click="restore(row)">恢复</el-button><el-button v-if="userStore.systemRole === 'ADMIN'" text type="danger" :loading="busy === keyOf(row)" @click="permanentlyDelete(row)">永久删除</el-button></template></el-table-column>
        </el-table>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { listRecycleBin, permanentlyDeleteRecycleItem, restoreRecycleItem } from '@/api/recycleBin'

const router = useRouter(); const userStore = useUserStore(); const items = ref([]); const loading = ref(false); const busy = ref('')
const labels = { PROJECT: '项目', TASK: '任务', WIKI: '文档', ATTACHMENT: '附件' }
const typeLabel = type => labels[type] || type
const keyOf = row => `${row.type}-${row.id}`
const formatTime = value => value ? String(value).replace('T', ' ').slice(0, 16) : '—'
async function fetchItems() { loading.value = true; try { items.value = (await listRecycleBin()).data.data || [] } finally { loading.value = false } }
async function restore(row) { busy.value = keyOf(row); try { await restoreRecycleItem(row.type, row.id); ElMessage.success('已恢复到原项目'); await fetchItems() } finally { busy.value = '' } }
async function permanentlyDelete(row) { try { await ElMessageBox.confirm(`永久删除「${row.title}」后将无法恢复，确定继续吗？`, '永久删除', { type: 'error', confirmButtonText: '永久删除', cancelButtonText: '取消' }) } catch { return }; busy.value = keyOf(row); try { await permanentlyDeleteRecycleItem(row.type, row.id); ElMessage.success('已永久删除'); await fetchItems() } finally { busy.value = '' } }
onMounted(fetchItems)
</script>

<style scoped>
.recycle-page { min-height: 100vh; background: #171716; color: #f5f0e8; }.topbar { height: 58px; padding: 0 28px; display:flex; align-items:center; justify-content:space-between; background:#242321; border-bottom:1px solid #3a3732; }.topbar button { color:#cfc6b7; border:0; background:transparent; cursor:pointer; font-size:13px; }.topbar button:hover { color:#e2a43a; }.brand { display:flex; align-items:center; gap:10px; }.brand > span:last-child { padding-left:12px; border-left:1px solid #4a443b; color:#b9b1a5; font-size:13px; }.brand-mark { display:grid; place-items:center; width:27px; height:27px; border-radius:8px; color:#211a12; background:linear-gradient(135deg,#e2a43a,#a95d12); font-weight:800; }.content { max-width:1200px; margin:0 auto; padding:54px 30px; }.heading { display:flex; justify-content:space-between; align-items:flex-end; margin-bottom:28px; }.heading p { margin:0 0 8px; font-size:10px; letter-spacing:.14em; color:#e2a43a; font-weight:700; }.heading h1 { margin:0; font-size:32px; letter-spacing:-.04em; }.heading small { display:block; margin-top:9px; color:#aaa398; font-size:14px; }.heading :deep(.el-button) { border-color:#5a554d; color:#e2a43a; background:transparent; }.notice { display:flex; gap:16px; padding:15px 18px; margin-bottom:20px; background:#26231e; border:1px solid #4b402b; border-left:3px solid #d58a22; border-radius:8px; font-size:13px; }.notice strong { color:#e2a43a; white-space:nowrap; }.notice span { color:#bcb3a6; }.bin { overflow:hidden; border:1px solid #403c35; border-radius:12px; background:#242321; }.bin-head { display:flex; justify-content:space-between; padding:17px 20px; border-bottom:1px solid #403c35; }.bin-head small,.muted { color:#a59c8f; font-size:12px; }.bin-table { --el-table-bg-color:#242321; --el-table-tr-bg-color:#242321; --el-table-header-bg-color:#2c2a27; --el-table-row-hover-bg-color:#2c2a27; --el-table-text-color:#e8e2d8; --el-table-header-text-color:#aba295; --el-table-border-color:#403c35; }.item-title { display:flex; align-items:center; gap:9px; }.type { padding:3px 7px; border-radius:4px; font-size:10px; font-weight:700; background:#403c35; color:#d0c8bc; }.type.project { background:#4b3823; color:#e2a43a; }.type.task { background:#283d46; color:#8dd3ec; }.type.wiki { background:#3b354b; color:#c5b0ee; }.type.attachment { background:#354332; color:#a9d99b; }@media (max-width:680px) { .topbar,.content { padding-left:16px; padding-right:16px; }.heading { align-items:flex-start; gap:16px; flex-direction:column; }.notice { flex-direction:column; gap:6px; } }
</style>
