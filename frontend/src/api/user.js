import request, { normalizePageResult } from './request'

// 6.1 个人诚信统计
export function getMyStatsApi() {
  return request.get('/user/stats')
}

// 6.2 修改个人资料（昵称）
export function updateProfileApi(data) {
  return request.put('/user/profile', data)
}

// 5.1 我的资金流水（分页结构统一归一化）
export function getTransactionsApi(params) {
  return request
    .get('/account/transactions', { params })
    .then((data) => normalizePageResult(data))
}
