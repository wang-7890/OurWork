<template>
  <div class="notification-page">
    <header class="page-header">
      <div>
        <h1>通知中心</h1>
        <p>
          共 {{ page.total }} 条通知，其中
          <strong class="unread-text">{{ authStore.unreadCount }}</strong>
          条未读
        </p>
      </div>

      <el-button
        type="primary"
        plain
        :disabled="authStore.unreadCount === 0 || markingAll"
        :loading="markingAll"
        @click="handleMarkAllRead"
      >
        全部标记已读
      </el-button>
    </header>

    <el-card class="filter-card" shadow="never">
      <el-radio-group v-model="filter.read" @change="handleFilterChange">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="false">未读</el-radio-button>
        <el-radio-button value="true">已读</el-radio-button>
      </el-radio-group>
    </el-card>

    <div v-loading="loading" class="notice-list">
      <el-empty
        v-if="!loading && !notifications.length"
        class="empty-state"
        description="暂无相关通知"
      />

      <div
        v-for="item in notifications"
        :key="item.notificationId"
        class="notice-item"
        :class="{ unread: !item.isRead, clickable: Boolean(item.relatedContractId) }"
        @click="handleItemClick(item)"
      >
        <div class="notice-icon" :class="`icon-${item.type.toLowerCase()}`">
          <el-icon :size="20">
            <component :is="typeMeta[item.type]?.icon || Bell" />
          </el-icon>
        </div>

        <div class="notice-body">
          <div class="notice-title-row">
            <el-tag
              size="small"
              effect="light"
              round
              :type="typeMeta[item.type]?.tagType || 'info'"
            >
              {{ typeMeta[item.type]?.text || '系统通知' }}
            </el-tag>
            <h2 class="notice-title">{{ item.title }}</h2>
            <span v-if="!item.isRead" class="unread-dot"></span>
          </div>

          <p class="notice-content">{{ item.content }}</p>

          <div class="notice-footer">
            <span class="notice-time">
              <el-icon><Clock /></el-icon>
              {{ formatDateTime(item.createdAt) }}
            </span>
            <el-button
              v-if="item.relatedContractId"
              text
              type="primary"
              class="detail-link"
            >
              查看合约
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <PagePagination
      :page="page.page"
      :size="page.size"
      :total="page.total"
      @change="handlePageChange"
    />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Bell,
  CircleCheck,
  Clock,
  Document,
  EditPen,
  InfoFilled,
  User
} from '@element-plus/icons-vue'
import PagePagination from '../components/PagePagination.vue'
import {
  getNotificationsApi,
  markAllNotificationsReadApi,
  markNotificationReadApi
} from '../api/notification'
import { useAuthStore } from '../stores/auth'
import { formatDateTime } from '../utils/contract'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const markingAll = ref(false)
const notifications = ref([])

const filter = reactive({ read: '' })
const page = reactive({ page: 1, size: 10, total: 0 })

// 通知类型与图标、标签颜色的映射
const typeMeta = {
  INVITE: { text: '邀请见证', icon: User, tagType: 'primary' },
  PROOF: { text: '凭证提醒', icon: Document, tagType: 'warning' },
  VOTE: { text: '投票提醒', icon: EditPen, tagType: 'danger' },
  SETTLE: { text: '裁决结果', icon: CircleCheck, tagType: 'success' },
  SYSTEM: { text: '系统通知', icon: InfoFilled, tagType: 'info' }
}

async function loadNotifications() {
  loading.value = true

  try {
    const data = await getNotificationsApi({
      page: page.page,
      size: page.size,
      read: filter.read || undefined
    })

    notifications.value = data.records
    page.total = data.total
  } catch {
    notifications.value = []
    page.total = 0
  } finally {
    loading.value = false
  }
}

function handleFilterChange() {
  page.page = 1
  loadNotifications()
}

function handlePageChange({ page: nextPage, size }) {
  page.page = nextPage
  page.size = size
  loadNotifications()
}

async function handleItemClick(item) {
  // 未读通知点击后先标记已读，同步铃铛角标，再决定是否跳转
  if (!item.isRead) {
    try {
      const result = await markNotificationReadApi(item.notificationId)
      item.isRead = true
      authStore.setUnreadCount(result?.unreadCount ?? authStore.unreadCount - 1)
    } catch {
      return
    }
  }

  if (item.relatedContractId) {
    router.push(`/contract/${item.relatedContractId}`)
  }
}

async function handleMarkAllRead() {
  markingAll.value = true

  try {
    const result = await markAllNotificationsReadApi()
    authStore.setUnreadCount(result?.unreadCount ?? 0)

    // 当前列表内的未读项就地更新，避免整页重新加载带来的闪烁
    notifications.value.forEach((item) => {
      item.isRead = true
    })

    ElMessage.success('全部通知已标记为已读')

    // 如果正停留在“未读”筛选，标记后列表应为空，重新拉取保证分页总数正确
    if (filter.read === 'false') {
      loadNotifications()
    }
  } catch {
    // request.js 已统一提示
  } finally {
    markingAll.value = false
  }
}

onMounted(() => {
  loadNotifications()
})
</script>

<style scoped>
.notification-page { max-width: 960px; margin: 0 auto; padding: 32px 24px 48px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; margin-bottom: 20px; }
.page-header h1 { margin: 0 0 8px; color: var(--ink); font-size: 30px; }
.page-header p { margin: 0; color: var(--ink-3); }
.unread-text { color: var(--danger); }
.filter-card { margin-bottom: 18px; border: 1px solid var(--line); border-radius: var(--radius-card); }
.notice-list { display: grid; gap: 12px; min-height: 200px; }
.empty-state { padding: 60px 0; background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-card); }
.notice-item { display: flex; gap: 14px; padding: 18px 20px; border: 1px solid var(--line); border-radius: var(--radius-card); background: var(--surface); transition: border-color 160ms ease, box-shadow 160ms ease; }
.notice-item.clickable { cursor: pointer; }
.notice-item.clickable:hover { border-color: var(--brand-border); box-shadow: var(--shadow-card); }
.notice-item.unread { border-color: var(--brand-border); background: var(--brand-bg); }
.notice-icon { display: flex; align-items: center; justify-content: center; width: 42px; height: 42px; flex-shrink: 0; border: 1px solid var(--line); border-radius: 50%; background: var(--panel); color: var(--brand); }
.notice-icon.icon-invite { color: var(--brand); background: var(--brand-bg); }
.notice-icon.icon-proof { color: var(--warning-dark); background: #f5efe4; }
.notice-icon.icon-vote { color: var(--danger); background: #f6eae7; }
.notice-icon.icon-settle { color: var(--success); background: #e7f0eb; }
.notice-icon.icon-system { color: var(--ink-3); background: var(--panel); }
.notice-body { flex: 1; min-width: 0; }
.notice-title-row { display: flex; align-items: center; gap: 10px; }
.notice-title { margin: 0; color: var(--ink); font-family: var(--font-display); font-size: 16px; font-weight: 700; letter-spacing: .025em; }
.unread-dot { width: 7px; height: 7px; flex-shrink: 0; border-radius: 50%; background: var(--brand); }
.notice-content { margin: 10px 0 0; color: var(--ink-2); font-size: 14px; line-height: 1.7; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.notice-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; }
.notice-time { display: flex; align-items: center; gap: 5px; color: var(--ink-4); font-size: 12px; }
.detail-link { padding: 0; }
@media (max-width: 640px) {
  .page-header { flex-direction: column; align-items: stretch; }
  .notice-item { padding: 14px; }
}
</style>
