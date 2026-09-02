import request from '@/utils/request'

export function login(username, password) {
  return request.post('/user/login', null, { params: { username, password } })
}

export function register(username, password, nickname) {
  return request.post('/user/register', null, { params: { username, password, nickname } })
}

export function updateIdentity(identity) {
  return request.put('/user/identity', null, { params: { identity } })
}
