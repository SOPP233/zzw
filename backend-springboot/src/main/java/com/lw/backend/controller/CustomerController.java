package com.lw.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lw.backend.modules.mes.entity.Customer;
import com.lw.backend.modules.mes.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerMapper customerMapper;

    @PostMapping
    public Map<String, Object> create(@RequestBody Customer customer) {
        customerMapper.insert(customer);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", customer);
        return result;
    }

    @PutMapping("/{customerId}")
    public Map<String, Object> update(@PathVariable String customerId, @RequestBody Customer customer) {
        customer.setCustomerId(customerId);
        int affected = customerMapper.updateById(customer);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", affected > 0);
        result.put("affected", affected);
        return result;
    }

    @GetMapping("/{customerId}")
    public Map<String, Object> detail(@PathVariable String customerId) {
        Customer customer = customerMapper.selectById(customerId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", customer != null);
        result.put("data", customer);
        return result;
    }

    @DeleteMapping("/{customerId}")
    public Map<String, Object> delete(@PathVariable String customerId) {
        int affected = customerMapper.deleteById(customerId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", affected > 0);
        result.put("affected", affected);
        return result;
    }

    @GetMapping
    public Map<String, Object> page(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String customerName
    ) {
        Page<Customer> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(customerName)) {
            wrapper.like(Customer::getCustomerName, customerName);
        }
        wrapper.orderByDesc(Customer::getUpdatedAt);
        IPage<Customer> pageResult = customerMapper.selectPage(page, wrapper);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", pageResult);
        return result;
    }
}

