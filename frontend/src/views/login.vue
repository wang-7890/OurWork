<template>
  <AuthShell mode="login">
    <div class="auth-form-wrap">
      <div class="auth-form-heading">
        <p>KEEP YOUR WORD</p>
        <h2>归来赴约</h2>
        <p>你的承诺，正在这里等你。</p>
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

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            :prefix-icon="Lock"
            autocomplete="current-password"
            placeholder="请输入 6–20 位密码"
            show-password
            type="password"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item label="安全验证" required>
          <ClickCaptcha ref="captchaRef" v-model="captchaPassed" />
        </el-form-item>

        <el-button
          class="auth-submit"
          type="primary"
          :loading="loading"
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>

      <div class="auth-switch-row">
        <span>还没有账号？</span>
        <RouterLink :to="registerLink" class="link">创建账号</RouterLink>
      </div>

      <div class="auth-demo">
        <span class="auth-demo-badge">DEMO</span>
        <p>演示账号：demo1 / 123456<br>demo2 / 123456</p>
      </div>
    </div>
  </AuthShell>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
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
  username: 'demo1',
  password: '123456'
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_]{4,20}$/,
      message: '用户名必须为 4-20 位字母、数字或下划线',
      trigger: 'blur'
    }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ]
}

const registerLink = computed(() => ({
  path: '/register',
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

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)

  if (!valid) return

  if (!captchaPassed.value) {
    ElMessage.warning('请先完成点击验证码')
    return
  }

  loading.value = true

  try {
    await authStore.login({
      username: form.username,
      password: form.password
    })

    ElMessage.success('登录成功')
    router.replace(getSafeRedirect())
  } catch {
    captchaPassed.value = false
    captchaRef.value?.reset()
  } finally {
    loading.value = false
  }
}
</script>
