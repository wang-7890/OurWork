<template>
  <div class="auth-page">
    <section class="brand-panel">
      <div class="brand-content">
        <div class="brand-badge">公开承诺 · 共同见证</div>
        <h1>加入微承诺</h1>
        <p>
          注册即可领取 1000 虚拟币体验金，
          创建你的第一个公开承诺合约。
        </p>

        <div class="feature-list">
          <div class="feature-item">
            <span class="feature-dot"></span>
            新用户赠送 1000 虚拟币
          </div>
          <div class="feature-item">
            <span class="feature-dot"></span>
            每笔资金变动都有流水记录
          </div>
          <div class="feature-item">
            <span class="feature-dot"></span>
            诚信档案实时统计履约率
          </div>
        </div>
      </div>
    </section>

    <section class="form-panel">
      <el-card class="auth-card" shadow="never">
        <div class="card-header">
          <h2>创建账号</h2>
          <p>填写以下信息完成注册</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model.trim="form.username"
              :prefix-icon="User"
              autocomplete="username"
              placeholder="4-20 位字母、数字或下划线"
              clearable
            />
          </el-form-item>

          <el-form-item label="昵称" prop="nickname">
            <el-input
              v-model.trim="form.nickname"
              :prefix-icon="Avatar"
              placeholder="请输入昵称，最多 30 个字符"
              clearable
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              :prefix-icon="Lock"
              autocomplete="new-password"
              placeholder="至少 6 位"
              show-password
              type="password"
            />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              :prefix-icon="Lock"
              autocomplete="new-password"
              placeholder="请再次输入密码"
              show-password
              type="password"
              @keyup.enter="handleRegister"
            />
          </el-form-item>

          <el-form-item label="安全验证" required>
            <ClickCaptcha ref="captchaRef" v-model="captchaPassed" />
          </el-form-item>

          <el-button
            class="submit-button"
            type="primary"
            :loading="loading"
            @click="handleRegister"
          >
            注册并登录
          </el-button>
        </el-form>

        <div class="auth-footer">
          <span>已有账号？</span>
          <RouterLink :to="loginLink" class="link">返回登录</RouterLink>
        </div>
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Avatar, Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import ClickCaptcha from '../components/ClickCaptcha.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const formRef = ref()
const captchaRef = ref()
const loading = ref(false)
const captchaPassed = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
    return
  }

  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }

  callback()
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_]{4,20}$/,
      message: '用户名必须为 4-20 位字母、数字或下划线',
      trigger: 'blur'
    }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 30, message: '昵称不能超过 30 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const loginLink = computed(() => ({
  path: '/login',
  query: route.query.redirect ? { redirect: route.query.redirect } : {}
}))

function getSafeRedirect() {
  const redirect = route.query.redirect

  // 只允许站内地址，防止跳转到外部网站
  if (
    typeof redirect === 'string' &&
    redirect.startsWith('/') &&
    !redirect.startsWith('//')
  ) {
    return redirect
  }

  return '/square'
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)

  if (!valid) {
    return
  }

  if (!captchaPassed.value) {
    ElMessage.warning('请先完成点击验证码')
    return
  }

  loading.value = true

  try {
    await authStore.register({
      username: form.username,
      nickname: form.nickname,
      password: form.password
    })

    ElMessage.success('注册成功，已自动登录')
    router.replace(getSafeRedirect())
  } catch {
    // request.js 已经统一显示后端错误；注册失败后验证码作废，需要重新点选
    captchaPassed.value = false
    captchaRef.value?.reset()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(420px, 1fr) minmax(480px, 1fr);
  background:
    radial-gradient(circle at 15% 20%, rgb(91 95 239 / 14%), transparent 28%),
    #f5f7fb;
}

.brand-panel {
  display: flex;
  align-items: center;
  padding: 64px;
  color: #fff;
  background:
    linear-gradient(145deg, rgb(24 34 48 / 94%), rgb(45 50 120 / 92%)),
    #182230;
}

.brand-content {
  max-width: 560px;
}

.brand-badge {
  display: inline-flex;
  padding: 7px 12px;
  border: 1px solid rgb(255 255 255 / 18%);
  border-radius: 999px;
  color: #c9cdfb;
  background: rgb(255 255 255 / 8%);
  font-size: 13px;
}

.brand-panel h1 {
  margin: 24px 0 18px;
  font-size: clamp(38px, 5vw, 58px);
  line-height: 1.08;
  letter-spacing: -1px;
}

.brand-panel p {
  margin: 0;
  color: #d6d9e8;
  font-size: 17px;
  line-height: 1.9;
}

.feature-list {
  display: grid;
  gap: 14px;
  margin-top: 40px;
}

.feature-item {
  display: flex;
  gap: 10px;
  align-items: center;
  color: #eef0ff;
}

.feature-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #8f94ff;
  box-shadow: 0 0 0 5px rgb(143 148 255 / 14%);
}

.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 32px;
}

.auth-card {
  width: 100%;
  max-width: 430px;
  border: 1px solid #e7eaf0;
  border-radius: 18px;
  box-shadow: 0 16px 50px rgb(24 34 48 / 8%);
}

.card-header {
  margin-bottom: 28px;
  text-align: center;
}

.card-header h2 {
  margin: 0 0 8px;
  color: #182230;
  font-size: 28px;
}

.card-header p {
  margin: 0;
  color: #667085;
}

.submit-button {
  width: 100%;
  height: 44px;
  margin-top: 4px;
  border: 0;
  border-radius: 10px;
  background: #5b5fef;
  font-weight: 600;
}

.submit-button:hover {
  background: #4b4ee8;
}

.auth-footer {
  display: flex;
  gap: 6px;
  justify-content: center;
  margin-top: 24px;
  color: #667085;
  font-size: 14px;
}

.link {
  color: #5b5fef;
  font-weight: 600;
}

.link:hover {
  text-decoration: underline;
}

@media (max-width: 860px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    display: none;
  }

  .form-panel {
    min-height: 100vh;
    padding: 24px 16px;
  }
}
</style>
