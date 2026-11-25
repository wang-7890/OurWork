<template>
  <AppLayout v-if="!isFullscreen">
    <RouterView v-slot="{ Component }">
      <Transition name="fade-page" mode="out-in">
        <component :is="Component" :key="route.fullPath" />
      </Transition>
    </RouterView>
  </AppLayout>
  <RouterView v-else v-slot="{ Component }">
    <Transition name="fade-page" mode="out-in">
      <component :is="Component" :key="route.fullPath" />
    </Transition>
  </RouterView>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppLayout from './layouts/AppLayout.vue'
import { useAuthStore } from './stores/auth'

const route = useRoute()
const authStore = useAuthStore()

// 登录、注册为全屏页面，不显示顶部导航
const isFullscreen = computed(() => Boolean(route.meta.fullscreen))

// 刷新页面后，用本地 token 重新拉取用户信息
onMounted(() => {
  if (authStore.token && !authStore.profile) {
    authStore.fetchProfile().catch(() => {})
  }
})
</script>
