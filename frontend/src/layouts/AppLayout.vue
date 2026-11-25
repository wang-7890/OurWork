<template>
  <div class="app-layout">
    <header class="app-header">
      <div class="header-inner">
        <RouterLink to="/square" class="logo">
          <span class="logo-mark">微</span>
          <span class="logo-text">微承诺</span>
        </RouterLink>

        <el-menu
          class="nav-menu"
          mode="horizontal"
          :default-active="activeMenu"
          :ellipsis="false"
          router
        >
          <el-menu-item index="/square">合约广场</el-menu-item>
          <el-menu-item v-if="authStore.isLoggedIn" index="/contract/create">
            发起对赌
          </el-menu-item>
          <el-menu-item v-if="authStore.isLoggedIn" index="/voting">
            待裁决
          </el-menu-item>
          <el-menu-item
            v-if="authStore.isLoggedIn && isMockMode"
            index="/demo/control"
          >
            演示控制
          </el-menu-item>
        </el-menu>

        <div class="header-right">
          <template v-if="authStore.isLoggedIn">
            <div
              class="balance-chip"
              :title="`可用 ${formatMoney(authStore.profile?.balance)}，冻结 ${formatMoney(authStore.profile?.frozen)}`"
              @click="router.push('/profile')"
            >
              <el-icon><Wallet /></el-icon>
              <span class="balance-value">
                {{ formatMoney(authStore.profile?.balance) }}
              </span>
            </div>

            <el-badge
              :value="authStore.unreadCount"
              :hidden="authStore.unreadCount === 0"
              class="notice-badge"
            >
              <el-button text @click="router.push('/notifications')">
                <el-icon :size="20"><Bell /></el-icon>
              </el-button>
            </el-badge>

            <el-dropdown trigger="click" @command="handleCommand">
              <div class="user-info">
                <el-avatar :size="32" class="user-avatar">
                  {{ userInitial }}
                </el-avatar>
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
            <el-button type="primary" @click="router.push('/register')">
              注册
            </el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="app-main">
      <slot />
    </main>
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

// 演示控制入口仅 Mock 模式显示，联调/生产环境自动隐藏
const isMockMode = import.meta.env.VITE_USE_MOCK === 'true'

// 详情页不属于独立菜单，进入详情时保持“合约广场”高亮
const activeMenu = computed(() => {
  const path = route.path

  if (path.startsWith('/contract/create')) {
    return '/contract/create'
  }

  if (path.startsWith('/contract/') || path.startsWith('/square')) {
    return '/square'
  }

  if (path.startsWith('/voting')) {
    return '/voting'
  }

  return path
})

const userInitial = computed(() => {
  const name = authStore.profile?.nickname || '?'
  return name.slice(0, 1)
})

async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  }

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
.app-layout {
  min-height: 100vh;
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  height: 64px;
  background: rgb(255 255 255 / 96%);
  border-bottom: 1px solid #e7eaf0;
  backdrop-filter: blur(8px);
}

.header-inner {
  max-width: 1200px;
  height: 100%;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 32px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 9px;
  flex-shrink: 0;
}

.logo-mark {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #5b5fef, #8f94ff);
  color: #fff;
  font-weight: 700;
  box-shadow: 0 4px 12px rgb(91 95 239 / 32%);
}

.logo-text {
  color: #182230;
  font-size: 18px;
  font-weight: 700;
}

.nav-menu {
  flex: 1;
  border-bottom: 0;
  background: transparent;
}

.nav-menu :deep(.el-menu-item) {
  height: 64px;
  line-height: 64px;
}

.nav-menu :deep(.el-menu-item.is-active) {
  color: #5b5fef;
  border-bottom-color: #5b5fef;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.balance-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  background: #f4f5fb;
  color: #5b5fef;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.balance-chip:hover {
  background: #e9eaff;
}

.balance-value {
  white-space: nowrap;
}

.notice-badge {
  display: flex;
  align-items: center;
}

.notice-badge :deep(.el-badge__content) {
  animation: pulse-dot 2s infinite;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 8px;
}

.user-info:hover {
  background: #f4f5fb;
}

.user-avatar {
  background: linear-gradient(135deg, #5b5fef, #8f94ff);
  color: #fff;
  font-size: 13px;
}

.user-name {
  color: #344054;
  font-size: 14px;
}

.app-main {
  min-height: calc(100vh - 64px);
}

@media (max-width: 768px) {
  .header-inner {
    gap: 12px;
    padding: 0 16px;
  }

  .logo-text,
  .user-name {
    display: none;
  }
}
</style>
