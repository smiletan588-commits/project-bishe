<template>
  <div class="dashboard">
    <header class="topbar">
      <div class="topbar-left">
        <svg width="28" height="28" viewBox="0 0 40 40" fill="none">
          <rect width="40" height="40" rx="10" :fill="'url(#g)'"/>
          <rect x="8" y="10" width="10" height="8" rx="2" fill="white" opacity="0.9"/>
          <rect x="22" y="10" width="10" height="8" rx="2" fill="white" opacity="0.7"/>
          <rect x="8" y="22" width="10" height="8" rx="2" fill="white" opacity="0.6"/>
          <rect x="22" y="22" width="10" height="8" rx="2" fill="white" opacity="0.8"/>
          <defs>
            <linearGradient id="g" x1="0" y1="0" x2="40" y2="40">
              <stop stop-color="#E2A43A"/><stop offset="1" stop-color="#A95D12"/>
            </linearGradient>
          </defs>
        </svg>
        <span class="brand">SmartPM</span>
      </div>
      <div class="topbar-right">
        <button class="analytics-btn" @click="$router.push('/analytics')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="12" width="4" height="8" rx="1"/><rect x="10" y="7" width="4" height="13" rx="1"/><rect x="17" y="3" width="4" height="17" rx="1"/>
          </svg>
          数据大屏
        </button>
        <span class="avatar-dot">{{ userStore.userInfo?.username?.[0]?.toUpperCase() }}</span>
        <span class="username">{{ userStore.userInfo?.username }}</span>
        <button class="logout-btn" @click="handleLogout">退出</button>
      </div>
    </header>

    <main class="main">
      <div class="page-header">
        <div>
          <h2>我的项目</h2>
          <p class="page-desc">{{ projects.length }} 个项目</p>
        </div>
        <el-button type="primary" size="large" @click="dialogVisible = true">
          + 新建项目
        </el-button>
      </div>

      <div v-loading="loading" class="card-grid">
        <div
          v-for="(item, i) in projects"
          :key="item.id"
          class="project-card"
          @click="$router.push(`/project/${item.id}`)"
        >
          <div class="card-accent" :style="{ background: cardColors[i % cardColors.length] }" />
          <div class="card-body">
            <div class="card-header">
              <h4>{{ item.name }}</h4>
              <el-dropdown trigger="click" @click.stop>
                <span class="card-more-btn" @click.stop>
                  <el-icon><MoreFilled /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click.stop="openMembersDialog(item)">
                      <el-icon><UserFilled /></el-icon> 团队成员
                    </el-dropdown-item>
                    <el-dropdown-item @click.stop="goToWiki(item)">
                      <el-icon><Document /></el-icon> 文档中心
                    </el-dropdown-item>
                    <el-dropdown-item @click.stop="openEditDialog(item)" divided>
                      <el-icon><Edit /></el-icon> 修改项目信息
                    </el-dropdown-item>
                    <el-dropdown-item @click.stop="handleDelete(item)" divided>
                      <el-icon><Delete /></el-icon> 删除项目
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <p class="card-desc">{{ item.description || '暂无描述' }}</p>
            <div class="card-meta">
              <span class="card-date">{{ item.createdAt?.slice(0, 10) }}</span>
              <span class="card-arrow">&rarr;</span>
            </div>
          </div>
        </div>

        <div v-if="!loading && projects.length === 0" class="empty-state">
          <div class="empty-icon">
            <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
              <rect x="6" y="8" width="36" height="28" rx="4" stroke="#94A3B8" stroke-width="1.5" stroke-dasharray="4 3"/>
              <line x1="6" y1="18" x2="42" y2="18" stroke="#94A3B8" stroke-width="1.5"/>
              <circle cx="12" cy="13" r="1.5" fill="#94A3B8"/>
              <circle cx="17" cy="13" r="1.5" fill="#94A3B8"/>
            </svg>
          </div>
          <h3>还没有项目</h3>
          <p>创建你的第一个项目，开始管理任务</p>
          <el-button type="primary" @click="dialogVisible = true">创建项目</el-button>
        </div>
      </div>
    </main>

    <el-dialog v-model="dialogVisible" title="新建项目" width="440px" :close-on-click-modal="false">
      <div class="dialog-form">
        <div class="input-group">
          <label>项目名称</label>
          <el-input v-model="form.name" placeholder="例如：官网改版" size="large" />
        </div>
        <div class="input-group">
          <label>项目描述 <span class="optional">选填</span></label>
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="简短描述项目目标" />
        </div>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">创建项目</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialogVisible" title="修改项目信息" width="440px" :close-on-click-modal="false">
      <div class="dialog-form">
        <div class="input-group">
          <label>项目名称</label>
          <el-input v-model="editForm.name" placeholder="项目名称" size="large" />
        </div>
        <div class="input-group">
          <label>项目描述 <span class="optional">选填</span></label>
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="简短描述项目目标" />
        </div>
      </div>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="handleUpdate">保存修改</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="membersDialogVisible" :title="`${memberProject?.name || ''} · 团队成员`" width="720px" class="members-dialog" :close-on-click-modal="false">
      <div class="member-invite" v-if="memberManageable">
        <div class="member-invite-title">邀请成员加入项目</div>
        <div class="member-invite-form">
          <el-input v-model="inviteForm.username" placeholder="输入对方用户名" clearable />
          <el-select v-model="inviteForm.identity" placeholder="项目身份" style="width: 150px">
            <el-option v-for="item in identityOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="inviteForm.permission" placeholder="权限" style="width: 140px">
            <el-option label="普通成员" value="MEMBER" />
            <el-option label="项目管理员" value="PROJECT_ADMIN" />
            <el-option label="只读成员" value="VIEWER" />
          </el-select>
          <el-button type="primary" :loading="inviteSubmitting" @click="handleInvite">邀请</el-button>
        </div>
      </div>

      <el-table :data="members" v-loading="memberLoading" class="members-table">
        <el-table-column label="成员" min-width="160">
          <template #default="{ row }">
            <div class="member-person"><span class="member-avatar">{{ (row.nickname || row.username)?.[0]?.toUpperCase() }}</span><span>{{ row.nickname || row.username }}</span><small>@{{ row.username }}</small></div>
          </template>
        </el-table-column>
        <el-table-column label="项目身份" width="150">
          <template #default="{ row }">
            <el-select v-if="memberManageable && !row.owner" :model-value="row.identity" size="small" @change="value => changeMember(row, value, row.permission)">
              <el-option v-for="item in identityOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <span v-else>{{ identityLabel(row.identity) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="权限" width="140">
          <template #default="{ row }">
            <el-select v-if="memberManageable && !row.owner" :model-value="row.permission" size="small" @change="value => changeMember(row, row.identity, value)">
              <el-option label="普通成员" value="MEMBER" /><el-option label="项目管理员" value="PROJECT_ADMIN" /><el-option label="只读成员" value="VIEWER" />
            </el-select>
            <el-tag v-else :type="row.owner ? 'warning' : 'info'">{{ row.owner ? '负责人' : permissionLabel(row.permission) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" v-if="memberManageable">
          <template #default="{ row }">
            <el-button v-if="!row.owner" text type="warning" size="small" @click="handleTransfer(row)">转移负责人</el-button>
            <el-button v-if="!row.owner" text type="danger" size="small" @click="handleRemoveMember(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 身份选择弹窗 -->
    <el-dialog
      v-model="showIdentityDialog"
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      width="520px"
      class="identity-dialog"
    >
      <template #header>
        <div class="identity-dialog-header">
          <div class="identity-icon-wrapper">
            <svg width="36" height="36" viewBox="0 0 48 48" fill="none">
              <circle cx="24" cy="16" r="8" stroke="url(#idGrad)" stroke-width="2.5"/>
              <path d="M10 40c0-7.732 6.268-14 14-14s14 6.268 14 14" stroke="url(#idGrad)" stroke-width="2.5" stroke-linecap="round"/>
              <defs>
                <linearGradient id="idGrad" x1="0" y1="0" x2="48" y2="48">
                  <stop stop-color="#E2A43A"/><stop offset="1" stop-color="#A95D12"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <h3>请选择您的专业身份</h3>
          <p class="identity-subtitle">这将帮助团队了解您的专业技能方向</p>
        </div>
      </template>
      <div class="identity-grid">
        <div
          v-for="item in identityOptions"
          :key="item.value"
          class="identity-card"
          :class="{ selected: selecting === item.value }"
          @click="handleSelectIdentity(item.value)"
        >
          <span class="identity-emoji">{{ item.emoji }}</span>
          <span class="identity-label">{{ item.label }}</span>
          <span class="identity-code">{{ item.value }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MoreFilled, Edit, Delete, Document, UserFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { listProjects, createProject, updateProject, deleteProject, listProjectMembers, inviteProjectMember, updateProjectMember, removeProjectMember, transferProjectOwner } from '@/api/project'

const router = useRouter()
const userStore = useUserStore()
const projects = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const form = reactive({ name: '', description: '' })

const editDialogVisible = ref(false)
const editSubmitting = ref(false)
const editForm = reactive({ id: null, name: '', description: '' })

const membersDialogVisible = ref(false)
const memberProject = ref(null)
const members = ref([])
const memberLoading = ref(false)
const memberManageable = ref(false)
const inviteSubmitting = ref(false)
const inviteForm = reactive({ username: '', identity: 'FRONTEND_DEV', permission: 'MEMBER' })

const showIdentityDialog = ref(false)
const selecting = ref(null)

const identityOptions = [
  { value: 'PROJECT_MANAGER', label: '项目经理', emoji: '📋' },
  { value: 'FRONTEND_DEV',   label: '前端工程师', emoji: '💻' },
  { value: 'BACKEND_DEV',    label: '后端工程师', emoji: '☕' },
  { value: 'QA_TESTER',      label: '测试工程师', emoji: '🧪' },
  { value: 'UI_DESIGNER',    label: 'UI设计师',   emoji: '🎨' }
]

async function handleSelectIdentity(identity) {
  if (selecting.value) return
  selecting.value = identity
  try {
    await userStore.updateIdentity(identity)
    ElMessage.success('身份设置成功')
    showIdentityDialog.value = false
  } catch (e) {
    ElMessage.error('设置失败，请重试')
  } finally {
    selecting.value = null
  }
}

const cardColors = ['#D58A22', '#B96A18', '#8D6A3B', '#6F7A57', '#B46F4A', '#8B5E3C']

async function fetchProjects() {
  console.log('[Dashboard] 开始加载项目列表...')
  loading.value = true
  try {
    const res = await listProjects()
    console.log('[Dashboard] 接口返回:', res)
    const list = res.data?.data
    if (Array.isArray(list)) {
      projects.value = list
      console.log('[Dashboard] 成功加载 ' + list.length + ' 个项目')
    } else {
      console.error('[Dashboard] 返回数据格式异常:', res.data)
      projects.value = []
      ElMessage.warning('项目数据格式异常，请联系管理员')
    }
  } catch (error) {
    console.error('[Dashboard] 加载项目列表失败:', error)
    // Axios 拦截器已处理 401/404/405/500 弹窗，此处处理网络断开等特殊情况
    if (!error.response) {
      ElMessage.error('网络连接失败，请检查后端服务是否启动')
    }
  } finally {
    loading.value = false
    console.log('[Dashboard] 加载完成, loading=', loading.value)
  }
}

async function handleCreate() {
  if (!form.name.trim()) { ElMessage.warning('请输入项目名称'); return }
  submitting.value = true
  try {
    await createProject(form.name, form.description)
    ElMessage.success('项目创建成功')
    dialogVisible.value = false
    form.name = ''
    form.description = ''
    await fetchProjects()
  } catch (error) {
    console.error('[Dashboard] 创建项目失败:', error)
    if (!error.response) {
      ElMessage.error('创建失败，请检查网络连接')
    }
  } finally {
    submitting.value = false
  }
}

function goToWiki(project) {
  router.push({ path: `/project/${project.id}/wiki`, query: { projectName: project.name } })
}

const identityLabels = Object.fromEntries(identityOptions.map(item => [item.value, item.label]))
function identityLabel(value) { return identityLabels[value] || '未设置' }
function permissionLabel(value) { return { PROJECT_ADMIN: '项目管理员', MEMBER: '普通成员', VIEWER: '只读成员' }[value] || '普通成员' }

async function openMembersDialog(project) {
  memberProject.value = project
  membersDialogVisible.value = true
  await fetchMembersForManagement()
}

async function fetchMembersForManagement() {
  if (!memberProject.value) return
  memberLoading.value = true
  try {
    const res = await listProjectMembers(memberProject.value.id)
    members.value = res.data?.data || []
    memberManageable.value = members.value[0]?.canManage || memberProject.value.creatorId === userStore.userInfo?.userId
  } catch (e) {
    ElMessage.error('加载团队成员失败')
  } finally { memberLoading.value = false }
}

async function handleInvite() {
  if (!inviteForm.username.trim()) { ElMessage.warning('请输入用户名'); return }
  inviteSubmitting.value = true
  try {
    await inviteProjectMember(memberProject.value.id, inviteForm.username.trim(), inviteForm.identity, inviteForm.permission)
    ElMessage.success('成员已加入项目')
    inviteForm.username = ''
    await fetchMembersForManagement()
  } catch (e) { ElMessage.error(e.response?.data?.msg || '邀请失败') }
  finally { inviteSubmitting.value = false }
}

async function changeMember(row, identity, permission) {
  try {
    await updateProjectMember(memberProject.value.id, row.userId, identity, permission)
    row.identity = identity; row.permission = permission
    ElMessage.success('成员设置已更新')
  } catch (e) { ElMessage.error(e.response?.data?.msg || '更新成员失败'); await fetchMembersForManagement() }
}

async function handleRemoveMember(row) {
  try { await ElMessageBox.confirm(`确定移除 ${row.nickname || row.username}？`, '移除成员', { type: 'warning' }) } catch { return }
  try { await removeProjectMember(memberProject.value.id, row.userId); ElMessage.success('成员已移除'); await fetchMembersForManagement() }
  catch (e) { ElMessage.error(e.response?.data?.msg || '移除失败') }
}

async function handleTransfer(row) {
  try { await ElMessageBox.confirm(`转移后你将成为项目管理员，确定将负责人转给 ${row.nickname || row.username}？`, '转移项目负责人', { type: 'warning' }) } catch { return }
  try { await transferProjectOwner(memberProject.value.id, row.userId); ElMessage.success('项目负责人已转移'); await fetchMembersForManagement() }
  catch (e) { ElMessage.error(e.response?.data?.msg || '转移失败') }
}

function openEditDialog(project) {
  editForm.id = project.id
  editForm.name = project.name
  editForm.description = project.description || ''
  editDialogVisible.value = true
}

async function handleUpdate() {
  if (!editForm.name.trim()) { ElMessage.warning('项目名称不能为空'); return }
  editSubmitting.value = true
  try {
    await updateProject(editForm.id, editForm.name, editForm.description)
    ElMessage.success('项目信息已更新')
    editDialogVisible.value = false
    await fetchProjects()
  } catch (error) {
    console.error('[Dashboard] 修改项目失败:', error)
    if (!error.response) {
      ElMessage.error('修改失败，请检查网络连接')
    }
  } finally {
    editSubmitting.value = false
  }
}

async function handleDelete(project) {
  try {
    await ElMessageBox.confirm(
      '您确定要删除该项目及旗下的所有任务吗？此操作不可逆。',
      '删除项目',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deleteProject(project.id)
    ElMessage.success('项目已删除')
    await fetchProjects()
  } catch (error) {
    console.error('[Dashboard] 删除项目失败:', error)
    if (!error.response) {
      ElMessage.error('删除失败，请检查网络连接')
    }
  }
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  fetchProjects()
  if (userStore.needsIdentityPrompt) {
    showIdentityDialog.value = true
  }
})
</script>

<style scoped>
.dashboard { min-height: 100vh; }
.topbar {
  display: flex; justify-content: space-between; align-items: center;
  height: 56px; padding: 0 24px;
  background: #242321; border-bottom: 1px solid #3A3732;
}
.topbar-left { display: flex; align-items: center; gap: 10px; }
.brand { font-size: 16px; font-weight: 700; color: #F7F1E7; letter-spacing: .02em; }
.topbar-right { display: flex; align-items: center; gap: 8px; }
.avatar-dot {
  width: 28px; height: 28px; border-radius: 50%;
  background: var(--brand-gradient); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 600;
}
.username { font-size: 13px; color: #B9B1A5; }
.logout-btn {
  background: none; border: none; color: #B9B1A5;
  font-size: 13px; cursor: pointer; padding: 4px 8px; border-radius: 4px;
}
.logout-btn:hover { color: #E2A43A; background: rgba(226,164,58,.12); }
.analytics-btn {
  display: inline-flex; align-items: center; gap: 5px;
  background: #D58A22; color: #241D14; border: none;
  font-size: 13px; font-weight: 500; cursor: pointer;
  padding: 6px 14px; border-radius: var(--radius-sm);
  transition: opacity 0.15s; margin-right: 6px;
}
.analytics-btn:hover { opacity: 0.88; }

.main { max-width: 1200px; margin: 0 auto; padding: 52px 30px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 28px; }
.page-header h2 { margin: 0; font-family: var(--font-display); font-size: 30px; font-weight: 700; letter-spacing: -.03em; }
.page-desc { margin: 4px 0 0; font-size: 13px; color: var(--text-tertiary); }
.page-header :deep(.el-button--primary) {
  background: var(--brand-gradient); border: none; border-radius: var(--radius-sm);
  font-weight: 600;
}

.card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 22px; }
.project-card {
  background: #242321; border-radius: var(--radius);
  box-shadow: 0 12px 24px rgba(42,34,24,.12); border: 1px solid #393632;
  cursor: pointer; transition: transform 0.18s, box-shadow 0.18s; overflow: hidden;
}
.project-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
.card-accent { height: 4px; }
.card-body { padding: 20px 22px 18px; }
.card-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  gap: 8px; margin-bottom: 6px;
}
.card-header h4 { margin: 0; font-size: 17px; font-weight: 650; color: #F7F1E7; flex: 1; }
.card-more-btn {
  display: inline-flex; align-items: center; justify-content: center;
  width: 28px; height: 28px; border-radius: 6px;
  color: var(--text-tertiary); cursor: pointer;
  transition: background 0.15s, color 0.15s; flex-shrink: 0;
}
.card-more-btn:hover { background: #3A3732; color: #F7F1E7; }
.card-desc {
  margin: 8px 0 24px; font-size: 13px; color: #B9B1A5;
  line-height: 1.5; min-height: 20px;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.card-meta { display: flex; justify-content: space-between; align-items: center; }
.card-date { font-size: 12px; color: #8F887E; }
.card-arrow { font-size: 16px; color: #D58A22; transition: transform 0.15s; }
.project-card:hover .card-arrow { color: #E2A43A; transform: translateX(3px); }

.empty-state {
  grid-column: 1 / -1; text-align: center; padding: 64px 24px;
}
.empty-icon { margin-bottom: 16px; }
.empty-state h3 { margin: 0; font-size: 18px; color: var(--text-primary); }
.empty-state p { margin: 8px 0 20px; color: var(--text-tertiary); font-size: 14px; }

.dialog-form .input-group { margin-bottom: 16px; }
.dialog-form label { display: block; font-size: 13px; font-weight: 500; color: var(--text-secondary); margin-bottom: 6px; }
.optional { font-weight: 400; color: var(--text-tertiary); font-size: 12px; }
.dialog-form :deep(.el-input__wrapper) { border-radius: var(--radius-sm); }

.members-dialog :deep(.el-dialog__body) { padding-top: 8px; }
.member-invite { padding: 16px; margin-bottom: 18px; background: #F4EFE6; border: 1px solid var(--border); border-radius: var(--radius); }
.member-invite-title { margin-bottom: 10px; font-size: 13px; font-weight: 700; color: var(--text-primary); }
.member-invite-form { display: flex; gap: 8px; align-items: center; }
.member-invite-form .el-input { flex: 1; }
.member-person { display: flex; align-items: center; gap: 8px; font-weight: 600; }
.member-person small { color: var(--text-tertiary); font-size: 11px; font-weight: 400; }
.member-avatar { width: 28px; height: 28px; display: inline-flex; align-items: center; justify-content: center; border-radius: 50%; background: var(--brand); color: #fff; font-size: 12px; }
.members-table :deep(.el-table__header th) { background: #F4EFE6; color: var(--text-secondary); }

@media (max-width: 640px) {
  .card-grid { grid-template-columns: 1fr; }
  .page-header { flex-direction: column; gap: 12px; }
}

/* ── 身份选择弹窗 ── */
.identity-dialog :deep(.el-dialog) {
  border-radius: 16px;
}
.identity-dialog :deep(.el-dialog__header) {
  margin-right: 0;
  padding-bottom: 0;
}
.identity-dialog-header {
  text-align: center;
  padding: 8px 0 4px;
}
.identity-icon-wrapper {
  margin-bottom: 12px;
  display: inline-block;
}
.identity-dialog-header h3 {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}
.identity-subtitle {
  margin: 0;
  font-size: 13px;
  color: var(--text-tertiary);
}

.identity-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  padding: 8px 0 4px;
}
.identity-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 18px 10px 14px;
  border-radius: 12px;
  border: 2px solid var(--border);
  cursor: pointer;
  transition: all 0.18s;
  background: var(--bg-surface);
}
.identity-card:hover {
  border-color: var(--brand);
  background: #EEF2FF;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.15);
}
.identity-card.selected {
  border-color: var(--brand);
  background: #EEF2FF;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2);
}
.identity-emoji {
  font-size: 28px;
  line-height: 1;
}
.identity-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}
.identity-code {
  font-size: 11px;
  color: var(--text-tertiary);
  font-family: monospace;
}

@media (max-width: 500px) {
  .identity-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 680px) {
  .member-invite-form { flex-wrap: wrap; }
  .member-invite-form .el-input { flex-basis: 100%; }
}
</style>
