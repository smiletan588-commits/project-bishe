import request from '@/utils/request'

export function createProject(name, description) {
  return request.post('/project/create', null, { params: { name, description } })
}

export function listProjects() {
  return request.get('/project/list')
}

export function updateProject(id, name, description) {
  return request.put('/project/update', null, { params: { id, name, description } })
}

export function deleteProject(id) {
  return request.delete(`/project/${id}`)
}

export function listProjectMembers(projectId) {
  return request.get(`/project/${projectId}/members`)
}
