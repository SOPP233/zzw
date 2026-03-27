# lw-platform

多端项目骨架：

- `frontend-web`：Vue 3 + Vite（浏览器端）
- `frontend-uniapp`：uni-app + Vue 3（微信小程序端）
- `backend-springboot`：Spring Boot 3 + MySQL（后端服务）
- `infra`：基础设施（MySQL Docker Compose）

## 目录结构

```text
lw-platform/
  frontend-web/
  frontend-uniapp/
  backend-springboot/
  infra/
```

## 1. 启动 MySQL

```bash
cd infra
docker compose up -d
```

默认配置：

- host: `127.0.0.1`
- port: `3306`
- database: `lw_db`
- username: `lw_user`
- password: `lw_pass_123`

## 2. 启动后端

```bash
cd backend-springboot
./mvnw spring-boot:run
```

健康检查：

- `GET http://localhost:8080/api/health`

## 3. 启动 Web 前端（浏览器）

```bash
cd frontend-web
npm install
npm run dev
```

默认地址：

- `http://localhost:5173`

## 4. 启动 uni-app（微信小程序）

```bash
cd frontend-uniapp
npm install
npm run dev:mp-weixin
```

然后用微信开发者工具导入 `frontend-uniapp/dist/dev/mp-weixin`。

## 5. 联调说明

前端默认后端地址：`http://localhost:8080`

- Web 端在 `frontend-web/.env.development`
- uni-app 端在 `frontend-uniapp/src/config/index.ts`

