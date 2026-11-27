<template>
  <div class="create-page">
    <div class="page-top">
      <el-button text @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
    </div>

    <div class="page-layout">
      <el-card class="form-card" shadow="never">
        <template #header>
          <div class="card-header">
            <h1>发起对赌</h1>
            <p>公开你的承诺，缴纳押金并邀请见证人，按时完成即可取回押金</p>
          </div>
        </template>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="create-form"
        >
          <el-form-item label="合约标题" prop="title">
            <el-input
              v-model.trim="form.title"
              maxlength="50"
              show-word-limit
              placeholder="5-50 个字符，例如：连续 30 天每天背 50 个英语单词"
              clearable
            />
          </el-form-item>

          <el-form-item label="合约分类" prop="category">
            <el-select
              v-model="form.category"
              class="full-width"
              placeholder="请选择分类"
            >
              <el-option
                v-for="(text, value) in CONTRACT_CATEGORY_TEXT"
                :key="value"
                :label="text"
                :value="value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="验收标准" prop="acceptCriteria">
            <el-input
              v-model.trim="form.acceptCriteria"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              placeholder="明确怎样才算完成，见证人将依据此标准投票，例如：每天提交单词打卡截图，缺卡不超过 1 天"
            />
          </el-form-item>

          <el-form-item label="押金金额（虚拟币）" prop="deposit">
            <el-input-number
              v-model="form.deposit"
              class="full-width"
              :min="1"
              :max="availableBalance"
              :step="50"
              :precision="0"
              controls-position="right"
              placeholder="请输入正整数"
            />
            <div class="field-hint">
              创建后押金将立即冻结；履约成功全额退回，未履约则按规则扣除
            </div>
          </el-form-item>

          <el-form-item label="截止时间" prop="deadline">
            <el-date-picker
              v-model="form.deadline"
              class="full-width"
              type="datetime"
              placeholder="请选择晚于当前时间的截止时间"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              :disabled-date="disabledPastDate"
              :clearable="false"
            />
            <div class="field-hint">截止时间到达后，合约将进入见证人投票裁决阶段</div>
          </el-form-item>

          <el-form-item>
            <div class="form-actions">
              <el-button @click="router.push('/square')">取消</el-button>
              <el-button
                type="primary"
                :loading="submitting"
                @click="handleSubmit"
              >
                确认发起
              </el-button>
            </div>
          </el-form-item>
        </el-form>
      </el-card>

      <div class="side-column">
        <el-card class="balance-card" shadow="never">
          <template #header>
            <h2>我的资金</h2>
          </template>

          <div class="balance-row">
            <span>可用余额</span>
            <strong class="available">{{ formatMoney(availableBalance) }}</strong>
          </div>
          <div class="balance-row">
            <span>冻结金额</span>
            <strong>{{ formatMoney(authStore.profile?.frozen) }}</strong>
          </div>
          <div class="balance-row">
            <span>本次押金</span>
            <strong class="deposit">{{ formatMoney(form.deposit || 0) }}</strong>
          </div>
          <el-divider class="balance-divider" />
          <div class="balance-row">
            <span>发起后可用余额</span>
            <strong :class="{ insufficient: remainingBalance < 0 }">
              {{ formatMoney(remainingBalance) }}
            </strong>
          </div>

          <el-alert
            v-if="availableBalance <= 0"
            class="balance-alert"
            type="error"
            :closable="false"
            show-icon
            title="可用余额不足，暂无法发起对赌"
          />
        </el-card>

        <el-card class="rule-card" shadow="never">
          <template #header>
            <h2>发起规则</h2>
          </template>
          <ul class="rule-list">
            <li>押金为正整数虚拟币，且不能超过可用余额</li>
            <li>截止时间必须晚于当前时间</li>
            <li>合约创建后可邀请朋友成为见证人</li>
            <li>到期后由见证人按验收标准投票裁决</li>
          </ul>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { createContractApi } from '../api/contract'
import { useAuthStore } from '../stores/auth'
import { CONTRACT_CATEGORY_TEXT, formatMoney } from '../utils/contract'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref()
const submitting = ref(false)

const form = reactive({
  title: '',
  category: '',
  acceptCriteria: '',
  deposit: 100,
  deadline: ''
})

const availableBalance = computed(() =>
  Number(authStore.profile?.balance ?? 0)
)

const remainingBalance = computed(() => availableBalance.value - Number(form.deposit || 0))

const validateDeposit = (rule, value, callback) => {
  if (value === null || value === undefined || value === '') {
    callback(new Error('请输入押金金额'))
    return
  }

  if (!Number.isInteger(Number(value)) || Number(value) <= 0) {
    callback(new Error('押金必须为大于 0 的整数'))
    return
  }

  if (Number(value) > availableBalance.value) {
    callback(new Error('押金不能超过可用余额'))
    return
  }

  callback()
}

const validateDeadline = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请选择截止时间'))
    return
  }

  const deadlineTime = new Date(String(value).replace(' ', 'T')).getTime()

  if (Number.isNaN(deadlineTime)) {
    callback(new Error('截止时间格式不正确'))
    return
  }

  if (deadlineTime <= Date.now()) {
    callback(new Error('截止时间必须晚于当前时间'))
    return
  }

  callback()
}

const rules = {
  title: [
    { required: true, message: '请输入合约标题', trigger: 'blur' },
    { min: 5, max: 50, message: '合约标题需为 5-50 个字符', trigger: 'blur' }
  ],
  category: [{ required: true, message: '请选择合约分类', trigger: 'change' }],
  acceptCriteria: [
    { required: true, message: '请输入验收标准', trigger: 'blur' },
    { max: 500, message: '验收标准不能超过 500 字', trigger: 'blur' }
  ],
  deposit: [{ required: true, validator: validateDeposit, trigger: 'change' }],
  deadline: [{ required: true, validator: validateDeadline, trigger: 'change' }]
}

// 日期选择器层面禁选今天之前的日期（当天仍需配合时间校验）
function disabledPastDate(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)

  if (!valid) {
    return
  }

  submitting.value = true

  try {
    const data = await createContractApi({
      title: form.title,
      category: form.category,
      acceptCriteria: form.acceptCriteria,
      deposit: Number(form.deposit),
      deadline: form.deadline
    })

    // 刷新余额，让顶部导航和侧栏立即反映冻结结果
    await authStore.fetchProfile()

    ElMessage.success('合约创建成功，押金已冻结')
    router.replace(`/contract/${data.contractId}`)
  } catch {
    // request.js 已统一展示后端错误
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  // 刷新页面后 pinia 内存丢失，需要重新拉取用户信息才能拿到余额
  if (authStore.token && !authStore.profileLoaded) {
    authStore.fetchProfile().catch(() => {})
  }
})
</script>

<style scoped>
.create-page { max-width: 1200px; margin: 0 auto; padding: 24px 24px 48px; }
.page-top { margin-bottom: 12px; }
.page-layout { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 20px; align-items: start; }
.form-card, .balance-card, .rule-card { border: 1px solid var(--line); border-radius: var(--radius-card); }
.card-header h1 { margin: 0 0 8px; color: var(--ink); font-family: var(--font-display); font-size: 25px; letter-spacing: .04em; }
.card-header p { margin: 0; color: var(--ink-3); font-size: 13px; }
.create-form { max-width: 640px; }
.full-width { width: 100%; }
.field-hint { margin-top: 6px; color: var(--ink-4); font-size: 12px; line-height: 1.6; }
.form-actions { display: flex; justify-content: flex-end; gap: 12px; width: 100%; }
.side-column { display: grid; gap: 20px; }
.balance-card h2, .rule-card h2 { margin: 0; color: var(--ink); font-family: var(--font-display); font-size: 17px; letter-spacing: .04em; }
.balance-row { display: flex; justify-content: space-between; align-items: center; padding: 9px 0; color: var(--ink-3); font-size: 14px; }
.balance-row strong { color: var(--ink); font-weight: 600; }
.balance-row strong.available { color: var(--brand); font-size: 17px; }
.balance-row strong.deposit { color: var(--warning-dark); }
.balance-row strong.insufficient { color: var(--danger); }
.balance-divider { margin: 8px 0; }
.balance-alert { margin-top: 8px; }
.rule-list { margin: 0; padding-left: 18px; color: var(--ink-3); font-size: 13px; line-height: 2; }
@media (max-width: 960px) {
  .page-layout { grid-template-columns: 1fr; }
}
</style>
