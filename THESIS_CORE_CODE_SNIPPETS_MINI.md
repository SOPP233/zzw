# 论文核心代码摘录（统一示范风格）

## 1. 订单管理：主从订单创建
来源：`OrderQueryController.java`

```java
@PostMapping("/orders/full")
public Result createOrder(@RequestBody OrderFullCreateRequest req) {
    //... [省略非核心的参数校验与基础赋值代码]...
    Map<String, Object> data = orderService.createFullOrder(req);
    return Result.success(data);
}

public Map<String, Object> createFullOrder(OrderFullCreateRequest req) {
    // 学术注释：事务边界确保主单与明细单“要么同时成功，要么同时回滚”。
    String contractNo = firstNonBlank(req.getContractNo(), req.getContractId());
    OrderMaster master = orderMasterMapper.selectOne(
            new LambdaQueryWrapper<OrderMaster>().eq(OrderMaster::getContractNo, contractNo).last("limit 1"));
    String orderNo = (master == null) ? contractNo + "D" : master.getOrderNo();

    if (master == null) {
        master = new OrderMaster();
        master.setOrderNo(orderNo);
        master.setContractNo(contractNo);
        master.setOrderStatus(1);
        orderMasterMapper.insert(master);
    }

    for (OrderDetailCreateRequest item : req.getDetails()) {
        // 学术注释：明细循环插入体现“一主多从”的制造触发单元建模。
        OrderDetail d = new OrderDetail();
        d.setDetailId(resolveDetailId(item.getDetailId(), orderNo));
        d.setOrderNo(orderNo);
        d.setProductModel(item.getProductModel());
        d.setDetailStatus(0);
        orderDetailMapper.insert(d);
    }
    return Map.of("orderNo", orderNo, "contractNo", contractNo);
}
```

该段代码将订单主从数据写入封装为统一业务方法，显著降低了数据不一致风险。主单复用与明细扩展并存，适配了离散制造中“同合同多规格”的业务结构。该设计为后续排产与追踪提供了稳定的数据入口。

---

## 2. 生产协同：合批排产与映射关系
来源：`MergeScheduleServiceImpl.java`

```java
@PostMapping("/schedule/merge")
public Result merge(@RequestBody MergeScheduleRequest req) {
    //... [省略非核心的参数校验与基础赋值代码]...
    Map<String, Object> data = scheduleService.mergeSchedule(req);
    return Result.success(data);
}

@Transactional(rollbackFor = Exception.class)
public Map<String, Object> mergeSchedule(MergeScheduleRequest req) {
    List<OrderDetail> details = fetchAndValidateDetails(req.getDetailIds());
    validateMergeEligibility(details); // 学术注释：同型号/同透气量约束，保证工艺同质性。

    String weavingBatchNo = resolveWeavingBatchNo(req.getWeavingBatchNo());
    createWeavingProcess(req, weavingBatchNo); // 学术注释：生成统一织造批次记录。

    int mode = details.size() > 1 ? 1 : 2;
    for (OrderDetail d : details) {
        // 学术注释：写入 map_order_weaving，建立“订单明细->生产批次”追溯链路。
        mapOrderWeavingMapper.insert(new MapOrderWeaving(d.getDetailId(), weavingBatchNo, 1));
        orderDetailMapper.update(null, new LambdaUpdateWrapper<OrderDetail>()
                .eq(OrderDetail::getDetailId, d.getDetailId())
                .set(OrderDetail::getDetailStatus, 1)
                .set(OrderDetail::getWeavingModeStatus, mode));
    }
    updateOrderMasterStatus(details);
    return Map.of("weavingBatchNo", weavingBatchNo, "detailCount", details.size());
}
```

该实现把合批排产抽象为“批次创建 + 映射写入 + 状态推进”三步，直接回应了小批量多品种场景下的排产效率问题。映射表机制在保证合批效率的同时保留了订单级可追溯性。事务控制确保任一步失败都不会产生半完成数据。

---

## 3. 工序串联：织造报工驱动下一工序
来源：`ProductionOrderController.java`

```java
@PostMapping("/production/weaving-orders/{batchNo}/report")
public Result submitReport(@PathVariable String batchNo, @RequestBody WeavingReportRequest req) {
    //... [省略非核心的参数校验与基础赋值代码]...
    Map<String, Object> data = productionService.submitWeavingReport(batchNo, req);
    return Result.success(data);
}

@Transactional(rollbackFor = Exception.class)
public Map<String, Object> submitWeavingReport(String batchNo, WeavingReportRequest req) {
    LocalDateTime start = parseDateTime(req.getActualStartTime(), "actualStartTime");
    LocalDateTime end = LocalDateTime.now(); // 学术注释：系统统一生成完工时间戳。

    jdbcTemplate.update(
        "UPDATE prd_weaving_report SET machine_id=?,operator_id=?,material_batch_no=?,tension_params=?,actual_length=?,actual_start_time=?,actual_end_time=? WHERE weaving_batch_no=?",
        req.getMachineId(), req.getOperatorId(), req.getMaterialBatchNo(), req.getTensionParams(),
        req.getActualLength(), Timestamp.valueOf(start), Timestamp.valueOf(end), batchNo);

    // 学术注释：工序状态从执行态推进到完工态。
    jdbcTemplate.update("UPDATE prd_weaving_process SET process_status=3, completed_at=? WHERE weaving_batch_no=?",
        Timestamp.valueOf(end), batchNo);

    // 学术注释：同步订单状态并激活后继定型工序，形成先后约束任务链。
    syncOrderStatusForWeavingBatch(batchNo);
    activateSettingProcess(batchNo);
    return Map.of("weavingBatchNo", batchNo, "processStatus", 3, "settingActivated", true);
}
```

该代码实现了“报工即流转”的核心机制，将工艺数据落库、状态推进与后继任务激活统一在一次事务中完成。其优势在于减少人工交接环节造成的流程断裂。该设计保障了工序链连续性与执行数据一致性。

---

## 4. 库存管理：分类汇总与单据驱动入口
来源：`InventoryController.java`

```java
@GetMapping("/inventory/summary")
public Result summary() {
    //... [省略非核心的参数校验与基础赋值代码]...
    Map<String, Object> data = inventoryService.calcSummary();
    return Result.success(data);
}

public Map<String, Object> calcSummary() {
    List<MaterialInventory> rows = materialInventoryMapper.selectList(new QueryWrapper<>());
    BigDecimal raw = BigDecimal.ZERO, wip = BigDecimal.ZERO, finished = BigDecimal.ZERO;

    for (MaterialInventory row : rows) {
        // 学术注释：库存按原料/在制/成品三类聚合，支撑管理层分层监控。
        String category = mapCategory(row.getMaterialType());
        BigDecimal stock = row.getCurrentStock() == null ? BigDecimal.ZERO : row.getCurrentStock();
        if ("raw".equals(category)) raw = raw.add(stock);
        else if ("wip".equals(category)) wip = wip.add(stock);
        else if ("finished".equals(category)) finished = finished.add(stock);
    }
    return Map.of("rawMaterial", raw, "wip", wip, "finishedGoods", finished);
}
```

该片段展示了库存模块的核心计算路径，即按业务语义进行分类聚合而非简单总和。该做法可以直接服务“原料保障、在制控制、成品交付”三类管理目标。并且其上层接口保留了单据驱动扩展点，便于后续接入原子增减事务。

---

## 5. 基础数据：产品工艺路线模板化
来源：`BasicDataController.java`

```java
@GetMapping("/basic/products")
public Result listProducts(@RequestParam(required = false) String productModel) {
    //... [省略非核心的参数校验与基础赋值代码]...
    List<Map<String, Object>> data = basicService.listProductTemplates(productModel);
    return Result.success(data);
}

public List<Map<String, Object>> listProductTemplates(String productModel) {
    List<OrderDetail> details = orderDetailMapper.selectList(new LambdaQueryWrapper<OrderDetail>()
            .select(OrderDetail::getProductModel, OrderDetail::getUpdatedAt)
            .isNotNull(OrderDetail::getProductModel)
            .ne(OrderDetail::getProductModel, "")
            .like(StringUtils.hasText(productModel), OrderDetail::getProductModel, productModel)
            .orderByDesc(OrderDetail::getUpdatedAt));

    List<String> route = List.of("织造", "定型", "裁网", "插接", "二次定型");
    Map<String, Map<String, Object>> uniqueByModel = new LinkedHashMap<>();
    for (OrderDetail d : details) {
        if (uniqueByModel.containsKey(d.getProductModel())) continue;
        // 学术注释：将历史订单沉淀为标准模板，实现主数据复用。
        uniqueByModel.put(d.getProductModel(), Map.of(
                "productModel", d.getProductModel(),
                "materialTemplateCode", "BOM-" + d.getProductModel(),
                "processRoute", route));
    }
    return new ArrayList<>(uniqueByModel.values());
}
```

该设计将分散的交易数据抽象为标准化模板，显著降低了新订单配置成本。通过工艺路线字典化，系统可以在业务扩展时保持过程定义一致。它有效支撑了主数据驱动的稳定运行机制。

---

## 6. RBAC：登录鉴权与路由拦截（示范同款风格）
来源：`AuthController.java` + `router/index.js`

```java
@PostMapping("/login")
public Result login(@RequestBody LoginRequest request) {
    //... [省略非核心的参数校验与基础赋值代码]...
    Map<String, Object> loginUser = authService.login(request);
    return Result.success(loginUser);
}

public Map<String, Object> login(LoginRequest request) {
    // 学术注释：身份检索 + 密码校验 + 账号状态校验，形成认证闭环。
    SysUser dbUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getUsername, request.getUsername().trim()).last("limit 1"));
    if (dbUser == null) throw new BizException("user_not_exist");
    if (dbUser.getStatus() == null || dbUser.getStatus() != 1) throw new BizException("account_disabled");
    if (!passwordMatches(request.getPassword(), dbUser.getPasswordHash())) throw new BizException("password_error");

    // 学术注释：角色来源于 sys_user_role，实现用户与权限模型解耦。
    String roleCode = resolveRoleCode(dbUser.getUserId());
    String token = "MES-" + UUID.randomUUID();
    SESSION_STORE.put(token, new LoginSession(dbUser.getUserId(), dbUser.getUsername(), dbUser.getUsername(), roleCode, LocalDateTime.now()));
    return Map.of("token", token, "roleCode", roleCode, "userId", dbUser.getUserId());
}
```

```javascript
router.beforeEach((to) => {
  const authStore = useAuthStore();
  // 学术注释：未认证主体统一拦截，防止绕过登录访问业务资源。
  if (!authStore.isAuthenticated.value) return { path: "/login", query: { redirect: to.fullPath } };
  // 学术注释：基于角色-路径矩阵执行授权判定，拒绝越权路由。
  if (!hasRouteAccess(to.path, authStore.roleCode.value)) return "/unauthorized";
  return true;
});
```

该模块以示范风格实现了“控制器转发 + 服务层鉴权 + 前端守卫拦截”的分层闭环。认证、授权和路由控制职责边界清晰，便于后续策略扩展。该设计能有效抑制越权访问并提升权限体系可维护性。

