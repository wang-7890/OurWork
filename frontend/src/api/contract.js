import request, { normalizePageResult } from './request'

// 3.3 合约详情（含见证人、凭证、计票等完整信息）
export function getContractDetailApi(contractId) {
  return request.get(`/contracts/${contractId}`)
}

// 3.1 创建对赌
export function createContractApi(data) {
  return request.post('/contracts', data)
}

// 3.2 我的合约（分页结构统一归一化）
export function getMyContractsApi(params) {
  return request
    .get('/contracts/my', { params })
    .then((data) => normalizePageResult(data))
}

// 3.4 生成邀请链接
export function createInviteApi(contractId) {
  return request.post(`/contracts/${contractId}/invite`)
}

// 3.5 按邀请码查看合约摘要
export function getInviteInfoApi(inviteCode) {
  return request.get(`/contracts/invite/${inviteCode}`)
}

// 3.5 接受邀请成为见证人
export function acceptInviteApi(inviteCode) {
  return request.post(`/contracts/invite/${inviteCode}/accept`)
}

// 4.1 我的待裁决列表
export function getPendingVotesApi() {
  return request.get('/verdicts/pending')
}

// 4.2 提交投票（verdict 为 SUCCESS / FAIL）
export function submitVoteApi(contractId, data) {
  return request.post(`/contracts/${contractId}/vote`, data)
}

// 3.6 提交凭证
export function submitProofApi(contractId, data) {
  return request.post(`/contracts/${contractId}/proof`, data)
}