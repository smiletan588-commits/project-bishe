<template>
  <AppShell :project-id="projectId" :project-name="projectName">
    <main class="management-main" v-loading="loading">
      <PageHeader eyebrow="项目管理" :title="projectName"><template #actions><el-button type="primary" @click="openMilestone()">新建里程碑</el-button></template></PageHeader>
      <section class="summary-grid">
        <div class="summary-card"><span>任务总数</span><strong>{{ tasks.length }}</strong></div>
        <div class="summary-card"><span>进行中</span><strong>{{ tasks.filter(t => t.status === 'IN_PROGRESS').length }}</strong></div>
        <div class="summary-card danger"><span>逾期风险</span><strong>{{ overdueTasks.length }}</strong></div>
        <div class="summary-card"><span>里程碑</span><strong>{{ milestones.length }}</strong></div>
      </section>

      <section class="panel gantt-panel">
        <div class="panel-heading">
          <div><p class="eyebrow">时间线与依赖</p><h2>甘特图</h2></div>
          <p class="panel-note">红色任务已逾期；带“被阻塞”标记的任务需等待前置任务完成。</p>
        </div>
        <div v-if="datedTasks.length" class="gantt-wrap">
          <div class="gantt-head">
            <span>任务</span>
            <div class="gantt-days"><span v-for="day in timelineLabels" :key="day.date">{{ day.label }}</span></div>
          </div>
          <div v-for="task in datedTasks" :key="task.id" class="gantt-row">
            <div class="gantt-task-name">
              <strong>{{ task.title }}</strong>
              <small v-if="task.blocked">被阻塞：{{ (task.blockedByTaskTitles || []).join('、') }}</small>
              <small v-else-if="task.dependencyIds">依赖：{{ dependencyTitles(task).join('、') }}</small>
            </div>
            <div class="gantt-track">
              <div class="gantt-bar" :class="{ done: task.status === 'DONE', overdue: isOverdue(task) }" :style="ganttStyle(task)">
                <span>{{ task.startDate || '未设开始' }} → {{ task.dueDate || '未设截止' }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty">尚无带开始日期或截止日期的任务，请在看板任务详情中补充排期。</div>
      </section>

      <section class="two-column">
        <div class="panel">
          <div class="panel-heading"><div><p class="eyebrow">关键节点</p><h2>项目里程碑</h2></div></div>
          <div v-if="milestones.length" class="milestone-list">
            <article v-for="milestone in milestones" :key="milestone.id" class="milestone-card">
              <div class="milestone-date">{{ milestone.targetDate || '待定' }}</div>
              <div class="milestone-content">
                <div class="milestone-title"><h3>{{ milestone.name }}</h3><span :class="milestone.status === 'COMPLETED' ? 'complete' : 'planned'">{{ milestone.status === 'COMPLETED' ? '已达成' : '进行中' }}</span></div>
                <p v-if="milestone.description">{{ milestone.description }}</p>
                <small>关联 {{ milestoneTasks(milestone).length }} 个任务 / 已完成 {{ milestoneDoneCount(milestone) }} 个</small>
              </div>
              <div class="milestone-actions"><el-button text @click="openMilestone(milestone)">编辑</el-button><el-button text type="danger" @click="removeMilestone(milestone)">删除</el-button></div>
            </article>
          </div>
          <div v-else class="empty">还没有里程碑。可创建“需求评审、Alpha 版本、测试验收、正式上线”等关键节点。</div>
        </div>

        <div class="panel">
          <div class="panel-heading"><div><p class="eyebrow">团队负载</p><h2>工作量统计</h2></div><span class="panel-note">单位：小时</span></div>
          <div class="workload-list">
            <div v-for="person in workload" :key="person.id" class="workload-row">
              <div class="workload-person"><span class="avatar">{{ person.name.slice(0, 1) }}</span><div><strong>{{ person.name }}</strong><small>{{ person.role }} / {{ person.active }} 项进行中</small></div></div>
              <div class="workload-hours"><strong>{{ person.actual || 0 }}</strong><span>/ {{ person.estimated || 0 }}</span><small>实际 / 预计</small></div>
              <div class="workload-meter"><i :style="{ width: workloadPercent(person) + '%' }"></i></div>
            </div>
            <div v-if="!workload.length" class="empty">暂无已分配任务。</div>
          </div>
        </div>
      </section>
    </main>

    <el-dialog v-model="milestoneVisible" :title="editingMilestone ? '编辑里程碑' : '新建里程碑'" width="520px" :close-on-click-modal="false">
      <div class="form-grid">
        <label>名称<el-input v-model="milestoneForm.name" placeholder="例如：Alpha 版本" /></label>
        <label>目标日期<el-date-picker v-model="milestoneForm.targetDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" /></label>
        <label>状态<el-select v-model="milestoneForm.status" style="width:100%"><el-option label="进行中" value="PLANNED" /><el-option label="已达成" value="COMPLETED" /></el-select></label>
        <label class="span-2">说明<el-input v-model="milestoneForm.description" type="textarea" :rows="2" placeholder="本阶段的目标和验收条件" /></label>
        <label class="span-2">关联任务<el-select v-model="milestoneForm.taskIds" multiple filterable placeholder="选择多个任务" style="width:100%"><el-option v-for="task in tasks" :key="task.id" :label="task.title" :value="task.id" /></el-select></label>
      </div>
      <template #footer><el-button @click="milestoneVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveMilestone">保存</el-button></template>
    </el-dialog>
  </AppShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listProjects, listProjectMembers, listMilestones, createMilestone, updateMilestone, deleteMilestone } from '@/api/project'
import { listTasks } from '@/api/task'
import AppShell from '@/components/AppShell.vue'
import PageHeader from '@/components/PageHeader.vue'

const route = useRoute()
const projectId = Number(route.params.id)
const loading = ref(false)
const projectName = ref('项目')
const tasks = ref([])
const members = ref([])
const milestones = ref([])
const milestoneVisible = ref(false)
const saving = ref(false)
const editingMilestone = ref(null)
const milestoneForm = reactive({ name: '', description: '', targetDate: null, status: 'PLANNED', taskIds: [] })

const taskMap = computed(() => Object.fromEntries(tasks.value.map(task => [task.id, task])))
const datedTasks = computed(() => tasks.value.filter(task => task.startDate || task.dueDate))
const overdueTasks = computed(() => tasks.value.filter(isOverdue))
const timelineStart = computed(() => {
  const dates = datedTasks.value.flatMap(task => [task.startDate, task.dueDate]).filter(Boolean).map(parseDate)
  return dates.length ? new Date(Math.min(...dates.map(date => date.getTime()))) : new Date()
})
const timelineEnd = computed(() => {
  const dates = datedTasks.value.flatMap(task => [task.startDate, task.dueDate]).filter(Boolean).map(parseDate)
  const fallback = new Date(timelineStart.value); fallback.setDate(fallback.getDate() + 13)
  return dates.length ? new Date(Math.max(fallback.getTime(), ...dates.map(date => date.getTime()))) : fallback
})
const timelineDays = computed(() => Math.min(60, Math.max(14, dayDiff(timelineStart.value, timelineEnd.value) + 1)))
const timelineLabels = computed(() => Array.from({ length: timelineDays.value }, (_, index) => {
  const date = new Date(timelineStart.value); date.setDate(date.getDate() + index)
  return { date: date.toISOString(), label: index % Math.ceil(timelineDays.value / 7) === 0 ? `${date.getMonth() + 1}/${date.getDate()}` : '' }
}))
const workload = computed(() => {
  const people = new Map(members.value.map(member => [member.userId, { id: member.userId, name: member.nickname || member.username, role: roleLabel(member.identity), estimated: 0, actual: 0, active: 0 }]))
  tasks.value.filter(task => task.assigneeId).forEach(task => {
    const person = people.get(task.assigneeId) || { id: task.assigneeId, name: '项目成员', role: '未设置岗位', estimated: 0, actual: 0, active: 0 }
    person.estimated += task.estimatedHours || 0
    person.actual += task.actualHours || 0
    if (task.status === 'IN_PROGRESS') person.active++
    people.set(task.assigneeId, person)
  })
  return [...people.values()].filter(person => person.estimated || person.actual || person.active)
})

function parseDate(value) { return new Date(`${value}T00:00:00`) }
function dayDiff(from, to) { return Math.round((parseDate(to.toISOString().slice(0, 10)) - parseDate(from.toISOString().slice(0, 10))) / 86400000) }
function isOverdue(task) { return task.status !== 'DONE' && task.dueDate && parseDate(task.dueDate) < new Date(new Date().toDateString()) }
function ganttStyle(task) {
  const start = task.startDate ? parseDate(task.startDate) : parseDate(task.dueDate)
  const end = task.dueDate ? parseDate(task.dueDate) : start
  const offset = Math.max(0, dayDiff(timelineStart.value, start))
  const duration = Math.max(1, dayDiff(start, end) + 1)
  return { left: `${Math.min(96, offset / timelineDays.value * 100)}%`, width: `${Math.max(4, Math.min(100, duration / timelineDays.value * 100))}%` }
}
function dependencyTitles(task) { return String(task.dependencyIds || '').split(',').filter(Boolean).map(id => taskMap.value[id]?.title || `任务 #${id}`) }
function milestoneTasks(milestone) { return String(milestone.taskIds || '').split(',').filter(Boolean).map(id => taskMap.value[id]).filter(Boolean) }
function milestoneDoneCount(milestone) { return milestoneTasks(milestone).filter(task => task.status === 'DONE').length }
function workloadPercent(person) { return Math.min(100, person.estimated ? Math.round(person.actual / person.estimated * 100) : 0) }
function roleLabel(role) { return ({ PROJECT_MANAGER: '项目经理', FRONTEND_DEV: '前端', BACKEND_DEV: '后端', QA_TESTER: '测试', UI_DESIGNER: 'UI 设计' })[role] || '未设置岗位' }

async function load() {
  loading.value = true
  try {
    const [projectRes, taskRes, memberRes, milestoneRes] = await Promise.all([listProjects(), listTasks(projectId), listProjectMembers(projectId), listMilestones(projectId)])
    projectName.value = projectRes.data.data.find(project => project.id === projectId)?.name || '项目'
    tasks.value = taskRes.data.data || []
    members.value = memberRes.data.data || []
    milestones.value = milestoneRes.data.data || []
  } finally { loading.value = false }
}
function openMilestone(milestone) {
  editingMilestone.value = milestone || null
  Object.assign(milestoneForm, milestone ? { name: milestone.name, description: milestone.description || '', targetDate: milestone.targetDate, status: milestone.status, taskIds: String(milestone.taskIds || '').split(',').filter(Boolean).map(Number) } : { name: '', description: '', targetDate: null, status: 'PLANNED', taskIds: [] })
  milestoneVisible.value = true
}
async function saveMilestone() {
  if (!milestoneForm.name.trim()) return ElMessage.warning('请输入里程碑名称')
  saving.value = true
  try {
    const payload = { ...(editingMilestone.value ? { id: editingMilestone.value.id } : {}), ...milestoneForm, taskIds: milestoneForm.taskIds.join(',') }
    if (editingMilestone.value) await updateMilestone(projectId, payload)
    else await createMilestone(projectId, payload)
    milestoneVisible.value = false; ElMessage.success('里程碑已保存'); await load()
  } finally { saving.value = false }
}
async function removeMilestone(milestone) {
  try {
    await ElMessageBox.confirm(`确认删除“${milestone.name}”？`, '删除里程碑', { type: 'warning' })
    await deleteMilestone(projectId, milestone.id); ElMessage.success('已删除'); await load()
  } catch { /* 取消 */ }
}
onMounted(load)
</script>

<style scoped media="not all">
.management-page { min-height: 100vh; color: var(--text-primary); }
.topbar { height: 58px; display:flex; align-items:center; justify-content:space-between; padding:0 28px; border-bottom:1px solid var(--border-light); background:rgba(255,255,255,.82); }
.topbar-left { display:flex; align-items:center; gap:10px; }.topbar h3 { margin:0; font-size:16px; }.sep { color:var(--text-tertiary); }.management-main { max-width:1280px; margin:0 auto; padding:30px; }
.summary-grid { display:grid; grid-template-columns:repeat(4,1fr); gap:16px; margin-bottom:20px; }.summary-card,.panel { background:rgba(255,255,255,.92); border:1px solid var(--border-light); border-radius:14px; box-shadow:0 8px 24px rgba(15,23,42,.04); }.summary-card { padding:18px 20px; }.summary-card span,.summary-card small { color:var(--text-tertiary); font-size:12px; }.summary-card strong { display:block; margin-top:8px; font-size:28px; }.summary-card.danger strong { color:#dc5a3d; }
.panel { padding:22px; }.panel-heading { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:18px; }.eyebrow { margin:0 0 4px; color:#b47725; font-size:11px; font-weight:700; letter-spacing:.08em; text-transform:uppercase; }.panel h2 { margin:0; font-size:18px; }.panel-note { margin:5px 0 0; color:var(--text-tertiary); font-size:12px; }.gantt-wrap { overflow-x:auto; }.gantt-head,.gantt-row { display:grid; grid-template-columns:220px minmax(620px,1fr); gap:14px; }.gantt-head { color:var(--text-tertiary); font-size:12px; border-bottom:1px solid var(--border-light); padding-bottom:8px; }.gantt-days { display:grid; grid-template-columns:repeat(14,1fr); }.gantt-row { min-height:58px; align-items:center; border-bottom:1px solid rgba(148,163,184,.13); }.gantt-task-name { min-width:0; }.gantt-task-name strong { display:block; overflow:hidden; white-space:nowrap; text-overflow:ellipsis; font-size:13px; }.gantt-task-name small { color:#b45309; font-size:11px; }.gantt-track { position:relative; height:28px; border-radius:7px; background:repeating-linear-gradient(90deg,transparent,transparent calc(7.14% - 1px),rgba(148,163,184,.15) calc(7.14% - 1px),rgba(148,163,184,.15) 7.14%); }.gantt-bar { position:absolute; top:4px; min-width:34px; height:20px; border-radius:5px; padding:2px 6px; box-sizing:border-box; overflow:hidden; white-space:nowrap; color:white; background:#b47725; font-size:10px; }.gantt-bar.done { background:#4f8a6b; }.gantt-bar.overdue { background:#d55a45; }.two-column { display:grid; grid-template-columns:1.1fr .9fr; gap:20px; margin-top:20px; }.milestone-card { display:flex; gap:14px; padding:14px 0; border-bottom:1px solid var(--border-light); }.milestone-date { min-width:72px; color:#b47725; font-weight:650; font-size:12px; }.milestone-content { flex:1; }.milestone-title { display:flex; gap:8px; align-items:center; }.milestone-title h3 { margin:0; font-size:14px; }.milestone-title span { padding:2px 7px; border-radius:9px; font-size:10px; }.planned { background:#fdf1db; color:#a66711; }.complete { background:#e4f3e9; color:#287248; }.milestone-content p { margin:6px 0; color:var(--text-secondary); font-size:12px; }.milestone-content small { color:var(--text-tertiary); }.milestone-actions { display:flex; }.workload-row { display:grid; grid-template-columns:1fr auto; gap:8px 12px; padding:12px 0; border-bottom:1px solid var(--border-light); }.workload-person { display:flex; align-items:center; gap:9px; }.avatar { width:30px; height:30px; display:grid; place-items:center; border-radius:50%; background:#f3e8d3; color:#9a631d; font-size:13px; }.workload-person strong,.workload-hours strong { font-size:13px; }.workload-person small,.workload-hours small { display:block; color:var(--text-tertiary); font-size:11px; }.workload-hours { text-align:right; }.workload-hours span { color:var(--text-tertiary); font-size:12px; }.workload-meter { grid-column:1 / -1; height:4px; border-radius:3px; overflow:hidden; background:#edf0f4; }.workload-meter i { display:block; height:100%; background:#b47725; }.empty { padding:28px 8px; color:var(--text-tertiary); text-align:center; font-size:13px; }.form-grid { display:grid; grid-template-columns:1fr 1fr; gap:14px; }.form-grid label { display:block; color:var(--text-secondary); font-size:12px; }.form-grid :deep(.el-input),.form-grid :deep(.el-select),.form-grid :deep(.el-date-editor) { margin-top:6px; }.span-2 { grid-column:span 2; }
@media (max-width:800px) { .management-main{padding:16px}.summary-grid,.two-column{grid-template-columns:1fr 1fr}.two-column{display:block}.two-column .panel+ .panel{margin-top:16px}.gantt-head,.gantt-row{grid-template-columns:130px minmax(620px,1fr)} }
</style>

<style scoped>
.management-main { max-width:1280px; margin:0 auto; padding:34px 36px 52px; color:var(--text-primary); }
.summary-grid { display:grid; grid-template-columns:repeat(4,1fr); margin:24px 0 18px; border:1px solid var(--border); border-radius:12px; background:var(--surface); box-shadow:var(--shadow-xs); }
.summary-card { min-width:0; padding:19px 22px; }.summary-card + .summary-card { border-left:1px solid var(--border-light); }.summary-card span { color:var(--text-tertiary); font-size:12px; font-weight:650; }.summary-card strong { display:block; margin-top:7px; font-size:29px; line-height:1; font-variant-numeric:tabular-nums; }.summary-card.danger strong { color:var(--danger); }
.panel { min-width:0; padding:22px; border:1px solid var(--border); border-radius:12px; background:var(--surface); box-shadow:var(--shadow-xs); }.panel-heading { display:flex; justify-content:space-between; align-items:flex-start; gap:16px; margin-bottom:18px; }.eyebrow { margin:0 0 4px; color:var(--brand); font-size:11px; font-weight:700; }.panel h2 { margin:0; font-size:17px; }.panel-note { margin:4px 0 0; color:var(--text-tertiary); font-size:12px; }
.gantt-wrap { max-width:100%; overflow-x:auto; overscroll-behavior-inline:contain; }.gantt-head,.gantt-row { display:grid; grid-template-columns:220px minmax(680px,1fr); gap:14px; min-width:914px; }.gantt-head { position:sticky; top:0; z-index:2; padding-bottom:8px; border-bottom:1px solid var(--border-light); color:var(--text-tertiary); background:var(--surface); font-size:12px; }.gantt-days { display:grid; grid-template-columns:repeat(14,1fr); }.gantt-row { min-height:58px; align-items:center; border-bottom:1px solid var(--border-light); }.gantt-task-name { position:sticky; left:0; z-index:1; min-width:0; padding-right:8px; background:var(--surface); }.gantt-task-name strong { display:block; overflow:hidden; font-size:13px; text-overflow:ellipsis; white-space:nowrap; }.gantt-task-name small { display:block; overflow:hidden; color:var(--warning); font-size:11px; text-overflow:ellipsis; white-space:nowrap; }.gantt-track { position:relative; height:28px; border-radius:7px; background:repeating-linear-gradient(90deg,transparent,transparent calc(7.14% - 1px),var(--border-light) calc(7.14% - 1px),var(--border-light) 7.14%); }.gantt-bar { position:absolute; top:4px; box-sizing:border-box; min-width:34px; height:20px; overflow:hidden; padding:2px 6px; border-radius:5px; color:white; background:var(--brand); font-size:10px; white-space:nowrap; }.gantt-bar.done { background:var(--success); }.gantt-bar.overdue { background:var(--danger); }
.two-column { display:grid; grid-template-columns:minmax(0,1.1fr) minmax(0,.9fr); gap:18px; margin-top:18px; }.milestone-card { display:flex; gap:14px; padding:14px 0; border-bottom:1px solid var(--border-light); }.milestone-date { min-width:72px; color:var(--brand-deep); font-size:12px; font-weight:650; }.milestone-content { min-width:0; flex:1; }.milestone-title { display:flex; align-items:center; gap:8px; }.milestone-title h3 { margin:0; font-size:14px; }.milestone-title span { padding:2px 7px; border-radius:999px; font-size:10px; }.planned { background:var(--warning-soft); color:var(--warning); }.complete { background:var(--success-soft); color:var(--success); }.milestone-content p { margin:6px 0; color:var(--text-secondary); font-size:12px; }.milestone-content small { color:var(--text-tertiary); }.milestone-actions { display:flex; }
.workload-row { display:grid; grid-template-columns:1fr auto; gap:8px 12px; padding:12px 0; border-bottom:1px solid var(--border-light); }.workload-person { display:flex; align-items:center; gap:9px; }.avatar { display:grid; place-items:center; width:30px; height:30px; border-radius:50%; color:var(--brand-deep); background:var(--brand-light); font-size:13px; }.workload-person strong,.workload-hours strong { font-size:13px; }.workload-person small,.workload-hours small { display:block; color:var(--text-tertiary); font-size:11px; }.workload-hours { text-align:right; }.workload-hours span { color:var(--text-tertiary); font-size:12px; }.workload-meter { grid-column:1 / -1; height:4px; overflow:hidden; border-radius:3px; background:var(--surface-strong); }.workload-meter i { display:block; height:100%; background:var(--brand); }.empty { padding:28px 8px; color:var(--text-tertiary); text-align:center; font-size:13px; }.form-grid { display:grid; grid-template-columns:1fr 1fr; gap:14px; }.form-grid label { display:block; color:var(--text-secondary); font-size:12px; }.form-grid :deep(.el-input),.form-grid :deep(.el-select),.form-grid :deep(.el-date-editor) { margin-top:6px; }.span-2 { grid-column:span 2; }
@media (max-width:1024px) { .management-main { padding:28px 24px 44px; }.two-column { grid-template-columns:1fr; } }
@media (max-width:767px) { .management-main { max-width:100vw; padding:22px 16px 36px; overflow:hidden; }.summary-grid { grid-template-columns:1fr 1fr; }.summary-card:nth-child(3) { border-left:0; border-top:1px solid var(--border-light); }.summary-card:nth-child(4) { border-top:1px solid var(--border-light); }.summary-card { padding:16px; }.summary-card strong { font-size:25px; }.panel { padding:16px; }.panel-heading { flex-direction:column; }.gantt-wrap { margin-inline:-16px; padding-inline:16px; }.gantt-head,.gantt-row { grid-template-columns:140px minmax(620px,1fr); min-width:774px; }.milestone-card { flex-wrap:wrap; }.milestone-actions { width:100%; justify-content:flex-end; }.form-grid { grid-template-columns:1fr; }.span-2 { grid-column:auto; } }
</style>
