import { defineStore } from 'pinia'
import { TOKEN_KEY } from '../api/request'
import {
  registerApi,
  loginApi,
  logoutApi,
  getProfileApi
} from '../api/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    profile: null,
    unreadCount: 0,
    profileLoaded: false
  }),

  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },

  actions: {
    setToken(token) {
      this.token = token || ''
      localStorage.setItem(TOKEN_KEY, this.token)
    },

    async register(payload) {
      const data = await registerApi(payload)

      if (data?.token) {
        this.setToken(data.token)
        await this.fetchProfile()
      }

      return data
    },

    async login(payload) {
      const data = await loginApi(payload)

      if (data?.token) {
        this.setToken(data.token)
        await this.fetchProfile()
      }

      return data
    },

    async fetchProfile() {
      if (!this.token) {
        return null
      }

      const profile = await getProfileApi()

      this.profile = profile
      this.unreadCount = Number(profile?.unreadCount || 0)
      this.profileLoaded = true

      return profile
    },

    // 局部更新资料（如昵称），保证顶部导航等全局位置立即同步
    patchProfile(partial) {
      if (this.profile) {
        this.profile = { ...this.profile, ...partial }
      }
    },

    async logout() {
      try {
        if (this.token) {
          await logoutApi()
        }
      } finally {
        this.clearAuth()
      }
    },

    clearAuth() {
      this.token = ''
      this.profile = null
      this.unreadCount = 0
      this.profileLoaded = false
      localStorage.removeItem(TOKEN_KEY)
    },

    setUnreadCount(count) {
      this.unreadCount = Number(count || 0)
    },

    // 单条通知标记已读后，角标减一（不会低于 0）
    decrementUnread() {
      this.unreadCount = Math.max(0, this.unreadCount - 1)

      if (this.profile) {
        this.profile = {
          ...this.profile,
          unreadCount: this.unreadCount
        }
      }
    }
  }
})