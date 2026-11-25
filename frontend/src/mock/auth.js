import {
  demoUsers,
  findUserByToken,
  getToken,
  growNextUserId,
  nextUserId
} from './db.js'

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

function toLoginData(user) {
  return {
    token: `mock-token-${user.userId}`,
    userId: user.userId,
    nickname: user.nickname,
    avatar: user.avatar,
    balance: user.balance,
    frozen: user.frozen,
    unreadCount: user.unreadCount
  }
}

export default [
  // 2.1 注册
  {
    url: '/api/auth/register',
    method: 'post',
    response: ({ body }) => {
      const { username, password, nickname } = body || {}

      if (!username || !password || !nickname) {
        return fail(400, '用户名、密码和昵称不能为空')
      }

      if (!/^[a-zA-Z0-9_]{4,20}$/.test(username)) {
        return fail(400, '用户名必须为 4-20 位字母、数字或下划线')
      }

      if (String(password).length < 6) {
        return fail(400, '密码长度至少 6 位')
      }

      if (String(nickname).length > 30) {
        return fail(400, '昵称不能超过 30 个字符')
      }

      const exists = demoUsers.some(
        (user) => user.username.toLowerCase() === String(username).toLowerCase()
      )

      if (exists) {
        return fail(409, '用户名已存在')
      }

      const user = {
        userId: nextUserId,
        username,
        password,
        nickname,
        avatar: null,
        balance: 1000,
        frozen: 0,
        unreadCount: 0
      }

      growNextUserId()
      demoUsers.push(user)

      // 严格按 2.1 返回；注册成功后再由前端请求 /auth/profile
      return success({
        userId: user.userId,
        token: `mock-token-${user.userId}`,
        giftAmount: 1000
      })
    }
  },

  // 2.2 登录
  {
    url: '/api/auth/login',
    method: 'post',
    response: ({ body }) => {
      const { username, password } = body || {}

      if (!username || !password) {
        return fail(400, '用户名和密码不能为空')
      }

      const user = demoUsers.find(
        (item) => item.username.toLowerCase() === String(username).toLowerCase()
      )

      if (!user || user.password !== password) {
        return fail(400, '用户名或密码错误')
      }

      return success(toLoginData(user))
    }
  },

  // 2.3 退出登录
  {
    url: '/api/auth/logout',
    method: 'post',
    response: () => success(null)
  },

  // 2.4 当前用户信息
  {
    url: '/api/auth/profile',
    method: 'get',
    response: ({ headers }) => {
      const user = findUserByToken(headers)

      if (!user) {
        return fail(401, '未登录或登录已过期')
      }

      return success(toProfile(user))
    }
  }
]

// 保留工具导出，便于其他 mock 模块复用 token 解析
export { getToken }
