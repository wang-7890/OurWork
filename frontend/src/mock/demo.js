import { findUserByToken, resetDb } from './db.js'
import {
  contracts,
  expireContract,
  resetContractState,
  runSettleJob
} from './contract.js'
import { resetNotificationState } from './notification.js'

function success(data) {
  return {
    code: 0,
    message: 'success',
    data
  }
}

function fail(code, message) {
  return {
    code,
    message,
    data: null
  }
}

function nowText() {
  const date = new Date()
  const pad = (value) => String(value).padStart(2, '0')

  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  )
}

export default [
  // 9.1 合约立即到期：ACTIVE -> VOTING，并通知全体见证人
  {
    url: /^\/api\/demo\/contracts\/\d+\/expire$/,
    method: 'post',
    response: ({ url, headers }) => {
      const viewer = findUserByToken(headers)

      if (!viewer) {
        return fail(401, '未登录或登录已过期')
      }

      const contractId = Number(url.split('/').slice(-2, -1)[0])
      const result = expireContract(contractId)

      if (result.error) {
        return fail(result.error, result.message)
      }

      return success(result.contract)
    }
  },

  // 9.2 一键重置演示数据：恢复三个演示账号、预置合约、流水与通知
  {
    url: '/api/demo/reset',
    method: 'post',
    response: ({ headers }) => {
      const viewer = findUserByToken(headers)

      if (!viewer) {
        return fail(401, '未登录或登录已过期')
      }

      // 顺序：先清业务数据，再清账户与通知，避免中途产生脏关联
      resetContractState()
      resetDb()
      resetNotificationState()

      return success({
        resetAt: nowText(),
        userCount: 3,
        contractCount: contracts.length
      })
    }
  },

  // 9.3 手动触发结算扫描：到期扫描 + 待裁决合约立即结算
  {
    url: '/api/demo/jobs/settle',
    method: 'post',
    response: ({ headers }) => {
      const viewer = findUserByToken(headers)

      if (!viewer) {
        return fail(401, '未登录或登录已过期')
      }

      const scanResult = runSettleJob()

      return success({
        executedAt: nowText(),
        ...scanResult
      })
    }
  }
]
