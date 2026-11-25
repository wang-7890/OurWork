<template>
  <el-card
    class="contract-card"
    :style="{ '--card-accent': categoryAccent }"
    shadow="never"
    @click="handleClick"
  >
    <div class="card-top">
      <el-tag size="small" effect="plain" round>{{ categoryText }}</el-tag>
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
        <el-avatar :size="28" class="owner-avatar">
          {{ ownerInitial }}
        </el-avatar>
        <span class="owner-name">{{ contract.ownerNickname }}</span>
      </div>

      <div class="card-stats">
        <span class="stat">
          <el-icon><User /></el-icon>
          {{ contract.witnessCount ?? 0 }}
        </span>
        <span class="stat">
          <el-icon><View /></el-icon>
          {{ contract.viewCount ?? 0 }}
        </span>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { User, View } from '@element-plus/icons-vue'
import ContractStatusTag from './ContractStatusTag.vue'
import {
  formatDateTime,
  formatMoney,
  getCategoryText
} from '../utils/contract'

const props = defineProps({
  contract: {
    type: Object,
    required: true
  }
})

const router = useRouter()

const categoryText = computed(() => getCategoryText(props.contract.category))

// 按分类给卡片顶部渐变条配色
const CATEGORY_ACCENT = {
  STUDY: 'linear-gradient(90deg, #5b5fef, #8f94ff)',
  FITNESS: 'linear-gradient(90deg, #16a36a, #4fd698)',
  WORK: 'linear-gradient(90deg, #b45309, #f0a050)',
  LIFE: 'linear-gradient(90deg, #0e8aa8, #4fc3dd)',
  OTHER: 'linear-gradient(90deg, #667085, #a9b2c3)'
}

const categoryAccent = computed(
  () => CATEGORY_ACCENT[props.contract.category] || CATEGORY_ACCENT.OTHER
)

const ownerInitial = computed(() => {
  const name = props.contract.ownerNickname || '?'
  return name.slice(0, 1)
})

function handleClick() {
  router.push(`/contract/${props.contract.contractId}`)
}
</script>

<style scoped>
.contract-card {
  position: relative;
  height: 100%;
  border: 1px solid #e7eaf0;
  border-radius: 14px;
  cursor: pointer;
  overflow: hidden;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

/* 分类主题渐变条 */
.contract-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--card-accent, linear-gradient(90deg, #667085, #a9b2c3));
  opacity: 0.85;
  transition: height 0.18s ease, opacity 0.18s ease;
}

.contract-card:hover {
  transform: translateY(-3px);
  border-color: #c7c9fb;
  box-shadow: 0 14px 36px rgb(24 34 48 / 12%);
}

.contract-card:hover::before {
  height: 4px;
  opacity: 1;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.card-title {
  margin: 0 0 10px;
  color: #182230;
  font-size: 17px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-criteria {
  margin: 0 0 18px;
  min-height: 42px;
  color: #667085;
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  padding: 12px;
  margin-bottom: 14px;
  border-radius: 10px;
  background: #f7f8fc;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.meta-label {
  color: #98a2b3;
  font-size: 12px;
}

.meta-value {
  color: #182230;
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-value.deposit {
  color: #5b5fef;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.owner {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.owner-avatar {
  flex-shrink: 0;
  background: #5b5fef;
  color: #fff;
  font-size: 13px;
}

.owner-name {
  color: #344054;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-stats {
  display: flex;
  gap: 12px;
  color: #667085;
  font-size: 13px;
}

.stat {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>