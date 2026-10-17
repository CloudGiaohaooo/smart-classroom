# 前端多阶段构建：Node 打包 → Nginx 托管（Phase 5）
FROM node:20-alpine AS build
WORKDIR /app
# 先拷贝依赖清单，利用 Docker 层缓存
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:1.25-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
