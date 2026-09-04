import request from '@/utils/request'

export function listAdminUsers() {
  return request.get('/admin/users')
}

export function updateAdminUserStatus(userId, status) {
  return request.put(`/admin/users/${userId}/status`, null, { params: { status } })
}

export function updateAdminUserRole(userId, systemRole) {
  return request.put(`/admin/users/${userId}/role`, null, { params: { systemRole } })
}

export function resetAdminUserPassword(userId, password) {
  return request.put(`/admin/users/${userId}/password`, null, { params: { password } })
}
