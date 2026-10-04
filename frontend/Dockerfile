# ---- 构建阶段 ----
FROM node:22-alpine AS build
WORKDIR /build
COPY package.json ./
# 官方源不可达时可用 --build-arg NPM_REGISTRY=https://registry.npmmirror.com 覆盖
ARG NPM_REGISTRY=https://registry.npmjs.org
RUN npm config set registry ${NPM_REGISTRY} && npm install --no-audit --no-fund
COPY . .
RUN npm run build

# ---- 运行阶段：nginx 托管静态资源并反代后端 ----
FROM nginx:1.27-alpine
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
