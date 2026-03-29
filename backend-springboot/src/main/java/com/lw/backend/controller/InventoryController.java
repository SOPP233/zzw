package com.lw.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lw.backend.modules.mes.entity.MaterialInventory;
import com.lw.backend.modules.mes.mapper.MaterialInventoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final MaterialInventoryMapper materialInventoryMapper;

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

    @GetMapping("/issue-docs")
    public Map<String, Object> issueDocs(@RequestParam(defaultValue = "1") long pageNo,
                                         @RequestParam(defaultValue = "200") long pageSize) {
        return successWithData(emptyPage(pageNo, pageSize));
    }

    @GetMapping("/inbound-docs")
    public Map<String, Object> inboundDocs(@RequestParam(defaultValue = "1") long pageNo,
                                           @RequestParam(defaultValue = "200") long pageSize) {
        return successWithData(emptyPage(pageNo, pageSize));
    }

    @PostMapping("/issue-docs/{docNo}/approve")
    public Map<String, Object> approveIssue(@PathVariable String docNo) {
        return successMessage("预领料单审核完成", docNo);
    }

    @PostMapping("/inbound-docs/{docNo}/confirm")
    public Map<String, Object> confirmInbound(@PathVariable String docNo) {
        return successMessage("成品入库确认完成", docNo);
    }

    private String mapCategory(String materialType) {
        if (materialType == null) {
            return "";
        }
        String normalized = materialType.toLowerCase(Locale.ROOT);
        if (normalized.contains("raw") || normalized.contains("原")) {
            return "raw";
        }
        if (normalized.contains("wip") || normalized.contains("半")) {
            return "wip";
        }
        if (normalized.contains("finished") || normalized.contains("成")) {
            return "finished";
        }
        return "";
    }

    private Map<String, Object> emptyPage(long pageNo, long pageSize) {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("records", new ArrayList<>());
        page.put("total", 0L);
        page.put("current", pageNo);
        page.put("size", pageSize);
        page.put("pages", 0L);
        return page;
    }

    private Map<String, Object> successWithData(Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", data);
        return result;
    }

    private Map<String, Object> successMessage(String message, String docNo) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("docNo", docNo);
        data.put("message", message);
        return successWithData(data);
    }
}

