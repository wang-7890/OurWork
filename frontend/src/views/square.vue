<template>
  <div class="square-page">
    <header class="hero-banner">
      <div class="hero-grid-overlay"></div>
      <div class="hero-content">
        <div class="hero-badge">公开承诺 · 共同见证</div>
        <h1>发现值得见证的目标</h1>
        <p>立下目标、押上押金、邀请朋友见证——按时兑现取回押金。</p>
        <el-button v-if="authStore.isLoggedIn" class="hero-cta" type="primary" size="large" @click="router.push('/contract/create')">
          <el-icon><Promotion /></el-icon>发起对赌
        </el-button>
        <el-button v-else class="hero-cta" type="primary" size="large" @click="router.push('/login')">登录后发起契约</el-button>
      </div>

      <div class="hero-deco" aria-hidden="true">
        <span class="hero-watermark">诺</span>
        <div class="hero-note">
          <p>今日立下小约，<br>来日坦然相见。</p>
          <small>一诺既出 · 山海可证</small>
        </div>
        <div class="hero-stamp">共同见证</div>
      </div>
    </header>

    <el-card class="filter-card" shadow="never">
      <div class="filter-row">
        <el-input
          v-model.trim="filters.keyword"
          class="keyword-input"
          placeholder="搜索合约标题或验收标准"
          clearable
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>

        <el-select v-model="filters.category" class="filter-select" placeholder="全部分类" clearable @change="handleSearch">
          <el-option v-for="(text, value) in CONTRACT_CATEGORY_TEXT" :key="value" :label="text" :value="value" />
        </el-select>

        <el-select v-model="filters.status" class="filter-select" placeholder="全部状态" clearable @change="handleSearch">
          <el-option v-for="(text, value) in CONTRACT_STATUS_TEXT" :key="value" :label="text" :value="value" />
        </el-select>

        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <div v-loading="loading" class="contract-grid-wrapper">
      <div v-if="contracts.length" class="contract-grid">
        <ContractCard v-for="contract in contracts" :key="contract.contractId" :contract="contract" />
      </div>
      <el-empty v-else-if="!loading" class="empty-state" description="没有符合条件的合约，换个关键词试试" />
    </div>

    <PagePagination :page="pagination.page" :size="pagination.size" :total="pagination.total" @change="handlePageChange" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Promotion, Search } from '@element-plus/icons-vue'
import ContractCard from '../components/ContractCard.vue'
import PagePagination from '../components/PagePagination.vue'
import { getSquareContractsApi } from '../api/square'
import { useAuthStore } from '../stores/auth'
import { CONTRACT_CATEGORY_TEXT, CONTRACT_STATUS_TEXT } from '../utils/contract'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const contracts = ref([])
const filters = reactive({ keyword: '', category: '', status: '' })
const pagination = reactive({ page: 1, size: 9, total: 0 })

async function loadContracts() {
  loading.value = true
  try {
    const data = await getSquareContractsApi({
      page: pagination.page,
      size: pagination.size,
      keyword: filters.keyword || undefined,
      category: filters.category || undefined,
      status: filters.status || undefined
    })
    contracts.value = data.records
    pagination.total = data.total
  } catch {
    contracts.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadContracts()
}
function handleReset() {
  filters.keyword = ''
  filters.category = ''
  filters.status = ''
  pagination.page = 1
  loadContracts()
}
function handlePageChange({ page, size }) {
  pagination.page = page
  pagination.size = size
  loadContracts()
}
onMounted(loadContracts)
</script>

<style scoped>
.square-page { max-width: 1200px; margin: 0 auto; padding: 32px 24px 48px; }
.hero-banner { position: relative; display: flex; justify-content: space-between; align-items: center; gap: 40px; margin-bottom: 24px; padding: 44px 48px; border: 1px solid var(--line); border-radius: var(--radius-card); overflow: hidden; color: var(--ink); background: #e7eae6; animation: rise-in .35s ease both; }
.hero-grid-overlay { position: absolute; inset: 0; background-image: linear-gradient(rgb(40 46 42 / 3%) 1px, transparent 1px), linear-gradient(90deg, rgb(40 46 42 / 3%) 1px, transparent 1px); background-size: 42px 42px; mask-image: linear-gradient(90deg, #000, transparent 75%); pointer-events: none; }
.hero-content { position: relative; z-index: 1; max-width: 610px; }
.hero-badge { display: flex; align-items: center; gap: 10px; color: var(--ink-3); font-size: 12px; letter-spacing: .14em; }
.hero-badge::before { content: ''; width: 24px; height: 1px; background: var(--brand); }
.hero-content h1 { margin: 18px 0 12px; font-size: clamp(32px, 3.6vw, 44px); line-height: 1.3; }
.hero-content p { margin: 0 0 26px; color: var(--ink-3); font-size: 15px; line-height: 1.8; }
.hero-cta { height: 46px; padding: 0 26px; border-radius: 3px; font-weight: 600; }
.hero-deco { position: relative; z-index: 1; flex: 0 0 330px; min-height: 210px; }
.hero-watermark { position: absolute; right: -10px; bottom: -70px; color: rgb(40 46 42 / 6%); font-family: var(--font-display); font-size: 220px; font-weight: 700; line-height: 1; }
.hero-note { position: relative; z-index: 1; width: 260px; padding: 22px 24px; border: 1px solid var(--ink); background: #e7eae6; }
.hero-note p { margin: 0; font-family: var(--font-display); font-size: 20px; line-height: 1.75; }
.hero-note small { display: block; margin-top: 14px; color: var(--ink-3); font-size: 10px; letter-spacing: .12em; }
.hero-stamp { position: absolute; z-index: 2; right: 0; bottom: 4px; display: grid; place-items: center; width: 82px; height: 82px; border: 3px double var(--brand); border-radius: 50%; color: var(--brand); font-family: var(--font-display); font-size: 16px; font-weight: 700; letter-spacing: .1em; transform: rotate(-12deg); }
.filter-card { margin-bottom: 24px; border: 1px solid var(--line); border-radius: var(--radius-card); }
.filter-row { display: flex; gap: 12px; flex-wrap: wrap; }
.keyword-input { flex: 1; min-width: 240px; }
.filter-select { width: 150px; }
.contract-grid-wrapper { min-height: 200px; }
.contract-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.empty-state { padding: 60px 0; }
@media (max-width: 1024px) {
  .contract-grid { grid-template-columns: repeat(2, 1fr); }
  .hero-deco { display: none; }
}
@media (max-width: 640px) {
  .contract-grid { grid-template-columns: 1fr; }
  .hero-banner { padding: 32px 24px; }
}
</style>
