# 微承诺对赌平台 · 前端

Vue 3 + Vite + Element Plus + Pinia，内置 Mock 层，无需后端即可开发联调全部页面。

## 命令

```bash
npm install       # 安装依赖
npm run dev       # 开发（默认 Mock 模式，端口 5173）
npm run build     # 生产构建（自动关闭 Mock）
npm run preview   # 本地预览构建产物
```

## 环境变量

| 变量 | 默认 | 说明 |
| --- | --- | --- |
| `VITE_USE_MOCK` | dev 下 `true` | Mock 开关；`.env.production` 中固定为 `false` |
| `VITE_API_TARGET` | `http://localhost:8080` | 真实后端地址，dev 代理 `/api` 用 |

- `.env.local`：本地开发配置（Mock 开启）
- `.env.production`：生产构建配置（Mock 关闭、演示控制入口隐藏），优先级高于 `.env.local`

## Mock 说明

`src/mock/` 下按模块模拟了接口文档全部接口（内存数据库见 `mock/db.js`），由 `vite.config.js` 中的轻量中间件接管 `/api/*` 请求。Mock 模式额外开放「演示控制」页面（立即到期 / 结算扫描 / 一键重置），对应后端第 9 章演示接口。

## 页面路由

| 路由 | 页面 | 权限 |
| --- | --- | --- |
| `/square` | 合约广场 | 公开 |
| `/contract/:id` | 合约详情（状态进度条 + 倒计时） | 登录 |
| `/contract/create` | 发起对赌 | 登录 |
| `/witness/accept/:code` | 接受见证邀请 | 登录 |
| `/voting` | 待裁决列表与投票 | 登录 |
| `/notifications` | 通知中心 | 登录 |
| `/profile` | 个人中心（诚信档案、资金流水） | 登录 |
| `/demo/control` | 演示控制 | 仅 Mock 模式 |
| `/login` `/register` | 登录 / 注册 | 公开 |
