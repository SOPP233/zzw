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
- 所有业务表接口统一采用 REST 风格：`POST/GET(id)/GET(page)/PUT/DELETE`。
- 全量接口测试样例统一维护在 `backend-springboot/TEST_CASES.md`。
- 合并排产核心链路统一走 `MergeScheduleService`，必须使用 `@Transactional(rollbackFor = Exception.class)` 保证原子性。
- 合并排产接口路径：`POST /api/schedule/merge`，业务校验失败统一抛 `BizException`。
- 合并排产在更新 `order_detail` 状态前后必须打印日志：
  - `log.info("准备更新明细状态")`
  - `log.info("明细状态更新完成，影响行数: {}", rows)`
- 工序报工接口路径：`POST /api/tasks/{taskId}/complete`，由 `TaskReportService` 统一处理。
- 工序报工必须事务化：更新当前任务与激活下一工序在一个事务内提交/回滚。

## Frontend Rules

- Web 管理端代码统一放在 `frontend-web`。
- 前端基建固定包含：`Vue 3 + Vite + Element Plus + Vue Router + Axios`。
- Axios 封装文件统一放在 `src/utils/request.js`，默认 `baseURL=http://localhost:8080`。
- 后台布局入口使用 `src/layout/index.vue`，核心菜单至少包含“订单列表”和“排产工作台”。
- 订单综合追踪页路径：`src/views/order/OrderList.vue`，主表使用 `el-table` 展示 `order_master`，并使用可展开行展示 `order_detail` 子表。
- 订单列表请求约定：`GET /api/orders`，支持按 `customerName` 进行前端检索参数透传。
- 后端已提供订单综合查询接口：`GET /api/orders`，返回主单字段与 `details` 子数组，供前端展开行直接渲染。
- 前端容错：当 `GET /api/orders` 返回 404 时，`OrderList.vue` 自动降级请求 `GET /api/order-masters`，避免页面白屏和未捕获异常。
- 订单状态统一字典（前端）：`0新建草稿、1业务审核中、2待排产、3生产中、4部分入库、5已发货、6已完结`，定义于 `src/constants/order.js`。
- 订单维护页：`src/views/order/OrderManage.vue`，包含主从订单表、状态筛选、流转日志抽屉。
- 订单维护页 API 骨架：
  - `GET /api/orders/full`（订单主从聚合列表）
  - `GET /api/order-flow-logs?orderId=...`（状态流转日志）
- 菜单与路由采用五大业务模块组织：订单管理、生产协同、基础数据、库存管理、数据统计。
- 当前五模块路由入口：
  - `/orders/tracking`
  - `/orders/manage`
  - `/production/workbench`
  - `/production/tasks`
  - `/basic/customers`
  - `/basic/materials`
  - `/inventory/ledger`
  - `/stats/dashboard`
- 生产协同模块前端实现：
  - `src/views/production/ProductionWorkbench.vue`：待排产明细筛选、合并排产下发（`POST /api/schedule/merge`）。
  - `src/views/production/TaskCenter.vue`：工序任务列表、按工序采集参数、报工完成（`POST /api/tasks/{taskId}/complete`）。
- 工序状态约定：
  - `processType`: 1织造、2定型、3裁网、4插接、5二次定型
  - `status`: 0待接收、1执行中、2完工审批、3已完成
- 生产模块常量统一维护在 `src/constants/production.js`。
- 库存管理模块前端实现：`src/views/inventory/InventoryLedger.vue`。
- 库存业务规则：不提供直接改库存入口，所有增减必须通过单据动作触发（预领料单、成品入库单）。
- 库存模块 API 骨架：
  - `GET /api/inventory/summary`
  - `GET /api/inventory/issue-docs`
  - `POST /api/inventory/issue-docs/{docNo}/approve`
  - `GET /api/inventory/inbound-docs`
  - `POST /api/inventory/inbound-docs/{docNo}/confirm`
- 基础数据模块前端页面：
  - `src/views/basic/BasicProducts.vue`：产品清单与标准工艺路线配置
  - `src/views/basic/BasicEquipments.vue`：设备与产能信息模型
- 基础数据模块路由：
  - `/basic/products`
  - `/basic/equipments`
- 基础数据 API 骨架：
  - `GET /api/basic/products`
  - `GET /api/basic/equipments`
- 数据统计模块前端页面：`src/views/stats/StatsDashboard.vue`。
- 统计模块核心指标：
  - 平均订单交付周期趋势（订单录入时间 -> 成品入库时间）
  - 车间周度良品率对比（裁网与插接废料上报）
  - 在制批次数、逾期订单数
- 统计模块 API 骨架：
  - `GET /api/stats/overview`
  - `GET /api/stats/delivery-cycle-trend`
  - `GET /api/stats/weekly-yield-rate`
  - `GET /api/stats/alerts`
- RBAC 前端实现：
  - 角色常量与路由权限矩阵：`src/constants/rbac.js`
  - 角色状态存储与鉴权函数：`src/stores/auth.js`
  - 路由守卫：`src/router/index.js`（无权限跳转 `/unauthorized`）
  - 布局菜单按角色动态展示：`src/layout/index.vue`
  - 未授权页面：`src/views/system/Unauthorized.vue`
- 角色体系编码：
  - `LEADER` 企业领导
  - `SALES` 销售员
  - `DIRECTOR` 车间主任
  - `WORKER` 一线员工
  - `INVENTORY_ADMIN` 库存管理员

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

## RBAC Update Note

- Added `SYSTEM_ADMIN` role as super role in frontend RBAC config (`src/constants/rbac.js`), with full route access.
