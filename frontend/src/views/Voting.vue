<template>
  <div v-loading="loading" class="voting-page">
    <header class="page-header">
      <div>
        <h1>待裁决</h1>
        <p>以下合约需要你以见证人身份投票，投票后不可修改</p>
      </div>
      <div class="header-actions">
        <el-button text :loading="loading" @click="loadPending">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-tag v-if="!loading && contracts.length" type="warning" effect="light" round>
          {{ contracts.length }} 笔待投票
        </el-tag>
      </div>
    </header>

    <el-result
      v-if="!loading && loadError"
      class="error-state"
      icon="error"
      title="待裁决列表加载失败"
      sub-title="当前网络异常或服务不可用，请稍后重试"
    >
      <template #extra>
        <el-button type="primary" @click="loadPending">重新加载</el-button>
      </template>
    </el-result>

    <el-empty
      v-else-if="!loading && !contracts.length"
      class="empty-state"
      description="暂无待裁决合约，所有见证投票都已处理完"
    >
      <div class="empty-guide">
        <div class="guide-title">合约进入待裁决的流程</div>
        <div class="guide-steps">
          <div class="guide-step">
            <span class="step-no">1</span>
            <span>在合约广场接受见证邀请</span>
          </div>
          <el-icon class="step-arrow"><ArrowRight /></el-icon>
          <div class="guide-step">
            <span class="step-no">2</span>
            <span>创建者履约，合约到期</span>
          </div>
          <el-icon class="step-arrow"><ArrowRight /></el-icon>
          <div class="guide-step">
            <span class="step-no">3</span>
            <span>回到本页投票裁决</span>
          </div>
        </div>
      </div>

      <div class="empty-actions">
        <el-button type="primary" @click="router.push('/square')">
          去合约广场逛逛
        </el-button>
        <el-button
          v-if="isMockMode"
          plain
          @click="router.push('/demo/control')"
        >
          去演示控制制造待裁决
        </el-button>
      </div>
    </el-empty>

    <div v-else class="contract-list">
      <el-card
        v-for="contract in contracts"
        :key="contract.contractId"
        class="vote-card"
        shadow="never"
      >
        <div class="card-head">
          <div class="title-tags">
            <el-tag size="small" effect="plain" round>
              {{ getCategoryText(contract.category) }}
            </el-tag>
            <ContractStatusTag :status="contract.status" />
          </div>
          <h2 class="card-title">{{ contract.title }}</h2>
          <div class="card-meta">
            <span>创建者：{{ contract.ownerNickname }}</span>
            <span>截止时间：{{ formatDateTime(contract.deadline) }}</span>
          </div>
        </div>

        <div class="proof-block">
          <div class="block-title">履约凭证</div>
          <template v-if="contract.proofSubmitted">
            <p class="proof-text">{{ contract.proofText }}</p>
            <div class="proof-time">
              提交时间：{{ formatDateTime(contract.submittedAt) }}
            </div>
          </template>
          <el-alert
            v-else
            type="warning"
            :closable="false"
            show-icon
            title="创建者尚未提交履约凭证"
          />
        </div>

        <div class="card-footer">
          <div class="summary">
            当前已投票 {{ contract.voteSummary?.total ?? 0 }} 票
            （成功 {{ contract.voteSummary?.success ?? 0 }} /
            失败 {{ contract.voteSummary?.fail ?? 0 }}），
            其他见证人投票内容保密
          </div>
          <div class="footer-actions">
            <el-button @click="router.push(`/contract/${contract.contractId}`)">
              查看详情
            </el-button>
            <el-button type="primary" @click="openVoteDialog(contract)">
              去投票
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 投票弹窗 -->
    <el-dialog v-model="dialogVisible" title="见证人投票" width="560px">
      <template v-if="currentContract">
        <div class="dialog-contract">
          <el-tag size="small" effect="plain" round>
            {{ getCategoryText(currentContract.category) }}
          </el-tag>
          <strong>{{ currentContract.title }}</strong>
        </div>

        <el-form
          ref="voteFormRef"
          :model="voteForm"
          :rules="voteRules"
          label-position="top"
        >
          <el-form-item label="你的裁决" prop="verdict">
            <el-radio-group v-model="voteForm.verdict" class="verdict-group">
              <el-radio value="SUCCESS" border>
                <span class="verdict-success">履约成功</span>
              </el-radio>
              <el-radio value="FAIL" border>
                <span class="verdict-fail">履约失败</span>
              </el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="投票评语" prop="comment">
            <el-input
              v-model.trim="voteForm.comment"
              type="textarea"
              :rows="4"
              maxlength="200"
              show-word-limit
              placeholder="请结合验收标准和履约凭证说明理由，合约完结后评语将公开"
            />
          </el-form-item>
        </el-form>
      </template>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitVote">
          确认投票
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Refresh } from '@element-plus/icons-vue'
import ContractStatusTag from '../components/ContractStatusTag.vue'
import { getPendingVotesApi, submitVoteApi } from '../api/contract'
import {
  formatDateTime,
  getCategoryText
} from '../utils/contract'

const router = useRouter()

// 演示控制入口仅 Mock 模式显示
const isMockMode = import.meta.env.VITE_USE_MOCK === 'true'

const loading = ref(false)
const submitting = ref(false)
const loadError = ref(false)
const contracts = ref([])

const dialogVisible = ref(false)
const currentContract = ref(null)
const voteFormRef = ref()

const voteForm = reactive({
  verdict: '',
  comment: ''
})

const voteRules = {
  verdict: [{ required: true, message: '请选择裁决结果', trigger: 'change' }],
  comment: [
    { required: true, message: '请填写投票评语', trigger: 'blur' },
    { max: 200, message: '投票评语不能超过 200 字', trigger: 'blur' }
  ]
}

async function loadPending() {
  loading.value = true
  loadError.value = false

  try {
    contracts.value = await getPendingVotesApi()
  } catch {
    // 区分“加载失败”与“确实没有待裁决合约”，失败时展示重试入口
    contracts.value = []
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function openVoteDialog(contract) {
  currentContract.value = contract
  voteForm.verdict = ''
  voteForm.comment = ''
  dialogVisible.value = true
}

async function handleSubmitVote() {
  const valid = await voteFormRef.value.validate().catch(() => false)

  if (!valid) {
    return
  }

  submitting.value = true

  try {
    const result = await submitVoteApi(currentContract.value.contractId, {
      verdict: voteForm.verdict,
      comment: voteForm.comment
    })

    // 无论是否触发自动结算，本人都不能再投，直接从待办列表移除
    contracts.value = contracts.value.filter(
      (item) => item.contractId !== currentContract.value.contractId
    )

    dialogVisible.value = false

    if (result?.settled) {
      ElMessage.success('投票成功，全部见证人已投票，合约已完成裁决')
    } else {
      ElMessage.success('投票成功，等待其他见证人投票')
    }
  } catch {
    // request.js 已统一展示 403/409 等错误
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadPending()
})
</script>

<style scoped>
.voting-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 32px 24px 48px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0 0 8px;
  color: #182230;
  font-size: 28px;
}

.page-header p {
  margin: 0;
  color: #667085;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.empty-state,
.error-state {
  padding: 44px 24px;
  background: #fff;
  border: 1px solid #e7eaf0;
  border-radius: 14px;
}

.empty-guide {
  margin-bottom: 22px;
}

.guide-title {
  margin-bottom: 14px;
  color: #344054;
  font-size: 13px;
  font-weight: 600;
}

.guide-steps {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.guide-step {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #667085;
  font-size: 13px;
}

.step-no {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #eaf1ff;
  color: #2f6bff;
  font-size: 12px;
  font-weight: 600;
}

.step-arrow {
  color: #b6c0d0;
}

.empty-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.contract-list {
  display: grid;
  gap: 18px;
}

.vote-card {
  border: 1px solid #e7eaf0;
  border-radius: 14px;
}

.title-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.card-title {
  margin: 0 0 10px;
  color: #182230;
  font-size: 18px;
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  color: #98a2b3;
  font-size: 13px;
}

.proof-block {
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 10px;
  background: #f7f8fc;
}

.block-title {
  margin-bottom: 8px;
  color: #344054;
  font-size: 13px;
  font-weight: 600;
}

.proof-text {
  margin: 0;
  color: #344054;
  font-size: 14px;
  line-height: 1.8;
}

.proof-time {
  margin-top: 8px;
  color: #98a2b3;
  font-size: 12px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
}

.summary {
  color: #667085;
  font-size: 13px;
}

.footer-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.dialog-contract {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #f7f8fc;
}

.verdict-group {
  display: flex;
  gap: 12px;
}

.verdict-success {
  color: #16a36a;
  font-weight: 600;
}

.verdict-fail {
  color: #e05252;
  font-weight: 600;
}

@media (max-width: 720px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .card-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .footer-actions {
    justify-content: flex-end;
  }

  /* 窄屏引导步骤纵向排列，隐藏步骤间箭头 */
  .guide-steps {
    flex-direction: column;
    align-items: flex-start;
  }

  .step-arrow {
    display: none;
  }
}
</style>
