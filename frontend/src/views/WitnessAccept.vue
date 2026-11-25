<template>
  <div class="accept-page">
    <div class="page-top">
      <el-button text @click="router.push('/square')">
        <el-icon><ArrowLeft /></el-icon>
        返回合约广场
      </el-button>
    </div>

    <el-card v-loading="loading" class="accept-card" shadow="never">
      <el-result
        v-if="loadError"
        icon="error"
        title="邀请链接无效"
        :sub-title="loadError"
      >
        <template #extra>
          <el-button type="primary" @click="router.push('/square')">
            去合约广场看看
          </el-button>
        </template>
      </el-result>

      <template v-else-if="info">
        <div class="invite-head">
          <div class="title-tags">
            <el-tag size="small" effect="plain" round>
              {{ getCategoryText(info.category) }}
            </el-tag>
            <ContractStatusTag :status="info.status" />
          </div>
          <h1>{{ info.title }}</h1>
          <p class="criteria">{{ info.acceptCriteria }}</p>
        </div>

        <el-divider />

        <div class="info-grid">
          <div class="info-item">
            <span class="label">创建者</span>
            <strong>{{ info.ownerNickname }}</strong>
          </div>
          <div class="info-item">
            <span class="label">押金金额</span>
            <strong class="deposit">{{ formatMoney(info.deposit) }}</strong>
          </div>
          <div class="info-item">
            <span class="label">截止时间</span>
            <strong>{{ formatDateTime(info.deadline) }}</strong>
          </div>
          <div class="info-item">
            <span class="label">现有见证人</span>
            <strong>{{ info.witnesses?.length || 0 }} 人</strong>
          </div>
        </div>

        <el-alert
          v-if="info.alreadyWitness"
          class="state-alert"
          type="success"
          :closable="false"
          show-icon
          title="你已经是该合约的见证人"
          description="合约到期后可在“待裁决”页面参与投票"
        />

        <el-alert
          v-else-if="info.isOwner"
          class="state-alert"
          type="warning"
          :closable="false"
          show-icon
          title="不能成为自己合约的见证人"
          description="请把邀请链接发给其他朋友"
        />

        <el-alert
          v-else-if="info.status !== 'ACTIVE'"
          class="state-alert"
          type="info"
          :closable="false"
          show-icon
          title="该合约已不在邀请见证人阶段"
          description="见证人名单已锁定，无法继续接受邀请"
        />

        <el-alert
          v-else
          class="state-alert"
          type="info"
          :closable="false"
          show-icon
          title="接受邀请后你将成为见证人"
          description="合约到期时，需要你依据验收标准对履约结果投票裁决，投票记录会公开留痕"
        />

        <div class="actions">
          <el-button @click="router.push('/square')">稍后再说</el-button>

          <el-button
            v-if="info.alreadyWitness || info.status !== 'ACTIVE' || info.isOwner"
            type="primary"
            @click="router.push(`/contract/${info.contractId}`)"
          >
            查看合约详情
          </el-button>

          <el-button
            v-else
            type="primary"
            :loading="submitting"
            @click="handleAccept"
          >
            接受邀请，成为见证人
          </el-button>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import ContractStatusTag from '../components/ContractStatusTag.vue'
import { acceptInviteApi, getInviteInfoApi } from '../api/contract'
import {
  formatDateTime,
  formatMoney,
  getCategoryText
} from '../utils/contract'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const loadError = ref('')
const info = ref(null)

async function loadInviteInfo() {
  loading.value = true
  loadError.value = ''

  try {
    info.value = await getInviteInfoApi(route.params.code)
  } catch (error) {
    info.value = null
    loadError.value = error.message || '邀请链接无效或合约不存在'
  } finally {
    loading.value = false
  }
}

async function handleAccept() {
  submitting.value = true

  try {
    await acceptInviteApi(route.params.code)
    ElMessage.success('已接受邀请，你现在是该合约的见证人')
    router.replace(`/contract/${info.value.contractId}`)
  } catch {
    // request.js 已统一展示 400/409 等错误
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadInviteInfo()
})
</script>

<style scoped>
.accept-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 24px 24px 48px;
}

.page-top {
  margin-bottom: 12px;
}

.accept-card {
  border: 1px solid #e7eaf0;
  border-radius: 14px;
}

.invite-head h1 {
  margin: 12px 0 10px;
  color: #182230;
  font-size: 24px;
  line-height: 1.4;
}

.title-tags {
  display: flex;
  gap: 8px;
}

.criteria {
  margin: 0;
  color: #667085;
  font-size: 14px;
  line-height: 1.8;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 8px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 10px;
  background: #f7f8fc;
}

.info-item .label {
  color: #98a2b3;
  font-size: 12px;
}

.info-item strong {
  color: #182230;
  font-size: 15px;
}

.info-item strong.deposit {
  color: #5b5fef;
}

.state-alert {
  margin-top: 20px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

@media (max-width: 640px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
