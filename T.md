# MES 项目开发交接总结（用于新窗口续接）

## 1. 当前项目总览

- 仓库路径：`C:\Users\22472\Desktop\LW`
- 最近已推送提交：`32a2c3c`（`main`）
- GitHub 仓库：`https://github.com/SOPP233/zzw.git`
- 技术栈：
  - 前端：`Vue 3 + Vite + Element Plus + Vue Router + Axios`
  - 后端：`Spring Boot 3 + MyBatis-Plus`
  - 数据库：`MySQL 8`

---

## 2. 后端当前状态（backend-springboot）

### 2.1 基础框架与数据访问

- 已接入 MyBatis-Plus 与 Lombok（`pom.xml`）
- 已配置：
  - Mapper 扫描：`com.lw.backend.modules.mes.mapper`
  - 分页拦截器：`MybatisPlusInterceptor + PaginationInnerInterceptor`
  - mapper xml 路径：`classpath*:mapper/*.xml`
- CORS 已全局开启（允许跨域访问）

### 2.2 已有核心实体/Mapper

- 已覆盖：订单、排产、工序、库存、RBAC 等主表实体与 Mapper。
- `process_task.output_data` 已使用 MyBatis-Plus JSON TypeHandler（`JacksonTypeHandler`）。

### 2.3 已完成关键业务接口

- 客户接口：`/api/customers`（CRUD + 分页）
- 合并排产：`POST /api/schedule/merge`
  - 含事务、型号一致性校验、创建批次、更新明细状态、创建首工序任务
- 工序报工：`POST /api/tasks/{taskId}/complete`
  - 含事务、写入 output_data、完工、激活下一工序
- 订单聚合查询：`GET /api/orders`
  - 返回主单 + `details` 子数组（用于前端可展开行）

### 2.4 待补充后端接口（前端已预留骨架）

- 订单管理增强：
  - `GET /api/orders/full`
  - `GET /api/order-flow-logs?orderId=...`
- 基础数据模块：
  - `GET /api/basic/products`
  - `GET /api/basic/equipments`
- 库存模块（单据驱动）：
  - `GET /api/inventory/summary`
  - `GET /api/inventory/issue-docs`
  - `POST /api/inventory/issue-docs/{docNo}/approve`
  - `GET /api/inventory/inbound-docs`
  - `POST /api/inventory/inbound-docs/{docNo}/confirm`
- 数据统计模块：
  - `GET /api/stats/overview`
  - `GET /api/stats/delivery-cycle-trend`
  - `GET /api/stats/weekly-yield-rate`
  - `GET /api/stats/alerts`

---

## 3. 前端当前状态（frontend-web）

### 3.1 工程与基础设施

- 项目已重建并可运行（`npm install` 完成）
- 已全局引入 Element Plus
- Axios 封装已完成：`src/utils/request.js`
- 路由与布局已经模块化完成

### 3.2 五大模块页面现状

#### 订单管理

- `src/views/order/OrderList.vue`
  - 订单综合追踪（主表 + 可展开明细子表）
  - 支持按客户搜索
  - 调用 `/api/orders`
  - 404 自动降级到 `/api/order-masters`，避免页面崩溃
- `src/views/order/OrderManage.vue`
  - 订单维护管理页（搜索、状态、主从展示、流转日志抽屉）
  - 预留 API：`/api/orders/full`、`/api/order-flow-logs`

#### 生产协同

- `src/views/production/ProductionWorkbench.vue`
  - 待排产明细筛选 + 合并排产按钮
  - 调用：`POST /api/schedule/merge`
- `src/views/production/TaskCenter.vue`
  - 工序任务列表 + 报工弹窗 + 工序参数采集
  - 调用：`POST /api/tasks/{taskId}/complete`

#### 基础数据

- `src/views/basic/BasicProducts.vue`
  - 产品清单与工艺路线配置页（API 骨架已接）
- `src/views/basic/BasicEquipments.vue`
  - 设备与产能信息模型页（API 骨架已接）

#### 库存管理

- `src/views/inventory/InventoryLedger.vue`
  - 强调“单据驱动库存变更”
  - 预领料单、成品入库单、库存摘要
  - 相关 API 骨架已接并带 404 友好提示

#### 数据统计

- `src/views/stats/StatsDashboard.vue`
  - KPI 卡片 + 交付周期趋势 + 周度良品率 + 预警清单
  - 对接统计 API 骨架；未接入时使用 mock 展示，不白屏

### 3.3 RBAC（前端）

- 新增并启用角色矩阵：`src/constants/rbac.js`
- 角色状态管理：`src/stores/auth.js`
- 路由守卫：未授权跳转 `/unauthorized`
- 布局菜单按角色动态显示，支持顶部切换角色
- 未授权页面：`src/views/system/Unauthorized.vue`

当前角色体系：

- `SYSTEM_ADMIN`（新加，超管，全模块）
- `LEADER`
- `SALES`
- `DIRECTOR`
- `WORKER`
- `INVENTORY_ADMIN`

---

## 4. 数据库与脚本现状

- `LW.sql` 已存在并包含核心业务表与 RBAC 表。
- 历史上出现过编码显示乱码（PowerShell 显示层），但核心结构已可用。
- 若本地缺表，请优先重新导入 `LW.sql` 至 `lw_db`。

---

## 5. 已知问题/注意点

1. `frontend-web` 构建可通过，但主包偏大（vite 报 chunk > 500kb 警告），属于优化项非阻断项。
2. 多个前端页面已接“真实 API 骨架”，若后端接口未实现会显示 404 提示（这是预期容错行为）。
3. 业务权限（如“销售员仅能改本人订单”“状态锁单”）目前主要在前端体现，后端需继续做强校验。
4. 旧占位文件仍有少量历史文件存在（如 `src/views/OrderList.vue`），不影响路由主链路。

---

## 6. 下一窗口建议优先级（建议按顺序）

1. 补后端基础数据接口：`/api/basic/products`、`/api/basic/equipments`
2. 补后端库存单据流接口（5个）
3. 补后端统计聚合接口（4个）
4. 补后端订单流转日志接口并接入 `OrderManage.vue`
5. 在后端落实数据级 RBAC（按角色 + 用户范围）

---

## 7. 本地启动与联调命令

### 后端

```bash
cd backend-springboot
mvn spring-boot:run
```

### 前端

```bash
cd frontend-web
npm install
npm run dev
```

---

## 8. 关键文件索引（高频续改）

- 前端路由：`frontend-web/src/router/index.js`
- 前端布局：`frontend-web/src/layout/index.vue`
- RBAC 常量：`frontend-web/src/constants/rbac.js`
- 订单页：`frontend-web/src/views/order/OrderList.vue`
- 订单维护：`frontend-web/src/views/order/OrderManage.vue`
- 生产协同：`frontend-web/src/views/production/ProductionWorkbench.vue`
- 工序中心：`frontend-web/src/views/production/TaskCenter.vue`
- 库存管理：`frontend-web/src/views/inventory/InventoryLedger.vue`
- 数据统计：`frontend-web/src/views/stats/StatsDashboard.vue`
- 后端合并排产：`backend-springboot/src/main/java/com/lw/backend/modules/mes/service/impl/MergeScheduleServiceImpl.java`
- 后端报工：`backend-springboot/src/main/java/com/lw/backend/modules/mes/service/impl/TaskReportServiceImpl.java`
- 后端订单聚合：`backend-springboot/src/main/java/com/lw/backend/controller/OrderQueryController.java`

