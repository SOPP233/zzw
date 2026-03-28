# Agents Guide

本文件用于规范本仓库内 Agent/开发者协作方式，确保多人并行改动时一致、可追踪、可回滚。

## Project Scope

- `frontend-web`: Vue 3 + Vite（浏览器端）
- `frontend-uniapp`: uni-app + Vue 3（微信小程序端）
- `backend-springboot`: Spring Boot 3 后端
- `infra`: MySQL Docker Compose
- `LW.sql`: MySQL 8.0 全量建表脚本（含核心业务表 + RBAC）

## Runbook

1. 启动 MySQL
```bash
cd infra
docker compose up -d
```

2. 启动后端
```bash
cd backend-springboot
mvn spring-boot:run
```

3. 启动 Web 前端
```bash
cd frontend-web
npm install
npm run dev
```

4. 启动 uni-app 小程序端
```bash
cd frontend-uniapp
npm install
npm run dev:mp-weixin
```

## Database Rules

- 统一以 `LW.sql` 作为结构基线，新增/变更表结构需同步更新该文件。
- 当前权限模型为 RBAC：`sys_user`、`sys_role`、`sys_menu`、`sys_user_role`、`sys_role_menu`。
- 密码字段仅允许存储哈希值（`password_hash`），禁止明文密码入库。
- 涉及订单号、批次号检索的查询，优先复用现有索引设计，不随意删除/弱化索引。

## Collaboration Rules

- 接口或字段变更必须同步检查 `frontend-web` 与 `frontend-uniapp` 调用端。
- 不提交密钥、令牌、生产环境凭据。
- 每次提交聚焦单一目的，提交信息清晰（建议 `feat/fix/chore/docs` 前缀）。
- 变更基础设施或数据结构时，必须同时更新 `README.md` 或本文件。

## Commit Checklist

1. `git status` 确认改动范围正确
2. 本地最小可运行验证（至少启动受影响模块）
3. 提交并推送到 `origin/main`
