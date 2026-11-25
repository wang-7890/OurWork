import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import mockAuth from './src/mock/auth.js'
import mockContract from './src/mock/contract.js'
import mockDemo from './src/mock/demo.js'
import mockNotification from './src/mock/notification.js'
import mockUser from './src/mock/user.js'

// 复用 src/mock 下的接口定义，替代 vite-plugin-mock
const mockModules = [
  ...mockAuth,
  ...mockContract,
  ...mockDemo,
  ...mockNotification,
  ...mockUser
]

function readRequestBody(req) {
  return new Promise((resolve) => {
    const chunks = []

    req.on('data', (chunk) => chunks.push(chunk))
    req.on('end', () => {
      const raw = Buffer.concat(chunks).toString('utf-8')

      if (!raw) {
        resolve({})
        return
      }

      try {
        resolve(JSON.parse(raw))
      } catch {
        resolve({})
      }
    })
    req.on('error', () => resolve({}))
  })
}

// 轻量 Mock 中间件：支持字符串/正则 URL，响应结构与 src/mock 文件保持一致
function customMockPlugin(enable) {
  return {
    name: 'custom-mock-server',
    configureServer(server) {
      if (!enable) {
        return
      }

      server.middlewares.use(async (req, res, next) => {
        const fullUrl = req.url || ''
        const pathname = fullUrl.split('?')[0]

        if (!pathname.startsWith('/api/')) {
          return next()
        }

        const method = (req.method || 'get').toLowerCase()

        const matched = mockModules.find((item) => {
          if (item.method !== method) {
            return false
          }

          return item.url instanceof RegExp
            ? item.url.test(pathname)
            : item.url === pathname
        })

        if (!matched) {
          return next()
        }

        try {
          const query = Object.fromEntries(
            new URL(fullUrl, 'http://localhost').searchParams
          )
          const body = method === 'get' ? {} : await readRequestBody(req)

          const result = await matched.response({
            url: pathname,
            body,
            query,
            headers: req.headers
          })

          res.setHeader('Content-Type', 'application/json; charset=utf-8')
          res.statusCode = 200
          res.end(JSON.stringify(result))
        } catch (error) {
          next(error)
        }
      })
    }
  }
}

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  // 开发阶段默认启用 Mock；联调真实后端时在 .env.local 设置 VITE_USE_MOCK=false
  const useMock = command === 'serve' && env.VITE_USE_MOCK !== 'false'

  // 后端本地地址；后端部署到其他机器时可通过环境变量修改
  const apiTarget = env.VITE_API_TARGET || 'http://localhost:8080'

  return {
    plugins: [vue(), customMockPlugin(useMock)],
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true
          // 不重写路径，因为后端接口本身就以 /api 开头
        }
      }
    }
  }
})
