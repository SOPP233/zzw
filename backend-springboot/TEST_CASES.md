# API Test Cases

Base URL: `http://localhost:8080`

通用规则（所有资源）：
- 新增：`POST /api/{resource}`
- 查询详情：`GET /api/{resource}/{id}`
- 分页：`GET /api/{resource}?pageNo=1&pageSize=10`
- 更新：`PUT /api/{resource}/{id}`
- 删除：`DELETE /api/{resource}/{id}`

## 1. customer
- POST `/api/customers`
```json
{
  "customerId": "CUS001",
  "customerName": "华东客户A",
  "contactName": "张三",
  "contactPhone": "13800000001",
  "status": 1
}
```

## 2. sys_role
- POST `/api/sys-roles`
```json
{
  "roleCode": "ADMIN",
  "roleName": "系统管理员",
  "roleDesc": "全权限角色",
  "status": 1
}
```

## 3. sys_user
- POST `/api/sys-users`
```json
{
  "userId": "U001",
  "username": "admin",
  "passwordHash": "$2a$10$N9qo8uLOickgx2ZMRZoMy.Mrq7QwTIQuk2qYfVfM/BGQ6Qz3Wf0he",
  "realName": "管理员",
  "status": 1
}
```

## 4. sys_menu
- POST `/api/sys-menus`
```json
{
  "menuId": "MENU_ORDER",
  "parentId": null,
  "menuName": "订单管理",
  "menuType": 2,
  "path": "/order",
  "component": "views/order/index",
  "permissionCode": "order:read",
  "sortNo": 10,
  "visible": 1,
  "status": 1
}
```

## 5. sys_user_role
- POST `/api/sys-user-roles`
```json
{
  "userId": "U001",
  "roleCode": "ADMIN"
}
```

## 6. sys_role_menu
- POST `/api/sys-role-menus`
```json
{
  "roleCode": "ADMIN",
  "menuId": "MENU_ORDER"
}
```

## 7. order_master
- POST `/api/order-masters`
```json
{
  "orderId": "ORD20260328001",
  "contractId": "CON20260328001",
  "customerId": "CUS001",
  "totalAmount": 120000.50,
  "expectedDate": "2026-04-15",
  "orderStatus": 2,
  "remark": "首批订单"
}
```

## 7.1 contract_master
- POST `/api/contracts`
```json
{
  "contractId": "CON20260328001",
  "customerId": "CUS001",
  "contractAmount": 180000.00,
  "signDate": "2026-03-28",
  "deliveryAddress": "苏州市工业园区",
  "contractStatus": 1,
  "remark": "年度框架合同"
}
```
- GET `/api/contracts?pageNo=1&pageSize=10&contractId=CON&customerId=CUS001`

## 8. order_detail
- POST `/api/order-details`
```json
{
  "detailId": "DET20260328001",
  "orderId": "ORD20260328001",
  "productModel": "MODEL-A",
  "airPermeability": 500,
  "lengthReq": 500,
  "widthReq": 200,
  "craftReq": "防静电",
  "detailStatus": 1
}
```

## 9. production_plan
- POST `/api/production-plans`
```json
{
  "batchId": "BAT260328001",
  "machineId": "MC-01",
  "planStartDate": "2026-03-30",
  "planStatus": 0,
  "createdBy": "U001"
}
```

## 10. plan_detail_relation
- POST `/api/plan-detail-relations`
```json
{
  "batchId": "BAT260328001",
  "detailId": "DET20260328001",
  "allocatedQty": 100.000
}
```

## 11. process_task
- POST `/api/process-tasks`
```json
{
  "taskId": "TASK260328001",
  "batchId": "BAT260328001",
  "processType": 1,
  "operatorId": "U001",
  "status": 0,
  "outputData": {
    "temperature": 180,
    "lossRate": 0.03,
    "note": "首件验证"
  }
}
```

## 12. material_inventory
- POST `/api/material-inventories`
```json
{
  "materialId": "MAT-001",
  "materialType": "PET",
  "currentStock": 1000.000,
  "frozenStock": 200.000,
  "minStock": 100,
  "versionNo": 0,
  "updatedBy": "U001"
}
```

## 13. 合并排产（核心）
- POST `/api/schedule/merge`
```json
{
  "detailIds": [
    "DET20260328001",
    "DET20260328002"
  ],
  "machineId": "MC-01"
}
```

说明：
- `detailIds` 中的明细必须都存在，且 `productModel` 完全一致。
- `detailIds` 中的明细 `airPermeability` 也必须完全一致。
- 成功后会自动：
  1) 创建 `production_plan`
  2) 更新 `order_detail.detail_status=3`（生产中）
  3) 插入 `plan_detail_relation`
  4) 创建 `process_task` 首道织造任务（`processType=1`, `status=0`）

## 14. 工序报工与状态激活（核心）
- POST `/api/tasks/{taskId}/complete`
```json
{
  "operatorId": "U001",
  "outputData": {
    "actualOutputMeters": 1250.5,
    "lossRate": 0.018,
    "remark": "首批报工完成"
  }
}
```

说明：
- 接口会将当前任务状态更新为已完成（`status=3`），并写入 `output_data`、`end_time`。
- 若当前任务工序不是最后一道（`process_type < 5`），系统会自动生成下一工序任务，状态为待接收（`status=0`）。

## 15. 基础数据-产品与工艺路线
- GET `/api/basic/products`
- GET `/api/basic/products?productModel=MODEL-A`

说明：
- 数据来源于 `order_detail.product_model` 去重归档。
- 返回字段包含：`productModel`、`materialTemplateCode`、`processRoute`、`remark`。

## 16. 基础数据-设备与产能模型
- GET `/api/basic/equipments`
- GET `/api/basic/equipments?assetCode=MC`
- GET `/api/basic/equipments?workSection=织造段`

说明：
- 数据来源于 `production_plan.machine_id` 去重归档。
- 返回字段包含：`equipmentId`、`assetCode`、`equipmentName`、`workSection`、`supportedProductRange`、`capacityFactor`、`enabled`。

## 17. 订单管理聚合分页
- GET `/api/orders/full?pageNo=1&pageSize=10`
- GET `/api/orders/full?pageNo=1&pageSize=10&orderId=ORD2026&customerName=华东&orderStatus=2`

说明：
- 返回 `order_master` 聚合客户名与 `details` 明细列表。
- 分页结构：`records`、`total`、`current`、`size`。

## 18. 订单主从一体录入
- POST `/api/orders/full`
```json
{
  "contractId": "CON20260328008",
  "customerId": "CUS001",
  "totalAmount": 68000.50,
  "expectedDate": "2026-04-20",
  "orderStatus": 1,
  "deliveryAddress": "苏州市工业园区",
  "details": [
    {
      "productModel": "MODEL-A",
      "airPermeability": 500,
      "lengthReq": 500,
      "widthReq": 220,
      "craftReq": "防静电"
    }
  ]
}
```

说明：
- 一个事务内写入 `order_master` 与 `order_detail`。
- 未传 `orderId` 时由后端自动生成。

## 19. 生产审核（业务审核中 -> 待排产）
- POST `/api/order-masters/{orderId}/production-review`

说明：
- 仅允许 `order_status=1` 的订单执行该操作。
- 成功后订单状态更新为 `2`（待排产）。
