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
        <el-button size="small" @click="$router.push(`/project/${projectId}/manage`)">项目管理</el-button>
        <el-button class="ai-init-btn" size="small" :loading="planLoading" :disabled="hasTasks" @click="generateProjectPlan">
          ✨ AI 完整计划
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
      <div class="board-filters">
        <el-select v-model="filters.assigneeId" clearable placeholder="全部负责人" size="small">
          <el-option v-for="member in projectMembers" :key="member.userId" :label="member.nickname || member.username" :value="member.userId" />
        </el-select>
        <el-select v-model="filters.role" clearable placeholder="全部角色" size="small">
          <el-option v-for="role in projectRoleOptions" :key="role.value" :label="role.label" :value="role.value" />
        </el-select>
        <el-select v-model="filters.tag" clearable placeholder="全部标签" size="small">
          <el-option v-for="tag in tagOptions" :key="tag.value" :label="tag.label" :value="tag.value" />
        </el-select>
        <el-button text size="small" @click="clearFilters">清除筛选</el-button>
      </div>
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
                  <div class="task-meta-chips">
                    <span class="priority-chip" :class="`priority-${element.priority || 'MEDIUM'}`">{{ priorityLabel(element.priority) }}</span>
                    <span v-for="tag in taskTags(element)" :key="tag" class="task-label">{{ tagLabel(tag) }}</span>
                    <span v-if="element.blocked" class="blocked-chip">被阻塞</span>
                  </div>
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
                  <div class="task-meta-chips">
                    <span class="priority-chip" :class="`priority-${element.priority || 'MEDIUM'}`">{{ priorityLabel(element.priority) }}</span>
                    <span v-for="tag in taskTags(element)" :key="tag" class="task-label">{{ tagLabel(tag) }}</span>
                    <span v-if="element.blocked" class="blocked-chip">被阻塞</span>
                  </div>
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
                  <div class="task-meta-chips">
                    <span class="priority-chip" :class="`priority-${element.priority || 'MEDIUM'}`">{{ priorityLabel(element.priority) }}</span>
                    <span v-for="tag in taskTags(element)" :key="tag" class="task-label">{{ tagLabel(tag) }}</span>
                    <span v-if="element.blocked" class="blocked-chip">被阻塞</span>
                  </div>
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
            <el-button size="small" @click="openEdit(selectedTask)">编辑</el-button>
            <el-button size="small" :loading="optimizationLoading" @click="optimizeTask">AI 优化</el-button>
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
        <div class="detail-meta detail-task-fields">
          <span class="priority-chip" :class="`priority-${selectedTask.priority || 'MEDIUM'}`">{{ priorityLabel(selectedTask.priority) }}</span>
          <span v-for="tag in taskTags(selectedTask)" :key="tag" class="task-label">{{ tagLabel(tag) }}</span>
          <span v-if="selectedTask.startDate">开始：{{ selectedTask.startDate }}</span>
          <span v-if="selectedTask.estimatedHours !== null && selectedTask.estimatedHours !== undefined">预计：{{ selectedTask.estimatedHours }}h</span>
          <span v-if="selectedTask.actualHours !== null && selectedTask.actualHours !== undefined">实际：{{ selectedTask.actualHours }}h</span>
        </div>
        <div v-if="selectedTask.blocked" class="blocked-notice">此任务被前置任务阻塞：{{ (selectedTask.blockedByTaskTitles || []).join('、') }}</div>
        <div v-else-if="dependencyTitles(selectedTask).length" class="detail-meta">前置依赖：{{ dependencyTitles(selectedTask).join('、') }}</div>
        <div v-if="selectedTask.acceptanceCriteria" class="acceptance-section">
          <strong>验收标准</strong><p>{{ selectedTask.acceptanceCriteria }}</p>
        </div>
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
        <div class="attachment-section">
          <div class="subtask-section-header">
            <span class="subtask-label">任务附件</span>
            <el-button size="small" @click="attachmentInput?.click()">上传附件</el-button>
            <input ref="attachmentInput" class="hidden-file-input" type="file" accept="image/*,.pdf,.zip,.rar,.7z" @change="handleAttachmentUpload" />
          </div>
          <p class="attachment-tip">支持图片、PDF、ZIP / RAR / 7Z，单个文件不超过 20MB。</p>
          <div v-for="attachment in attachments" :key="attachment.id" class="attachment-row">
            <span>📎 {{ attachment.originalName }}</span><small>{{ formatFileSize(attachment.size) }}</small>
            <div><el-button text size="small" @click="downloadAttachment(attachment)">下载</el-button><el-button text size="small" @click="showDownloadLogs(attachment)">记录</el-button><el-button text type="danger" size="small" @click="removeAttachment(attachment)">删除</el-button></div>
          </div>
          <p v-if="!attachments.length" class="attachment-tip">暂无附件</p>
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
            <label>优先级</label>
            <el-select v-model="form.priority" size="large" style="width:100%">
              <el-option label="高优先级" value="HIGH" /><el-option label="中优先级" value="MEDIUM" /><el-option label="低优先级" value="LOW" />
            </el-select>
          </div>
        </div>
        <div class="input-row">
          <div class="input-group">
            <label>开始日期 <span class="optional">选填</span></label>
            <el-date-picker v-model="form.startDate" type="date" placeholder="选择日期"
              value-format="YYYY-MM-DD" size="large" style="width:100%" />
          </div>
          <div class="input-group">
            <label>截止日期 <span class="optional">选填</span></label>
            <el-date-picker v-model="form.dueDate" type="date" placeholder="选择日期"
              value-format="YYYY-MM-DD" size="large" style="width:100%" />
          </div>
        </div>
        <div class="input-row">
          <div class="input-group"><label>预计工时（小时）</label><el-input-number v-model="form.estimatedHours" :min="0" :max="10000" style="width:100%" /></div>
          <div class="input-group"><label>实际工时（小时）</label><el-input-number v-model="form.actualHours" :min="0" :max="10000" style="width:100%" /></div>
        </div>
        <div class="input-group">
          <label>标签 <span class="optional">可多选</span></label>
          <el-select v-model="form.tags" multiple collapse-tags placeholder="选择任务标签" style="width:100%"><el-option v-for="tag in tagOptions" :key="tag.value" :label="tag.label" :value="tag.value" /></el-select>
        </div>
        <div class="input-group">
          <label>前置依赖 <span class="optional">完成后才能开始本任务</span></label>
          <el-select v-model="form.dependencyIds" multiple filterable collapse-tags placeholder="选择前置任务" style="width:100%"><el-option v-for="task in dependencyCandidates" :key="task.id" :label="task.title" :value="task.id" /></el-select>
        </div>
        <div class="input-group">
          <label>验收标准 <span class="optional">选填</span></label>
          <el-input v-model="form.acceptanceCriteria" type="textarea" :rows="2" placeholder="任务完成后应满足哪些可验证条件" />
        </div>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ editingTask ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="projectRoleDialogVisible" width="520px" class="project-role-dialog"
      :show-close="false" :close-on-click-modal="false" :close-on-press-escape="false">
      <template #header>
        <div class="project-role-header">
          <span class="project-role-icon">👋</span>
          <h3>欢迎加入 {{ projectName }}</h3>
          <p>请选择你在这个项目中的岗位，方便团队分配任务。</p>
        </div>
      </template>
      <div class="project-role-grid">
        <button v-for="role in projectRoleOptions" :key="role.value" class="project-role-card"
          :disabled="savingProjectRole" @click="selectProjectRole(role.value)">
          <span>{{ role.emoji }}</span><strong>{{ role.label }}</strong>
        </button>
      </div>
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

    <el-dialog v-model="planVisible" title="AI 完整项目计划" width="820px" :close-on-click-modal="false">
      <div v-if="projectPlan" class="ai-plan-preview">
        <p class="plan-overview">{{ projectPlan.overview }}</p>
        <section><h4>项目阶段</h4><div class="plan-stage-list"><span v-for="stage in projectPlan.stages" :key="stage.name">{{ stage.name }} · {{ stage.startDate }} 至 {{ stage.endDate }}</span></div></section>
        <section><h4>计划任务（{{ projectPlan.tasks?.length || 0 }}）</h4><article v-for="(task, index) in projectPlan.tasks" :key="`${task.title}-${index}`" class="plan-task"><strong>{{ index + 1 }}. {{ task.title }}</strong><span>{{ roleLabel(task.recommendedRole) }} · {{ priorityLabel(task.priority) }} · {{ task.estimatedHours }}h</span><p>{{ task.description }}</p><small>验收：{{ task.acceptanceCriteria }}</small></article></section>
        <section v-if="projectPlan.milestones?.length"><h4>里程碑</h4><div class="plan-stage-list"><span v-for="milestone in projectPlan.milestones" :key="milestone.name">{{ milestone.name }} · {{ milestone.targetDate }}</span></div></section>
        <section v-if="projectPlan.risks?.length"><h4>风险清单</h4><article v-for="risk in projectPlan.risks" :key="risk.title" class="plan-risk"><strong>{{ risk.level }} · {{ risk.title }}</strong><p>{{ risk.description }}</p><small>建议：{{ risk.mitigation }}</small></article></section>
      </div>
      <template #footer><el-button @click="planVisible = false">取消</el-button><el-button type="primary" :loading="planApplying" @click="applyProjectPlan">确认创建任务与里程碑</el-button></template>
    </el-dialog>

    <el-dialog v-model="optimizationVisible" title="AI 任务优化建议" width="620px" :close-on-click-modal="false">
      <div v-if="optimization" class="optimization-preview">
        <label>优化标题</label><strong>{{ optimization.title }}</strong>
        <label>任务描述</label><p>{{ optimization.description }}</p>
        <label>验收标准</label><p>{{ optimization.acceptanceCriteria }}</p>
        <div v-if="optimization.oversized" class="blocked-notice"><strong>建议拆分：</strong>{{ optimization.splitAdvice }}</div>
        <p v-else class="optimization-ok">该任务粒度适中，可直接执行。</p>
      </div>
      <template #footer><el-button @click="optimizationVisible = false">保留原任务</el-button><el-button type="primary" @click="applyOptimization">应用优化内容</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Delete, Loading, Calendar, Document, VideoPlay, CircleCheck } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import MarkdownIt from 'markdown-it'
import { useUserStore } from '@/store/user'
import { listProjects, listProjectMembers, updateMyProjectIdentity, generateAiProjectPlan, applyAiProjectPlan } from '@/api/project'
import { listTasks, createTask, updateTask, deleteTask, decomposeTask, listSubtasks, toggleSubtask, initProjectTasks, listTaskAttachments, uploadTaskAttachment, deleteTaskAttachment, listAttachmentDownloadLogs, optimizeTaskWithAi } from '@/api/task'
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
const allTasks = ref([])
const filters = reactive({ assigneeId: null, role: '', tag: '' })

// 项目成员：userId -> { nickname, identity }
const memberMap = ref({})
const projectMembers = ref([])
const projectRoleDialogVisible = ref(false)
const savingProjectRole = ref(false)

const projectRoleOptions = [
  { value: 'PROJECT_MANAGER', label: '项目经理', emoji: '📋' },
  { value: 'FRONTEND_DEV', label: '前端工程师', emoji: '💻' },
  { value: 'BACKEND_DEV', label: '后端工程师', emoji: '☕' },
  { value: 'QA_TESTER', label: '测试工程师', emoji: '🧪' },
  { value: 'UI_DESIGNER', label: 'UI 设计师', emoji: '🎨' }
]

const roleConfig = {
  PROJECT_MANAGER: { label: '项目经理', color: '#D58A22' },
  FRONTEND_DEV:   { label: '前端',     color: '#68839A' },
  BACKEND_DEV:    { label: '后端',     color: '#6F7A57' },
  QA_TESTER:      { label: '测试',     color: '#B46F4A' },
  UI_DESIGNER:    { label: 'UI设计',   color: '#EC4899' }
}

const tagOptions = [
  { value: 'BUG', label: 'Bug' }, { value: 'REQUIREMENT', label: '需求' },
  { value: 'DESIGN', label: '设计' }, { value: 'DEVELOPMENT', label: '开发' },
  { value: 'TESTING', label: '测试' }, { value: 'DOCUMENTATION', label: '文档' }
]
const priorityConfig = { HIGH: '高优先级', MEDIUM: '中优先级', LOW: '低优先级' }

const hasTasks = computed(() =>
  allTasks.value.length > 0
)
const dependencyCandidates = computed(() => allTasks.value.filter(task => task.id !== editingTask.value?.id))

function assigneeName(task) {
  if (!task?.assigneeId) return ''
  return memberMap.value[task.assigneeId]?.nickname ||
    (task.assigneeId === userStore.userInfo?.userId ? userStore.userInfo?.nickname || userStore.userInfo?.username : '')
}
function priorityLabel(priority) { return priorityConfig[priority || 'MEDIUM'] || '中优先级' }
function roleLabel(role) { return roleConfig[role]?.label || role || '待分配' }
function tagLabel(tag) { return tagOptions.find(option => option.value === tag)?.label || tag }
function taskTags(task) { return String(task?.tags || '').split(',').filter(Boolean) }
function dependencyTitles(task) {
  return String(task?.dependencyIds || '').split(',').filter(Boolean)
    .map(id => allTasks.value.find(candidate => candidate.id === Number(id))?.title || `任务 #${id}`)
}
function clearFilters() { Object.assign(filters, { assigneeId: null, role: '', tag: '' }) }

async function fetchMembers() {
  try {
    const res = await listProjectMembers(projectId)
    const members = res.data.data || []
    projectMembers.value = members
    const map = {}
    members.forEach(m => {
      map[m.userId] = { nickname: m.nickname || m.username, identity: m.identity }
    })
    memberMap.value = map
    applyFilters()
    const currentMember = members.find(m => m.userId === userStore.userInfo?.userId)
    if (route.query.setupRole === '1' && !currentMember?.identity) {
      projectRoleDialogVisible.value = true
    }
  } catch { /* ignore */ }
}

async function selectProjectRole(identity) {
  if (savingProjectRole.value) return
  savingProjectRole.value = true
  try {
    await updateMyProjectIdentity(projectId, identity)
    await userStore.updateIdentity(identity)
    projectRoleDialogVisible.value = false
    ElMessage.success('项目岗位已设置')
    await fetchMembers()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '岗位设置失败，请重试')
  } finally {
    savingProjectRole.value = false
  }
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
const form = reactive({ title: '', description: '', status: 'TODO', dueDate: null, startDate: null, priority: 'MEDIUM', tags: [], dependencyIds: [], estimatedHours: null, actualHours: null, acceptanceCriteria: '' })

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
    allTasks.value = res.data.data || []
    applyFilters()
    fetchAllSubtaskCounts(allTasks.value)
  } catch { /* handled by interceptor */ }
}

function applyFilters() {
  const matches = task => {
    if (filters.assigneeId && task.assigneeId !== filters.assigneeId) return false
    const assigneeRole = task.assigneeId ? memberMap.value[task.assigneeId]?.identity : null
    if (filters.role && task.recommendedRole !== filters.role && assigneeRole !== filters.role) return false
    if (filters.tag && !taskTags(task).includes(filters.tag)) return false
    return true
  }
  const filtered = allTasks.value.filter(matches)
  todoList.value = filtered.filter(task => task.status === 'TODO').sort((a, b) => (a.orderIndex || 0) - (b.orderIndex || 0))
  inProgressList.value = filtered.filter(task => task.status === 'IN_PROGRESS').sort((a, b) => (a.orderIndex || 0) - (b.orderIndex || 0))
  doneList.value = filtered.filter(task => task.status === 'DONE').sort((a, b) => (a.orderIndex || 0) - (b.orderIndex || 0))
}
watch(filters, applyFilters, { deep: true })

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
  await fetchAttachments(task.id)
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
const planLoading = ref(false)
const planApplying = ref(false)
const planVisible = ref(false)
const projectPlan = ref(null)

async function handleInitTasks() {
  if (hasTasks.value || initTasksLoading.value) return
  initTasksLoading.value = true
  try {
    await initProjectTasks(projectId)
    ElMessage.success('AI 已根据项目目标生成开发任务，请工程师接取后拖入进行中')
    await fetchTasks()
  } catch (error) {
    console.error('[TaskList] AI 生成任务失败:', error)
    ElMessage.error(error.response?.data?.msg || error.message || 'AI 生成任务失败，请重试')
  } finally {
    initTasksLoading.value = false
  }
}

async function generateProjectPlan() {
  if (hasTasks.value || planLoading.value) return
  planLoading.value = true
  try {
    const response = await generateAiProjectPlan(projectId)
    projectPlan.value = response.data.data
    planVisible.value = true
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || error.message || 'AI 项目计划生成失败，请重试')
  } finally { planLoading.value = false }
}

async function applyProjectPlan() {
  if (!projectPlan.value || planApplying.value) return
  planApplying.value = true
  try {
    const response = await applyAiProjectPlan(projectId, projectPlan.value)
    planVisible.value = false
    ElMessage.success(`已创建 ${response.data.data?.length || 0} 个任务和相关里程碑`)
    await fetchTasks()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || error.message || '应用项目计划失败，请重试')
  } finally { planApplying.value = false }
}

// AI 拆解
const decomposingTaskId = ref(null)

async function handleDecompose(task) {
  decomposingTaskId.value = task.id
  try {
    await decomposeTask(task.id)
    ElMessage.success('AI 已拆解子任务')
    await fetchSubtasksForTask(task.id)
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || error.message || 'AI 拆解失败，请重试')
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
  form.startDate = null
  form.priority = 'MEDIUM'
  form.tags = []
  form.dependencyIds = []
  form.estimatedHours = null
  form.actualHours = null
  form.acceptanceCriteria = ''
  dialogVisible.value = true
}

function openEdit(task) {
  editingTask.value = task
  form.title = task.title || ''
  form.description = task.description || ''
  form.status = task.status || 'TODO'
  form.dueDate = task.dueDate || null
  form.startDate = task.startDate || null
  form.priority = task.priority || 'MEDIUM'
  form.tags = taskTags(task)
  form.dependencyIds = String(task.dependencyIds || '').split(',').filter(Boolean).map(Number)
  form.estimatedHours = task.estimatedHours ?? null
  form.actualHours = task.actualHours ?? null
  form.acceptanceCriteria = task.acceptanceCriteria || ''
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
        dueDate: form.dueDate || '',
        startDate: form.startDate || '',
        priority: form.priority,
        tags: form.tags.join(','),
        dependencyIds: form.dependencyIds.join(','),
        estimatedHours: form.estimatedHours,
        actualHours: form.actualHours,
        acceptanceCriteria: form.acceptanceCriteria
      })
      ElMessage.success('任务已更新')
    } else {
      await createTask(projectId, form.title, form.description, undefined, form.dueDate || undefined,
        form.startDate || undefined, form.priority, form.tags.join(','), form.dependencyIds.join(','),
        form.estimatedHours ?? undefined, form.actualHours ?? undefined, form.acceptanceCriteria || undefined)
      ElMessage.success('任务已创建')
    }
    dialogVisible.value = false
    await fetchTasks()
  } finally { submitting.value = false }
}

const optimizationVisible = ref(false)
const optimizationLoading = ref(false)
const optimization = ref(null)
async function optimizeTask() {
  if (!selectedTask.value || optimizationLoading.value) return
  optimizationLoading.value = true
  try {
    const response = await optimizeTaskWithAi(selectedTask.value.id)
    optimization.value = response.data.data
    optimizationVisible.value = true
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || error.message || 'AI 任务优化失败，请重试')
  } finally { optimizationLoading.value = false }
}
async function applyOptimization() {
  if (!selectedTask.value || !optimization.value) return
  try {
    await updateTask({ id: selectedTask.value.id, title: optimization.value.title, description: optimization.value.description, acceptanceCriteria: optimization.value.acceptanceCriteria })
    Object.assign(selectedTask.value, optimization.value)
    optimizationVisible.value = false
    ElMessage.success('已应用 AI 优化内容')
    await fetchTasks()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || error.message || '应用优化失败，请重试')
  }
}

// 附件：下载由带授权头的请求完成，确保只有项目成员能访问。
const attachments = ref([])
const attachmentInput = ref(null)
async function fetchAttachments(taskId) {
  try { attachments.value = (await listTaskAttachments(taskId)).data.data || [] }
  catch { attachments.value = [] }
}
async function handleAttachmentUpload(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file || !selectedTask.value) return
  try {
    await uploadTaskAttachment(selectedTask.value.id, file)
    ElMessage.success('附件已上传')
    await fetchAttachments(selectedTask.value.id)
  } catch { /* 已由请求拦截器提示 */ }
}
async function downloadAttachment(attachment) {
  try {
    const response = await request.get(`/task/attachments/${attachment.id}/download`, { responseType: 'blob' })
    const url = URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a'); link.href = url; link.download = attachment.originalName; link.click()
    URL.revokeObjectURL(url)
  } catch { /* 已由请求拦截器提示 */ }
}
async function removeAttachment(attachment) {
  try {
    await ElMessageBox.confirm(`确认将附件“${attachment.originalName}”移入回收站？`, '移入回收站', { type: 'warning' })
    await deleteTaskAttachment(attachment.id)
    ElMessage.success('附件已移入回收站')
    await fetchAttachments(selectedTask.value.id)
  } catch { /* 取消 */ }
}
async function showDownloadLogs(attachment) {
  try {
    const logs = (await listAttachmentDownloadLogs(attachment.id)).data.data || []
    const content = logs.length ? logs.map(log => `${memberMap.value[log.downloaderId]?.nickname || `成员 #${log.downloaderId}`} · ${log.downloadedAt}`).join('<br>') : '暂无下载记录'
    await ElMessageBox.alert(content, `${attachment.originalName} 的下载记录`, { dangerouslyUseHTMLString: true, confirmButtonText: '关闭' })
  } catch { /* 非管理员或请求失败时由拦截器提示 */ }
}
function formatFileSize(size) {
  if (!size) return '0 B'
  return size < 1024 * 1024 ? `${Math.ceil(size / 1024)} KB` : `${(size / 1024 / 1024).toFixed(1)} MB`
}

async function handleDelete(task) {
  try {
    await ElMessageBox.confirm('确定将该任务移入回收站？其子任务会一并保留，可在回收站恢复。', '移入回收站', {
      type: 'warning', confirmButtonText: '移入回收站', cancelButtonText: '取消'
    })
    await deleteTask(task.id)
    delete subtaskData[task.id]
    ElMessage.success('任务已移入回收站')
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

.board-main { flex: 1; overflow-x: auto; padding: 24px 30px 30px; }
.board-filters { display: flex; align-items: center; gap: 10px; max-width: 1280px; margin: 0 auto 16px; }
.board-filters :deep(.el-select) { width: 150px; }
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
.task-meta-chips { display: flex; flex-wrap: wrap; gap: 5px; margin: 8px 0 4px; }
.priority-chip, .task-label, .blocked-chip { display: inline-flex; align-items: center; border-radius: 10px; padding: 2px 7px; font-size: 10px; line-height: 16px; }
.priority-HIGH { background: #fee8e5; color: #be3f2a; }.priority-MEDIUM { background: #fff1d6; color: #a9650d; }.priority-LOW { background: #e8f0fa; color: #507394; }
.task-label { background: #edf1f5; color: #607080; }.blocked-chip { background: #fee8e5; color: #be3f2a; }
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
.detail-task-fields { flex-wrap: wrap; gap: 7px 10px; margin-bottom: 10px; }
.blocked-notice { margin: 10px 0; padding: 9px 11px; border-radius: 8px; background: #fff0ed; color: #bd482f; font-size: 12px; }
.acceptance-section { margin: 12px 0 18px; padding: 11px 13px; border-radius: 8px; background: #f4f7fa; color: var(--text-secondary); font-size: 13px; }
.acceptance-section strong { color: var(--text-primary); }.acceptance-section p { margin: 6px 0 0; line-height: 1.6; }
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
.project-role-dialog :deep(.el-dialog) { border-radius: 16px; }
.project-role-header { text-align: center; padding: 4px 0 8px; }
.project-role-header h3 { margin: 8px 0 6px; color: var(--text-primary); }
.project-role-header p { margin: 0; color: var(--text-tertiary); font-size: 13px; }
.project-role-icon { font-size: 32px; }
.project-role-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.project-role-card { padding: 17px 8px; border: 1px solid var(--border); border-radius: 10px; background: var(--bg-surface); color: var(--text-primary); cursor: pointer; display: flex; flex-direction: column; align-items: center; gap: 7px; transition: .16s; }
.project-role-card:hover:not(:disabled) { border-color: var(--brand); transform: translateY(-2px); box-shadow: 0 5px 14px rgba(213,138,34,.18); }
.project-role-card span { font-size: 26px; }
.project-role-card strong { font-size: 13px; }
@media (max-width: 500px) { .project-role-grid { grid-template-columns: repeat(2, 1fr); } }
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
.attachment-section { margin-top: 22px; padding-top: 18px; border-top: 1px solid var(--border); }
.hidden-file-input { display: none; }
.attachment-tip { margin: 8px 0; color: var(--text-tertiary); font-size: 12px; }
.attachment-row { display: flex; align-items: center; gap: 8px; padding: 7px 0; border-bottom: 1px solid var(--border); font-size: 12px; }
.attachment-row > span { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.attachment-row small { color: var(--text-tertiary); }
.ai-plan-preview { max-height: 58vh; overflow-y: auto; padding-right: 6px; }.plan-overview { margin: 0 0 16px; color: var(--text-secondary); line-height: 1.6; }.ai-plan-preview section { margin-top: 18px; }.ai-plan-preview h4 { margin: 0 0 8px; font-size: 14px; }.plan-stage-list { display: flex; flex-wrap: wrap; gap: 7px; }.plan-stage-list span { padding: 5px 8px; border-radius: 8px; background: #fdf1db; color: #965d14; font-size: 12px; }.plan-task, .plan-risk { padding: 10px 12px; margin-top: 7px; border: 1px solid var(--border); border-radius: 8px; }.plan-task strong, .plan-risk strong { display: block; font-size: 13px; }.plan-task span, .plan-task small, .plan-risk small { display: block; margin-top: 4px; color: var(--text-tertiary); font-size: 12px; }.plan-task p, .plan-risk p { margin: 5px 0; color: var(--text-secondary); font-size: 12px; line-height: 1.5; }.optimization-preview label { display: block; margin: 14px 0 5px; color: var(--text-tertiary); font-size: 12px; }.optimization-preview label:first-child { margin-top: 0; }.optimization-preview p { margin: 0; padding: 10px 12px; border-radius: 8px; background: #f4f7fa; color: var(--text-secondary); font-size: 13px; line-height: 1.6; }.optimization-ok { color: #287248 !important; background: #edf8f0 !important; }

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
