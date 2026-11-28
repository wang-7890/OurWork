<template>
  <div class="app-layout">
    <header class="app-header">
      <div class="header-inner">
        <RouterLink to="/square" class="logo" aria-label="微承诺首页">
          <svg class="logo-mark" viewBox="0 0 36 36" fill="none" aria-hidden="true">
            <path d="M7 9.5 18 4l11 5.5v17L18 32 7 26.5v-17Z" stroke="currentColor" stroke-width="1.6" />
            <path d="m11.5 18 4.2 4.2 8.8-9" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <span class="logo-text"><strong>微承诺</strong><small>MICRO PACT</small></span>
        </RouterLink>

        <el-menu
          class="nav-menu"
          mode="horizontal"
          :default-active="activeMenu"
          :ellipsis="false"
          router
        >
          <el-menu-item index="/square">合约广场</el-menu-item>
          <el-menu-item v-if="authStore.isLoggedIn" index="/contract/create">发起契约</el-menu-item>
          <el-menu-item v-if="authStore.isLoggedIn" index="/voting">待裁决</el-menu-item>
          <el-menu-item v-if="authStore.isLoggedIn && isMockMode" index="/demo/control">演示控制</el-menu-item>
        </el-menu>

        <div class="header-right">
          <template v-if="authStore.isLoggedIn">
            <div
              class="balance-chip"
              :title="`可用 ${formatMoney(authStore.profile?.balance)}，冻结 ${formatMoney(authStore.profile?.frozen)}`"
              role="button"
              tabindex="0"
              @click="router.push('/profile')"
              @keyup.enter="router.push('/profile')"
            >
              <el-icon><Wallet /></el-icon>
              <span class="balance-value">{{ formatMoney(authStore.profile?.balance) }}</span>
            </div>

            <el-badge
              :value="authStore.unreadCount"
              :hidden="authStore.unreadCount === 0"
              class="notice-badge"
            >
              <el-button text aria-label="通知中心" @click="router.push('/notifications')">
                <el-icon :size="20"><Bell /></el-icon>
              </el-button>
            </el-badge>

            <el-dropdown trigger="click" @command="handleCommand">
              <div class="user-info" tabindex="0">
                <el-avatar :size="32" class="user-avatar">{{ userInitial }}</el-avatar>
                <span class="user-name">{{ authStore.profile?.nickname || '用户' }}</span>
                <el-icon><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>

          <template v-else>
            <el-button text @click="router.push('/login')">登录</el-button>
            <el-button type="primary" @click="router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="app-main"><slot /></main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowDown, Bell, Wallet } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { formatMoney } from '../utils/contract'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isMockMode = import.meta.env.VITE_USE_MOCK === 'true'

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/contract/create')) return '/contract/create'
  if (path.startsWith('/contract/') || path.startsWith('/square')) return '/square'
  if (path.startsWith('/voting')) return '/voting'
  return path
})

const userInitial = computed(() => {
  const name = authStore.profile?.nickname || '?'
  return name.slice(0, 1)
})

async function handleCommand(command) {
  if (command === 'profile') router.push('/profile')

  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定退出登录吗？', '提示', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch {
      return
    }

    await authStore.logout()
    router.replace('/login')
  }
}
</script>

<style scoped>
.app-layout { min-height: 100vh; }
.app-header {
  position: sticky;
  top: 0;
  z-index: 10;
  height: 68px;
  background: rgb(247 248 245 / 96%);
  border-bottom: 1px solid var(--line);
  backdrop-filter: blur(10px);
}
.header-inner { max-width: 1200px; height: 100%; margin: 0 auto; padding: 0 24px; display: flex; align-items: center; gap: 32px; }
.logo { display: flex; align-items: center; gap: 10px; flex-shrink: 0; min-height: 44px; }
.logo-mark { width: 34px; height: 34px; color: var(--brand); }
.logo-text strong { display: block; color: var(--ink); font-size: 16px; letter-spacing: .1em; line-height: 1.25; }
.logo-text small { display: block; margin-top: 2px; color: var(--ink-3); font-size: 8px; letter-spacing: .16em; }
.nav-menu { flex: 1; border-bottom: 0; background: transparent; }
.nav-menu :deep(.el-menu-item) { height: 68px; line-height: 68px; color: var(--ink-3); border-bottom-width: 1px; }
.nav-menu :deep(.el-menu-item:hover) { color: var(--brand); background: transparent; }
.nav-menu :deep(.el-menu-item.is-active) { color: var(--brand); border-bottom-color: var(--brand); }
.header-right { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.balance-chip { display: flex; align-items: center; gap: 7px; min-height: 34px; padding: 5px 11px; border: 1px solid var(--line); border-radius: 3px; background: var(--panel); color: var(--brand); font-size: 13px; font-weight: 600; cursor: pointer; transition: border-color 160ms ease, background-color 160ms ease; }
.balance-chip:hover { border-color: var(--brand-border); background: var(--brand-bg); }
.balance-value { white-space: nowrap; }
.notice-badge { display: flex; align-items: center; }
.notice-badge :deep(.el-badge__content) { background: var(--brand); animation: pulse-dot 2s infinite; }
.user-info { display: flex; align-items: center; gap: 8px; min-height: 44px; padding: 4px 6px; border-radius: 3px; cursor: pointer; }
.user-info:hover { background: var(--brand-bg); }
.user-avatar { background: var(--brand); color: #fff8f3; font-family: var(--font-display); font-size: 15px; }
.user-name { color: var(--ink-2); font-size: 14px; }
.app-main { min-height: calc(100vh - 68px); }
@media (max-width: 1100px) {
  .header-inner { gap: 16px; padding: 0 16px; }
  .logo-text small, .user-name { display: none; }
  .nav-menu { min-width: 0; }
  .nav-menu :deep(.el-menu-item) { padding: 0 11px; }
  .header-right { gap: 6px; }
}
@media (max-width: 900px) {
  .logo-text { display: none; }
  .nav-menu :deep(.el-menu-item) { padding: 0 9px; font-size: 13px; }
}
@media (max-width: 768px) {
  .header-inner { gap: 10px; padding: 0 12px; }
  .logo-text, .user-name { display: none; }
  .balance-chip { padding-inline: 8px; }
}
</style>
