<template>
  <div v-loading="loading" class="detail-page">
    <el-result
      v-if="loadError"
      class="detail-error"
      icon="error"
      title="合约加载失败"
      sub-title="合约可能已被删除，或当前网络异常，请稍后重试"
    >
      <template #extra>
        <el-button type="primary" @click="router.push('/square')">
          返回合约广场
        </el-button>
      </template>
    </el-result>

    <template v-else-if="detail">
      <div class="page-top">
        <el-button text @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
      </div>

      <el-card class="title-card" shadow="never">
        <div class="title-row">
          <div>
            <div class="title-tags">
              <el-tag size="small" effect="plain" round>
                {{ getCategoryText(detail.category) }}
              </el-tag>
              <ContractStatusTag :status="detail.status" />
            </div>
            <h1>{{ detail.title }}</h1>
          </div>

          <div class="action-buttons">
            <template v-if="isOwner && detail.status === 'ACTIVE'">
              <el-button @click="openInviteDialog">邀请见证人</el-button>
              <el-button type="primary" @click="openProofDialog">
                提交凭证
              </el-button>
            </template>

            <el-button
              v-if="isWitness && detail.status === 'VOTING' && !detail.myVote"
              type="primary"
              @click="router.push('/voting')"
            >
              去投票
            </el-button>

            <el-tag v-if="isWitness && detail.myVote" type="info" effect="plain">
              你已投票：{{ detail.myVote === 'SUCCESS' ? '履约成功' : '履约失败' }}
            </el-tag>
          </div>
        </div>

        <el-steps :active="stepActive" :status="stepStatus" align-center class="progress-steps">
          <el-step title="发起合约" :description="formatDateTime(detail.createdAt)" />
          <el-step title="等待到期" :description="formatDateTime(detail.deadline)" />
          <el-step title="见证裁决" />
          <el-step title="最终结果" />
        </el-steps>
      </el-card>

      <div class="content-grid">
        <div class="main-column">
          <el-card class="info-card" shadow="never">
            <template #header>
              <h2>验收标准</h2>
            </template>
            <p class="criteria-text">{{ detail.acceptCriteria }}</p>
          </el-card>

          <el-card class="info-card" shadow="never">
            <template #header>
              <h2>履约凭证</h2>
            </template>

            <template v-if="detail.proofSubmitted">
              <p class="criteria-text">{{ detail.proofText }}</p>
              <div class="proof-meta">
                提交时间：{{ formatDateTime(detail.submittedAt) }}
              </div>

              <div v-if="detail.proofImages?.length" class="proof-images">
                <el-image
                  v-for="(image, index) in detail.proofImages"
                  :key="index"
                  :src="image"
                  :preview-src-list="detail.proofImages"
                  :initial-index="index"
                  fit="cover"
                  class="proof-image"
                  preview-teleported
                >
                  <template #error>
                    <div class="image-error">图片加载失败</div>
                  </template>
                </el-image>
              </div>

              <el-empty
                v-else
                :image-size="80"
                description="暂无凭证图片"
              />
            </template>

            <el-empty v-else :image-size="100" description="创建者还未提交凭证" />
          </el-card>

          <el-card class="info-card" shadow="never">
            <template #header>
              <h2>投票结果</h2>
            </template>

            <div class="vote-summary">
              <div class="vote-item success">
                <span class="vote-number">{{ detail.voteSummary?.success ?? 0 }}</span>
                <span class="vote-label">认为履约成功</span>
              </div>
              <div class="vote-item fail">
                <span class="vote-number">{{ detail.voteSummary?.fail ?? 0 }}</span>
                <span class="vote-label">认为履约失败</span>
              </div>
              <div class="vote-item">
                <span class="vote-number">{{ detail.voteSummary?.total ?? 0 }}</span>
                <span class="vote-label">已投票人数</span>
              </div>
            </div>

            <el-table
              v-if="isFinished && detail.voteDetails?.length"
              :data="detail.voteDetails"
              class="vote-table"
            >
              <el-table-column prop="nickname" label="见证人" min-width="120" />
              <el-table-column label="投票结果" min-width="110">
                <template #default="{ row }">
                  <el-tag
                    :type="row.verdict === 'SUCCESS' ? 'success' : 'danger'"
                    size="small"
                  >
                    {{ row.verdict === 'SUCCESS' ? '履约成功' : '履约失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="comment" label="评语" min-width="200" />
              <el-table-column label="投票时间" min-width="170">
                <template #default="{ row }">
                  {{ formatDateTime(row.votedAt) }}
                </template>
              </el-table-column>
            </el-table>

            <el-alert
              v-else-if="!isFinished"
              class="vote-secret-tip"
              type="warning"
              :closable="false"
              show-icon
              title="投票进行中，其他见证人的具体投票和评语将在合约完结后公开"
            />
          </el-card>
        </div>

        <div class="side-column">
          <el-card v-if="detail.status === 'ACTIVE'" class="info-card countdown-card" shadow="never">
            <template #header>
              <h2>距离截止</h2>
            </template>

            <div v-if="countdown" class="countdown-digits">
              <template v-if="countdown.days > 0">
                <div class="digit-block">
                  <strong>{{ countdown.days }}</strong>
                  <span>天</span>
                </div>
              </template>
              <div class="digit-block">
                <strong>{{ countdown.hours }}</strong>
                <span>时</span>
              </div>
              <div class="digit-block">
                <strong>{{ countdown.minutes }}</strong>
                <span>分</span>
              </div>
              <div class="digit-block seconds">
                <strong>{{ countdown.seconds }}</strong>
                <span>秒</span>
              </div>
            </div>

            <el-alert
              v-else
              type="warning"
              :closable="false"
              show-icon
              title="已过截止时间"
              description="等待系统扫描后进入见证人裁决阶段"
            />
          </el-card>

          <el-card class="info-card" shadow="never">
            <template #header>
              <h2>押金信息</h2>
            </template>
            <div class="side-row">
              <span>押金金额</span>
              <strong class="deposit-text">{{ formatMoney(detail.deposit) }}</strong>
            </div>
            <div class="side-row">
              <span>截止时间</span>
              <strong>{{ formatDateTime(detail.deadline) }}</strong>
            </div>
            <div class="side-row">
              <span>围观次数</span>
              <strong>{{ detail.viewCount ?? 0 }}</strong>
            </div>
            <div class="side-row" v-if="isFinished">
              <span>最终结果</span>
              <ContractStatusTag :status="detail.status" />
            </div>
          </el-card>

          <el-card class="info-card" shadow="never">
            <template #header>
              <h2>创建者</h2>
            </template>
            <div class="person-row">
              <el-avatar :size="36" class="person-avatar">
                {{ (detail.ownerNickname || '?').slice(0, 1) }}
              </el-avatar>
              <div>
                <div class="person-name">{{ detail.ownerNickname }}</div>
                <div class="person-sub">发起于 {{ formatDateTime(detail.createdAt) }}</div>
              </div>
            </div>
          </el-card>

          <el-card class="info-card" shadow="never">
            <template #header>
              <h2>见证人（{{ detail.witnesses?.length || 0 }}）</h2>
            </template>

            <div
              v-for="witness in detail.witnesses"
              :key="witness.userId"
              class="person-row witness-row"
            >
              <el-avatar :size="34" class="person-avatar small">
                {{ (witness.nickname || '?').slice(0, 1) }}
              </el-avatar>
              <div>
                <div class="person-name">{{ witness.nickname }}</div>
                <div class="person-sub">
                  接受于 {{ formatDateTime(witness.acceptedAt) }}
                </div>
              </div>
            </div>

            <el-empty
              v-if="!detail.witnesses?.length"
              :image-size="80"
              description="暂无见证人"
            />
          </el-card>
        </div>
      </div>

      <!-- 邀请见证人弹窗 -->
      <el-dialog v-model="inviteDialogVisible" title="邀请见证人" width="520px">
        <div v-loading="inviteLoading">
          <el-alert
            type="info"
            :closable="false"
            show-icon
            class="invite-tip"
            title="把链接发给朋友，对方登录后打开即可接受邀请成为见证人"
          />

          <div v-if="inviteUrl" class="invite-link-box">
            <el-input :model-value="inviteUrl" readonly>
              <template #append>
                <el-button @click="handleCopyInvite">复制链接</el-button>
              </template>
            </el-input>
            <p class="invite-code">邀请码：{{ inviteCode }}</p>
          </div>
        </div>

        <template #footer>
          <el-button @click="inviteDialogVisible = false">关闭</el-button>
          <el-button
            v-if="inviteUrl"
            type="primary"
            @click="handleCopyInvite"
          >
            复制并关闭
          </el-button>
        </template>
      </el-dialog>

      <!-- 提交凭证弹窗 -->
      <el-dialog v-model="proofDialogVisible" title="提交履约凭证" width="560px">
        <el-form
          ref="proofFormRef"
          :model="proofForm"
          :rules="proofRules"
          label-position="top"
        >
          <el-form-item label="文字说明" prop="proofText">
            <el-input
              v-model.trim="proofForm.proofText"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              placeholder="描述你的履约情况，例如打卡记录、完成结果等"
            />
          </el-form-item>

          <el-form-item label="凭证图片地址">
            <div class="image-url-list">
              <div
                v-for="(url, index) in proofForm.proofImages"
                :key="index"
                class="image-url-row"
              >
                <el-input
                  v-model.trim="proofForm.proofImages[index]"
                  placeholder="粘贴图片地址，例如 https://example.com/proof.png"
                  clearable
                />
                <el-button
                  type="danger"
                  plain
                  @click="removeImageUrl(index)"
                >
                  删除
                </el-button>
              </div>
            </div>

            <el-button class="add-image-button" @click="addImageUrl">
              <el-icon><Plus /></el-icon>
              添加图片地址
            </el-button>
            <div class="form-hint">
              图片上传功能等后端文件接口完成后接入，当前先填写图片地址
            </div>
          </el-form-item>
        </el-form>

        <template #footer>
          <el-button @click="proofDialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            :loading="proofSubmitting"
            @click="handleSubmitProof"
          >
            提交凭证
          </el-button>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import ContractStatusTag from '../components/ContractStatusTag.vue'
import {
  createInviteApi,
  getContractDetailApi,
  submitProofApi
} from '../api/contract'
import { increaseViewCountApi } from '../api/square'
import { useAuthStore } from '../stores/auth'
import {
  formatDateTime,
  formatMoney,
  getCategoryText
} from '../utils/contract'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const loadError = ref(false)
const detail = ref(null)

const currentUserId = computed(() => authStore.profile?.userId ?? null)

const isOwner = computed(
  () => Boolean(currentUserId.value) && detail.value?.ownerUserId === currentUserId.value
)

const isWitness = computed(() =>
  Boolean(
    detail.value?.witnesses?.some(
      (witness) => witness.userId === currentUserId.value
    )
  )
)

const isFinished = computed(() =>
  ['SUCCESS', 'FAILED'].includes(detail.value?.status)
)

// 步骤条：ACTIVE=1，VOTING=2，完结=4（失败时整体标红）
const stepActive = computed(() => {
  const statusMap = {
    ACTIVE: 1,
    VOTING: 2,
    SUCCESS: 4,
    FAILED: 4
  }

  return statusMap[detail.value?.status] ?? 0
})

const stepStatus = computed(() =>
  detail.value?.status === 'FAILED' ? 'error' : 'finish'
)

// ---------- 截止倒计时（仅进行中展示） ----------
const now = ref(Date.now())
let countdownTimer = null

const countdown = computed(() => {
  if (!detail.value?.deadline) {
    return null
  }

  const deadlineTime = new Date(
    String(detail.value.deadline).replace(' ', 'T')
  ).getTime()

  if (Number.isNaN(deadlineTime)) {
    return null
  }

  const remain = deadlineTime - now.value

  if (remain <= 0) {
    return null
  }

  const pad = (num) => String(num).padStart(2, '0')
  const totalSeconds = Math.floor(remain / 1000)

  return {
    days: Math.floor(totalSeconds / 86400),
    hours: pad(Math.floor((totalSeconds % 86400) / 3600)),
    minutes: pad(Math.floor((totalSeconds % 3600) / 60)),
    seconds: pad(totalSeconds % 60)
  }
})

// ---------- 邀请见证人 ----------
const inviteDialogVisible = ref(false)
const inviteLoading = ref(false)
const inviteUrl = ref('')
const inviteCode = ref('')

async function openInviteDialog() {
  inviteDialogVisible.value = true
  inviteUrl.value = ''
  inviteCode.value = ''
  inviteLoading.value = true

  try {
    const data = await createInviteApi(route.params.id)
    inviteUrl.value = data.inviteUrl
    inviteCode.value = data.inviteCode
  } catch {
    inviteDialogVisible.value = false
  } finally {
    inviteLoading.value = false
  }
}

async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text)
  } catch {
    // 非安全上下文或浏览器限制时的兜底方案
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
  }
}

async function handleCopyInvite() {
  if (!inviteUrl.value) {
    return
  }

  await copyText(inviteUrl.value)
  ElMessage.success('邀请链接已复制')
  inviteDialogVisible.value = false
}

// ---------- 提交凭证 ----------
const proofDialogVisible = ref(false)
const proofSubmitting = ref(false)
const proofFormRef = ref()

const proofForm = reactive({
  proofText: '',
  proofImages: []
})

const proofRules = {
  proofText: [
    { required: true, message: '请填写凭证文字说明', trigger: 'blur' },
    { max: 500, message: '文字说明不能超过 500 字', trigger: 'blur' }
  ]
}

function openProofDialog() {
  proofForm.proofText = ''
  proofForm.proofImages = []
  proofDialogVisible.value = true
}

function addImageUrl() {
  proofForm.proofImages.push('')
}

function removeImageUrl(index) {
  proofForm.proofImages.splice(index, 1)
}

async function handleSubmitProof() {
  const valid = await proofFormRef.value.validate().catch(() => false)

  if (!valid) {
    return
  }

  proofSubmitting.value = true

  try {
    await submitProofApi(route.params.id, {
      proofText: proofForm.proofText,
      proofImages: proofForm.proofImages.filter((url) => url)
    })

    ElMessage.success('凭证提交成功')
    proofDialogVisible.value = false
    await loadDetail()
  } catch {
    // request.js 已统一展示 400/409 等后端错误
  } finally {
    proofSubmitting.value = false
  }
}

async function loadDetail() {
  loading.value = true
  loadError.value = false

  try {
    detail.value = await getContractDetailApi(route.params.id)

    // 围观数 +1，失败不影响页面展示
    increaseViewCountApi(route.params.id).catch(() => {})
  } catch {
    detail.value = null
    loadError.value = true
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDetail()
  countdownTimer = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onBeforeUnmount(() => {
  if (countdownTimer) {
    window.clearInterval(countdownTimer)
  }
})
</script>

<style scoped>
.detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 24px 48px;
}

.page-top {
  margin-bottom: 12px;
}

.detail-error {
  padding-top: 60px;
  background: #fff;
  border: 1px solid #e7eaf0;
  border-radius: 14px;
}

.title-card,
.info-card {
  border: 1px solid #e7eaf0;
  border-radius: 14px;
}

.title-card {
  margin-bottom: 20px;
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 28px;
}

.title-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.title-row h1 {
  margin: 0;
  color: #182230;
  font-size: 24px;
  line-height: 1.4;
}

.action-buttons {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.progress-steps {
  margin-top: 8px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(300px, 1fr);
  gap: 20px;
}

.info-card {
  margin-bottom: 20px;
}

.info-card :deep(.el-card__header) {
  padding: 14px 18px;
}

.info-card h2 {
  margin: 0;
  color: #182230;
  font-size: 16px;
}

.criteria-text {
  margin: 0;
  color: #344054;
  font-size: 14px;
  line-height: 1.8;
}

.proof-meta {
  margin-top: 12px;
  color: #98a2b3;
  font-size: 13px;
}

.proof-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.proof-image {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e7eaf0;
}

.image-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #98a2b3;
  font-size: 12px;
  background: #f7f8fc;
}

.vote-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.vote-item {
  padding: 18px 12px;
  border-radius: 12px;
  background: #f7f8fc;
  text-align: center;
}

.vote-item.success .vote-number {
  color: #16a36a;
}

.vote-item.fail .vote-number {
  color: #e05252;
}

.vote-number {
  display: block;
  font-size: 28px;
  font-weight: 700;
  color: #182230;
}

.vote-label {
  display: block;
  margin-top: 6px;
  color: #667085;
  font-size: 13px;
}

.vote-secret-tip {
  margin-top: 4px;
}

.side-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  color: #667085;
  font-size: 14px;
  border-bottom: 1px solid #f0f1f5;
}

.side-row:last-child {
  border-bottom: 0;
}

.side-row strong {
  color: #182230;
}

.deposit-text {
  color: #5b5fef !important;
}

.person-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* ===== 倒计时卡片 ===== */
.countdown-card {
  border-color: #c7c9fb;
  background: linear-gradient(160deg, #f6f6ff, #fff 62%);
}

.countdown-digits {
  display: flex;
  justify-content: center;
  gap: 10px;
}

.digit-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 58px;
  padding: 12px 8px;
  border: 1px solid #e3e4ff;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 6px 16px rgb(91 95 239 / 10%);
}

.digit-block strong {
  color: #5b5fef;
  font-size: 26px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.digit-block.seconds strong {
  color: #e05252;
}

.digit-block span {
  color: #98a2b3;
  font-size: 12px;
}

.witness-row {
  padding: 10px 0;
  border-bottom: 1px solid #f0f1f5;
}

.witness-row:last-of-type {
  border-bottom: 0;
}

.person-avatar {
  flex-shrink: 0;
  background: #5b5fef;
  color: #fff;
}

.person-name {
  color: #182230;
  font-size: 14px;
  font-weight: 600;
}

.person-sub {
  margin-top: 2px;
  color: #98a2b3;
  font-size: 12px;
}

.invite-tip {
  margin-bottom: 16px;
}

.invite-code {
  margin: 10px 0 0;
  color: #667085;
  font-size: 13px;
}

.image-url-list {
  width: 100%;
}

.image-url-row {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

.add-image-button {
  width: 100%;
  border-style: dashed;
}

.form-hint {
  margin-top: 8px;
  color: #98a2b3;
  font-size: 12px;
}

@media (max-width: 900px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .title-row {
    flex-direction: column;
  }
}
</style>