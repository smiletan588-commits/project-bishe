import request from '@/utils/request'

export function createTask(projectId, title, description, assigneeId, dueDate) {
  return request.post('/task/create', null, {
    params: { projectId, title, description, assigneeId, dueDate }
  })
}

export function listTasks(projectId) {
  return request.get(`/task/list/${projectId}`)
}

export function updateTask(dto) {
  return request.put('/task/update', dto)
}

export function deleteTask(id) {
  return request.delete(`/task/${id}`)
}

export function decomposeTask(taskId) {
  return request.post(`/task/${taskId}/ai-decompose`)
}

export function listSubtasks(taskId) {
  return request.get(`/task/${taskId}/subtasks`)
}

export function toggleSubtask(taskId) {
  return request.put(`/task/${taskId}/toggle-subtask`)
}

export function initProjectTasks(projectId) {
  return request.post(`/task/${projectId}/ai-init-tasks`)
}
