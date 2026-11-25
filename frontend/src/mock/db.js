// Mock 阶段的共享“内存数据库”
// auth / contract 等 mock 模块共用同一份用户数据，保证余额、昵称等变更能互相读到
export const demoUsers = [
  {
    userId: 10001,
    username: 'demo1',
    password: '123456',
    nickname: '演示用户一',
    avatar: null,
    balance: 1000,
    frozen: 0,
    unreadCount: 2
  },
  {
    userId: 10002,
    username: 'demo2',
    password: '123456',
    nickname: '演示用户二',
    avatar: null,
    balance: 1000,
    frozen: 0,
    unreadCount: 1
  },
  {
    userId: 10003,
    username: 'demo3',
    password: '123456',
    nickname: '演示用户三',
    avatar: null,
    balance: 1000,
    frozen: 0,
    unreadCount: 0
  }
]

export let nextUserId = 10004

export function growNextUserId() {
  nextUserId += 1
}

export function getToken(headers) {
  const authorization = headers?.authorization || ''

  if (!authorization.startsWith('Bearer ')) {
    return ''
  }

  return authorization.slice(7)
}

export function findUserByToken(headers) {
  const token = getToken(headers)

  if (!token.startsWith('mock-token-')) {
    return null
  }

  const userId = Number(token.replace('mock-token-', ''))
  return demoUsers.find((user) => user.userId === userId) || null
}

// ---------------- 资金流水（只增不改删，对应 acc_transaction 表）----------------

function formatTxTime(date) {
  const pad = (num) => String(num).padStart(2, '0')

  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  )
}

// userId -> 流水数组（最新在前）
export const transactionStore = {}

// 种子流水 ID 占用 30001-30003，运行期新流水从 30004 开始
let nextTransactionId = 30004

// 每个演示用户初始化一条注册赠送流水，与余额 1000 对齐
demoUsers.forEach((user, index) => {
  transactionStore[user.userId] = [
    {
      transactionId: 30001 + index,
      userId: user.userId,
      type: 'GIFT',
      amount: 1000,
      balanceAfter: 1000,
      frozenAfter: 0,
      contractId: null,
      remark: '新用户注册赠送体验金',
      createdAt: formatTxTime(new Date(Date.now() - (index + 2) * 86400000))
    }
  ]
})

// 追加一条流水。调用前需先完成用户余额/冻结金额的变动，
// 这样流水中的 balanceAfter / frozenAfter 才是变动后的快照
export function recordTransaction(user, payload) {
  if (!transactionStore[user.userId]) {
    transactionStore[user.userId] = []
  }

  const record = {
    transactionId: nextTransactionId,
    userId: user.userId,
    type: payload.type,
    amount: payload.amount,
    balanceAfter: user.balance,
    frozenAfter: user.frozen,
    contractId: payload.contractId ?? null,
    remark: payload.remark || '',
    createdAt: formatTxTime(new Date())
  }

  nextTransactionId += 1
  transactionStore[user.userId].unshift(record)

  return record
}

// 按类型筛选并分页，返回统一分页结构
export function listTransactions(userId, { type, page = 1, size = 10 } = {}) {
  const all = (transactionStore[userId] || []).filter(
    (item) => !type || item.type === type
  )

  const pageNum = Math.max(1, Number(page) || 1)
  const sizeNum = Math.min(50, Math.max(1, Number(size) || 10))
  const start = (pageNum - 1) * sizeNum

  return {
    total: all.length,
    page: pageNum,
    size: sizeNum,
    records: all.slice(start, start + sizeNum)
  }
}

// ---------------- 演示数据重置（9.2 一键重置使用）----------------

// 种子数据落地后留一份深拷贝快照；所有数据都是可 JSON 序列化的普通对象
const initialUsersSnapshot = JSON.parse(JSON.stringify(demoUsers))
const initialTransactionsSnapshot = JSON.parse(JSON.stringify(transactionStore))
const INITIAL_NEXT_USER_ID = 10004
const INITIAL_NEXT_TRANSACTION_ID = 30004

// 原地恢复，保证其他模块持有的数组/对象引用不失效
export function resetDb() {
  demoUsers.splice(
    0,
    demoUsers.length,
    ...JSON.parse(JSON.stringify(initialUsersSnapshot))
  )

  Object.keys(transactionStore).forEach((key) => {
    delete transactionStore[key]
  })

  Object.assign(
    transactionStore,
    JSON.parse(JSON.stringify(initialTransactionsSnapshot))
  )

  nextUserId = INITIAL_NEXT_USER_ID
  nextTransactionId = INITIAL_NEXT_TRANSACTION_ID
}
