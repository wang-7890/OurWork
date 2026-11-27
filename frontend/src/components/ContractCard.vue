<template>
  <el-card class="contract-card" shadow="never" @click="handleClick">
    <div class="card-top">
      <el-tag size="small" effect="plain">{{ categoryText }}</el-tag>
      <ContractStatusTag :status="contract.status" />
    </div>

    <h3 class="card-title">{{ contract.title }}</h3>
    <p class="card-criteria">{{ contract.acceptCriteria }}</p>

    <div class="card-meta">
      <div class="meta-item">
        <span class="meta-label">押金</span>
        <span class="meta-value deposit">{{ formatMoney(contract.deposit) }}</span>
      </div>
      <div class="meta-item">
        <span class="meta-label">截止</span>
        <span class="meta-value">{{ formatDateTime(contract.deadline) }}</span>
      </div>
    </div>

    <div class="card-footer">
      <div class="owner">
        <el-avatar :size="28" class="owner-avatar">{{ ownerInitial }}</el-avatar>
        <span class="owner-name">{{ contract.ownerNickname }}</span>
      </div>
      <div class="card-stats">
        <span class="stat"><el-icon><User /></el-icon>{{ contract.witnessCount ?? 0 }}</span>
        <span class="stat"><el-icon><View /></el-icon>{{ contract.viewCount ?? 0 }}</span>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { User, View } from '@element-plus/icons-vue'
import ContractStatusTag from './ContractStatusTag.vue'
import { formatDateTime, formatMoney, getCategoryText } from '../utils/contract'

const props = defineProps({
  contract: {
    type: Object,
    required: true
  }
})

const router = useRouter()
const categoryText = computed(() => getCategoryText(props.contract.category))
const ownerInitial = computed(() => {
  const name = props.contract.ownerNickname || '?'
  return name.slice(0, 1)
})

function handleClick() {
  router.push(`/contract/${props.contract.contractId}`)
}
</script>

<style scoped>
.contract-card { position: relative; height: 100%; border: 1px solid var(--line); border-radius: var(--radius-card); cursor: pointer; overflow: hidden; }
.contract-card:hover { transform: translateY(-3px); border-color: var(--brand-border); box-shadow: var(--shadow-card); }
.card-top { display: flex; justify-content: space-between; align-items: center; gap: 8px; margin-bottom: 16px; }
.card-title { margin: 0 0 10px; color: var(--ink); font-family: var(--font-display); font-size: 19px; line-height: 1.55; letter-spacing: .025em; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-criteria { margin: 0 0 18px; min-height: 42px; color: var(--ink-3); font-size: 13px; line-height: 1.65; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-meta { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; padding: 12px 0; margin-bottom: 14px; border-top: 1px solid var(--line-soft); border-bottom: 1px solid var(--line-soft); }
.meta-item { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.meta-label { color: var(--ink-4); font-size: 11px; }
.meta-value { color: var(--ink); font-size: 13px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.meta-value.deposit { color: var(--brand); }
.card-footer { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.owner { display: flex; align-items: center; gap: 8px; min-width: 0; }
.owner-avatar { flex-shrink: 0; background: var(--brand); color: #fff8f3; font-family: var(--font-display); font-size: 14px; }
.owner-name { color: var(--ink-2); font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-stats { display: flex; gap: 12px; color: var(--ink-3); font-size: 13px; }
.stat { display: flex; align-items: center; gap: 4px; }
</style>
