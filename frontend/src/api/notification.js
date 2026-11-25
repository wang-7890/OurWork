import request, { normalizePageResult } from './request'

// 8.1 通知列表（分页结构统一归一化，read: 'true' 已读 / 'false' 未读）
export function getNotificationsApi(params) {
  return request
    .get('/notifications', { params })
    .then((data) => normalizePageResult(data))
}

// 8.2 单条标记已读
export function markNotificationReadApi(notificationId) {
  return request.put(`/notifications/${notificationId}/read`)
}

// 8.3 全部标记已读
export function markAllNotificationsReadApi() {
  return request.put('/notifications/read-all')
}
