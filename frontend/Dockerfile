# 前端镜像：Node 构建 + Nginx 托管
# 注意：构建上下文是项目根目录（见根目录 docker-compose.yml），不是 frontend/ 本身

# ---- 构建阶段 ----
FROM node:20-alpine AS build
WORKDIR /app

# 先装依赖，利用 Docker 层缓存
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund

# 再拷贝源码并构建（.env.production 会把 VITE_USE_MOCK 置为 false）
COPY frontend/ ./
RUN npm run build

# ---- 运行阶段 ----
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
