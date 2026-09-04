<template>
  <div class="admin-page">
    <header class="topbar">
      <div class="brand-wrap"><span class="brand-mark">S</span><strong>SmartPM</strong><span class="section-name">系统管理</span></div>
      <div class="header-actions"><button class="back-btn" @click="router.push('/dashboard')">返回项目</button><span class="admin-chip">系统管理员</span></div>
    </header>

    <main class="content">
      <section class="page-heading">
        <div><p class="eyebrow">ACCOUNT DIRECTORY</p><h1>用户与访问权限</h1><p>统一管理平台账号、系统管理员权限与登录状态。</p></div>
        <el-button type="primary" :loading="loading" @click="fetchUsers">刷新用户列表</el-button>
      </section>

      <section class="admin-notice"><span>管理员提示</span><p>为保护项目归属记录，用户账号不提供直接删除；可停用账号、修改权限或重置密码。</p></section>

      <section class="user-panel" v-loading="loading">
        <div class="panel-title"><span>全部用户</span><small>{{ users.length }} 个账号</small></div>
        <el-table :data="users" class="user-table" empty-text="暂无用户数据">
          <el-table-column label="用户" min-width="190">
            <template #default="{ row }"><div class="user-name"><span class="avatar">{{ (row.nickname || row.username)?.slice(0, 1).toUpperCase() }}</span><div><strong>{{ row.nickname || row.username }}</strong><small>@{{ row.username }}</small></div></div></template>
          </el-table-column>
          <el-table-column label="专业身份" min-width="125"><template #default="{ row }"><span class="muted">{{ identityLabel(row.identity) }}</span></template></el-table-column>
          <el-table-column label="系统权限" width="145"><template #default="{ row }"><el-select :model-value="row.systemRole" size="small" :disabled="pendingId === row.id" @change="value => saveRole(row, value)"><el-option label="普通用户" value="USER" /><el-option label="系统管理员" value="ADMIN" /></el-select></template></el-table-column>
          <el-table-column label="账号状态" width="132"><template #default="{ row }"><el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" effect="plain">{{ row.status === 'ACTIVE' ? '正常' : '已停用' }}</el-tag></template></el-table-column>
          <el-table-column label="注册时间" width="124"><template #default="{ row }"><span class="muted">{{ formatDate(row.createdAt) }}</span></template></el-table-column>
          <el-table-column label="操作" width="210" fixed="right"><template #default="{ row }"><el-button text type="primary" :disabled="pendingId === row.id" @click="openPasswordDialog(row)">重置密码</el-button><el-button text :type="row.status === 'ACTIVE' ? 'danger' : 'success'" :disabled="pendingId === row.id" @click="toggleStatus(row)">{{ row.status === 'ACTIVE' ? '停用' : '启用' }}</el-button></template></el-table-column>
        </el-table>
      </section>
    </main>

    <el-dialog v-model="passwordDialogVisible" width="390px" title="重置用户密码" :close-on-click-modal="false">
      <p class="dialog-hint">为 <strong>{{ passwordTarget?.nickname || passwordTarget?.username }}</strong> 设置新密码。</p>
      <el-input v-model="newPassword" type="password" show-password placeholder="至少 3 位" @keyup.enter="savePassword" />
      <template #footer><el-button @click="passwordDialogVisible = false">取消</el-button><el-button type="primary" :loading="savingPassword" @click="savePassword">保存新密码</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { listAdminUsers, resetAdminUserPassword, updateAdminUserRole, updateAdminUserStatus } from '@/api/admin'

const router = useRouter()
const users = ref([])
const loading = ref(false)
const pendingId = ref(null)
const passwordDialogVisible = ref(false)
const passwordTarget = ref(null)
const newPassword = ref('')
const savingPassword = ref(false)

const identities = { PROJECT_MANAGER: '项目经理', FRONTEND_DEV: '前端工程师', BACKEND_DEV: '后端工程师', QA_TESTER: '测试工程师', UI_DESIGNER: 'UI 设计师' }
const identityLabel = value => identities[value] || '未设置'
const formatDate = value => value ? String(value).slice(0, 10) : '—'

async function fetchUsers() {
  loading.value = true
  try { const res = await listAdminUsers(); users.value = res.data?.data || [] } finally { loading.value = false }
}

async function saveRole(row, systemRole) {
  pendingId.value = row.id
  try { await updateAdminUserRole(row.id, systemRole); ElMessage.success('系统权限已更新'); await fetchUsers() } finally { pendingId.value = null }
}

async function toggleStatus(row) {
  const status = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const action = status === 'ACTIVE' ? '启用' : '停用'
  try { await ElMessageBox.confirm(`确定${action}账号「${row.username}」吗？`, `${action}账号`, { type: 'warning' }) } catch { return }
  pendingId.value = row.id
  try { await updateAdminUserStatus(row.id, status); ElMessage.success(`账号已${action}`); await fetchUsers() } finally { pendingId.value = null }
}

function openPasswordDialog(row) { passwordTarget.value = row; newPassword.value = ''; passwordDialogVisible.value = true }
async function savePassword() {
  if (newPassword.value.length < 3) { ElMessage.warning('新密码至少需要 3 位'); return }
  savingPassword.value = true
  try { await resetAdminUserPassword(passwordTarget.value.id, newPassword.value); ElMessage.success('密码已重置'); passwordDialogVisible.value = false } finally { savingPassword.value = false }
}

onMounted(fetchUsers)
</script>

<style scoped>
.admin-page { min-height: 100vh; background: #171716; color: #f5f0e8; }
.topbar { height: 58px; padding: 0 28px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #3b3731; background: #242321; }
.brand-wrap,.header-actions { display: flex; align-items: center; gap: 10px; }.brand-mark { display: grid; place-items: center; width: 27px; height: 27px; border-radius: 8px; background: linear-gradient(135deg,#e2a43a,#a95d12); color: #211a12; font-weight: 800; }.section-name { margin-left: 5px; padding-left: 14px; border-left: 1px solid #4a443b; color: #b9b1a5; font-size: 13px; }.back-btn { border: 0; background: transparent; color: #d4cabb; cursor: pointer; font-size: 13px; }.back-btn:hover { color: #e2a43a; }.admin-chip { border: 1px solid rgba(226,164,58,.45); color: #e2a43a; padding: 4px 9px; border-radius: 999px; font-size: 12px; }
.content { max-width: 1240px; margin: 0 auto; padding: 54px 30px; }.page-heading { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 28px; }.eyebrow { margin: 0 0 8px; color: #e2a43a; letter-spacing: .14em; font-size: 10px; font-weight: 700; }.page-heading h1 { margin: 0; font-size: 32px; letter-spacing: -.04em; }.page-heading p:not(.eyebrow) { margin: 8px 0 0; color: #aaa398; font-size: 14px; }.page-heading :deep(.el-button--primary) { background: #d58a22; border: none; color: #20190f; font-weight: 700; }
.admin-notice { display: flex; align-items: baseline; gap: 15px; padding: 14px 18px; margin-bottom: 20px; border: 1px solid #4b402b; border-left: 3px solid #d58a22; background: #26231e; border-radius: 8px; }.admin-notice span { color: #e2a43a; font-weight: 700; font-size: 13px; white-space: nowrap; }.admin-notice p { margin: 0; color: #bbb3a7; font-size: 13px; }
.user-panel { overflow: hidden; border: 1px solid #403c35; border-radius: 12px; background: #242321; }.panel-title { display: flex; justify-content: space-between; padding: 17px 20px; border-bottom: 1px solid #403c35; font-weight: 700; }.panel-title small { color: #a59c8f; font-weight: 400; }.user-table { --el-table-bg-color: #242321; --el-table-tr-bg-color: #242321; --el-table-header-bg-color: #2c2a27; --el-table-row-hover-bg-color: #2c2a27; --el-table-text-color: #e8e2d8; --el-table-header-text-color: #aba295; --el-table-border-color: #403c35; }.user-name { display: flex; align-items: center; gap: 10px; }.avatar { display: grid; place-items: center; width: 30px; height: 30px; border-radius: 8px; background: #4b3823; color: #e2a43a; font-size: 12px; font-weight: 700; }.user-name strong,.user-name small { display: block; }.user-name small,.muted { color: #a59c8f; font-size: 12px; }.dialog-hint { color: #666; margin: 0 0 14px; }
@media (max-width: 720px) { .topbar,.content { padding-left: 16px; padding-right: 16px; }.page-heading { align-items: flex-start; gap: 16px; flex-direction: column; }.section-name { display: none; }.admin-notice { align-items: flex-start; flex-direction: column; gap: 5px; } }
</style>
