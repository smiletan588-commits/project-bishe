import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/store/user'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000
})

// 请求拦截器 — 自动附加 Token
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器 — 统一错误处理
request.interceptors.response.use(
  response => {
    // 后端统一返回 R，但部分业务异常仍可能使用 HTTP 200。
    // 这里统一把 code 非 200 的业务响应转为 rejected，避免页面误报成功。
    if (response.data && response.data.code && response.data.code !== 200) {
      const error = new Error(response.data.msg || '请求处理失败')
      error.response = response
      return Promise.reject(error)
    }
    return response
  },
  error => {
    if (error.response) {
      const { status, data } = error.response
      switch (status) {
        case 401: {
          const userStore = useUserStore()
          userStore.logout()
          ElMessage.error('登录已过期，请重新登录')
          router.push('/login')
          break
        }
        case 404:
          ElMessage.error(data?.msg || '请求的资源不存在')
          break
        case 405:
          ElMessage.error(data?.msg || '请求方法不允许')
          break
        case 500:
          ElMessage.error(data?.msg || '服务器内部错误')
          break
        default:
          ElMessage.error(data?.msg || `请求失败 (${status})`)
      }
    } else {
      ElMessage.error('网络异常，请检查连接')
    }
    return Promise.reject(error)
  }
)

export default request
