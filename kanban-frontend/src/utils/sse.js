/**
 * 共享 SSE 流式请求工具。
 *
 * 使用原生 fetch + ReadableStream 逐块读取 SSE 流。
 * 兼容 "data:content"（Spring SseEmitter）和 "data: content"（标准 SSE）两种格式。
 *
 * 用法:
 *   streamSSE('/api/project/1/ai-summary', {
 *     onChunk(chunk) { ... },
 *     onDone() { ... },
 *     onError(err) { ... }
 *   })
 */
export function streamSSE(url, { onChunk, onDone, onError }) {
  const token = localStorage.getItem('token')

  console.log('[SSE] 发起请求:', url)

  fetch(url, {
    headers: { Authorization: `Bearer ${token || ''}` }
  })
    .then(response => {
      if (!response.ok) {
        return response.text().then(msg => {
          const err = new Error(msg || `HTTP ${response.status}`)
          console.error('[SSE] 响应错误:', response.status, msg)
          onError?.(err)
        })
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      function read() {
        reader.read().then(({ done, value }) => {
          if (done) {
            // 处理缓冲区残留
            if (buffer.trim()) {
              processLine(buffer.trim(), onChunk, onError)
            }
            onDone?.()
            return
          }

          buffer += decoder.decode(value, { stream: true })

          const lines = buffer.split('\n')
          buffer = lines.pop() // 最后一段可能不完整

          for (const line of lines) {
            const trimmed = line.trim()
            if (trimmed) {
              const shouldStop = processLine(trimmed, onChunk, onError)
              if (shouldStop) return
            }
          }
          read()
        }).catch(err => {
          console.error('[SSE] 读取流失败:', err)
          onError?.(err)
        })
      }
      read()
    })
    .catch(err => {
      console.error('[SSE] 请求失败:', err)
      onError?.(err)
    })
}

/**
 * 处理单行 SSE 数据。
 * 返回 true 表示需要停止读取（遇到 [ERROR]）。
 */
function processLine(line, onChunk, onError) {
  if (!line.startsWith('data:')) return false

  // 去掉 "data:" 前缀，再去掉可能存在的空格
  let content = line.slice(5)
  if (content.startsWith(' ')) {
    content = content.slice(1)
  }

  if (!content) return false

  // 服务端错误标记
  if (content.startsWith('[ERROR]')) {
    const errMsg = content.slice(7).trim()
    console.error('[SSE] 服务端错误:', errMsg)
    onError?.(new Error(errMsg || '服务端未知错误'))
    return true
  }

  onChunk?.(content)
  return false
}
