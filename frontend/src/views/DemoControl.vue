<template>
  <div v-loading="loading" class="demo-page">
    <header class="page-header">
      <div>
        <h1>演示控制</h1>
        <p>仅 Mock 演示环境开放，用于现场快速演示合约完整生命周期</p>
      </div>
    </header>

    <el-alert
      class="mode-alert"
      type="warning"
      :closable="false"
      show-icon
      title="本页面只在前端 Mock 模式可见"
      description="联调真实后端（VITE_USE_MOCK=false）后入口与路由会自动关闭，对应后端配置项 demo.enabled，生产环境必须关闭。"
    />

    <!-- 全局操作 -->
    <el-card class="action-card" shadow="never">
      <div class="action-list">
        <div class="action-item">
          <div class="action-meta">
            <strong>执行结算扫描</strong>
            <span>
              先把已过截止时间的进行中合约转入待裁决，再立即结算全部待裁决合约（Mock
              压缩了 24 小时超时等待）
            </span>
          </div>
          <el-button type="primary" :loading="jobRunning" @click="handleSettleJob">
            立即执行扫描
          </el-button>
        </div>

        <el-divider class="action-divider" />

        <div class="action-item">
          <div class="action-meta">
            <strong>一键重置演示数据</strong>
            <span>
              清空运行期产生的全部数据，恢复 3 个演示账号、5 个预置合约、种子流水与通知
            </span>
          </div>
          <el-button type="danger" plain :loading="resetting" @click="handleReset">
            重置全部数据
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 合约状态表 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <h2>全部合约（{{ contracts.length }}）</h2>
          <el-button text @click="loadContracts">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <el-table :data="contracts" stripe class="demo-table">
        <el-table-column label="ID" prop="contractId" width="90" />
        <el-table-column label="合约标题" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" @click="router.push(`/contract/${row.contractId}`)">
              {{ row.title }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="创建者" width="110">
          <template #default="{ row }">{{ row.ownerNickname }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <ContractStatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="见证人" prop="witnessCount" width="80" align="center" />
        <el-table-column label="截止时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.deadline) }}</template>
        </el-table-column>
        <el-table-column label="演示操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'ACTIVE'"
              type="primary"
              size="small"
              :loading="expiringId === row.contractId"
              @click="handleExpire(row)"
            >
              立即到期
            </el-button>

            <el-tag v-else-if="row.status === 'VOTING'" type="warning" effect="light">
              待结算扫描
            </el-tag>

            <el-tag v-else type="info" effect="plain">已完结</el-tag>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无合约数据，可点上方“一键重置”恢复预置数据" />
        </template>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import ContractStatusTag from '../components/ContractStatusTag.vue'
import { getSquareContractsApi } from '../api/square'
import {
  expireContractDemoApi,
  resetDemoDataApi,
  triggerSettleJobApi
} from '../api/demo'
import { useAuthStore } from '../stores/auth'
import { formatDateTime } from '../utils/contract'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const jobRunning = ref(false)
const resetting = ref(false)
const expiringId = ref(null)
const contracts = ref([])

async function loadContracts() {
  loading.value = true

  try {
    // 演示页需要一次性看到全部合约，给足单页容量
    const data = await getSquareContractsApi({ page: 1, size: 100 })
    // 按合约 ID 升序，保证预置 20001-20005 展示顺序稳定
    contracts.value = [...data.records].sort(
      (a, b) => a.contractId - b.contractId
    )
  } catch {
    contracts.value = []
  } finally {
    loading.value = false
  }
}

async function handleExpire(contract) {
  expiringId.value = contract.contractId

  try {
    await expireContractDemoApi(contract.contractId)
    ElMessage.success(`合约「${contract.title}」已立即到期，进入待裁决`)
    await loadContracts()
  } catch {
    // request.js 已统一提示 409 等错误
  } finally {
    expiringId.value = null
  }
}

async function handleSettleJob() {
  jobRunning.value = true

  try {
    const result = await triggerSettleJobApi()
    const expiredCount = result?.expired?.length || 0
    const settledCount = result?.settled?.length || 0

    if (expiredCount === 0 && settledCount === 0) {
      ElMessage.info('本次扫描没有需要处理的合约')
    } else {
      ElMessage.success(`扫描完成：${expiredCount} 个合约进入待裁决，${settledCount} 个合约完成结算`)
    }

    await loadContracts()
  } catch {
    // request.js 已统一提示
  } finally {
    jobRunning.value = false
  }
}

async function handleReset() {
  try {
    await ElMessageBox.confirm(
      '将清空运行期产生的合约、投票、流水与通知，恢复到初始演示数据，且不可恢复。确定继续吗？',
      '一键重置确认',
      {
        confirmButtonText: '确认重置',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
  } catch {
    return
  }

  resetting.value = true

  try {
    await resetDemoDataApi()

    // 余额与未读数已恢复初始值，刷新全局资料让顶部胶囊/铃铛同步
    if (authStore.token) {
      await authStore.fetchProfile().catch(() => {})
    }

    ElMessage.success('演示数据已重置为初始状态')
    await loadContracts()
  } catch {
    // request.js 已统一提示
  } finally {
    resetting.value = false
  }
}

onMounted(() => {
  loadContracts()
})
</script>

<style scoped>
.demo-page { max-width: 1200px; margin: 0 auto; padding: 32px 24px 48px; }
.page-header { margin-bottom: 18px; }
.page-header h1 { margin: 0 0 8px; color: var(--ink); font-size: 30px; }
.page-header p { margin: 0; color: var(--ink-3); }
.mode-alert { margin-bottom: 20px; border-radius: 4px; }
.action-card, .table-card { margin-bottom: 20px; border: 1px solid var(--line); border-radius: var(--radius-card); }
.action-list { display: grid; gap: 4px; }
.action-item { display: flex; justify-content: space-between; align-items: center; gap: 24px; padding: 6px 2px; }
.action-meta { display: flex; flex-direction: column; gap: 6px; }
.action-meta strong { color: var(--ink); font-family: var(--font-display); font-size: 16px; letter-spacing: .03em; }
.action-meta span { color: var(--ink-3); font-size: 13px; line-height: 1.6; }
.action-divider { margin: 14px 0; }
.table-header { display: flex; justify-content: space-between; align-items: center; }
.table-header h2 { margin: 0; color: var(--ink); font-family: var(--font-display); font-size: 17px; }
.demo-table { width: 100%; }
@media (max-width: 768px) {
  .action-item { flex-direction: column; align-items: stretch; gap: 12px; }
}
</style>
