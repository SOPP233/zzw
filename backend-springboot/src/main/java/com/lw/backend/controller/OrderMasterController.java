package com.lw.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lw.backend.modules.mes.entity.OrderDetail;
import com.lw.backend.modules.mes.entity.OrderMaster;
import com.lw.backend.modules.mes.exception.BizException;
import com.lw.backend.modules.mes.mapper.OrderDetailMapper;
import com.lw.backend.modules.mes.mapper.OrderMasterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
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
        QueryWrapper<OrderMaster> wrapper = new QueryWrapper<OrderMaster>().orderByDesc("created_at");
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

        long pendingReviewCount = orderDetailMapper.selectCount(new LambdaQueryWrapper<OrderDetail>()
                .eq(OrderDetail::getOrderNo, id)
                .eq(OrderDetail::getDetailStatus, 1));
        if (pendingReviewCount <= 0) {
            throw new BizException("该订单无待审核明细，无法流转织造");
        }

        int detailAffected = orderDetailMapper.update(
                null,
                new LambdaUpdateWrapper<OrderDetail>()
                        .eq(OrderDetail::getOrderNo, id)
                        .eq(OrderDetail::getDetailStatus, 1)
                        .set(OrderDetail::getDetailStatus, 2)
        );

        Integer nextOrderStatus = calculateOrderStatusByDetails(id);
        master.setOrderStatus(nextOrderStatus);
        int affected = mapper.updateById(master);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", affected > 0);
        result.put("affected", affected);
        result.put("detailAffected", detailAffected);
        result.put("orderId", id);
        result.put("orderStatus", nextOrderStatus);
        return result;
    }

    private Integer calculateOrderStatusByDetails(String orderNo) {
        List<OrderDetail> details = orderDetailMapper.selectList(new LambdaQueryWrapper<OrderDetail>()
                .eq(OrderDetail::getOrderNo, orderNo));
        if (details.isEmpty()) {
            return 1;
        }
        boolean anyInWeaving = details.stream().anyMatch(d -> d.getDetailStatus() != null && d.getDetailStatus() >= 2);
        return anyInWeaving ? 3 : 1;
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