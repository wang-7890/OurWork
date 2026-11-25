import request from './request'

// 9.1 合约立即到期（ACTIVE -> VOTING）
export function expireContractDemoApi(contractId) {
  return request.post(`/demo/contracts/${contractId}/expire`)
}

// 9.2 一键重置演示数据
export function resetDemoDataApi() {
  return request.post('/demo/reset')
}

// 9.3 手动触发结算扫描
export function triggerSettleJobApi() {
  return request.post('/demo/jobs/settle')
}
