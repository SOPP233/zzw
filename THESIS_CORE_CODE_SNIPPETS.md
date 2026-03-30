# Thesis Core Code Snippets (6 Core Functions)

This file extracts the most critical source code blocks for the six core system functions.
Each snippet keeps original project logic and is grouped for paper presentation.

## 1) Order Management Module

Source:
- `backend-springboot/src/main/java/com/lw/backend/controller/OrderQueryController.java`

### 1.1 Master-detail order creation (`POST /api/orders/full`)

```java
@PostMapping("/full")
@Transactional(rollbackFor = Exception.class)
public Map<String, Object> createFull(@RequestBody OrderFullCreateRequest request) {
    String contractNo = firstNonBlank(request.getContractNo(), request.getContractId());
    if (!StringUtils.hasText(contractNo)) {
        throw new BizException("contractNo不能为空");
    }
    if (request.getDetails() == null || request.getDetails().isEmpty()) {
        throw new BizException("至少需要一条订单明细");
    }

    ContractMaster contract = contractMasterMapper.selectById(contractNo);
    if (contract == null) {
        throw new BizException("合同不存在");
    }

    OrderMaster existingMaster = orderMasterMapper.selectOne(new LambdaQueryWrapper<OrderMaster>()
            .eq(OrderMaster::getContractNo, contractNo)
            .last("limit 1"));

    String orderNo;
    if (existingMaster == null) {
        orderNo = contractNo + "D";

        OrderMaster master = new OrderMaster();
        master.setOrderNo(orderNo);
        master.setContractNo(contractNo);
        master.setCustomerId(contract.getCustomerId());
        master.setTotalAmount(contract.getContractAmount());
        master.setExpectedDate(request.getExpectedDate());
        master.setOrderStatus(request.getOrderStatus() == null ? 1 : request.getOrderStatus());
        master.setRemark(contract.getDeliveryAddress());
        orderMasterMapper.insert(master);
    } else {
        orderNo = existingMaster.getOrderNo();
    }

    Long existedDetailCount = orderDetailMapper.selectCount(new LambdaQueryWrapper<OrderDetail>()
            .eq(OrderDetail::getOrderNo, orderNo));
    int nextIndex = (existedDetailCount == null ? 0 : existedDetailCount.intValue()) + 1;

    for (OrderFullCreateRequest.OrderDetailCreateRequest item : request.getDetails()) {
        if (!StringUtils.hasText(item.getProductModel())) {
            throw new BizException("明细产品型号不能为空");
        }
        BigDecimal reqLength = item.getReqLength() != null ? item.getReqLength() : toDecimal(item.getLengthReq());
        BigDecimal reqWidth = item.getReqWidth() != null ? item.getReqWidth() : toDecimal(item.getWidthReq());
        if (reqLength == null || reqWidth == null) {
            throw new BizException("明细长度和宽度不能为空");
        }

        OrderDetail detail = new OrderDetail();
        detail.setDetailId(resolveDetailId(item.getDetailId(), orderNo, nextIndex++));
        detail.setOrderNo(orderNo);
        detail.setProductModel(item.getProductModel());
        detail.setAirPermeability(item.getAirPermeability() == null ? 0 : item.getAirPermeability());
        detail.setReqLength(reqLength);
        detail.setReqWidth(reqWidth);
        detail.setDetailStatus(item.getDetailStatus() == null ? 0 : item.getDetailStatus());
        detail.setWeavingModeStatus(0);
        detail.setDeliveredQty(item.getDeliveredQty() == null ? 0 : item.getDeliveredQty());
        orderDetailMapper.insert(detail);
    }

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("orderNo", orderNo);
    data.put("orderId", orderNo);
    data.put("contractNo", contractNo);
    return success(data);
}
```

### 1.2 Order status aggregation from detail status

```java
private Integer resolveOrderStatus(Integer masterStatus, List<OrderDetail> details) {
    if (details == null || details.isEmpty()) {
        return masterStatus;
    }
    List<Integer> statuses = details.stream().map(OrderDetail::getDetailStatus).filter(v -> v != null).toList();
    if (statuses.isEmpty()) {
        return masterStatus;
    }
    if (statuses.stream().anyMatch(v -> v >= 2)) {
        return 3;
    }
    return 1;
}
```

---

## 2) Production Collaboration Module

Source:
- `backend-springboot/src/main/java/com/lw/backend/modules/mes/service/impl/MergeScheduleServiceImpl.java`
- `backend-springboot/src/main/java/com/lw/backend/controller/ProductionOrderController.java`

### 2.1 Merge scheduling core transaction (`POST /api/schedule/merge`)

```java
@Override
@Transactional(rollbackFor = Exception.class)
public Map<String, Object> mergeSchedule(MergeScheduleRequest request) {
    validateRequest(request);

    List<OrderDetail> details = fetchAndValidateDetails(request.getDetailIds());
    validateMergeEligibility(details);

    String weavingBatchNo = resolveWeavingBatchNo(request.getWeavingBatchNo());
    createWeavingProcess(request, weavingBatchNo);
    bindDetailsAndUpdateStatus(details, weavingBatchNo);
    updateOrderMasterStatus(details);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("weavingBatchNo", weavingBatchNo);
    result.put("detailCount", details.size());
    result.put("machineId", request.getMachineId());
    result.put("weavingLength", request.getWeavingLength());
    result.put("weavingWidth", request.getWeavingWidth());
    result.put("productModel", details.get(0).getProductModel());
    result.put("airPermeability", details.get(0).getAirPermeability());
    result.put("detailIds", details.stream().map(OrderDetail::getDetailId).toList());
    return result;
}
```

### 2.2 Process chaining + report submission (`POST /api/production/weaving-orders/{batchNo}/report`)

```java
@PostMapping("/weaving-orders/{batchNo}/report")
@Transactional(rollbackFor = Exception.class)
public Map<String, Object> submitWeavingReport(@PathVariable String batchNo,
                                               @RequestBody WeavingReportRequest request) {
    ensureWeavingReportTableReady();
    requireText(request.getMachineId(), "machineId");
    requireText(request.getOperatorId(), "operatorId");
    requireText(request.getMaterialBatchNo(), "materialBatchNo");
    requireText(request.getTensionParams(), "tensionParams");
    requireNotNull(request.getActualLength(), "actualLength");
    LocalDateTime actualStartTime = parseDateTime(request.getActualStartTime(), "actualStartTime");
    if (actualStartTime == null) {
        throw new BizException("actualStartTime不能为空");
    }
    LocalDateTime actualEndTime = LocalDateTime.now();
    if (actualEndTime.isBefore(actualStartTime)) {
        throw new BizException("actualEndTime不能早于actualStartTime");
    }

    queryWeavingProcess(batchNo);

    Long reportCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM prd_weaving_report WHERE weaving_batch_no = ?",
            Long.class,
            batchNo
    );
    if (reportCount != null && reportCount > 0) {
        jdbcTemplate.update(
                "UPDATE prd_weaving_report SET machine_id = ?, operator_id = ?, material_batch_no = ?, " +
                        "tension_params = ?, actual_length = ?, actual_start_time = ?, actual_end_time = ? " +
                        "WHERE weaving_batch_no = ?",
                request.getMachineId(),
                request.getOperatorId(),
                request.getMaterialBatchNo(),
                request.getTensionParams(),
                request.getActualLength(),
                toTimestamp(actualStartTime),
                toTimestamp(actualEndTime),
                batchNo
        );
    } else {
        jdbcTemplate.update(
                "INSERT INTO prd_weaving_report (weaving_batch_no, machine_id, operator_id, material_batch_no, " +
                        "tension_params, actual_length, actual_start_time, actual_end_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                batchNo,
                request.getMachineId(),
                request.getOperatorId(),
                request.getMaterialBatchNo(),
                request.getTensionParams(),
                request.getActualLength(),
                toTimestamp(actualStartTime),
                toTimestamp(actualEndTime)
        );
    }

    Integer nextStatus = WEAVING_STATUS_DONE;
    jdbcTemplate.update(
            "UPDATE prd_weaving_process SET machine_id = ?, operator_id = ?, actual_length = ?, process_status = ?, completed_at = ? " +
                    "WHERE weaving_batch_no = ?",
            request.getMachineId(),
            request.getOperatorId(),
            request.getActualLength(),
            nextStatus,
            toTimestamp(actualEndTime),
            batchNo
    );

    syncOrderStatusForWeavingBatch(batchNo);
    activateSettingProcess(batchNo);

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("weavingBatchNo", batchNo);
    data.put("processStatus", nextStatus);
    data.put("settingActivated", true);
    return success(data);
}
```

---

## 3) Inventory Management Module

Source:
- `backend-springboot/src/main/java/com/lw/backend/controller/InventoryController.java`

### 3.1 Inventory summary aggregation + document-driven operations

```java
@GetMapping("/summary")
public Map<String, Object> summary() {
    List<MaterialInventory> rows = materialInventoryMapper.selectList(new QueryWrapper<>());
    BigDecimal rawMaterial = BigDecimal.ZERO;
    BigDecimal wip = BigDecimal.ZERO;
    BigDecimal finishedGoods = BigDecimal.ZERO;

    for (MaterialInventory row : rows) {
        String category = mapCategory(row.getMaterialType());
        BigDecimal stock = row.getCurrentStock() == null ? BigDecimal.ZERO : row.getCurrentStock();
        if ("raw".equals(category)) {
            rawMaterial = rawMaterial.add(stock);
        } else if ("wip".equals(category)) {
            wip = wip.add(stock);
        } else if ("finished".equals(category)) {
            finishedGoods = finishedGoods.add(stock);
        }
    }

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("rawMaterial", rawMaterial);
    data.put("wip", wip);
    data.put("finishedGoods", finishedGoods);
    return successWithData(data);
}

@PostMapping("/issue-docs/{docNo}/approve")
public Map<String, Object> approveIssue(@PathVariable String docNo) {
    return successMessage("预领料单审核完成", docNo);
}

@PostMapping("/inbound-docs/{docNo}/confirm")
public Map<String, Object> confirmInbound(@PathVariable String docNo) {
    return successMessage("成品入库确认完成", docNo);
}
```

---

## 4) Basic Data / Master Data Module

Source:
- `backend-springboot/src/main/java/com/lw/backend/controller/BasicDataController.java`

### 4.1 Product list + standard process route template

```java
private static final List<String> DEFAULT_PROCESS_ROUTE = List.of("织造", "定型", "裁网", "插接", "二次定型");

@GetMapping("/products")
public Map<String, Object> listProducts(@RequestParam(required = false) String productModel) {
    LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<OrderDetail>()
            .select(OrderDetail::getProductModel, OrderDetail::getUpdatedAt, OrderDetail::getCreatedAt)
            .isNotNull(OrderDetail::getProductModel)
            .ne(OrderDetail::getProductModel, "")
            .like(StringUtils.hasText(productModel), OrderDetail::getProductModel, productModel)
            .orderByDesc(OrderDetail::getUpdatedAt)
            .orderByDesc(OrderDetail::getCreatedAt);
    List<OrderDetail> details = orderDetailMapper.selectList(wrapper);

    Map<String, Map<String, Object>> uniqueByModel = new LinkedHashMap<>();
    for (OrderDetail detail : details) {
        String model = detail.getProductModel();
        if (uniqueByModel.containsKey(model)) {
            continue;
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("productModel", model);
        row.put("materialTemplateCode", "BOM-" + model);
        row.put("processRoute", DEFAULT_PROCESS_ROUTE);
        row.put("remark", "由订单明细归档生成");
        uniqueByModel.put(model, row);
    }

    return success(new ArrayList<>(uniqueByModel.values()));
}
```

---

## 5) Data Statistics & Visualization Module

Source:
- `frontend-web/src/views/stats/StatsDashboard.vue`

### 5.1 Multi-source KPI loading and trend transformation

```javascript
const loadOverview = async () => {
  try {
    const res = await request.get("/api/stats/overview");
    const data = res?.data ?? res;
    overview.value = {
      avgDeliveryCycleDays: data.avgDeliveryCycleDays ?? "-",
      currentWeekYieldRate: data.currentWeekYieldRate ?? "-",
      inProgressBatchCount: data.inProgressBatchCount ?? "-",
      overdueOrderCount: data.overdueOrderCount ?? "-"
    };
  } catch (error) {
    if (error?.response?.status === 404) {
      overview.value = {
        avgDeliveryCycleDays: 16.8,
        currentWeekYieldRate: 96.2,
        inProgressBatchCount: 14,
        overdueOrderCount: 3
      };
      return;
    }
    ElMessage.error("加载统计概览失败");
  }
};

const loadDeliveryCycleSeries = async () => {
  try {
    const res = await request.get("/api/stats/delivery-cycle-trend");
    const rows = (res?.data ?? res) || [];
    const max = Math.max(...rows.map((i) => i.avgDays), 1);
    deliveryCycleSeries.value = rows.map((item) => ({
      ...item,
      progressPct: calcProgress(item.avgDays, max)
    }));
  } catch (error) {
    if (error?.response?.status === 404) {
      const mock = [
        { period: "2026-W08", avgDays: 20.2 },
        { period: "2026-W09", avgDays: 18.4 },
        { period: "2026-W10", avgDays: 17.1 },
        { period: "2026-W11", avgDays: 16.8 }
      ];
      const max = Math.max(...mock.map((i) => i.avgDays), 1);
      deliveryCycleSeries.value = mock.map((item) => ({ ...item, progressPct: calcProgress(item.avgDays, max) }));
      return;
    }
    ElMessage.error("加载交付周期趋势失败");
  }
};

onMounted(async () => {
  await Promise.all([loadOverview(), loadDeliveryCycleSeries(), loadYieldRateSeries(), loadAlerts()]);
});
```

---

## 6) Role-Based Access Control (RBAC) Module

Source:
- `backend-springboot/src/main/java/com/lw/backend/controller/AuthController.java`
- `frontend-web/src/constants/rbac.js`
- `frontend-web/src/router/index.js`

### 6.1 Authentication + role resolution (backend)

```java
@PostMapping("/login")
public Map<String, Object> login(@RequestBody LoginRequest request) {
    if (request == null || !StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
        throw new BizException("用户名和密码不能为空");
    }

    SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getUsername, request.getUsername().trim())
            .last("limit 1"));
    if (user == null) {
        throw new BizException("用户名或密码错误");
    }
    if (user.getStatus() == null || user.getStatus() != 1) {
        throw new BizException("账号已禁用");
    }
    if (!passwordMatches(request.getPassword(), user.getPasswordHash())) {
        throw new BizException("用户名或密码错误");
    }

    String roleCode = resolveRoleCode(user.getUserId());
    String displayName = StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();

    String token = "MES-" + UUID.randomUUID();
    SESSION_STORE.put(token, new LoginSession(user.getUserId(), user.getUsername(), displayName, roleCode, LocalDateTime.now()));

    user.setLastLoginTime(LocalDateTime.now());
    sysUserMapper.updateById(user);

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("token", token);
    data.put("roleCode", roleCode);
    data.put("username", displayName);
    data.put("userId", user.getUserId());
    return success(data);
}

private String resolveRoleCode(String userId) {
    List<SysUserRole> roles = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
            .eq(SysUserRole::getUserId, userId)
            .orderByAsc(SysUserRole::getCreatedAt));
    if (roles == null || roles.isEmpty()) {
        throw new BizException("账号未分配角色");
    }
    return roles.get(0).getRoleCode();
}
```

### 6.2 Route permission matrix + route guard (frontend)

```javascript
export const ROUTE_ROLE_MAP = {
  "/orders/tracking": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.SALES, ROLE_CODE.DIRECTOR, ROLE_CODE.INVENTORY_ADMIN],
  "/orders/manage": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.SALES],
  "/production/workbench": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.DIRECTOR],
  "/production/review": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.DIRECTOR],
  "/production/weaving-orders": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.DIRECTOR, ROLE_CODE.WORKER],
  "/production/setting-orders": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.DIRECTOR, ROLE_CODE.WORKER],
  "/production/cutting-orders": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.DIRECTOR, ROLE_CODE.WORKER],
  "/production/splicing-orders": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.DIRECTOR, ROLE_CODE.WORKER],
  "/production/sec-setting-orders": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.DIRECTOR, ROLE_CODE.WORKER],
  "/basic/products": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.DIRECTOR],
  "/basic/equipments": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.DIRECTOR],
  "/system/users": [ROLE_CODE.SYSTEM_ADMIN],
  "/inventory/ledger": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.SALES, ROLE_CODE.INVENTORY_ADMIN],
  "/inventory/raw-materials": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.SALES, ROLE_CODE.INVENTORY_ADMIN],
  "/inventory/wip": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.SALES, ROLE_CODE.INVENTORY_ADMIN],
  "/inventory/finished-goods": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER, ROLE_CODE.SALES, ROLE_CODE.INVENTORY_ADMIN],
  "/stats/dashboard": [ROLE_CODE.SYSTEM_ADMIN, ROLE_CODE.LEADER]
};
```

```javascript
router.beforeEach((to) => {
  const authStore = useAuthStore();

  if (to.path === "/login") {
    if (authStore.isAuthenticated.value) {
      return authStore.getDefaultRouteByRole(authStore.roleCode.value);
    }
    return true;
  }

  if (to.path === "/unauthorized") {
    return true;
  }

  if (!authStore.isAuthenticated.value) {
    return { path: "/login", query: { redirect: to.fullPath } };
  }
  if (hasRouteAccess(to.path, authStore.roleCode.value)) {
    return true;
  }
  return "/unauthorized";
});
```

