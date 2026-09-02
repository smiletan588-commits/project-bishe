import request from '@/utils/request'

export function listWiki(projectId) {
  return request.get(`/wiki/list/${projectId}`)
}

export function getWiki(id) {
  return request.get(`/wiki/${id}`)
}

export function createWiki(projectId, title, content) {
  return request.post('/wiki/create', null, { params: { projectId, title, content } })
}

export function updateWiki(id, title, content) {
  return request.put('/wiki/update', null, { params: { id, title, content } })
}

export function deleteWiki(id) {
  return request.delete(`/wiki/${id}`)
}

import { streamSSE } from '@/utils/sse'

export function streamAiCopilot(prompt, text, callbacks) {
  const url = `/api/wiki/ai-copilot?prompt=${encodeURIComponent(prompt)}&text=${encodeURIComponent(text)}`
  streamSSE(url, callbacks)
}
