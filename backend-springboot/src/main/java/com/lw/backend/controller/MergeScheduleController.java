package com.lw.backend.controller;

import com.lw.backend.modules.mes.dto.MergeScheduleRequest;
import com.lw.backend.modules.mes.service.MergeScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class MergeScheduleController {

    private final MergeScheduleService mergeScheduleService;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/workbench")
    public Map<String, Object> workbench(@RequestParam(defaultValue = "1") long pageNo,
                                         @RequestParam(defaultValue = "10") long pageSize,
                                         @RequestParam(required = false) String orderNo,
                                         @RequestParam(required = false) String productModel,
                                         @RequestParam(required = false) Integer airPermeability) {
        long safePageNo = Math.max(pageNo, 1);
        long safePageSize = Math.max(pageSize, 1);
        long offset = (safePageNo - 1) * safePageSize;

        StringBuilder where = new StringBuilder(" WHERE d.detail_status = 0 ");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(orderNo)) {
            where.append(" AND d.order_no LIKE ? ");
            args.add("%" + orderNo.trim() + "%");
        }
        if (StringUtils.hasText(productModel)) {
            where.append(" AND d.product_model LIKE ? ");
            args.add("%" + productModel.trim() + "%");
        }
        if (airPermeability != null) {
            where.append(" AND d.air_permeability = ? ");
            args.add(airPermeability);
        }

        String countSql = "SELECT COUNT(1) FROM order_detail d" + where;
        Long total = jdbcTemplate.queryForObject(countSql, args.toArray(), Long.class);

        String dataSql = "SELECT d.detail_id AS detailId, d.order_no AS orderNo, d.product_model AS productModel, " +
                "d.air_permeability AS airPermeability, d.req_length AS reqLength, d.req_width AS reqWidth, " +
                "d.detail_status AS detailStatus, om.contract_no AS contractNo, c.customer_id AS customerId, " +
                "cu.customer_name AS customerName, om.expected_date AS expectedDate " +
                "FROM order_detail d " +
                "LEFT JOIN order_master om ON om.order_no = d.order_no " +
                "LEFT JOIN contract_master c ON c.contract_id = om.contract_no " +
                "LEFT JOIN customer cu ON cu.customer_id = c.customer_id " +
                where +
                " ORDER BY d.created_at DESC LIMIT ? OFFSET ?";

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

    @PostMapping("/merge")
    public Map<String, Object> merge(@RequestBody MergeScheduleRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", mergeScheduleService.mergeSchedule(request));
        return result;
    }
}
