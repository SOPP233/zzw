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

## 8. order_detail
- POST `/api/order-details`
```json
{
  "detailId": "DET20260328001",
  "orderId": "ORD20260328001",
  "productModel": "MODEL-A",
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
- 成功后会自动：
  1) 创建 `production_plan`
  2) 更新 `order_detail.detail_status=3`（生产中）
  3) 插入 `plan_detail_relation`
  4) 创建 `process_task` 首道织造任务（`processType=1`, `status=0`）
