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
  - `/production/tasks`（重定向至 `/production/tasks/weaving`）
  - `/production/tasks/weaving`
  - `/production/tasks/setting`
  - `/production/tasks/cutting`
  - `/production/tasks/jointing`
  - `/production/tasks/reshaping`
  - `/basic/customers`
  - `/basic/materials`
  - `/inventory/ledger`
  - `/stats/dashboard`
- 生产协同模块前端实现：
  - `src/views/production/ProductionWorkbench.vue`：待排产明细筛选、合并排产下发（`POST /api/schedule/merge`）。
  - `src/views/production/task-center/TaskCenterBase.vue`：工序任务通用页（任务查询、参数采集、报工完成）。
  - `src/views/production/task-center/WeavingTaskCenter.vue`：织造任务页（`/production/tasks/weaving`）。
  - `src/views/production/task-center/SettingTaskCenter.vue`：定型任务页（`/production/tasks/setting`）。
  - `src/views/production/task-center/CuttingTaskCenter.vue`：裁网任务页（`/production/tasks/cutting`）。
  - `src/views/production/task-center/JointingTaskCenter.vue`：插接任务页（`/production/tasks/jointing`）。
  - `src/views/production/task-center/ReshapingTaskCenter.vue`：二次定型任务页（`/production/tasks/reshaping`）。
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

## Change Log

- 2026-03-28: 修正前端订单状态字典与当前数据库基线一致（`LW.sql`）：`1业务审核中、2待排产、3生产中、4部分入库、5已完结`；保留 `0新建草稿、6已发货` 仅作历史兼容显示。更新文件：`frontend-web/src/constants/order.js`。
- 2026-03-28: 将“工序任务中心”拆分为 5 个独立页面（织造/定型/裁网/插接/二次定型），新增通用组件 `src/views/production/task-center/TaskCenterBase.vue` 复用报工逻辑；同步更新路由、左侧菜单与 RBAC 权限映射，并将 `/production/tasks` 重定向到 `/production/tasks/weaving`。
- 2026-03-28: 新增后端基础数据接口 `GET /api/basic/products` 与 `GET /api/basic/equipments`（文件：`backend-springboot/src/main/java/com/lw/backend/controller/BasicDataController.java`），用于对接 `frontend-web` 基础数据模块页面。
- 2026-03-28: 更新 `backend-springboot/TEST_CASES.md`，补充上述两个接口的联调样例与返回说明。
- 2026-03-28: 前端新增登录页与登录态拦截（`frontend-web/src/views/system/Login.vue`、`frontend-web/src/router/index.js`、`frontend-web/src/stores/auth.js`），并移除 Web 右上角角色切换显示（`frontend-web/src/layout/index.vue`）。
- 2026-03-28: 订单维护页新增“订单录入与分页”“客户录入与分页”双页签（`frontend-web/src/views/order/OrderManage.vue`），并补齐后端 `GET/POST /api/orders/full` 聚合分页与主从一体录入接口（`backend-springboot/src/main/java/com/lw/backend/controller/OrderQueryController.java`）。
- 2026-03-28: 修复 `backend-springboot` Maven Wrapper，替换占位 `mvnw/mvnw.cmd` 为可自举下载 Maven 的脚本，并新增 `.mvn/wrapper/maven-wrapper.properties`，使项目不依赖系统全局 `mvn`。
- 2026-03-28: 新增 `backend-springboot/.mvn/wrapper/.gitignore`，忽略 Wrapper 下载的 Maven 二进制产物（`apache-maven-*-bin.zip`、`apache-maven-*` 目录），避免误提交大文件。
- 2026-03-28: 增强 `mvnw/mvnw.cmd`：当检测到 Java 版本低于 17 时自动下载并使用项目内 Temurin JDK 17；同时扩展 `.mvn/wrapper/.gitignore` 忽略 JDK 下载产物。
- 2026-03-28: 将 `OrderManage.vue` 中订单录入与客户录入由弹窗改为页面内可见表单区（同屏“录入表单 + 分页列表”），确保进入页面即可直接录入。
- 2026-03-28: 修复前端路由守卫 `next()` 弃用告警（改为 `beforeEach` return 风格）；并为 `OrderManage.vue` 增加 `/api/orders/full` 的 404 降级查询与降级录入逻辑（回退到 `/api/order-masters` + `/api/order-details`）。
- 2026-03-28: 新增合同主表设计（`LW.sql`：`contract_master`）与后端接口 `GET/POST/PUT/DELETE /api/contracts`；订单录入改为基于合同号下单（`/api/orders/full` 强校验合同存在与合同-客户一致性），前端 `OrderManage.vue` 新增“合同录入与分页”页签并支持合同选择自动回填客户。
- 2026-03-28: 调整策略为“严格后端 API 优先”：移除 `OrderManage.vue` 对 `/api/contracts` 的本地存储降级逻辑，仅保留 `/api/orders/full` 到基础订单接口的兼容查询/录入降级，避免以本地数据替代后端主数据。
- 2026-03-28: 新增生产协同“生产审核”页面（`/production/review`），支持将订单状态从业务审核中(1)提交为待排产(2)；后端新增接口 `POST /api/order-masters/{id}/production-review` 并增强 `GET /api/order-masters` 筛选参数（orderId/orderStatus/customerId）。
- 2026-03-28: 修复生产审核后排产工作台不可见问题：`POST /api/order-masters/{id}/production-review` 改为事务化同步更新订单主表与订单明细状态（`order_master.order_status=2` 同时 `order_detail.detail_status=2`）。
- 2026-03-28: 新增订单明细“透气量”属性（`order_detail.air_permeability`），并将合并排产规则升级为“型号 + 透气量”双一致校验；同步更新订单录入、订单展示、排产工作台页面字段与筛选。
- 2026-03-28: 按业务要求移除 `order_detail.craft_req` 字段链路：后端实体/Mapper/XML/接口响应与前端录单展示均不再读写该字段；`LW.sql` 与现库结构已同步去除该列，保留 `air_permeability` 作为非空关键属性。
- 2026-03-28: 根据业务回调恢复 `order_detail.craft_req`（记录其他工艺参数）：后端实体/Mapper/XML/接口与前端订单录入/展示已恢复该字段；数据库现库已回补 `craft_req` 列，同时保留 `air_permeability` 非空约束与合批校验规则。
- 2026-03-28: 排产工作台机台号改为下拉选择（1-8号机台，对应 `MC-01`~`MC-08`），不再手动输入机台号。
- 2026-03-28: 优化排产工作台单条下发体验：明确提示“支持单条下发”；修复透气量筛选在 `null` 场景误过滤数据的问题，并增加勾选数据一致性校验提示。
