import axios from 'axios'
import { ElMessage } from 'element-plus'

export const TOKEN_KEY = 'token'

const DEFAULT_ERROR_MESSAGE = '请求失败，请稍后重试'

const service = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    Accept: 'application/json'
  }
})

// 请求拦截：自动携带 token
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  },
  (error) => Promise.reject(error)
)

function getBackendMessage(data, fallback = DEFAULT_ERROR_MESSAGE) {
  if (data && typeof data.message === 'string' && data.message.trim()) {
    return data.message
  }

  return fallback
}

function createRequestError(message, { code, status, config, response } = {}) {
  const error = new Error(message)
  error.code = code
  error.status = status
  error.config = config
  error.response = response
  return error
}

function showError(message, config) {
  // config.silent 为 true 时，允许某些页面自己处理错误，不重复弹窗
  if (!config?.silent) {
    ElMessage.error(message)
  }
}

function clearLoginState() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem('profile')
  localStorage.removeItem('unreadCount')
}

function redirectToLogin() {
  const currentPath = window.location.pathname + window.location.search

  if (window.location.pathname === '/login') {
    return
  }

  const redirect = encodeURIComponent(currentPath)
  window.location.href = `/login?redirect=${redirect}`
}

function handleUnauthorized(config) {
  clearLoginState()
  showError('登录状态已失效，请重新登录', config)
  redirectToLogin()
}

// 响应拦截：只把 code === 0 当作成功
service.interceptors.response.use(
  (response) => {
    const body = response.data

    if (!body || typeof body !== 'object' || !('code' in body)) {
      const message = '后端响应格式不正确'

      showError(message, response.config)

      return Promise.reject(
        createRequestError(message, {
          status: response.status,
          config: response.config,
          response
        })
      )
    }

    if (body.code === 0) {
      return body.data
    }

    if (body.code === 401) {
      handleUnauthorized(response.config)

      return Promise.reject(
        createRequestError('登录状态已失效，请重新登录', {
          code: body.code,
          status: response.status,
          config: response.config,
          response
        })
      )
    }

    const message = getBackendMessage(body)
    showError(message, response.config)

    return Promise.reject(
      createRequestError(message, {
        code: body.code,
        status: response.status,
        config: response.config,
        response
      })
    )
  },
  (error) => {
    const response = error.response

    // HTTP 状态码错误
    if (response) {
      const body = response.data

      if (response.status === 401 || body?.code === 401) {
        handleUnauthorized(error.config)

        return Promise.reject(
          createRequestError('登录状态已失效，请重新登录', {
            code: body?.code,
            status: response.status,
            config: error.config,
            response
          })
        )
      }

      const message = getBackendMessage(body, getHttpErrorMessage(response.status))
      showError(message, error.config)

      return Promise.reject(
        createRequestError(message, {
          code: body?.code,
          status: response.status,
          config: error.config,
          response
        })
      )
    }

    // 请求超时或网络异常
    const message =
      error.code === 'ECONNABORTED'
        ? '请求超时，请稍后重试'
        : '网络异常，请检查后端服务或网络连接'

    showError(message, error.config)

    return Promise.reject(
      createRequestError(message, {
        code: error.code,
        config: error.config
      })
    )
  }
)

function getHttpErrorMessage(status) {
  const messages = {
    400: '请求参数不正确',
    401: '登录状态已失效，请重新登录',
    403: '没有权限执行该操作',
    404: '请求的资源不存在',
    409: '当前数据状态冲突，请勿重复操作',
    500: '服务器内部错误'
  }

  return messages[status] || DEFAULT_ERROR_MESSAGE
}

// 统一分页结构，页面接口可以直接使用
export function normalizePageResult(data, defaultPage = 1, defaultSize = 10) {
  if (!data || typeof data !== 'object') {
    return {
      total: 0,
      page: defaultPage,
      size: defaultSize,
      records: []
    }
  }

  return {
    total: Number(data.total || 0),
    page: Number(data.page || defaultPage),
    size: Number(data.size || defaultSize),
    records: Array.isArray(data.records) ? data.records : []
  }
}

export default service