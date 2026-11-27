<template>
  <AuthShell mode="register">
    <div class="auth-form-wrap">
      <div class="auth-form-heading">
        <p>MAKE A PROMISE</p>
        <h2>立下第一份约定</h2>
        <p>注册即领取 1000 虚拟币体验金。</p>
      </div>

      <el-form
        ref="formRef"
        class="auth-form"
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
            placeholder="4–20 位字母、数字或下划线"
            clearable
          />
        </el-form-item>

        <el-form-item label="昵称" prop="nickname">
          <el-input
            v-model.trim="form.nickname"
            :prefix-icon="Avatar"
            placeholder="大家会如何称呼你"
            clearable
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            :prefix-icon="Lock"
            autocomplete="new-password"
            placeholder="请输入 6–20 位密码"
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
          class="auth-submit"
          type="primary"
          :loading="loading"
          @click="handleRegister"
        >
          注册并登录
        </el-button>
      </el-form>

      <div class="auth-switch-row">
        <span>已经有账号？</span>
        <RouterLink :to="loginLink" class="link">返回登录</RouterLink>
      </div>
    </div>
  </AuthShell>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Avatar, Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import ClickCaptcha from '../components/ClickCaptcha.vue'
import AuthShell from '../components/AuthShell.vue'

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

  if (!valid) return

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
    captchaPassed.value = false
    captchaRef.value?.reset()
  } finally {
    loading.value = false
  }
}
</script>
