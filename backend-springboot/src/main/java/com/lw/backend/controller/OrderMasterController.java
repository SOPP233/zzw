package com.lw.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lw.backend.modules.mes.exception.BizException;
import com.lw.backend.modules.mes.entity.OrderDetail;
import com.lw.backend.modules.mes.entity.OrderMaster;
import com.lw.backend.modules.mes.mapper.OrderDetailMapper;
import com.lw.backend.modules.mes.mapper.OrderMasterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/order-masters")
@RequiredArgsConstructor
public class OrderMasterController {

    private final OrderMasterMapper mapper;
    private final OrderDetailMapper orderDetailMapper;

    @PostMapping
    public Map<String, Object> create(@RequestBody OrderMaster data) {
        mapper.insert(data);
        return successWithData(data);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable String id, @RequestBody OrderMaster data) {
        data.setOrderNo(id);
        return affected(mapper.updateById(data));
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable String id) {
        return successWithData(mapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        return affected(mapper.deleteById(id));
    }

    @GetMapping
    public Map<String, Object> page(@RequestParam(defaultValue = "1") long pageNo,
                                    @RequestParam(defaultValue = "10") long pageSize,
                                    @RequestParam(required = false) String orderNo,
                                    @RequestParam(required = false) Integer orderStatus,
                                    @RequestParam(required = false) String contractNo) {
        QueryWrapper<OrderMaster> wrapper = new QueryWrapper<OrderMaster>()
                .orderByDesc("created_at");
        if (StringUtils.hasText(orderNo)) {
            wrapper.like("order_no", orderNo);
        }
        if (orderStatus != null) {
            wrapper.eq("order_status", orderStatus);
        }
        if (StringUtils.hasText(contractNo)) {
            wrapper.eq("contract_no", contractNo);
        }
        IPage<OrderMaster> result = mapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        return successWithData(result);
    }

    @PostMapping("/{id}/production-review")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> productionReview(@PathVariable String id) {
        OrderMaster master = mapper.selectById(id);
        if (master == null) {
            throw new BizException("订单不存在");
        }
        if (master.getOrderStatus() == null || master.getOrderStatus() != 1) {
            throw new BizException("仅业务审核中订单可提交为待排产");
        }
        master.setOrderStatus(2);
        int affected = mapper.updateById(master);

        int detailAffected = orderDetailMapper.update(
                null,
                new LambdaUpdateWrapper<OrderDetail>()
                        .eq(OrderDetail::getOrderNo, id)
                        .set(OrderDetail::getDetailStatus, 2)
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", affected > 0);
        result.put("affected", affected);
        result.put("detailAffected", detailAffected);
        result.put("orderId", id);
        result.put("orderStatus", 2);
        return result;
    }

    private Map<String, Object> successWithData(Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", data);
        return result;
    }

    private Map<String, Object> affected(int affected) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", affected > 0);
        result.put("affected", affected);
        return result;
    }
}
