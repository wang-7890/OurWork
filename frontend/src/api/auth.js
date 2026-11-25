import request from './request'

// 2.1 注册
export function registerApi(data) {
  return request.post('/auth/register', data)
}

// 2.2 登录
export function loginApi(data) {
  return request.post('/auth/login', data)
}

// 2.3 退出登录
export function logoutApi() {
  return request.post('/auth/logout')
}

// 2.4 当前用户信息
export function getProfileApi() {
  return request.get('/auth/profile')
}