import { streamSSE } from '@/utils/sse'

export function streamProjectSummary(projectId, callbacks) {
  streamSSE(`/api/project/${projectId}/ai-summary`, callbacks)
}
