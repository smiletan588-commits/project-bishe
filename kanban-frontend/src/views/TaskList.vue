<template>
  <div class="board">
    <header class="topbar">
      <div class="topbar-left">
        <el-button text @click="$router.push('/dashboard')">
          <el-icon><ArrowLeft /></el-icon>&nbsp;返回
        </el-button>
        <span class="sep">/</span>
        <h3>{{ projectName }}</h3>
      </div>
      <div class="topbar-right">
        <el-button size="small" @click="$router.push({ path: `/project/${projectId}/wiki`, query: { projectName } })">
          <el-icon><Document /></el-icon>&nbsp;文档中心
        </el-button>
        <el-button class="ai-init-btn" size="small" :loading="initTasksLoading"
          :disabled="hasTasks" @click="handleInitTasks">
          ✨ AI 生成任务
        </el-button>
        <el-button type="primary" size="small" :loading="summaryLoading" @click="openSummary">
          生成项目总结
        </el-button>
        <span class="avatar-dot">{{ userStore.userInfo?.username?.[0]?.toUpperCase() }}</span>
        <span class="username">{{ userStore.userInfo?.username }}</span>
      </div>
    </header>

    <main class="board-main">
      <div class="columns">

        <!-- TODO 列 -->
        <div class="column">
          <div class="column-header todo">
            <div class="col-title">
              <span class="col-dot" style="background:#F59E0B" />
              <span>待办</span>
              <span class="col-count">{{ todoList.length }}</span>
            </div>
            <el-button text size="small" @click="openCreate('TODO')">+</el-button>
          </div>
          <draggable v-model="todoList" group="tasks" item-key="id"
            class="column-body" ghost-class="ghost" animation="180"
            @change="(e) => onDragChange(e, 'TODO')">
            <template #item="{ element }">
              <div class="task-card" @click="openDetail(element)">
                <div class="card-grip">
                  <span v-for="i in 3" :key="i" class="grip-dot" />
                </div>
                <div class="card-content">
                  <p class="task-title">{{ element.title }}</p>
                  <p class="task-desc" v-if="element.description">{{ element.description }}</p>
                  <div v-if="element.recommendedRole || element.assigneeId" class="task-assignment-row">
                    <span v-if="element.recommendedRole" class="role-tag"
                      :style="{ background: (roleConfig[element.recommendedRole] || {}).color || '#94A3B8' }">
                      {{ (roleConfig[element.recommendedRole] || {}).label || element.recommendedRole }}
                    </span>
                    <span v-if="assigneeName(element)" class="assignee-name">负责人：{{ assigneeName(element) }}</span>
                    <span v-else class="assignee-name pending">待工程师接取</span>
                  </div>
                  <div class="task-footer">
                    <span v-if="element.dueDate" class="task-due"
                      :class="{ overdue: isOverdue(element.dueDate) }">{{ element.dueDate }}</span>
                    <span v-else />
                    <div class="footer-actions">
                      <div class="subtask-badge" v-if="subtaskProgress(element.id).total > 0">
                        <div class="subtask-minibar">
                          <div class="subtask-minibar-fill"
                            :style="{ width: subtaskProgress(element.id).percent + '%' }" />
                        </div>
                        <span class="subtask-minitext">
                          {{ subtaskProgress(element.id).done }}/{{ subtaskProgress(element.id).total }}
                        </span>
                      </div>
                      <el-button type="danger" text size="small" @click.stop="handleDelete(element)">
                        <el-icon><Delete /></el-icon>
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </draggable>
          <div v-if="todoList.length === 0" class="column-empty">拖拽任务到此处</div>
        </div>

        <!-- IN_PROGRESS 列 -->
        <div class="column">
          <div class="column-header progress">
            <div class="col-title">
              <span class="col-dot" style="background:#3B82F6" />
              <span>进行中</span>
              <span class="col-count">{{ inProgressList.length }}</span>
            </div>
            <el-button text size="small" @click="openCreate('IN_PROGRESS')">+</el-button>
          </div>
          <draggable v-model="inProgressList" group="tasks" item-key="id"
            class="column-body" ghost-class="ghost" animation="180"
            @change="(e) => onDragChange(e, 'IN_PROGRESS')">
            <template #item="{ element }">
              <div class="task-card" @click="openDetail(element)">
                <div class="card-grip">
                  <span v-for="i in 3" :key="i" class="grip-dot" />
                </div>
                <div class="card-content">
                  <p class="task-title">{{ element.title }}</p>
                  <p class="task-desc" v-if="element.description">{{ element.description }}</p>
                  <div v-if="element.recommendedRole || element.assigneeId" class="task-assignment-row">
                    <span v-if="element.recommendedRole" class="role-tag"
                      :style="{ background: (roleConfig[element.recommendedRole] || {}).color || '#94A3B8' }">
                      {{ (roleConfig[element.recommendedRole] || {}).label || element.recommendedRole }}
                    </span>
                    <span v-if="assigneeName(element)" class="assignee-name">负责人：{{ assigneeName(element) }}</span>
                    <span v-else class="assignee-name pending">待工程师接取</span>
                  </div>
                  <div class="task-footer">
                    <span v-if="element.dueDate" class="task-due"
                      :class="{ overdue: isOverdue(element.dueDate) }">{{ element.dueDate }}</span>
                    <span v-else />
                    <div class="footer-actions">
                      <div class="subtask-badge" v-if="subtaskProgress(element.id).total > 0">
                        <div class="subtask-minibar">
                          <div class="subtask-minibar-fill"
                            :style="{ width: subtaskProgress(element.id).percent + '%' }" />
                        </div>
                        <span class="subtask-minitext">
                          {{ subtaskProgress(element.id).done }}/{{ subtaskProgress(element.id).total }}
                        </span>
                      </div>
                      <el-button type="danger" text size="small" @click.stop="handleDelete(element)">
                        <el-icon><Delete /></el-icon>
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </draggable>
          <div v-if="inProgressList.length === 0" class="column-empty">拖拽任务到此处</div>
        </div>

        <!-- DONE 列 -->
        <div class="column">
          <div class="column-header done">
            <div class="col-title">
              <span class="col-dot" style="background:#10B981" />
              <span>已完成</span>
              <span class="col-count">{{ doneList.length }}</span>
            </div>
            <el-button text size="small" @click="openCreate('DONE')">+</el-button>
          </div>
          <draggable v-model="doneList" group="tasks" item-key="id"
            class="column-body" ghost-class="ghost" animation="180"
            @change="(e) => onDragChange(e, 'DONE')">
            <template #item="{ element }">
              <div class="task-card done-card" @click="openDetail(element)">
                <div class="card-grip">
                  <span v-for="i in 3" :key="i" class="grip-dot" />
                </div>
                <div class="card-content">
                  <p class="task-title">{{ element.title }}</p>
                  <p class="task-desc" v-if="element.description">{{ element.description }}</p>
                  <div v-if="element.recommendedRole || element.assigneeId" class="task-assignment-row">
                    <span v-if="element.recommendedRole" class="role-tag"
                      :style="{ background: (roleConfig[element.recommendedRole] || {}).color || '#94A3B8' }">
                      {{ (roleConfig[element.recommendedRole] || {}).label || element.recommendedRole }}
                    </span>
                    <span v-if="assigneeName(element)" class="assignee-name">负责人：{{ assigneeName(element) }}</span>
                    <span v-else class="assignee-name pending">待工程师接取</span>
                  </div>
                  <div class="task-footer">
                    <span v-if="element.dueDate" class="task-due">{{ element.dueDate }}</span>
                    <span v-else />
                    <div class="footer-actions">
                      <div class="subtask-badge" v-if="subtaskProgress(element.id).total > 0">
                        <div class="subtask-minibar">
                          <div class="subtask-minibar-fill"
                            :style="{ width: subtaskProgress(element.id).percent + '%' }" />
                        </div>
                        <span class="subtask-minitext">
                          {{ subtaskProgress(element.id).done }}/{{ subtaskProgress(element.id).total }}
                        </span>
                      </div>
                      <el-button type="danger" text size="small" @click.stop="handleDelete(element)">
                        <el-icon><Delete /></el-icon>
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </draggable>
          <div v-if="doneList.length === 0" class="column-empty">拖拽任务到此处</div>
        </div>

      </div>
    </main>

    <!-- 任务详情对话框（子任务清单） -->
    <el-dialog v-model="detailVisible" :close-on-click-modal="false"
      width="600px" class="detail-dialog">
      <template #header>
        <div class="detail-header">
          <span class="detail-status-tag" :class="selectedTask ? statusClass(selectedTask.status) : ''">
            {{ selectedTask ? statusLabel(selectedTask.status) : '' }}
          </span>
          <h2 class="detail-title">{{ selectedTask?.title }}</h2>
          <div class="detail-actions" v-if="selectedTask">
            <el-button v-if="selectedTask.status === 'TODO'" type="primary" size="small"
              :loading="taskActionLoading" @click="handleStartTask">
              <el-icon><VideoPlay /></el-icon>&nbsp;开始开发
            </el-button>
            <el-button v-if="selectedTask.status !== 'DONE'" type="success" size="small"
              :loading="taskActionLoading" @click="handleCompleteTask">
              <el-icon><CircleCheck /></el-icon>&nbsp;完成任务
            </el-button>
          </div>
        </div>
      </template>
      <div class="detail-body" v-if="selectedTask">
        <p class="detail-desc" v-if="selectedTask.description">{{ selectedTask.description }}</p>
        <p class="detail-meta" v-if="selectedTask.dueDate">
          <el-icon><Calendar /></el-icon>
          截止日期：{{ selectedTask.dueDate }}
        </p>
        <div class="detail-meta task-assignment" v-if="selectedTask.recommendedRole">
          <span class="role-tag" :style="{ background: (roleConfig[selectedTask.recommendedRole] || {}).color || '#94A3B8' }">
            推荐岗位：{{ (roleConfig[selectedTask.recommendedRole] || {}).label || selectedTask.recommendedRole }}
          </span>
          <span v-if="selectedTask.assigneeId && memberMap[selectedTask.assigneeId]">
            当前负责人：{{ memberMap[selectedTask.assigneeId].nickname }}
          </span>
          <span v-else class="assignee-info pending">等待匹配项目成员</span>
        </div>
        <div class="subtask-section">
          <div class="subtask-section-header">
            <div class="subtask-section-title">
              <span class="subtask-label">子任务清单</span>
              <span class="subtask-summary">
                {{ doneCount(selectedTask.id) }}/{{ totalCount(selectedTask.id) }} 已完成
              </span>
            </div>
            <el-button type="primary" size="small"
              :loading="decomposingTaskId === selectedTask.id"
              @click="handleDecompose(selectedTask)">
               AI 智能拆解步骤
            </el-button>
          </div>
          <div class="subtask-progress-bar" v-if="totalCount(selectedTask.id) > 0">
            <div class="progress-track">
              <div class="progress-fill" :style="{ width: progressPercent(selectedTask.id) + '%' }" />
            </div>
          </div>
          <div v-if="currentSubtasks.length === 0 && decomposingTaskId !== selectedTask.id"
            class="subtask-empty">
            暂无子任务，点击上方按钮让 AI 帮你拆解为具体步骤
          </div>
          <div v-for="sub in currentSubtasks" :key="sub.id" class="subtask-item"
            :class="{ done: sub.status === 'DONE' }">
            <el-checkbox :model-value="sub.status === 'DONE'"
              @change="(val) => toggleSubtaskStatus(sub, val)">
              <span class="subtask-title">{{ sub.title }}</span>
            </el-checkbox>
            <p class="subtask-desc" v-if="sub.description">{{ sub.description }}</p>
            <div class="subtask-meta" v-if="sub.recommendedRole">
              <span class="role-tag" :style="{ background: (roleConfig[sub.recommendedRole] || {}).color || '#94A3B8' }">
                推荐：{{ (roleConfig[sub.recommendedRole] || {}).label || sub.recommendedRole }}
              </span>
              <span class="assignee-info" v-if="sub.assigneeId && memberMap[sub.assigneeId]">
                指派给：{{ memberMap[sub.assigneeId].nickname }}
              </span>
              <span class="assignee-info pending" v-else-if="sub.recommendedRole && !sub.assigneeId">
                等待加入 / 指派
              </span>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 创建 / 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingTask ? '编辑任务' : '新建任务'"
      width="460px" :close-on-click-modal="false">
      <div class="dialog-form">
        <div class="input-group">
          <label>标题</label>
          <el-input v-model="form.title" placeholder="任务标题" size="large" />
        </div>
        <div class="input-group">
          <label>描述 <span class="optional">选填</span></label>
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="任务描述" />
        </div>
        <div class="input-row">
          <div class="input-group">
            <label>状态</label>
            <el-select v-model="form.status" size="large" style="width:100%">
              <el-option label="待办" value="TODO" />
              <el-option label="进行中" value="IN_PROGRESS" />
              <el-option label="已完成" value="DONE" />
            </el-select>
          </div>
          <div class="input-group">
            <label>截止日期 <span class="optional">选填</span></label>
            <el-date-picker v-model="form.dueDate" type="date" placeholder="选择日期"
              value-format="YYYY-MM-DD" size="large" style="width:100%" />
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ editingTask ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- AI 项目总结对话框 -->
    <el-dialog v-model="summaryDialogVisible" title="AI 项目周报" width="700px"
      :close-on-click-modal="false" @close="closeSummary">
      <div class="summary-body">
        <div v-if="summaryLoading && !summaryContent" class="summary-loading">
          <el-icon class="is-loading" :size="24"><Loading /></el-icon>
          <span>AI 正在生成项目周报...</span>
        </div>
        <div v-if="summaryContent" class="markdown-body" v-html="renderedMarkdown"></div>
      </div>
      <template #footer v-if="!summaryLoading">
        <el-button @click="closeSummary">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Delete, Loading, Calendar, Document, VideoPlay, CircleCheck } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import MarkdownIt from 'markdown-it'
import { useUserStore } from '@/store/user'
import { listProjects } from '@/api/project'
import { listTasks, createTask, updateTask, deleteTask, decomposeTask, listSubtasks, toggleSubtask, initProjectTasks } from '@/api/task'
import { listProjectMembers } from '@/api/project'
import { streamProjectSummary } from '@/api/summary'
import request from '@/utils/request'

const md = new MarkdownIt({ breaks: true, linkify: true })

const route = useRoute()
const userStore = useUserStore()
const projectId = Number(route.params.id)
const projectName = ref('')

// 看板三列（仅主任务）
const todoList = ref([])
const inProgressList = ref([])
const doneList = ref([])

// 项目成员：userId -> { nickname, identity }
const memberMap = ref({})

const roleConfig = {
  PROJECT_MANAGER: { label: '项目经理', color: '#D58A22' },
  FRONTEND_DEV:   { label: '前端',     color: '#68839A' },
  BACKEND_DEV:    { label: '后端',     color: '#6F7A57' },
  QA_TESTER:      { label: '测试',     color: '#B46F4A' },
  UI_DESIGNER:    { label: 'UI设计',   color: '#EC4899' }
}

const hasTasks = computed(() =>
  todoList.value.length > 0 || inProgressList.value.length > 0 || doneList.value.length > 0
)

function assigneeName(task) {
  if (!task?.assigneeId) return ''
  return memberMap.value[task.assigneeId]?.nickname ||
    (task.assigneeId === userStore.userInfo?.userId ? userStore.userInfo?.nickname || userStore.userInfo?.username : '')
}

async function fetchMembers() {
  try {
    const res = await listProjectMembers(projectId)
    const map = {}
    ;(res.data.data || []).forEach(m => {
      map[m.userId] = { nickname: m.nickname || m.username, identity: m.identity }
    })
    memberMap.value = map
  } catch { /* ignore */ }
}

// 子任务缓存：taskId -> Task[]
const subtaskData = reactive({})

/** 子任务进度 { done, total, percent } */
function subtaskProgress(taskId) {
  const list = subtaskData[taskId]
  if (!list || list.length === 0) return { done: 0, total: 0, percent: 0 }
  const done = list.filter(s => s.status === 'DONE').length
  return { done, total: list.length, percent: Math.round(done / list.length * 100) }
}
function doneCount(taskId) { return subtaskProgress(taskId).done }
function totalCount(taskId) { return subtaskProgress(taskId).total }
function progressPercent(taskId) { return subtaskProgress(taskId).percent }

// 详情弹窗
const detailVisible = ref(false)
const selectedTask = ref(null)
const currentSubtasks = computed(() => {
  if (!selectedTask.value) return []
  return subtaskData[selectedTask.value.id] || []
})

function statusLabel(s) {
  return { TODO: '待办', IN_PROGRESS: '进行中', DONE: '已完成' }[s] || s
}
function statusClass(s) {
  return { TODO: 'tag-todo', IN_PROGRESS: 'tag-progress', DONE: 'tag-done' }[s] || ''
}

// 创建/编辑弹窗
const dialogVisible = ref(false)
const submitting = ref(false)
const editingTask = ref(null)
const form = reactive({ title: '', description: '', status: 'TODO', dueDate: null })

function isOverdue(dateStr) {
  if (!dateStr) return false
  return new Date(dateStr) < new Date(new Date().toDateString())
}

// 数据加载
async function fetchProjectName() {
  try {
    const res = await listProjects()
    projectName.value = res.data.data.find(p => p.id === projectId)?.name || '未知项目'
  } catch { projectName.value = '未知项目' }
}

async function fetchTasks() {
  try {
    const res = await listTasks(projectId)
    const all = res.data.data || []
    todoList.value = all.filter(t => t.status === 'TODO').sort((a, b) => a.orderIndex - b.orderIndex)
    inProgressList.value = all.filter(t => t.status === 'IN_PROGRESS').sort((a, b) => a.orderIndex - b.orderIndex)
    doneList.value = all.filter(t => t.status === 'DONE').sort((a, b) => a.orderIndex - b.orderIndex)
    fetchAllSubtaskCounts(all)
  } catch { /* handled by interceptor */ }
}

async function fetchAllSubtaskCounts(tasks) {
  if (!tasks || tasks.length === 0) return
  const promises = tasks.map(task =>
    listSubtasks(task.id)
      .then(res => { subtaskData[task.id] = res.data.data || [] })
      .catch(() => { subtaskData[task.id] = [] })
  )
  await Promise.all(promises)
}

async function fetchSubtasksForTask(taskId) {
  try {
    const res = await listSubtasks(taskId)
    subtaskData[taskId] = res.data.data || []
  } catch {
    subtaskData[taskId] = []
  }
}

// 详情弹窗
async function openDetail(task) {
  selectedTask.value = task
  detailVisible.value = true
  if (!subtaskData[task.id]) {
    await fetchSubtasksForTask(task.id)
  }
}

const taskActionLoading = ref(false)

async function changeTaskStatus(status, successMessage) {
  if (!selectedTask.value || taskActionLoading.value) return
  taskActionLoading.value = true
  try {
    await updateTask({ id: selectedTask.value.id, status })
    selectedTask.value.status = status
    ElMessage.success(successMessage)
    await fetchTasks()
  } catch (error) {
    console.error('[TaskList] 更新任务状态失败:', error)
  } finally {
    taskActionLoading.value = false
  }
}

function handleStartTask() {
  return changeTaskStatus('IN_PROGRESS', '任务已进入开发中')
}

function handleCompleteTask() {
  return changeTaskStatus('DONE', '任务已完成')
}

// 子任务勾选（乐观更新）
async function toggleSubtaskStatus(sub, checked) {
  const prev = sub.status
  sub.status = checked ? 'DONE' : 'TODO'
  try {
    await toggleSubtask(sub.id)
  } catch {
    sub.status = prev
  }
}

// AI 生成初始任务：仅允许空项目执行，生成结果统一进入待办列。
const initTasksLoading = ref(false)

async function handleInitTasks() {
  if (hasTasks.value || initTasksLoading.value) return
  initTasksLoading.value = true
  try {
    await initProjectTasks(projectId)
    ElMessage.success('AI 已根据项目目标生成开发任务，请工程师接取后拖入进行中')
    await fetchTasks()
  } catch (error) {
    console.error('[TaskList] AI 生成任务失败:', error)
  } finally {
    initTasksLoading.value = false
  }
}

// AI 拆解
const decomposingTaskId = ref(null)

async function handleDecompose(task) {
  decomposingTaskId.value = task.id
  try {
    await decomposeTask(task.id)
    ElMessage.success('AI 已拆解子任务')
    await fetchSubtasksForTask(task.id)
  } catch {
    ElMessage.error('AI 拆解失败，请重试')
  } finally {
    decomposingTaskId.value = null
  }
}

// 拖拽
async function onDragChange(event, targetStatus) {
  let taskId = null
  let targetOrderIndex = null
  if (event.added) {
    taskId = event.added.element.id
    targetOrderIndex = event.added.newIndex
  } else if (event.moved) {
    taskId = event.moved.element.id
    targetOrderIndex = event.moved.newIndex
  } else {
    return
  }
  try {
    await request.put('/task/drag', { taskId, targetStatus, targetOrderIndex })
    // 将任务拖入“进行中”视为当前登录工程师正式接取任务。
    const draggedTask = event.added?.element || event.moved?.element
    if (targetStatus === 'IN_PROGRESS' && draggedTask && !draggedTask.assigneeId && userStore.userInfo?.userId) {
      await updateTask({ id: taskId, assigneeId: userStore.userInfo.userId })
      draggedTask.assigneeId = userStore.userInfo.userId
    }
    await fetchTasks()
  } catch {
    await fetchTasks()
  }
}

// 创建 / 编辑
function openCreate(defaultStatus) {
  editingTask.value = null
  form.title = ''
  form.description = ''
  form.status = defaultStatus
  form.dueDate = null
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.title.trim()) { ElMessage.warning('请输入任务标题'); return }
  submitting.value = true
  try {
    if (editingTask.value) {
      await updateTask({
        id: editingTask.value.id,
        title: form.title,
        description: form.description,
        status: form.status,
        dueDate: form.dueDate || undefined
      })
      ElMessage.success('任务已更新')
    } else {
      await createTask(projectId, form.title, form.description, undefined, form.dueDate || undefined)
      ElMessage.success('任务已创建')
    }
    dialogVisible.value = false
    await fetchTasks()
  } finally { submitting.value = false }
}

async function handleDelete(task) {
  try {
    await ElMessageBox.confirm('确定删除该任务？将同时删除其所有子任务。', '删除任务', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
    await deleteTask(task.id)
    delete subtaskData[task.id]
    ElMessage.success('已删除')
    await fetchTasks()
  } catch { /* 取消 */ }
}

// AI 项目总结
const summaryDialogVisible = ref(false)
const summaryLoading = ref(false)
const summaryContent = ref('')

const renderedMarkdown = computed(() => {
  return summaryContent.value ? md.render(summaryContent.value) : ''
})

function openSummary() {
  summaryDialogVisible.value = true
  summaryLoading.value = true
  summaryContent.value = ''
  streamProjectSummary(projectId, {
    onChunk(chunk) { summaryContent.value += chunk },
    onDone() { summaryLoading.value = false },
    onError(err) {
      summaryLoading.value = false
      summaryContent.value = summaryContent.value || '生成失败'
      ElMessage.error('AI 总结生成失败: ' + (err.message || '未知错误'))
    }
  })
}

function closeSummary() {
  summaryDialogVisible.value = false
  summaryLoading.value = false
  summaryContent.value = ''
}

// WebSocket 实时同步
const MAX_RETRIES = 5
const BASE_DELAY_MS = 1000
const MAX_DELAY_MS = 30000

const ws = ref(null)
const wsConnected = ref(false)
let retryCount = 0
let reconnectTimer = null

function connectWebSocket() {
  if (ws.value && (ws.value.readyState === WebSocket.OPEN || ws.value.readyState === WebSocket.CONNECTING)) return
  const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
  const token = userStore.token
  const url = `${protocol}://${location.host}/ws/project/${projectId}?token=${encodeURIComponent(token || '')}`
  ws.value = new WebSocket(url)

  ws.value.onopen = () => { wsConnected.value = true; retryCount = 0 }
  ws.value.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data)
      if (msg.type === 'TASK_UPDATED') {
        fetchTasks()
        if (detailVisible.value && selectedTask.value) {
          fetchSubtasksForTask(selectedTask.value.id)
        }
      }
    } catch { /* ignore */ }
  }
  ws.value.onclose = () => {
    wsConnected.value = false
    if (retryCount < MAX_RETRIES) {
      const delay = Math.min(BASE_DELAY_MS * Math.pow(2, retryCount), MAX_DELAY_MS)
      retryCount++
      reconnectTimer = setTimeout(connectWebSocket, delay)
    }
  }
  ws.value.onerror = () => {}
}

function disconnectWebSocket() {
  clearTimeout(reconnectTimer)
  reconnectTimer = null
  if (ws.value) {
    ws.value.onclose = null
    ws.value.onerror = null
    ws.value.onmessage = null
    ws.value.close()
    ws.value = null
  }
  wsConnected.value = false
  retryCount = 0
}

onMounted(() => {
  fetchProjectName()
  fetchTasks()
  fetchMembers()
  connectWebSocket()
})

onUnmounted(() => {
  disconnectWebSocket()
})
</script>

<style scoped>
.board { min-height: 100vh; display: flex; flex-direction: column; }
.topbar {
  display: flex; justify-content: space-between; align-items: center;
  height: 56px; padding: 0 20px; flex-shrink: 0;
  background: #242321; border-bottom: 1px solid #3A3732;
}
.topbar-left { display: flex; align-items: center; gap: 8px; }
.topbar-left h3 { margin: 0; font-size: 16px; font-weight: 600; color: #F7F1E7; }
.sep { color: #81796E; }
.topbar-right { display: flex; align-items: center; gap: 8px; }
.avatar-dot {
  width: 26px; height: 26px; border-radius: 50%;
  background: #D58A22; color: #241D14;
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 600;
}
.username { font-size: 13px; color: #B9B1A5; }

.board-main { flex: 1; overflow-x: auto; padding: 30px; }
.columns { display: flex; gap: 18px; min-width: 780px; height: 100%; max-width: 1280px; margin: 0 auto; }

.column {
  flex: 1; min-width: 260px; max-width: 360px;
  background: #2B2A28; border-radius: var(--radius);
  border: 1px solid #403D38; display: flex; flex-direction: column;
}
.column-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 16px; border-radius: var(--radius) var(--radius) 0 0; flex-shrink: 0;
}
.column-header.todo    { border-bottom: 2px solid #FDE68A; }
.column-header.progress { border-bottom: 2px solid #BFDBFE; }
.column-header.done    { border-bottom: 2px solid #A7F3D0; }
.col-title { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 600; color: #F7F1E7; }
.col-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.col-count {
  font-size: 11px; font-weight: 500; color: var(--text-tertiary);
  background: var(--border); border-radius: 10px; padding: 1px 7px; min-width: 18px; text-align: center;
}

.column-body {
  flex: 1; overflow-y: auto; padding: 8px 10px;
  display: flex; flex-direction: column; gap: 8px; min-height: 60px;
}
.column-empty {
  text-align: center; padding: 24px 12px; color: var(--text-tertiary);
  font-size: 13px; border: 1px dashed var(--border); border-radius: var(--radius-sm);
  margin: 0 10px 12px;
}

.task-card {
  display: flex; gap: 8px;
  background: #1F1F1E; border-radius: var(--radius-sm);
  padding: 14px; cursor: pointer; border: 1px solid #3A3732;
  box-shadow: var(--shadow-xs); transition: box-shadow 0.18s, border-color 0.18s;
}
.task-card:hover { box-shadow: var(--shadow-sm); border-color: var(--brand); }
.done-card .task-title { text-decoration: line-through; color: var(--text-tertiary); }

.card-grip {
  display: flex; flex-direction: column; gap: 2px; padding-top: 3px;
  flex-shrink: 0; opacity: 0.25;
}
.grip-dot {
  display: block; width: 3px; height: 3px; border-radius: 50%;
  background: var(--text-tertiary);
}

.card-content { flex: 1; min-width: 0; }
.task-title { margin: 0; font-size: 14px; font-weight: 500; color: #F7F1E7; line-height: 1.4; word-break: break-word; }
.task-desc {
  margin: 5px 0 0; font-size: 12px; color: var(--text-tertiary); line-height: 1.4;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.task-assignment-row {
  display: flex; align-items: center; gap: 7px; flex-wrap: wrap; margin-top: 9px;
}
.task-assignment-row .role-tag { padding: 2px 7px; border-radius: 4px; font-size: 10px; }
.assignee-name { font-size: 11px; color: #B9B1A5; }
.assignee-name.pending { color: var(--text-tertiary); font-style: italic; }
.task-footer {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: 12px; padding-top: 10px; border-top: 1px solid #3A3732;
}
.task-due { font-size: 11px; color: var(--text-tertiary); }
.task-due.overdue { color: #EF4444; font-weight: 500; }

.footer-actions { display: flex; gap: 6px; align-items: center; }

/* AI 初始任务按钮 */
.ai-init-btn {
  color: #E2A43A !important; border-color: #785A2E !important;
  background: #302A20 !important; font-weight: 600;
}
.ai-init-btn:hover { background: #3B3021 !important; border-color: #D58A22 !important; }
.ai-init-btn.is-disabled { opacity: 0.55; }
.subtask-badge {
  display: flex; align-items: center; gap: 6px; flex: 1; min-width: 0;
}
.subtask-minibar {
  flex: 1; height: 4px; max-width: 72px;
  background: var(--border); border-radius: 2px; overflow: hidden;
}
.subtask-minibar-fill {
  height: 100%; border-radius: 2px;
  background: var(--brand-gradient);
  transition: width 0.35s ease;
}
.subtask-minitext {
  font-size: 11px; font-weight: 500; color: var(--text-tertiary);
  white-space: nowrap; font-variant-numeric: tabular-nums;
}

.ghost {
  opacity: 0.4;
  background: var(--brand-light) !important;
  border: 2px dashed var(--brand) !important;
}

/* 详情弹窗 */
.detail-dialog :deep(.el-dialog__header) { padding-bottom: 0; }
.detail-header { display: flex; align-items: center; gap: 12px; }
.detail-status-tag {
  display: inline-block; padding: 2px 10px; border-radius: 12px;
  font-size: 12px; font-weight: 500; flex-shrink: 0;
}
.tag-todo { background: #FFFBEB; color: #B45309; }
.tag-progress { background: #EFF6FF; color: #1D4ED8; }
.tag-done { background: #ECFDF5; color: #047857; }

.detail-title { margin: 0; font-size: 18px; font-weight: 600; line-height: 1.3; }
.detail-actions { margin-left: auto; display: flex; gap: 6px; flex-shrink: 0; }
.detail-body { padding-top: 4px; }
.detail-desc {
  margin: 0 0 12px; font-size: 14px; color: var(--text-secondary); line-height: 1.6;
  background: #F8FAFC; padding: 12px 16px; border-radius: var(--radius-sm);
  border-left: 3px solid var(--brand);
}
.detail-meta {
  display: flex; align-items: center; gap: 4px;
  margin: 0 0 20px; font-size: 13px; color: var(--text-tertiary);
}
.task-assignment { gap: 10px; }

.subtask-section { border-top: 1px solid var(--border); padding-top: 18px; }
.subtask-section-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;
}
.subtask-section-title { display: flex; align-items: center; gap: 10px; }
.subtask-label { font-size: 14px; font-weight: 600; }
.subtask-summary { font-size: 12px; color: var(--text-tertiary); font-variant-numeric: tabular-nums; }

.subtask-progress-bar { margin-bottom: 14px; }
.progress-track {
  height: 6px; background: var(--border); border-radius: 3px; overflow: hidden;
}
.progress-fill {
  height: 100%; border-radius: 3px;
  background: var(--brand-gradient);
  transition: width 0.4s ease;
}

.subtask-item {
  padding: 10px 12px; border-radius: var(--radius-sm);
  margin-bottom: 4px; transition: background 0.15s;
}
.subtask-item:hover { background: #F8FAFC; }
.subtask-item.done { opacity: 0.65; }
.subtask-item.done .subtask-title { text-decoration: line-through; }
.subtask-item :deep(.el-checkbox) { display: flex; align-items: flex-start; }
.subtask-item :deep(.el-checkbox__label) { line-height: 1.4; }
.subtask-title { font-size: 13px; font-weight: 500; color: var(--text-primary); }
.subtask-desc {
  margin: 4px 0 0 24px; font-size: 12px; color: var(--text-tertiary); line-height: 1.4;
}
.subtask-meta {
  display: flex; align-items: center; gap: 10px; margin: 6px 0 0 24px;
}
.role-tag {
  display: inline-block; padding: 2px 8px; border-radius: 4px;
  font-size: 11px; font-weight: 600; color: #fff; white-space: nowrap;
}
.assignee-info {
  font-size: 12px; color: var(--text-secondary);
}
.assignee-info.pending {
  color: #94A3B8; font-style: italic;
}
.subtask-empty {
  text-align: center; padding: 32px 12px; color: var(--text-tertiary); font-size: 13px;
}

/* 创建/编辑 */
.dialog-form .input-group { margin-bottom: 16px; }
.dialog-form label { display: block; font-size: 13px; font-weight: 500; color: var(--text-secondary); margin-bottom: 6px; }
.optional { font-weight: 400; color: var(--text-tertiary); font-size: 12px; }
.input-row { display: flex; gap: 12px; }
.input-row .input-group { flex: 1; }
.dialog-form :deep(.el-input__wrapper) { border-radius: var(--radius-sm); }

/* AI 总结 */
.summary-body { min-height: 180px; max-height: 60vh; overflow-y: auto; }
.summary-loading {
  display: flex; flex-direction: column; align-items: center; gap: 12px;
  padding: 48px 0; color: var(--text-tertiary); font-size: 14px;
}
.markdown-body {
  color: var(--text-primary); line-height: 1.7; font-size: 14px;
}
.markdown-body :deep(h1) { font-size: 1.5em; margin: 0.6em 0 0.4em; border-bottom: 2px solid var(--border); padding-bottom: 0.3em; }
.markdown-body :deep(h2) { font-size: 1.25em; margin: 0.6em 0 0.35em; border-bottom: 1px solid var(--border); padding-bottom: 0.25em; }
.markdown-body :deep(h3) { font-size: 1.1em; margin: 0.5em 0 0.3em; }
.markdown-body :deep(p) { margin: 0.4em 0; }
.markdown-body :deep(ul), .markdown-body :deep(ol) { padding-left: 1.5em; margin: 0.4em 0; }
.markdown-body :deep(li) { margin: 0.2em 0; }
.markdown-body :deep(code) {
  background: #F1F5F9; padding: 2px 6px; border-radius: 4px;
  font-family: var(--font-mono); font-size: 0.9em;
}
.markdown-body :deep(pre) {
  background: #1E293B; color: #E2E8F0; padding: 14px 16px;
  border-radius: var(--radius-sm); overflow-x: auto; margin: 0.6em 0;
}
.markdown-body :deep(pre code) { background: none; padding: 0; color: inherit; }
.markdown-body :deep(blockquote) {
  border-left: 3px solid var(--brand); padding: 4px 14px; margin: 0.5em 0;
  color: var(--text-secondary); background: #F8FAFC; border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}
.markdown-body :deep(table) { border-collapse: collapse; width: 100%; margin: 0.6em 0; }
.markdown-body :deep(th), .markdown-body :deep(td) { border: 1px solid var(--border); padding: 8px 12px; text-align: left; }
.markdown-body :deep(th) { background: #F8FAFC; font-weight: 600; }
.markdown-body :deep(strong) { font-weight: 600; color: var(--text-primary); }
.markdown-body :deep(hr) { border: none; border-top: 1px solid var(--border); margin: 1em 0; }

</style>
