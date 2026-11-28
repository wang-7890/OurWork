<template>
  <div class="profile-page">
    <!-- 个人信息头部 -->
    <el-card v-loading="profileLoading" class="hero-card" shadow="never">
      <div class="hero-main">
        <el-avatar :size="72" class="hero-avatar">
          {{ nicknameInitial }}
        </el-avatar>

        <div class="hero-meta">
          <div class="nickname-row">
            <h1>{{ authStore.profile?.nickname || '--' }}</h1>
            <el-button text class="edit-button" @click="openNicknameDialog">
              <el-icon><EditPen /></el-icon>
              修改昵称
            </el-button>
          </div>
          <p class="username">账号：{{ authStore.profile?.username || '--' }}</p>
          <p class="hero-tip">公开承诺 · 共同见证 · 诚信留痕</p>
        </div>
      </div>

      <div class="hero-balance">
        <div class="balance-item">
          <span class="balance-label">可用余额</span>
          <strong class="balance-value">
            {{ formatMoney(authStore.profile?.balance) }}
          </strong>
        </div>
        <el-divider direction="vertical" class="balance-divider" />
        <div class="balance-item">
          <span class="balance-label">冻结金额</span>
          <strong class="balance-value frozen">
            {{ formatMoney(authStore.profile?.frozen) }}
          </strong>
        </div>
      </div>
    </el-card>

    <!-- 诚信统计 -->
    <div v-loading="statsLoading" class="stats-grid">
      <div class="stat-card">
        <span class="stat-label">发起合约</span>
        <strong class="stat-value">{{ stats.createdCount ?? 0 }}</strong>
      </div>
      <div class="stat-card">
        <span class="stat-label">参与见证</span>
        <strong class="stat-value">{{ stats.witnessCount ?? 0 }}</strong>
      </div>
      <div class="stat-card">
        <span class="stat-label">履约成功</span>
        <strong class="stat-value success">{{ stats.successCount ?? 0 }}</strong>
      </div>
      <div class="stat-card">
        <span class="stat-label">履约失败</span>
        <strong class="stat-value danger">{{ stats.failedCount ?? 0 }}</strong>
      </div>
      <div class="stat-card highlight">
        <span class="stat-label">履约率</span>
        <strong class="stat-value rate">{{ formatFulfillRate(stats.fulfillRate) }}</strong>
      </div>
    </div>

    <!-- 明细区 -->
    <el-card class="detail-card" shadow="never">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 我的合约 -->
        <el-tab-pane label="我的合约" name="contracts">
          <div class="toolbar">
            <el-radio-group v-model="contractFilter.status" @change="handleContractFilter">
              <el-radio-button value="">全部</el-radio-button>
              <el-radio-button
                v-for="(text, value) in CONTRACT_STATUS_TEXT"
                :key="value"
                :value="value"
              >
                {{ text }}
              </el-radio-button>
            </el-radio-group>
          </div>

          <div v-loading="contractsLoading" class="contract-grid-wrapper">
            <div v-if="contracts.length" class="contract-grid">
              <ContractCard
                v-for="contract in contracts"
                :key="contract.contractId"
                :contract="contract"
              />
            </div>
            <el-empty
              v-else-if="!contractsLoading"
              class="empty-state"
              description="暂无相关合约，去发起一个吧"
            >
              <el-button type="primary" @click="router.push('/contract/create')">
                发起合约
              </el-button>
            </el-empty>
          </div>

          <PagePagination
            :page="contractPage.page"
            :size="contractPage.size"
            :total="contractPage.total"
            @change="handleContractPageChange"
          />
        </el-tab-pane>

        <!-- 资金流水 -->
        <el-tab-pane label="资金流水" name="transactions">
          <div class="toolbar">
            <el-select
              v-model="txFilter.type"
              class="type-select"
              placeholder="全部类型"
              clearable
              @change="handleTxFilter"
            >
              <el-option
                v-for="(text, value) in TRANSACTION_TYPE_TEXT"
                :key="value"
                :label="text"
                :value="value"
              />
            </el-select>
          </div>

          <el-table
            v-loading="txLoading"
            :data="transactions"
            class="tx-table"
            stripe
          >
            <el-table-column label="时间" prop="createdAt" width="180" />
            <el-table-column label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="txTagType[row.type] || 'info'" effect="light" round>
                  {{ TRANSACTION_TYPE_TEXT[row.type] || row.type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="备注" prop="remark" min-width="220" show-overflow-tooltip />
            <el-table-column label="金额变动" width="140" align="right">
              <template #default="{ row }">
                <span :class="isInflow(row.type) ? 'amount-in' : 'amount-out'">
                  {{ isInflow(row.type) ? '+' : '-' }}{{ formatMoney(row.amount) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="变动后可用 / 冻结" width="180" align="right">
              <template #default="{ row }">
                {{ row.balanceAfter }} / {{ row.frozenAfter }}
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无资金流水记录" />
            </template>
          </el-table>

          <PagePagination
            :page="txPage.page"
            :size="txPage.size"
            :total="txPage.total"
            :page-sizes="[10, 20, 50]"
            @change="handleTxPageChange"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 修改昵称弹窗 -->
    <el-dialog v-model="nicknameDialog.visible" title="修改昵称" width="420px">
      <el-input
        v-model.trim="nicknameDialog.nickname"
        maxlength="30"
        show-word-limit
        placeholder="请输入新昵称，最多 30 个字符"
      />
      <template #footer>
        <el-button @click="nicknameDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="nicknameDialog.saving" @click="handleSaveNickname">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { EditPen } from '@element-plus/icons-vue'
import ContractCard from '../components/ContractCard.vue'
import PagePagination from '../components/PagePagination.vue'
import { getMyContractsApi } from '../api/contract'
import {
  getMyStatsApi,
  getTransactionsApi,
  updateProfileApi
} from '../api/user'
import { useAuthStore } from '../stores/auth'
import { formatFulfillRate } from '../utils/format'
import {
  CONTRACT_STATUS_TEXT,
  TRANSACTION_TYPE_TEXT,
  formatMoney
} from '../utils/contract'

const router = useRouter()
const authStore = useAuthStore()

// ---------------- 资料 ----------------

const profileLoading = ref(false)

const nicknameInitial = computed(() => {
  const name = authStore.profile?.nickname || '?'
  return name.slice(0, 1)
})

const nicknameDialog = reactive({
  visible: false,
  nickname: '',
  saving: false
})

function openNicknameDialog() {
  nicknameDialog.nickname = authStore.profile?.nickname || ''
  nicknameDialog.visible = true
}

async function handleSaveNickname() {
  const nickname = nicknameDialog.nickname.trim()

  if (!nickname) {
    ElMessage.warning('昵称不能为空')
    return
  }

  if (nickname.length > 30) {
    ElMessage.warning('昵称不能超过 30 个字符')
    return
  }

  nicknameDialog.saving = true

  try {
    const profile = await updateProfileApi({ nickname })
    authStore.patchProfile(profile)
    ElMessage.success('昵称已更新')
    nicknameDialog.visible = false
  } catch {
    // request.js 已统一提示
  } finally {
    nicknameDialog.saving = false
  }
}

// ---------------- 诚信统计 ----------------

const statsLoading = ref(false)
const stats = ref({})

async function loadStats() {
  statsLoading.value = true

  try {
    stats.value = await getMyStatsApi()
  } catch {
    stats.value = {}
  } finally {
    statsLoading.value = false
  }
}

// ---------------- 我的合约 ----------------

const contractsLoading = ref(false)
const contracts = ref([])
const contractFilter = reactive({ status: '' })
const contractPage = reactive({ page: 1, size: 9, total: 0 })

async function loadMyContracts() {
  contractsLoading.value = true

  try {
    const data = await getMyContractsApi({
      page: contractPage.page,
      size: contractPage.size,
      status: contractFilter.status || undefined
    })

    contracts.value = data.records
    contractPage.total = data.total
  } catch {
    contracts.value = []
    contractPage.total = 0
  } finally {
    contractsLoading.value = false
  }
}

function handleContractFilter() {
  contractPage.page = 1
  loadMyContracts()
}

function handleContractPageChange({ page, size }) {
  contractPage.page = page
  contractPage.size = size
  loadMyContracts()
}

// ---------------- 资金流水 ----------------

const txLoading = ref(false)
const transactions = ref([])
const txFilter = reactive({ type: '' })
const txPage = reactive({ page: 1, size: 10, total: 0 })
const txLoaded = ref(false)

const txTagType = {
  GIFT: 'success',
  FREEZE: 'warning',
  UNFREEZE: 'primary',
  DEDUCT: 'danger'
}

// GIFT 注册赠送、UNFREEZE 押金退回是可用余额流入；FREEZE/DEDUCT 是流出
function isInflow(type) {
  return type === 'GIFT' || type === 'UNFREEZE'
}

async function loadTransactions() {
  txLoading.value = true

  try {
    const data = await getTransactionsApi({
      page: txPage.page,
      size: txPage.size,
      type: txFilter.type || undefined
    })

    transactions.value = data.records
    txPage.total = data.total
  } catch {
    transactions.value = []
    txPage.total = 0
  } finally {
    txLoading.value = false
  }
}

function handleTxFilter() {
  txPage.page = 1
  loadTransactions()
}

function handleTxPageChange({ page, size }) {
  txPage.page = page
  txPage.size = size
  loadTransactions()
}

// 流水数据首次切到该页签时才加载，减少首屏请求
function handleTabChange(tabName) {
  if (tabName === 'transactions' && !txLoaded.value) {
    txLoaded.value = true
    loadTransactions()
  }
}

// ---------------- 初始化 ----------------

const activeTab = ref('contracts')

onMounted(async () => {
  profileLoading.value = true

  try {
    if (authStore.token && !authStore.profileLoaded) {
      await authStore.fetchProfile()
    }
  } catch {
    // 401 时 request.js 会自动跳登录
  } finally {
    profileLoading.value = false
  }

  loadStats()
  loadMyContracts()
})
</script>

<style scoped>
.profile-page { max-width: 1200px; margin: 0 auto; padding: 32px 24px 48px; }
.hero-card { display: flex; justify-content: space-between; align-items: center; gap: 24px; margin-bottom: 20px; border: 1px solid var(--line); border-radius: var(--radius-card); background: var(--surface); }
.hero-main { display: flex; align-items: center; gap: 20px; min-width: 0; }
.hero-avatar { flex-shrink: 0; background: var(--brand); color: #fff8f3; font-family: var(--font-display); font-size: 30px; font-weight: 700; }
.hero-meta { min-width: 0; }
.nickname-row { display: flex; align-items: center; gap: 10px; }
.nickname-row h1 { margin: 0; color: var(--ink); font-size: 26px; }
.edit-button { color: var(--brand); }
.username { margin: 8px 0 4px; color: var(--ink-3); font-size: 14px; }
.hero-tip { margin: 0; color: var(--ink-4); font-size: 12px; }
.hero-balance { display: flex; align-items: center; flex-shrink: 0; padding: 16px 24px; border-left: 1px solid var(--line); background: var(--panel); }
.balance-item { display: flex; flex-direction: column; gap: 6px; }
.balance-label { color: var(--ink-4); font-size: 12px; }
.balance-value { color: var(--brand); font-size: 23px; font-weight: 700; white-space: nowrap; }
.balance-value.frozen { color: var(--warning-dark); }
.balance-divider { height: 40px; margin: 0 24px; }
.stats-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 16px; margin-bottom: 20px; }
.stat-card { display: flex; flex-direction: column; gap: 10px; padding: 20px; border: 1px solid var(--line); border-radius: var(--radius-card); background: var(--surface); }
.stat-card.highlight { border-color: var(--brand-border); background: var(--brand-bg); }
.stat-label { color: var(--ink-3); font-size: 13px; }
.stat-value { color: var(--ink); font-size: 27px; font-weight: 700; }
.stat-value.success { color: var(--success); }
.stat-value.danger { color: var(--danger); }
.stat-value.rate { color: var(--brand); font-size: 25px; }
.detail-card { border: 1px solid var(--line); border-radius: var(--radius-card); }
.toolbar { margin-bottom: 18px; }
.type-select { width: 180px; }
.contract-grid-wrapper { min-height: 200px; }
.contract-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.empty-state { padding: 48px 0; }
.tx-table { width: 100%; }
.amount-in { color: var(--success); font-weight: 600; }
.amount-out { color: var(--danger); font-weight: 600; }
@media (max-width: 1024px) {
  .stats-grid { grid-template-columns: repeat(3, 1fr); }
  .contract-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 720px) {
  .hero-card { flex-direction: column; align-items: stretch; }
  .hero-balance { justify-content: space-around; border-top: 1px solid var(--line); border-left: 0; }
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .contract-grid { grid-template-columns: 1fr; }
}
</style>
