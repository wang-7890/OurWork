import request, { normalizePageResult } from './request'

// 7.1 合约广场：搜索、分类、状态筛选、分页
export function getSquareContractsApi(params) {
  return request
    .get('/square/contracts', {
      params
    })
    .then((data) => normalizePageResult(data, params?.page, params?.size))
}

// 7.2 合约广场详情（公开信息）
export function getContractDetailApi(contractId) {
  return request.get(`/square/contracts/${contractId}`)
}

// 7.2 围观数 +1
export function increaseViewCountApi(contractId) {
  return request.post(`/square/contracts/${contractId}/view`)
}
