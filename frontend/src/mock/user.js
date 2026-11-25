import { findUserByToken, listTransactions } from './db.js'
import { contracts, getWitnessContractIds } from './contract.js'

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

function toProfile(user) {
  return {
    userId: user.userId,
    username: user.username,
    nickname: user.nickname,
    avatar: user.avatar,
    balance: user.balance,
    frozen: user.frozen,
    unreadCount: user.unreadCount
  }
}

export default [
  // 6.1 个人诚信统计
  {
    url: '/api/user/stats',
    method: 'get',
    response: ({ headers }) => {
      const viewer = findUserByToken(headers)

      if (!viewer) {
        return fail(401, '未登录或登录已过期')
      }

      const mine = contracts.filter(
        (contract) => contract.ownerUserId === viewer.userId
      )

      const settled = mine.filter((contract) =>
        ['SUCCESS', 'FAILED'].includes(contract.status)
      )

      const successCount = settled.filter(
        (contract) => contract.status === 'SUCCESS'
      ).length
      const failedCount = settled.length - successCount

      return success({
        createdCount: mine.length,
        witnessCount: getWitnessContractIds(viewer.userId).length,
        settledCount: settled.length,
        successCount,
        failedCount,
        // 没有已结算合约时返回 null，前端显示“暂无记录”；0% 与 null 语义不同
        fulfillRate: settled.length ? successCount / settled.length : null
      })
    }
  },

  // 6.2 修改个人资料（当前仅支持昵称）
  {
    url: '/api/user/profile',
    method: 'put',
    response: ({ body, headers }) => {
      const viewer = findUserByToken(headers)

      if (!viewer) {
        return fail(401, '未登录或登录已过期')
      }

      const nickname = String(body?.nickname || '').trim()

      if (!nickname) {
        return fail(400, '昵称不能为空')
      }

      if (nickname.length > 30) {
        return fail(400, '昵称不能超过 30 个字符')
      }

      viewer.nickname = nickname

      return success(toProfile(viewer))
    }
  },

  // 5.1 我的资金流水（分页 + 类型筛选）
  {
    url: '/api/account/transactions',
    method: 'get',
    response: ({ query, headers }) => {
      const viewer = findUserByToken(headers)

      if (!viewer) {
        return fail(401, '未登录或登录已过期')
      }

      return success(
        listTransactions(viewer.userId, {
          type: query.type || '',
          page: query.page,
          size: query.size
        })
      )
    }
  }
]
