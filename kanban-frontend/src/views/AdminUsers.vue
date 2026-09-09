<template>
  <AppShell>
    <main class="content">
      <PageHeader eyebrow="系统管理" title="用户与访问权限" description="统一管理平台账号、系统管理员权限与登录状态。">
        <template #actions><el-button type="primary" :loading="loading" @click="fetchUsers">刷新用户列表</el-button></template>
      </PageHeader>

      <section class="admin-notice"><span>管理员提示</span><p>为保护项目归属记录，用户账号不提供直接删除；可停用账号、修改权限或重置密码。</p></section>

      <section class="user-panel" v-loading="loading">
        <div class="panel-title"><span>全部用户</span><small>{{ users.length }} 个账号</small></div>
        <el-table :data="users" class="user-table desktop-table" empty-text="暂无用户数据">
          <el-table-column label="用户" min-width="190">
            <template #default="{ row }"><div class="user-name"><span class="avatar">{{ (row.nickname || row.username)?.slice(0, 1).toUpperCase() }}</span><div><strong>{{ row.nickname || row.username }}</strong><small>@{{ row.username }}</small></div></div></template>
          </el-table-column>
          <el-table-column label="专业身份" min-width="125"><template #default="{ row }"><span class="muted">{{ identityLabel(row.identity) }}</span></template></el-table-column>
          <el-table-column label="系统权限" width="145"><template #default="{ row }"><el-select :model-value="row.systemRole" size="small" :disabled="pendingId === row.id" @change="value => saveRole(row, value)"><el-option label="普通用户" value="USER" /><el-option label="系统管理员" value="ADMIN" /></el-select></template></el-table-column>
          <el-table-column label="账号状态" width="132"><template #default="{ row }"><el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" effect="plain">{{ row.status === 'ACTIVE' ? '正常' : '已停用' }}</el-tag></template></el-table-column>
          <el-table-column label="注册时间" width="124"><template #default="{ row }"><span class="muted">{{ formatDate(row.createdAt) }}</span></template></el-table-column>
          <el-table-column label="操作" width="210" fixed="right"><template #default="{ row }"><el-button text type="primary" :disabled="pendingId === row.id" @click="openPasswordDialog(row)">重置密码</el-button><el-button text :type="row.status === 'ACTIVE' ? 'danger' : 'success'" :disabled="pendingId === row.id" @click="toggleStatus(row)">{{ row.status === 'ACTIVE' ? '停用' : '启用' }}</el-button></template></el-table-column>
        </el-table>
        <div class="mobile-records">
          <article v-for="user in users" :key="user.id" class="record-card">
            <div class="user-name"><span class="avatar">{{ (user.nickname || user.username)?.slice(0, 1).toUpperCase() }}</span><div><strong>{{ user.nickname || user.username }}</strong><small>@{{ user.username }}</small></div></div>
            <dl><div><dt>专业身份</dt><dd>{{ identityLabel(user.identity) }}</dd></div><div><dt>账号状态</dt><dd><el-tag :type="user.status === 'ACTIVE' ? 'success' : 'info'" effect="plain">{{ user.status === 'ACTIVE' ? '正常' : '已停用' }}</el-tag></dd></div><div><dt>注册时间</dt><dd>{{ formatDate(user.createdAt) }}</dd></div></dl>
            <el-select :model-value="user.systemRole" size="small" :disabled="pendingId === user.id" aria-label="系统权限" @change="value => saveRole(user, value)"><el-option label="普通用户" value="USER" /><el-option label="系统管理员" value="ADMIN" /></el-select>
            <div class="record-actions"><el-button text type="primary" :disabled="pendingId === user.id" @click="openPasswordDialog(user)">重置密码</el-button><el-button text :type="user.status === 'ACTIVE' ? 'danger' : 'success'" :disabled="pendingId === user.id" @click="toggleStatus(user)">{{ user.status === 'ACTIVE' ? '停用' : '启用' }}</el-button></div>
          </article>
        </div>
      </section>
    </main>

    <el-dialog v-model="passwordDialogVisible" width="390px" title="重置用户密码" :close-on-click-modal="false">
      <p class="dialog-hint">为 <strong>{{ passwordTarget?.nickname || passwordTarget?.username }}</strong> 设置新密码。</p>
      <el-input v-model="newPassword" type="password" show-password placeholder="至少 3 位" @keyup.enter="savePassword" />
      <template #footer><el-button @click="passwordDialogVisible = false">取消</el-button><el-button type="primary" :loading="savingPassword" @click="savePassword">保存新密码</el-button></template>
    </el-dialog>
  </AppShell>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAdminUsers, resetAdminUserPassword, updateAdminUserRole, updateAdminUserStatus } from '@/api/admin'
import AppShell from '@/components/AppShell.vue'
import PageHeader from '@/components/PageHeader.vue'

const users = ref([])
const loading = ref(false)
const pendingId = ref(null)
const passwordDialogVisible = ref(false)
const passwordTarget = ref(null)
const newPassword = ref('')
const savingPassword = ref(false)

const identities = { PROJECT_MANAGER: '项目经理', FRONTEND_DEV: '前端工程师', BACKEND_DEV: '后端工程师', QA_TESTER: '测试工程师', UI_DESIGNER: 'UI 设计师' }
const identityLabel = value => identities[value] || '未设置'
const formatDate = value => value ? String(value).slice(0, 10) : '未记录'

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
.content { max-width: 1240px; margin: 0 auto; padding: 34px 36px 52px; }
.admin-notice { display: flex; align-items: baseline; gap: 14px; padding: 14px 16px; margin: 24px 0 18px; border: 1px solid var(--border); border-left: 3px solid var(--brand); border-radius: 10px; background: var(--brand-light); }
.admin-notice span { color: var(--brand-deep); font-size: 13px; font-weight: 700; white-space: nowrap; }.admin-notice p { margin: 0; color: var(--text-secondary); font-size: 13px; }
.user-panel { overflow: hidden; border: 1px solid var(--border); border-radius: 12px; background: var(--surface); box-shadow: var(--shadow-xs); }.panel-title { display: flex; justify-content: space-between; padding: 16px 20px; border-bottom: 1px solid var(--border-light); color: var(--text-primary); font-weight: 700; }.panel-title small { color: var(--text-tertiary); font-weight: 400; }
.user-name { display: flex; align-items: center; gap: 10px; }.avatar { display: grid; place-items: center; flex: 0 0 auto; width: 32px; height: 32px; border-radius: 50%; background: var(--brand-light); color: var(--brand-deep); font-size: 12px; font-weight: 700; }.user-name strong,.user-name small { display: block; }.user-name small,.muted { color: var(--text-tertiary); font-size: 12px; }.dialog-hint { color: var(--text-secondary); margin: 0 0 14px; }
.mobile-records { display: none; }
@media (max-width: 767px) { .content { padding: 22px 16px 36px; }.admin-notice { align-items: flex-start; flex-direction: column; gap: 5px; }.desktop-table { display: none; }.mobile-records { display: grid; gap: 12px; padding: 14px; }.record-card { display: grid; gap: 14px; padding: 16px; border: 1px solid var(--border-light); border-radius: 10px; background: var(--surface); }.record-card dl { display: grid; gap: 8px; margin: 0; }.record-card dl div { display: flex; align-items: center; justify-content: space-between; gap: 12px; }.record-card dt { color: var(--text-tertiary); font-size: 12px; }.record-card dd { margin: 0; color: var(--text-secondary); font-size: 13px; }.record-actions { display: flex; justify-content: flex-end; border-top: 1px solid var(--border-light); padding-top: 8px; } }
</style>
