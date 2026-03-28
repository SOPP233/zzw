package com.lw.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lw.backend.modules.mes.entity.Customer;
import com.lw.backend.modules.mes.entity.OrderDetail;
import com.lw.backend.modules.mes.entity.OrderMaster;
import com.lw.backend.modules.mes.mapper.CustomerMapper;
import com.lw.backend.modules.mes.mapper.OrderDetailMapper;
import com.lw.backend.modules.mes.mapper.OrderMasterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderQueryController {

    private final OrderMasterMapper orderMasterMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final CustomerMapper customerMapper;

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String customerName) {
        List<Customer> customers = customerMapper.selectList(new LambdaQueryWrapper<Customer>()
                .like(StringUtils.hasText(customerName), Customer::getCustomerName, customerName));

        if (customers.isEmpty()) {
            return success(Collections.emptyList());
        }

        Set<String> customerIds = customers.stream().map(Customer::getCustomerId).collect(Collectors.toSet());
        Map<String, String> customerNameMap = customers.stream()
                .collect(Collectors.toMap(Customer::getCustomerId, Customer::getCustomerName, (a, b) -> a));

        List<OrderMaster> masters = orderMasterMapper.selectList(new LambdaQueryWrapper<OrderMaster>()
                .in(OrderMaster::getCustomerId, customerIds)
                .orderByDesc(OrderMaster::getCreatedAt));

        if (masters.isEmpty()) {
            return success(Collections.emptyList());
        }

        Set<String> orderIds = masters.stream().map(OrderMaster::getOrderId).collect(Collectors.toSet());
        List<OrderDetail> details = orderDetailMapper.selectList(new LambdaQueryWrapper<OrderDetail>()
                .in(OrderDetail::getOrderId, orderIds)
                .orderByAsc(OrderDetail::getCreatedAt));

        Map<String, List<OrderDetail>> detailGroup = details.stream()
                .collect(Collectors.groupingBy(OrderDetail::getOrderId));

        List<Map<String, Object>> rows = masters.stream().map(master -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderId", master.getOrderId());
            row.put("customerId", master.getCustomerId());
            row.put("customerName", customerNameMap.get(master.getCustomerId()));
            row.put("expectedDate", master.getExpectedDate());
            row.put("orderStatus", master.getOrderStatus());
            row.put("totalAmount", master.getTotalAmount());
            row.put("details", detailGroup.getOrDefault(master.getOrderId(), Collections.emptyList()));
            return row;
        }).toList();

        return success(rows);
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", data);
        return result;
    }
}

