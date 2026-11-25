// 金额均为整数虚拟币，禁止使用浮点数
export function formatAmount(value) {
  const amount = Number(value || 0)
  return `${amount.toLocaleString('zh-CN')} 虚拟币`
}

export function formatDateTime(value) {
  if (!value) {
    return '—'
  }

  const date = new Date(String(value).replace(' ', 'T'))

  if (Number.isNaN(date.getTime())) {
    return value
  }

  const pad = (num) => String(num).padStart(2, '0')

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(
    date.getDate()
  )} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function formatDate(value) {
  if (!value) {
    return '—'
  }

  const date = new Date(String(value).replace(' ', 'T'))

  if (Number.isNaN(date.getTime())) {
    return value
  }

  const pad = (num) => String(num).padStart(2, '0')

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

// 履约率为 null 表示暂无已结算记录；0 要正常显示 0%
export function formatFulfillRate(value) {
  if (value === null || value === undefined) {
    return '暂无记录'
  }

  const rate = Number(value)

  if (Number.isNaN(rate)) {
    return '暂无记录'
  }

  return `${Math.round(rate * 100)}%`
}
