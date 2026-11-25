import { demoUsers, findUserByToken, recordTransaction } from './db.js'
import { pushNotification } from './notification.js'

const now = new Date()

function offsetDateTime(days, hours = 0) {
  const date = new Date(now.getTime() + days * 24 * 3600 * 1000 + hours * 3600 * 1000)
  const pad = (value) => String(value).padStart(2, '0')

  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  )
}

export const contracts = [
  {
    contractId: 20001,
    title: '连续 30 天每天背 50 个英语单词',
    category: 'STUDY',
    status: 'ACTIVE',
    acceptCriteria: '每天提交单词打卡截图，缺卡不超过 1 天',
    deposit: 200,
    deadline: offsetDateTime(12),
    viewCount: 36,
    witnessCount: 1,
    ownerUserId: 10001,
    ownerNickname: '演示用户一',
    ownerAvatar: null,
    createdAt: offsetDateTime(-3)
  },
  {
    contractId: 20002,
    title: '一个月内减重 4 公斤并保持一周',
    category: 'FITNESS',
    status: 'VOTING',
    acceptCriteria: '提供体重秤照片和运动记录，由见证人投票确认',
    deposit: 500,
    deadline: offsetDateTime(-1),
    viewCount: 58,
    witnessCount: 2,
    ownerUserId: 10001,
    ownerNickname: '演示用户一',
    ownerAvatar: null,
    createdAt: offsetDateTime(-32)
  },
  {
    contractId: 20003,
    title: '两周内完成毕业设计初稿并提交导师',
    category: 'WORK',
    status: 'SUCCESS',
    acceptCriteria: '提交完整初稿文档及导师回复邮件截图',
    deposit: 300,
    deadline: offsetDateTime(-5),
    settleResult: 'SUCCESS',
    viewCount: 82,
    witnessCount: 2,
    ownerUserId: 10002,
    ownerNickname: '演示用户二',
    ownerAvatar: null,
    createdAt: offsetDateTime(-20)
  },
  {
    contractId: 20004,
    title: '每天 23 点前睡觉，坚持 21 天',
    category: 'LIFE',
    status: 'FAILED',
    acceptCriteria: '每日睡前提交手机屏幕使用时间截图',
    deposit: 150,
    deadline: offsetDateTime(-8),
    settleResult: 'FAILED',
    viewCount: 47,
    witnessCount: 2,
    ownerUserId: 10003,
    ownerNickname: '演示用户三',
    ownerAvatar: null,
    createdAt: offsetDateTime(-30)
  },
  {
    contractId: 20005,
    title: '周末前完成 LeetCode 热题 100 中的 20 道',
    category: 'STUDY',
    status: 'ACTIVE',
    acceptCriteria: '提交刷题记录链接，每题需有完整解题过程',
    deposit: 100,
    deadline: offsetDateTime(4),
    viewCount: 19,
    witnessCount: 1,
    ownerUserId: 10002,
    ownerNickname: '演示用户二',
    ownerAvatar: null,
    createdAt: offsetDateTime(-1)
  }
]

// 保存本次运行期间提交的凭证，让详情接口能读到最新结果
const proofStore = {}

// 运行期间新增的见证人：contractId -> witness[]
const witnessStore = {}

// 运行期间产生的投票：contractId -> vote[]
const voteStore = {}

// 预置合约的深拷贝快照，供 9.2 一键重置使用（extrasMap 为静态种子数据，无需重置）
const initialContractsSnapshot = JSON.parse(JSON.stringify(contracts))
const INITIAL_NEXT_CONTRACT_ID = 20006

// 原地恢复合约与全部运行期存储，保证其他模块持有的 contracts 引用不失效
export function resetContractState() {
  contracts.splice(
    0,
    contracts.length,
    ...JSON.parse(JSON.stringify(initialContractsSnapshot))
  )

  ;[proofStore, witnessStore, voteStore].forEach((store) => {
    Object.keys(store).forEach((key) => {
      delete store[key]
    })
  })

  nextContractId = INITIAL_NEXT_CONTRACT_ID
}

// 取真实当前时间，格式与后端约定一致：yyyy-MM-dd HH:mm:ss
function currentDateTime() {
  const date = new Date()
  const pad = (value) => String(value).padStart(2, '0')

  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  )
}

// 邀请码格式与 3.4 生成逻辑保持一致：MOCK{contractId}ABCD
function parseInviteCode(code) {
  const matched = /^MOCK(\d+)ABCD$/.exec(String(code || ''))
  return matched ? Number(matched[1]) : null
}

// 合并预置数据与运行时数据，得到某合约的全部见证人
function getAllWitnesses(contractId, extras) {
  return [...(extras?.witnesses || []), ...(witnessStore[contractId] || [])]
}

// 合并预置数据与运行时数据，得到某合约的全部投票
function getAllVotes(contractId, extras) {
  return [...(extras?.voteDetails || []), ...(voteStore[contractId] || [])]
}

function buildVoteSummary(votes) {
  return {
    success: votes.filter((item) => item.verdict === 'SUCCESS').length,
    fail: votes.filter((item) => item.verdict === 'FAIL').length,
    total: votes.length
  }
}

// 供个人统计使用：返回某用户担任见证人的全部合约 ID
export function getWitnessContractIds(userId) {
  return contracts
    .filter((contract) => {
      const extras = extrasMap[contract.contractId]
      return getAllWitnesses(contract.contractId, extras).some(
        (item) => item.userId === userId
      )
    })
    .map((contract) => contract.contractId)
}

// ---------------- 演示生命周期：到期 / 结算（9.1、9.3 复用）----------------

// ACTIVE -> VOTING：截止时间改为当前时间，并给全体见证人发投票提醒
function enterVoting(contract) {
  const timeText = currentDateTime()

  contract.status = 'VOTING'
  contract.deadline = timeText
  // 对应数据库待决策字段 voting_started_at，Mock 先在运行期落地
  contract.votingStartedAt = timeText

  const extras = extrasMap[contract.contractId]
  const witnesses = getAllWitnesses(contract.contractId, extras)

  witnesses.forEach((witness) => {
    pushNotification(witness.userId, {
      type: 'VOTE',
      title: '待裁决投票提醒',
      content: `你见证的合约「${contract.title}」已进入待裁决阶段，请及时依据验收标准完成投票。`,
      relatedContractId: contract.contractId
    })
  })
}

// 9.1 合约立即到期。返回 { error, message } 或 { contract }
export function expireContract(contractId) {
  const contract = contracts.find((item) => item.contractId === contractId)

  if (!contract) {
    return { error: 404, message: '合约不存在' }
  }

  if (contract.status !== 'ACTIVE') {
    return { error: 409, message: '只有进行中的合约可以立即到期' }
  }

  enterVoting(contract)

  return { contract }
}

// 结算单个合约：多数票履约成功，平票/少数票算未履约；同步处理押金与裁决通知
function applySettlement(contract, witnesses) {
  const extras = extrasMap[contract.contractId]
  const allVotes = getAllVotes(contract.contractId, extras)
  const successCount = allVotes.filter(
    (item) => item.verdict === 'SUCCESS'
  ).length

  const result = successCount > allVotes.length - successCount
    ? 'SUCCESS'
    : 'FAILED'

  contract.status = result
  contract.settleResult = result

  // 结算押金：成功则解冻退回可用余额，失败则扣除冻结押金
  const owner = demoUsers.find((user) => user.userId === contract.ownerUserId)

  // 只有运行期真实冻结过押金的合约才处置资金；
  // 预置合约的押金未进入 Mock 账户，只结算状态，避免冻结金额变成负数
  if (owner && owner.frozen >= contract.deposit) {
    if (result === 'SUCCESS') {
      owner.balance += contract.deposit
      owner.frozen -= contract.deposit

      recordTransaction(owner, {
        type: 'UNFREEZE',
        amount: contract.deposit,
        contractId: contract.contractId,
        remark: `合约「${contract.title}」履约成功，押金退回`
      })
    } else {
      owner.frozen -= contract.deposit

      recordTransaction(owner, {
        type: 'DEDUCT',
        amount: contract.deposit,
        contractId: contract.contractId,
        remark: `合约「${contract.title}」未履约，押金扣除`
      })
    }
  }

  // 裁决结果通知：推送给创建者和全体见证人
  const resultText = result === 'SUCCESS' ? '履约成功' : '未履约'
  const receivers = [
    contract.ownerUserId,
    ...witnesses.map((item) => item.userId)
  ]

  receivers.forEach((userId) => {
    pushNotification(userId, {
      type: 'SETTLE',
      title: '合约裁决结果通知',
      content: `合约「${contract.title}」已完成裁决，结果为：${resultText}。`,
      relatedContractId: contract.contractId
    })
  })

  return result
}

// 9.3 手动触发定时任务扫描：
// 1) 到期扫描：ACTIVE 且 deadline 已过 -> VOTING 并通知见证人
// 2) 结算扫描：Mock 压缩 24 小时超时等待，立即结算全部 VOTING 合约（按当前票数）
export function runSettleJob() {
  const expired = []
  const settled = []

  contracts.forEach((contract) => {
    if (contract.status !== 'ACTIVE') {
      return
    }

    const deadlineTime = new Date(
      contract.deadline.replace(' ', 'T')
    ).getTime()

    if (deadlineTime <= Date.now()) {
      enterVoting(contract)
      expired.push(contract.contractId)
    }
  })

  contracts.forEach((contract) => {
    if (contract.status !== 'VOTING') {
      return
    }

    const extras = extrasMap[contract.contractId]
    const witnesses = getAllWitnesses(contract.contractId, extras)
    const result = applySettlement(contract, witnesses)

    settled.push({ contractId: contract.contractId, result })
  })

  return { expired, settled }
}

// 预置合约的见证人、凭证、投票等扩展信息（模块级共享，各接口均可读取）
const extrasMap = {
  20001: {
    witnesses: [
      {
        userId: 10002,
        nickname: '演示用户二',
        avatar: null,
        acceptedAt: offsetDateTime(-2, 5)
      }
    ],
    proofSubmitted: false,
    proofText: null,
    proofImages: [],
    submittedAt: null,
    voteSummary: { success: 0, fail: 0, total: 0 },
    voteDetails: []
  },
  20002: {
    witnesses: [
      {
        userId: 10002,
        nickname: '演示用户二',
        avatar: null,
        acceptedAt: offsetDateTime(-30)
      },
      {
        userId: 10003,
        nickname: '演示用户三',
        avatar: null,
        acceptedAt: offsetDateTime(-29)
      }
    ],
    proofSubmitted: true,
    proofText: '已完成阶段性减重，附上体重记录与运动截图，请见证人审核。',
    proofImages: [],
    submittedAt: offsetDateTime(-1, -2),
    voteSummary: { success: 1, fail: 0, total: 1 },
    // 未完结：投票明细必须为空，不能泄露其他见证人投票
    voteDetails: []
  },
  20003: {
    witnesses: [
      {
        userId: 10001,
        nickname: '演示用户一',
        avatar: null,
        acceptedAt: offsetDateTime(-19)
      },
      {
        userId: 10003,
        nickname: '演示用户三',
        avatar: null,
        acceptedAt: offsetDateTime(-18)
      }
    ],
    proofSubmitted: true,
    proofText: '毕业设计初稿已完成，并收到导师确认邮件。',
    proofImages: [],
    submittedAt: offsetDateTime(-6),
    voteSummary: { success: 2, fail: 0, total: 2 },
    // 已完结：才返回投票明细
    voteDetails: [
      {
        userId: 10001,
        nickname: '演示用户一',
        verdict: 'SUCCESS',
        comment: '文档完整，导师邮件截图有效。',
        votedAt: offsetDateTime(-4)
      },
      {
        userId: 10003,
        nickname: '演示用户三',
        verdict: 'SUCCESS',
        comment: '确认按时完成，符合验收标准。',
        votedAt: offsetDateTime(-4, 1)
      }
    ]
  },
  20004: {
    witnesses: [
      {
        userId: 10001,
        nickname: '演示用户一',
        avatar: null,
        acceptedAt: offsetDateTime(-29)
      },
      {
        userId: 10002,
        nickname: '演示用户二',
        avatar: null,
        acceptedAt: offsetDateTime(-28)
      }
    ],
    proofSubmitted: true,
    proofText: '后期多次未按时睡觉，打卡中断。',
    proofImages: [],
    submittedAt: offsetDateTime(-9),
    voteSummary: { success: 0, fail: 2, total: 2 },
    voteDetails: [
      {
        userId: 10001,
        nickname: '演示用户一',
        verdict: 'FAIL',
        comment: '缺卡超过约定次数，判定未履约。',
        votedAt: offsetDateTime(-7)
      },
      {
        userId: 10002,
        nickname: '演示用户二',
        verdict: 'FAIL',
        comment: '屏幕使用时间记录与承诺不符。',
        votedAt: offsetDateTime(-7, 1)
      }
    ]
  },
  20005: {
    witnesses: [
      {
        userId: 10001,
        nickname: '演示用户一',
        avatar: null,
        acceptedAt: offsetDateTime(-1, 3)
      }
    ],
    proofSubmitted: false,
    proofText: null,
    proofImages: [],
    submittedAt: null,
    voteSummary: { success: 0, fail: 0, total: 0 },
    voteDetails: []
  }
}

// 新建合约的起始 ID（预置数据到 20005）
let nextContractId = 20006

const CATEGORY_VALUES = ['STUDY', 'FITNESS', 'WORK', 'LIFE', 'OTHER']

// 后端约定的时间格式：yyyy-MM-dd HH:mm:ss
const DEADLINE_PATTERN = /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/

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

export default [
  // 3.1 创建对赌
  {
    url: '/api/contracts',
    method: 'post',
    response: ({ body, headers }) => {
      const owner = findUserByToken(headers)

      if (!owner) {
        return fail(401, '未登录或登录已过期')
      }

      const title = String(body?.title || '').trim()
      const category = body?.category
      const acceptCriteria = String(body?.acceptCriteria || '').trim()
      const deposit = Number(body?.deposit)
      const deadline = String(body?.deadline || '').trim()

      if (title.length < 5 || title.length > 50) {
        return fail(400, '合约标题需为 5-50 个字符')
      }

      if (!CATEGORY_VALUES.includes(category)) {
        return fail(400, '请选择正确的合约分类')
      }

      if (!acceptCriteria) {
        return fail(400, '验收标准不能为空')
      }

      if (acceptCriteria.length > 500) {
        return fail(400, '验收标准不能超过 500 字')
      }

      // 金额必须为正整数虚拟币，禁止小数
      if (!Number.isInteger(deposit) || deposit <= 0) {
        return fail(400, '押金必须为大于 0 的整数')
      }

      if (deposit > owner.balance) {
        return fail(400, '押金不能超过可用余额')
      }

      if (!DEADLINE_PATTERN.test(deadline)) {
        return fail(400, '截止时间格式不正确')
      }

      const deadlineTime = new Date(deadline.replace(' ', 'T')).getTime()

      if (Number.isNaN(deadlineTime)) {
        return fail(400, '截止时间格式不正确')
      }

      if (deadlineTime <= Date.now()) {
        return fail(400, '截止时间必须晚于当前时间')
      }

      const contract = {
        contractId: nextContractId,
        title,
        category,
        status: 'ACTIVE',
        acceptCriteria,
        deposit,
        deadline,
        viewCount: 0,
        witnessCount: 0,
        ownerUserId: owner.userId,
        ownerNickname: owner.nickname,
        ownerAvatar: owner.avatar,
        createdAt: offsetDateTime(0)
      }

      nextContractId += 1
      // 新合约排到最前，广场默认按创建倒序展示
      contracts.unshift(contract)

      // 押金立即从可用余额冻结
      owner.balance -= deposit
      owner.frozen += deposit

      recordTransaction(owner, {
        type: 'FREEZE',
        amount: deposit,
        contractId: contract.contractId,
        remark: `合约「${title}」押金冻结`
      })

      return success(contract)
    }
  },

  // 3.2 我的合约（当前用户创建的，支持状态筛选与分页）
  {
    url: '/api/contracts/my',
    method: 'get',
    response: ({ query, headers }) => {
      const viewer = findUserByToken(headers)

      if (!viewer) {
        return fail(401, '未登录或登录已过期')
      }

      const page = Math.max(1, Number(query.page) || 1)
      const size = Math.min(50, Math.max(1, Number(query.size) || 10))

      let list = contracts.filter(
        (contract) => contract.ownerUserId === viewer.userId
      )

      if (query.status) {
        list = list.filter((contract) => contract.status === query.status)
      }

      // 新合约 ID 更大，按 ID 倒序即创建时间倒序
      list = [...list].sort((a, b) => b.contractId - a.contractId)

      const start = (page - 1) * size

      return success({
        total: list.length,
        page,
        size,
        records: list.slice(start, start + size)
      })
    }
  },

  // 7.1 合约广场
  {
    url: '/api/square/contracts',
    method: 'get',
    response: ({ query }) => {
      const page = Number(query.page || 1)
      const size = Number(query.size || 10)
      const keyword = String(query.keyword || '').trim().toLowerCase()
      const category = query.category
      const status = query.status

      let filtered = [...contracts]

      if (keyword) {
        filtered = filtered.filter(
          (item) =>
            item.title.toLowerCase().includes(keyword) ||
            item.acceptCriteria.toLowerCase().includes(keyword)
        )
      }

      if (category) {
        filtered = filtered.filter((item) => item.category === category)
      }

      if (status) {
        filtered = filtered.filter((item) => item.status === status)
      }

      const total = filtered.length
      const start = (page - 1) * size
      const records = filtered.slice(start, start + size)

      return success({
        total,
        page,
        size,
        records
      })
    }
  },

  // 7.2 合约详情（广场公开信息部分）
  {
    url: /\/api\/square\/contracts\/\d+$/,
    method: 'get',
    response: ({ url }) => {
      const contractId = Number(url.split('/').pop())
      const contract = contracts.find((item) => item.contractId === contractId)

      if (!contract) {
        return fail(404, '合约不存在')
      }

      return success({
        ...contract
      })
    }
  },

  // 7.2 围观数 +1
  {
    url: /\/api\/square\/contracts\/\d+\/view$/,
    method: 'post',
    response: ({ url }) => {
      const contractId = Number(url.split('/').slice(-2, -1)[0])
      const contract = contracts.find((item) => item.contractId === contractId)

      if (!contract) {
        return fail(404, '合约不存在')
      }

      contract.viewCount += 1

      return success({
        contractId,
        viewCount: contract.viewCount
      })
    }
  },

  // 3.3 合约详情（含见证人、凭证、计票、本人投票状态）
  {
    url: /\/api\/contracts\/\d+$/,
    method: 'get',
    response: ({ url, headers }) => {
      const contractId = Number(url.split('/').pop())
      const base = contracts.find((item) => item.contractId === contractId)

      if (!base) {
        return fail(404, '合约不存在')
      }

      const extras = extrasMap[contractId] || {
        witnesses: [],
        proofSubmitted: false,
        proofText: null,
        proofImages: [],
        submittedAt: null,
        voteSummary: { success: 0, fail: 0, total: 0 },
        voteDetails: []
      }

      // 从 token 解析当前登录用户，判断本人是否已投票
      const authorization = headers?.authorization || ''
      const currentUserId = authorization.startsWith('Bearer mock-token-')
        ? Number(authorization.replace('Bearer mock-token-', ''))
        : null

      // 合并运行时新增的见证人与投票
      const witnesses = getAllWitnesses(contractId, extras)
      const allVotes = getAllVotes(contractId, extras)
      const finished = ['SUCCESS', 'FAILED'].includes(base.status)

      // 保密规则：未完结时绝不能返回其他见证人的具体投票
      const voteDetails = finished ? allVotes : []
      const voteSummary = buildVoteSummary(allVotes)

      const myVote =
        allVotes.find((item) => item.userId === currentUserId)?.verdict || null

      return success({
        ...base,
        ...extras,
        witnesses,
        voteDetails,
        voteSummary,
        // 本次运行期间提交过凭证时，用最新凭证覆盖初始数据
        ...(proofStore[contractId] || {}),
        myVote
      })
    }
  },

  // 3.4 生成邀请链接
  {
    url: /\/api\/contracts\/\d+\/invite$/,
    method: 'post',
    response: ({ url }) => {
      const contractId = Number(url.split('/').slice(-2, -1)[0])

      return success({
        contractId,
        inviteCode: `MOCK${contractId}ABCD`,
        inviteUrl: `http://localhost:5173/witness/accept/MOCK${contractId}ABCD`
      })
    }
  },

  // 3.5 按邀请码查看合约摘要（接受邀请页）
  {
    url: /^\/api\/contracts\/invite\/[^/]+$/,
    method: 'get',
    response: ({ url, headers }) => {
      const code = url.split('/').pop()
      const contractId = parseInviteCode(code)
      const contract = contracts.find((item) => item.contractId === contractId)

      if (!contract) {
        return fail(404, '邀请链接无效或合约不存在')
      }

      const viewer = findUserByToken(headers)
      const extras = extrasMap[contractId]
      const witnesses = getAllWitnesses(contractId, extras)

      return success({
        ...contract,
        witnesses,
        isOwner: Boolean(viewer) && viewer.userId === contract.ownerUserId,
        alreadyWitness: Boolean(
          viewer && witnesses.some((item) => item.userId === viewer.userId)
        )
      })
    }
  },

  // 3.5 接受邀请成为见证人
  {
    url: /^\/api\/contracts\/invite\/[^/]+\/accept$/,
    method: 'post',
    response: ({ url, headers }) => {
      const code = url.split('/').slice(-2, -1)[0]
      const contractId = parseInviteCode(code)
      const contract = contracts.find((item) => item.contractId === contractId)

      if (!contract) {
        return fail(404, '邀请链接无效或合约不存在')
      }

      const viewer = findUserByToken(headers)

      if (!viewer) {
        return fail(401, '未登录或登录已过期')
      }

      if (viewer.userId === contract.ownerUserId) {
        return fail(400, '不能成为自己合约的见证人')
      }

      if (contract.status !== 'ACTIVE') {
        return fail(409, '该合约已不在邀请见证人阶段')
      }

      const extras = extrasMap[contractId]
      const witnesses = getAllWitnesses(contractId, extras)

      if (witnesses.some((item) => item.userId === viewer.userId)) {
        return fail(409, '你已经是该合约的见证人')
      }

      const witness = {
        userId: viewer.userId,
        nickname: viewer.nickname,
        avatar: viewer.avatar,
        acceptedAt: offsetDateTime(0)
      }

      if (!witnessStore[contractId]) {
        witnessStore[contractId] = []
      }

      witnessStore[contractId].push(witness)
      contract.witnessCount += 1

      // 通知合约创建者：有新见证人加入
      pushNotification(contract.ownerUserId, {
        type: 'INVITE',
        title: '新见证人加入通知',
        content: `${viewer.nickname} 已接受邀请，成为合约「${contract.title}」的见证人。`,
        relatedContractId: contractId
      })

      return success({
        contractId,
        witness
      })
    }
  },

  // 4.1 我的待裁决列表（我是见证人、合约待裁决、我还没投票）
  {
    url: '/api/voting/pending',
    method: 'get',
    response: ({ headers }) => {
      const viewer = findUserByToken(headers)

      if (!viewer) {
        return fail(401, '未登录或登录已过期')
      }

      const records = contracts
        .filter((contract) => contract.status === 'VOTING')
        .map((contract) => {
          const extras = extrasMap[contract.contractId]
          const witnesses = getAllWitnesses(contract.contractId, extras)
          const votes = getAllVotes(contract.contractId, extras)

          const isWitness = witnesses.some(
            (item) => item.userId === viewer.userId
          )

          if (!isWitness) {
            return null
          }

          const alreadyVoted = votes.some(
            (item) => item.userId === viewer.userId
          )

          if (alreadyVoted) {
            return null
          }

          return {
            ...contract,
            witnesses,
            proofSubmitted:
              proofStore[contract.contractId]?.proofSubmitted ??
              extras?.proofSubmitted ??
              false,
            proofText:
              proofStore[contract.contractId]?.proofText ??
              extras?.proofText ??
              null,
            proofImages:
              proofStore[contract.contractId]?.proofImages ??
              extras?.proofImages ??
              [],
            submittedAt:
              proofStore[contract.contractId]?.submittedAt ??
              extras?.submittedAt ??
              null,
            voteSummary: buildVoteSummary(votes)
          }
        })
        .filter(Boolean)

      return success(records)
    }
  },

  // 4.2 提交投票
  {
    url: /\/api\/contracts\/\d+\/vote$/,
    method: 'post',
    response: ({ url, body, headers }) => {
      const contractId = Number(url.split('/').slice(-2, -1)[0])
      const contract = contracts.find((item) => item.contractId === contractId)

      if (!contract) {
        return fail(404, '合约不存在')
      }

      const viewer = findUserByToken(headers)

      if (!viewer) {
        return fail(401, '未登录或登录已过期')
      }

      if (contract.status !== 'VOTING') {
        return fail(409, '当前合约状态不允许投票')
      }

      const extras = extrasMap[contractId]
      const witnesses = getAllWitnesses(contractId, extras)

      if (!witnesses.some((item) => item.userId === viewer.userId)) {
        return fail(403, '只有见证人可以投票')
      }

      const votes = getAllVotes(contractId, extras)

      if (votes.some((item) => item.userId === viewer.userId)) {
        return fail(409, '你已经投过票，不能重复投票')
      }

      // 投票枚举是 SUCCESS / FAIL，注意不是 FAILED
      const verdict = body?.verdict
      const comment = String(body?.comment || '').trim()

      if (!['SUCCESS', 'FAIL'].includes(verdict)) {
        return fail(400, '投票结果不正确')
      }

      if (!comment) {
        return fail(400, '请填写投票评语')
      }

      if (comment.length > 200) {
        return fail(400, '投票评语不能超过 200 字')
      }

      if (!voteStore[contractId]) {
        voteStore[contractId] = []
      }

      voteStore[contractId].push({
        userId: viewer.userId,
        nickname: viewer.nickname,
        verdict,
        comment,
        votedAt: offsetDateTime(0)
      })

      // 全部见证人投票完成后，按多数票自动结算（Mock 规则，平票算未履约）
      const allVotes = getAllVotes(contractId, extras)
      let settled = false

      if (allVotes.length >= witnesses.length && witnesses.length > 0) {
        applySettlement(contract, witnesses)
        settled = true
      }

      return success({
        contractId,
        verdict,
        settled,
        contractStatus: contract.status
      })
    }
  },

  // 3.6 提交凭证
  {
    url: /\/api\/contracts\/\d+\/proof$/,
    method: 'post',
    response: ({ url, body }) => {
      const contractId = Number(url.split('/').slice(-2, -1)[0])
      const base = contracts.find((item) => item.contractId === contractId)

      if (!base) {
        return fail(404, '合约不存在')
      }

      if (base.status !== 'ACTIVE') {
        return fail(409, '当前合约状态不允许提交凭证')
      }

      const proofText = String(body?.proofText || '').trim()
      const proofImages = Array.isArray(body?.proofImages)
        ? body.proofImages.filter((item) => String(item || '').trim())
        : []

      if (!proofText) {
        return fail(400, '凭证文字说明不能为空')
      }

      const submittedAt = offsetDateTime(0)

      proofStore[contractId] = {
        proofSubmitted: true,
        proofText,
        proofImages,
        submittedAt
      }

      return success({
        contractId,
        submittedAt
      })
    }
  }
]