import { demoUsers, findUserByToken } from './db.js'

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

function formatTime(date) {
  const pad = (num) => String(num).padStart(2, '0')

  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  )

}

// ---------------- 通知存储（对应 sys_notification 表）----------------

// userId -> 通知数组（最新在前）
export const notificationStore = {}

// 种子共 9 条占用 40001-40009，运行期新通知从 40010 开始
let nextNotificationId = 40001

function seedNotification(userId, payload, daysAgo, hoursAgo = 0) {
  const time = new Date(
    Date.now() - daysAgo * 86400000 - hoursAgo * 3600000
  )

  return {
    notificationId: nextNotificationId++,
    userId,
    type: payload.type,
    title: payload.title,
    content: payload.content,
    relatedContractId: payload.relatedContractId ?? null,
    isRead: payload.isRead ?? false,
    createdAt: formatTime(time)
  }
}

// 种子数据：未读数与 demoUsers 的 unreadCount 严格对齐（demo1=2, demo2=1, demo3=0）
notificationStore[10001] = [
  seedNotification(
    10001,
    {
      type: 'SETTLE',
      title: '合约裁决结果通知',
      content: '你见证的合约「两周内完成毕业设计初稿并提交导师」已完成裁决，结果为：履约成功，押金已退回创建者。',
      relatedContractId: 20003,
      isRead: false
    },
    1,
    3
  ),
  seedNotification(
    10001,
    {
      type: 'SETTLE',
      title: '合约裁决结果通知',
      content: '你创建的合约「每天 23 点前睡觉，坚持 21 天」已完成裁决，结果为：未履约，押金已按规则扣除。',
      relatedContractId: 20004,
      isRead: false
    },
    1,
    6
  ),
  seedNotification(
    10001,
    {
      type: 'PROOF',
      title: '履约凭证提交通知',
      content: '你创建的合约「每天 23 点前睡觉，坚持 21 天」收到了新的履约凭证，请留意后续裁决安排。',
      relatedContractId: 20004,
      isRead: true
    },
    2,
    2
  ),
  seedNotification(
    10001,
    {
      type: 'SYSTEM',
      title: '欢迎使用微承诺',
      content: '你的账号已开通，1000 虚拟币体验金已到账，去发起第一个公开承诺吧。',
      relatedContractId: null,
      isRead: true
    },
    3
  )
]

notificationStore[10002] = [
  seedNotification(
    10002,
    {
      type: 'VOTE',
      title: '待裁决投票提醒',
      content: '你见证的合约「一个月内减重 4 公斤并保持一周」已进入待裁决阶段，请在 7 天内依据验收标准完成投票。',
      relatedContractId: 20002,
      isRead: false
    },
    0,
    5
  ),
  seedNotification(
    10002,
    {
      type: 'SETTLE',
      title: '合约裁决结果通知',
      content: '你见证的合约「两周内完成毕业设计初稿并提交导师」已完成裁决，结果为：履约成功。',
      relatedContractId: 20003,
      isRead: true
    },
    1,
    3
  ),
  seedNotification(
    10002,
    {
      type: 'INVITE',
      title: '见证邀请通知',
      content: '演示用户一邀请你见证合约「连续 30 天每天背 50 个英语单词」，请在截止时间前确认是否接受。',
      relatedContractId: 20001,
      isRead: true
    },
    2,
    4
  )
]

notificationStore[10003] = [
  seedNotification(
    10003,
    {
      type: 'SETTLE',
      title: '合约裁决结果通知',
      content: '你创建的合约「两周内完成毕业设计初稿并提交导师」已完成裁决，结果为：履约成功，押金已退回你的账户。',
      relatedContractId: 20003,
      isRead: true
    },
    1,
    3
  ),
  seedNotification(
    10003,
    {
      type: 'VOTE',
      title: '待裁决投票提醒',
      content: '你见证的合约「一个月内减重 4 公斤并保持一周」已进入待裁决阶段，请及时完成投票。',
      relatedContractId: 20002,
      isRead: true
    },
    0,
    5
  )
]

// 种子通知深拷贝快照，供 9.2 一键重置使用
const initialNotificationsSnapshot = JSON.parse(
  JSON.stringify(notificationStore)
)
const INITIAL_NEXT_NOTIFICATION_ID = 40010

// 原地恢复通知数据，保证其他模块持有的 notificationStore 引用不失效
export function resetNotificationState() {
  Object.keys(notificationStore).forEach((key) => {
    delete notificationStore[key]
  })

  Object.assign(
    notificationStore,
    JSON.parse(JSON.stringify(initialNotificationsSnapshot))
  )

  nextNotificationId = INITIAL_NEXT_NOTIFICATION_ID
}

// 运行期业务事件推送入口（contract.js 等模块调用）
export function pushNotification(userId, payload) {
  if (!notificationStore[userId]) {
    notificationStore[userId] = []
  }

  const record = {
    notificationId: nextNotificationId++,
    userId,
    type: payload.type,
    title: payload.title,
    content: payload.content,
    relatedContractId: payload.relatedContractId ?? null,
    isRead: false,
    createdAt: formatTime(new Date())
  }

  // 最新的排最前
  notificationStore[userId].unshift(record)

  // 同步用户未读数，保证 /auth/profile 与铃铛角标一致
  const user = demoUsers.find((item) => item.userId === userId)

  if (user) {
    user.unreadCount += 1
  }

  return record
}

function countUnread(userId) {
  return (notificationStore[userId] || []).filter((item) => !item.isRead).length
}

export default [
  // 8.1 通知列表（分页 + 已读/未读筛选）
  {
    url: '/api/notifications',
    method: 'get',
    response: ({ query, headers }) => {
      const viewer = findUserByToken(headers)

      if (!viewer) {
        return fail(401, '未登录或登录已过期')
      }

      const page = Math.max(1, Number(query.page) || 1)
      const size = Math.min(50, Math.max(1, Number(query.size) || 10))

      let list = [...(notificationStore[viewer.userId] || [])]

      if (query.read === 'true') {
        list = list.filter((item) => item.isRead)
      } else if (query.read === 'false') {
        list = list.filter((item) => !item.isRead)
      }

      const start = (page - 1) * size

      return success({
        total: list.length,
        page,
        size,
        records: list.slice(start, start + size)
      })
    }
  },

  // 8.3 全部标记已读（放在 :id 正则之前，避免被动态路由抢先匹配）
  {
    url: '/api/notifications/read-all',
    method: 'put',
    response: ({ headers }) => {
      const viewer = findUserByToken(headers)

      if (!viewer) {
        return fail(401, '未登录或登录已过期')
      }

      const list = notificationStore[viewer.userId] || []
      let updated = 0

      list.forEach((item) => {
        if (!item.isRead) {
          item.isRead = true
          updated += 1
        }
      })

      viewer.unreadCount = 0

      return success({ updated, unreadCount: 0 })
    }
  },

  // 8.2 单条标记已读
  {
    url: /^\/api\/notifications\/\d+\/read$/,
    method: 'put',
    response: ({ url, headers }) => {
      const viewer = findUserByToken(headers)

      if (!viewer) {
        return fail(401, '未登录或登录已过期')
      }

      const notificationId = Number(url.split('/').slice(-2, -1)[0])
      const record = (notificationStore[viewer.userId] || []).find(
        (item) => item.notificationId === notificationId
      )

      if (!record) {
        return fail(404, '通知不存在')
      }

      if (!record.isRead) {
        record.isRead = true
        viewer.unreadCount = countUnread(viewer.userId)
      }

      return success({
        notificationId,
        isRead: true,
        unreadCount: viewer.unreadCount
      })
    }
  }
]
