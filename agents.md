# Agents Guide

本文件用于规范本仓库内 Agent/开发者协作方式，确保多人并行改动时一致、可追踪、可回滚。

## 强制规则

- 每次代码或配置修改后，必须同步检查并更新 `agents.md`（如变更范围、启动方式、约束、流程有变化）。

## Project Scope

- `frontend-web`: Vue 3 + Vite（浏览器端）
- `frontend-uniapp`: uni-app + Vue 3（微信小程序端）
- `backend-springboot`: Spring Boot 3 + MyBatis-Plus
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
- 涉及订单号、批次号检索的查询，优先复用现有索引设计，不随意删除或弱化索引。

## Backend Rules

- 实体、Mapper、XML 保持同名同目录分层，统一放在 `com.lw.backend.modules` 下。
- `process_task.output_data` 必须使用 MyBatis-Plus 的 JSON TypeHandler 映射。
- 新增 Mapper XML 后，确认 `mybatis-plus.mapper-locations` 覆盖到对应路径。
- 分页查询统一走 MyBatis-Plus 分页拦截器（`MybatisPlusInterceptor + PaginationInnerInterceptor`）。
- 客户管理接口基线路径：`/api/customers`（包含增删改查与分页查询）。

## Collaboration Rules

- 接口或字段变更必须同步检查 `frontend-web` 与 `frontend-uniapp` 调用端。
- 不提交密钥、令牌、生产环境凭据。
- 每次提交聚焦单一目的，提交信息清晰（建议 `feat/fix/chore/docs` 前缀）。
- 变更基础设施或数据结构时，必须同时更新 `README.md` 或本文件。

## Commit Checklist

1. `git status` 确认改动范围正确。
2. 本地最小可运行验证（至少启动受影响模块）。
3. 更新 `agents.md`（强制检查项）。
4. 提交并推送到 `origin/main`。
