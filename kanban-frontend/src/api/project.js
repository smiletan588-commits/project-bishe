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

export function inviteProjectMember(projectId, username, identity, permission) {
  return request.post(`/project/${projectId}/members/invite`, null, { params: { username, identity, permission } })
}

export function updateProjectMember(projectId, userId, identity, permission) {
  return request.put(`/project/${projectId}/members/${userId}`, null, { params: { identity, permission } })
}

export function removeProjectMember(projectId, userId) {
  return request.delete(`/project/${projectId}/members/${userId}`)
}

export function transferProjectOwner(projectId, userId) {
  return request.post(`/project/${projectId}/transfer-owner`, null, { params: { userId } })
}
