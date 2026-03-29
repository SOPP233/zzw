package com.lw.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/production")
@RequiredArgsConstructor
public class ProductionOrderController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/weaving-orders")
    public Map<String, Object> weavingOrders(@RequestParam(defaultValue = "1") long pageNo,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String batchNo,
                                             @RequestParam(required = false) Integer processStatus) {
        return queryPage(
                "prd_weaving_process",
                "weaving_batch_no",
                pageNo,
                pageSize,
                batchNo,
                processStatus
        );
    }

    @GetMapping("/setting-orders")
    public Map<String, Object> settingOrders(@RequestParam(defaultValue = "1") long pageNo,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String batchNo,
                                             @RequestParam(required = false) Integer processStatus) {
        return queryPage(
                "prd_setting_process",
                "setting_batch_no",
                pageNo,
                pageSize,
                batchNo,
                processStatus
        );
    }

    @GetMapping("/cutting-orders")
    public Map<String, Object> cuttingOrders(@RequestParam(defaultValue = "1") long pageNo,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String batchNo,
                                             @RequestParam(required = false) Integer processStatus) {
        return queryPage(
                "prd_cutting_process",
                "cut_batch_no",
                pageNo,
                pageSize,
                batchNo,
                processStatus
        );
    }

    @GetMapping("/splicing-orders")
    public Map<String, Object> splicingOrders(@RequestParam(defaultValue = "1") long pageNo,
                                              @RequestParam(defaultValue = "10") long pageSize,
                                              @RequestParam(required = false) String batchNo,
                                              @RequestParam(required = false) Integer processStatus) {
        return queryPage(
                "prd_splicing_process",
                "splice_batch_no",
                pageNo,
                pageSize,
                batchNo,
                processStatus
        );
    }

    @GetMapping("/sec-setting-orders")
    public Map<String, Object> secSettingOrders(@RequestParam(defaultValue = "1") long pageNo,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(required = false) String batchNo,
                                                @RequestParam(required = false) Integer processStatus) {
        return queryPage(
                "prd_sec_setting_process",
                "final_batch_no",
                pageNo,
                pageSize,
                batchNo,
                processStatus
        );
    }

    private Map<String, Object> queryPage(String table,
                                          String idColumn,
                                          long pageNo,
                                          long pageSize,
                                          String batchNo,
                                          Integer processStatus) {
        long safePageNo = Math.max(pageNo, 1);
        long safePageSize = Math.max(pageSize, 1);
        long offset = (safePageNo - 1) * safePageSize;

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(batchNo)) {
            where.append(" AND ").append(idColumn).append(" LIKE ? ");
            args.add("%" + batchNo.trim() + "%");
        }
        if (processStatus != null) {
            where.append(" AND process_status = ? ");
            args.add(processStatus);
        }

        String countSql = "SELECT COUNT(1) FROM " + table + where;
        Long total = jdbcTemplate.queryForObject(countSql, args.toArray(), Long.class);

        String dataSql = "SELECT * FROM " + table + where + " ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((int) safePageSize);
        queryArgs.add((int) offset);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(dataSql, queryArgs.toArray());

        Map<String, Object> page = new LinkedHashMap<>();
        page.put("records", records);
        page.put("total", total == null ? 0L : total);
        page.put("current", safePageNo);
        page.put("size", safePageSize);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", page);
        return result;
    }
}
