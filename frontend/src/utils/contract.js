// 合约状态：数据库与接口统一使用大写枚举
export const CONTRACT_STATUS = {
  ACTIVE: 'ACTIVE',
  VOTING: 'VOTING',
  SUCCESS: 'SUCCESS',
  FAILED: 'FAILED'
}

export const CONTRACT_STATUS_TEXT = {
  ACTIVE: '进行中',
  VOTING: '待裁决',
  SUCCESS: '已完成',
  FAILED: '未履约'
}

// Element Plus 标签类型
export const CONTRACT_STATUS_TAG_TYPE = {
  ACTIVE: 'primary',
  VOTING: 'warning',
  SUCCESS: 'success',
  FAILED: 'danger'
}

// 合约分类；后端若新增分类，在这里同步即可
export const CONTRACT_CATEGORY = {
  STUDY: 'STUDY',
  FITNESS: 'FITNESS',
  WORK: 'WORK',
  LIFE: 'LIFE',
  OTHER: 'OTHER'
}

export const CONTRACT_CATEGORY_TEXT = {
  STUDY: '学习',
  FITNESS: '健身',
  WORK: '工作',
  LIFE: '生活',
  OTHER: '其他'
}

// 投票选项：注意投票用 FAIL，合约结算结果用 FAILED
export const VERDICT = {
  SUCCESS: 'SUCCESS',
  FAIL: 'FAIL'
}

export const VERDICT_TEXT = {
  SUCCESS: '履约成功',
  FAIL: '履约失败'
}

export const SETTLE_RESULT_TEXT = {
  SUCCESS: '履约成功',
  FAILED: '未履约'
}

// 资金流水类型
export const TRANSACTION_TYPE_TEXT = {
  GIFT: '注册赠送',
  FREEZE: '押金冻结',
  UNFREEZE: '押金解冻',
  DEDUCT: '违约扣款'
}

export function getContractStatusText(status) {
  return CONTRACT_STATUS_TEXT[status] || status || '未知状态'
}

export function getCategoryText(category) {
  return CONTRACT_CATEGORY_TEXT[category] || category || '未分类'
}

// 兼容别名：部分页面可能使用 getContractCategoryText
export const getContractCategoryText = getCategoryText

// 金额统一显示为整数虚拟币，禁止使用浮点数
export function formatMoney(amount) {
  if (amount === null || amount === undefined || Number.isNaN(Number(amount))) {
    return '--'
  }

  return `${Number(amount).toLocaleString('zh-CN')} 币`
}

// 后端时间格式为 yyyy-MM-dd HH:mm:ss，直接展示即可
export function formatDateTime(value) {
  if (!value) {
    return '--'
  }

  return String(value).replace('T', ' ').slice(0, 19)
}
