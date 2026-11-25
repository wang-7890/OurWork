import { createRouter, createWebHistory } from 'vue-router'
import { TOKEN_KEY } from '../api/request'

const routes = [
  {
    path: '/',
    redirect: '/square'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: {
      public: true,
      fullscreen: true,
      title: '登录'
    }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: {
      public: true,
      fullscreen: true,
      title: '注册'
    }
  },
  {
    path: '/square',
    name: 'Square',
    component: () => import('../views/Square.vue'),
    meta: {
      public: true,
      title: '合约广场'
    }
  },
  {
    path: '/contract/create',
    name: 'ContractCreate',
    component: () => import('../views/ContractCreate.vue'),
    meta: {
      title: '发起对赌'
    }
  },
  {
    path: '/contract/:id',
    name: 'ContractDetail',
    component: () => import('../views/ContractDetail.vue'),
    meta: {
      title: '合约详情'
    }
  },
  {
    path: '/witness/accept/:code',
    name: 'WitnessAccept',
    component: () => import('../views/WitnessAccept.vue'),
    meta: {
      title: '接受见证邀请'
    }
  },
  {
    path: '/voting',
    name: 'Voting',
    component: () => import('../views/Voting.vue'),
    meta: {
      title: '待裁决'
    }
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('../views/Notifications.vue'),
    meta: {
      title: '通知中心'
    }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: {
      title: '个人中心'
    }
  },
  {
    path: '/demo/control',
    name: 'DemoControl',
    component: () => import('../views/DemoControl.vue'),
    meta: {
      title: '演示控制',
      // 仅 Mock 演示环境开放，联调真实后端时访问会被守卫拦回广场
      mockOnly: true
    }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/square',
    meta: {
      public: true
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta?.title) {
    document.title = `${to.meta.title} - 微承诺对赌平台`
  } else {
    document.title = '微承诺对赌平台'
  }

  if (to.meta?.public) {
    return true
  }

  // 演示控制页只在 Mock 模式下可用，对应后端 demo.enabled 开关
  if (to.meta?.mockOnly && import.meta.env.VITE_USE_MOCK !== 'true') {
    return { path: '/square' }
  }

  const token = localStorage.getItem(TOKEN_KEY)

  if (!token) {
    return {
      path: '/login',
      query: {
        redirect: to.fullPath
      }
    }
  }

  return true
})

export default router