package com.lw.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lw.backend.modules.mes.entity.OrderDetail;
import com.lw.backend.modules.mes.entity.ProductionPlan;
import com.lw.backend.modules.mes.mapper.OrderDetailMapper;
import com.lw.backend.modules.mes.mapper.ProductionPlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/basic")
@RequiredArgsConstructor
public class BasicDataController {

    private static final List<String> DEFAULT_PROCESS_ROUTE = List.of("织造", "定型", "裁网", "插接", "二次定型");

    private final OrderDetailMapper orderDetailMapper;
    private final ProductionPlanMapper productionPlanMapper;

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

    @GetMapping("/equipments")
    public Map<String, Object> listEquipments(@RequestParam(required = false) String assetCode,
                                              @RequestParam(required = false) String workSection) {
        LambdaQueryWrapper<ProductionPlan> wrapper = new LambdaQueryWrapper<ProductionPlan>()
                .select(ProductionPlan::getMachineId, ProductionPlan::getUpdatedAt, ProductionPlan::getCreatedAt, ProductionPlan::getPlanStatus)
                .isNotNull(ProductionPlan::getMachineId)
                .ne(ProductionPlan::getMachineId, "")
                .like(StringUtils.hasText(assetCode), ProductionPlan::getMachineId, assetCode)
                .orderByDesc(ProductionPlan::getUpdatedAt)
                .orderByDesc(ProductionPlan::getCreatedAt);
        List<ProductionPlan> plans = productionPlanMapper.selectList(wrapper);

        Map<String, Map<String, Object>> uniqueByMachine = new LinkedHashMap<>();
        for (ProductionPlan plan : plans) {
            String machineId = plan.getMachineId();
            if (uniqueByMachine.containsKey(machineId)) {
                continue;
            }
            String section = inferWorkSection(machineId);
            if (StringUtils.hasText(workSection) && !section.contains(workSection.trim())) {
                continue;
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("equipmentId", machineId);
            row.put("assetCode", machineId);
            row.put("equipmentName", "设备-" + machineId);
            row.put("workSection", section);
            row.put("supportedProductRange", "通用型号");
            row.put("capacityFactor", BigDecimal.ONE);
            row.put("enabled", plan.getPlanStatus() == null || plan.getPlanStatus() != 9);
            uniqueByMachine.put(machineId, row);
        }

        return success(new ArrayList<>(uniqueByMachine.values()));
    }

    private String inferWorkSection(String machineId) {
        String upper = machineId.toUpperCase();
        if (upper.contains("WEAV") || upper.contains("ZZ") || upper.startsWith("W")) {
            return "织造段";
        }
        if (upper.contains("SET") || upper.contains("DING") || upper.contains("DX")) {
            return "定型段";
        }
        if (upper.contains("CUT") || upper.contains("CAI") || upper.contains("CW")) {
            return "裁网段";
        }
        if (upper.contains("INS") || upper.contains("JIE") || upper.contains("CJ")) {
            return "插接段";
        }
        return "综合工段";
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", data);
        return result;
    }
}
