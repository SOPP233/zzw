package com.lw.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lw.backend.modules.mes.exception.BizException;
import com.lw.backend.modules.mes.entity.ContractMaster;
import com.lw.backend.modules.mes.entity.Customer;
import com.lw.backend.modules.mes.entity.OrderDetail;
import com.lw.backend.modules.mes.entity.OrderMaster;
import com.lw.backend.modules.mes.entity.PlanDetailRelation;
import com.lw.backend.modules.mes.entity.ProcessTask;
import com.lw.backend.modules.mes.mapper.ContractMasterMapper;
import com.lw.backend.modules.mes.mapper.CustomerMapper;
import com.lw.backend.modules.mes.mapper.OrderDetailMapper;
import com.lw.backend.modules.mes.mapper.OrderMasterMapper;
import com.lw.backend.modules.mes.mapper.PlanDetailRelationMapper;
import com.lw.backend.modules.mes.mapper.ProcessTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private final ContractMasterMapper contractMasterMapper;
    private final PlanDetailRelationMapper planDetailRelationMapper;
    private final ProcessTaskMapper processTaskMapper;

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
        Map<String, DetailProcessProgress> progressMap = buildDetailProgressMap(details);

        Map<String, List<OrderDetail>> detailGroup = details.stream()
                .collect(Collectors.groupingBy(OrderDetail::getOrderId));

        List<Map<String, Object>> rows = masters.stream().map(master -> {
            List<OrderDetail> orderDetails = detailGroup.getOrDefault(master.getOrderId(), Collections.emptyList());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderId", master.getOrderId());
            row.put("customerId", master.getCustomerId());
            row.put("customerName", customerNameMap.get(master.getCustomerId()));
            row.put("expectedDate", master.getExpectedDate());
            row.put("orderStatus", resolveOrderStatus(master.getOrderStatus(), orderDetails));
            row.put("totalAmount", master.getTotalAmount());
            row.put("details", orderDetails.stream().map(detail -> toDetailRow(detail, progressMap.get(detail.getDetailId()))).toList());
            return row;
        }).toList();

        return success(rows);
    }

    @GetMapping("/full")
    public Map<String, Object> fullPage(@RequestParam(defaultValue = "1") long pageNo,
                                        @RequestParam(defaultValue = "10") long pageSize,
                                        @RequestParam(required = false) String orderId,
                                        @RequestParam(required = false) String customerName,
                                        @RequestParam(required = false) Integer orderStatus) {
        List<Customer> customers = customerMapper.selectList(new LambdaQueryWrapper<Customer>()
                .like(StringUtils.hasText(customerName), Customer::getCustomerName, customerName));
        if (customers.isEmpty()) {
            return success(emptyPage(pageNo, pageSize));
        }

        Set<String> customerIds = customers.stream().map(Customer::getCustomerId).collect(Collectors.toSet());
        Map<String, String> customerNameMap = customers.stream()
                .collect(Collectors.toMap(Customer::getCustomerId, Customer::getCustomerName, (a, b) -> a));

        LambdaQueryWrapper<OrderMaster> wrapper = new LambdaQueryWrapper<OrderMaster>()
                .in(OrderMaster::getCustomerId, customerIds)
                .like(StringUtils.hasText(orderId), OrderMaster::getOrderId, orderId)
                .eq(orderStatus != null, OrderMaster::getOrderStatus, orderStatus)
                .orderByDesc(OrderMaster::getCreatedAt);
        IPage<OrderMaster> pageResult = orderMasterMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<OrderMaster> masters = pageResult.getRecords();
        if (masters.isEmpty()) {
            return success(emptyPage(pageNo, pageSize, pageResult.getTotal()));
        }

        Set<String> orderIds = masters.stream().map(OrderMaster::getOrderId).collect(Collectors.toSet());
        List<OrderDetail> details = orderDetailMapper.selectList(new LambdaQueryWrapper<OrderDetail>()
                .in(OrderDetail::getOrderId, orderIds)
                .orderByAsc(OrderDetail::getCreatedAt));
        Map<String, DetailProcessProgress> progressMap = buildDetailProgressMap(details);
        Map<String, List<OrderDetail>> detailGroup = details.stream()
                .collect(Collectors.groupingBy(OrderDetail::getOrderId));

        List<Map<String, Object>> records = masters.stream().map(master -> {
            List<OrderDetail> orderDetails = detailGroup.getOrDefault(master.getOrderId(), Collections.emptyList());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderId", master.getOrderId());
            row.put("contractId", master.getContractId());
            row.put("customerId", master.getCustomerId());
            row.put("customerName", customerNameMap.get(master.getCustomerId()));
            row.put("signDate", master.getCreatedAt() == null ? null : master.getCreatedAt().toLocalDate());
            row.put("totalAmount", master.getTotalAmount());
            row.put("deliveryAddress", master.getRemark());
            row.put("expectedDate", master.getExpectedDate());
            row.put("orderStatus", resolveOrderStatus(master.getOrderStatus(), orderDetails));
            row.put("details", orderDetails.stream()
                    .map(detail -> toDetailRow(detail, progressMap.get(detail.getDetailId())))
                    .toList());
            return row;
        }).toList();

        Map<String, Object> pageData = new LinkedHashMap<>();
        pageData.put("records", records);
        pageData.put("total", pageResult.getTotal());
        pageData.put("current", pageResult.getCurrent());
        pageData.put("size", pageResult.getSize());
        return success(pageData);
    }

    @PostMapping("/full")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createFull(@RequestBody OrderFullCreateRequest request) {
        if (!StringUtils.hasText(request.getContractId())) {
            throw new BizException("contractId 不能为空");
        }
        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new BizException("至少需要一条订单明细");
        }
        ContractMaster contract = contractMasterMapper.selectById(request.getContractId());
        if (contract == null) {
            throw new BizException("合同不存在");
        }
        String resolvedCustomerId = StringUtils.hasText(request.getCustomerId()) ? request.getCustomerId() : contract.getCustomerId();
        if (!contract.getCustomerId().equals(resolvedCustomerId)) {
            throw new BizException("订单客户与合同客户不一致");
        }

        Customer customer = customerMapper.selectById(resolvedCustomerId);
        if (customer == null) {
            throw new BizException("客户不存在");
        }

        String orderId = StringUtils.hasText(request.getOrderId())
                ? request.getOrderId()
                : "ORD" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        OrderMaster master = new OrderMaster();
        master.setOrderId(orderId);
        master.setContractId(request.getContractId());
        master.setCustomerId(resolvedCustomerId);
        master.setTotalAmount(request.getTotalAmount() == null ? BigDecimal.ZERO : request.getTotalAmount());
        master.setExpectedDate(request.getExpectedDate() == null ? LocalDate.now().plusDays(7) : request.getExpectedDate());
        master.setOrderStatus(request.getOrderStatus() == null ? 1 : request.getOrderStatus());
        master.setRemark(request.getDeliveryAddress());
        orderMasterMapper.insert(master);

        int index = 1;
        for (OrderFullCreateRequest.OrderDetailCreateRequest item : request.getDetails()) {
            if (!StringUtils.hasText(item.getProductModel())) {
                throw new BizException("明细产品型号不能为空");
            }
            if (item.getAirPermeability() == null) {
                throw new BizException("明细透气量不能为空");
            }
            OrderDetail detail = new OrderDetail();
            detail.setDetailId(StringUtils.hasText(item.getDetailId())
                    ? item.getDetailId()
                    : orderId.replace("ORD", "DET") + String.format("%03d", index++));
            detail.setOrderId(orderId);
            detail.setProductModel(item.getProductModel());
            detail.setAirPermeability(item.getAirPermeability());
            detail.setLengthReq(item.getLengthReq() == null ? 0 : item.getLengthReq());
            detail.setWidthReq(item.getWidthReq() == null ? 0 : item.getWidthReq());
            detail.setCraftReq(item.getCraftReq());
            detail.setDetailStatus(item.getDetailStatus() == null ? 1 : item.getDetailStatus());
            orderDetailMapper.insert(detail);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", orderId);
        return success(data);
    }

    private Map<String, Object> toDetailRow(OrderDetail detail, DetailProcessProgress progress) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("detailId", detail.getDetailId());
        row.put("orderId", detail.getOrderId());
        row.put("productModel", detail.getProductModel());
        row.put("airPermeability", detail.getAirPermeability());
        row.put("lengthReq", detail.getLengthReq());
        row.put("widthReq", detail.getWidthReq());
        row.put("craftReq", detail.getCraftReq());
        row.put("detailStatus", detail.getDetailStatus());
        row.put("currentProcessType", progress == null ? null : progress.getProcessType());
        row.put("currentTaskStatus", progress == null ? null : progress.getTaskStatus());
        return row;
    }

    private Map<String, DetailProcessProgress> buildDetailProgressMap(List<OrderDetail> details) {
        if (details == null || details.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> detailIds = details.stream().map(OrderDetail::getDetailId).collect(Collectors.toSet());
        List<PlanDetailRelation> relations = planDetailRelationMapper.selectList(
                new LambdaQueryWrapper<PlanDetailRelation>()
                        .in(PlanDetailRelation::getDetailId, detailIds)
                        .orderByDesc(PlanDetailRelation::getCreatedAt)
        );
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> detailBatchMap = new LinkedHashMap<>();
        for (PlanDetailRelation relation : relations) {
            detailBatchMap.putIfAbsent(relation.getDetailId(), relation.getBatchId());
        }
        Set<String> batchIds = detailBatchMap.values().stream().filter(StringUtils::hasText).collect(Collectors.toSet());
        if (batchIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ProcessTask> tasks = processTaskMapper.selectList(
                new LambdaQueryWrapper<ProcessTask>()
                        .in(ProcessTask::getBatchId, batchIds)
                        .orderByAsc(ProcessTask::getProcessType)
                        .orderByDesc(ProcessTask::getCreatedAt)
        );
        Map<String, List<ProcessTask>> batchTaskMap = tasks.stream().collect(Collectors.groupingBy(ProcessTask::getBatchId));
        Map<String, DetailProcessProgress> result = new LinkedHashMap<>();
        detailBatchMap.forEach((detailId, batchId) -> {
            List<ProcessTask> batchTasks = batchTaskMap.getOrDefault(batchId, Collections.emptyList());
            DetailProcessProgress progress = resolveProgress(batchTasks);
            if (progress != null) {
                result.put(detailId, progress);
            }
        });
        return result;
    }

    private DetailProcessProgress resolveProgress(List<ProcessTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return null;
        }
        ProcessTask active = tasks.stream()
                .filter(t -> t.getStatus() != null && t.getStatus() != 3)
                .sorted((a, b) -> Integer.compare(a.getProcessType(), b.getProcessType()))
                .findFirst()
                .orElse(null);
        if (active != null) {
            return new DetailProcessProgress(active.getProcessType(), active.getStatus());
        }
        ProcessTask last = tasks.stream()
                .filter(t -> t.getStatus() != null && t.getStatus() == 3)
                .max((a, b) -> Integer.compare(a.getProcessType(), b.getProcessType()))
                .orElse(null);
        if (last != null) {
            return new DetailProcessProgress(last.getProcessType(), last.getStatus());
        }
        return null;
    }

    private Integer resolveOrderStatus(Integer masterStatus, List<OrderDetail> details) {
        if (details == null || details.isEmpty()) {
            return masterStatus;
        }
        List<Integer> statuses = details.stream()
                .map(OrderDetail::getDetailStatus)
                .filter(v -> v != null)
                .toList();
        if (statuses.isEmpty()) {
            return masterStatus;
        }
        boolean allCompleted = statuses.stream().allMatch(v -> v == 5);
        if (allCompleted) {
            return 5;
        }
        if (statuses.stream().anyMatch(v -> v == 4)) {
            return 4;
        }
        if (statuses.stream().anyMatch(v -> v == 3)) {
            return 3;
        }
        if (statuses.stream().anyMatch(v -> v == 2)) {
            return 2;
        }
        if (statuses.stream().anyMatch(v -> v == 1)) {
            return 1;
        }
        return masterStatus;
    }

    private static class DetailProcessProgress {
        private final Integer processType;
        private final Integer taskStatus;

        private DetailProcessProgress(Integer processType, Integer taskStatus) {
            this.processType = processType;
            this.taskStatus = taskStatus;
        }

        public Integer getProcessType() {
            return processType;
        }

        public Integer getTaskStatus() {
            return taskStatus;
        }
    }

    private Map<String, Object> emptyPage(long pageNo, long pageSize) {
        return emptyPage(pageNo, pageSize, 0L);
    }

    private Map<String, Object> emptyPage(long pageNo, long pageSize, long total) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", Collections.emptyList());
        data.put("total", total);
        data.put("current", pageNo);
        data.put("size", pageSize);
        return data;
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", data);
        return result;
    }

    public static class OrderFullCreateRequest {
        private String orderId;
        private String contractId;
        private String customerId;
        private BigDecimal totalAmount;
        private LocalDate expectedDate;
        private Integer orderStatus;
        private String deliveryAddress;
        private List<OrderDetailCreateRequest> details = new ArrayList<>();

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public String getContractId() {
            return contractId;
        }

        public void setContractId(String contractId) {
            this.contractId = contractId;
        }

        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }

        public LocalDate getExpectedDate() {
            return expectedDate;
        }

        public void setExpectedDate(LocalDate expectedDate) {
            this.expectedDate = expectedDate;
        }

        public Integer getOrderStatus() {
            return orderStatus;
        }

        public void setOrderStatus(Integer orderStatus) {
            this.orderStatus = orderStatus;
        }

        public String getDeliveryAddress() {
            return deliveryAddress;
        }

        public void setDeliveryAddress(String deliveryAddress) {
            this.deliveryAddress = deliveryAddress;
        }

        public List<OrderDetailCreateRequest> getDetails() {
            return details;
        }

        public void setDetails(List<OrderDetailCreateRequest> details) {
            this.details = details;
        }

        public static class OrderDetailCreateRequest {
            private String detailId;
            private String productModel;
            private Integer airPermeability;
            private Integer lengthReq;
            private Integer widthReq;
            private String craftReq;
            private Integer detailStatus;

            public String getDetailId() {
                return detailId;
            }

            public void setDetailId(String detailId) {
                this.detailId = detailId;
            }

            public String getProductModel() {
                return productModel;
            }

            public void setProductModel(String productModel) {
                this.productModel = productModel;
            }

            public Integer getLengthReq() {
                return lengthReq;
            }

            public Integer getAirPermeability() {
                return airPermeability;
            }

            public void setAirPermeability(Integer airPermeability) {
                this.airPermeability = airPermeability;
            }

            public void setLengthReq(Integer lengthReq) {
                this.lengthReq = lengthReq;
            }

            public Integer getWidthReq() {
                return widthReq;
            }

            public void setWidthReq(Integer widthReq) {
                this.widthReq = widthReq;
            }

            public String getCraftReq() {
                return craftReq;
            }

            public void setCraftReq(String craftReq) {
                this.craftReq = craftReq;
            }

            public Integer getDetailStatus() {
                return detailStatus;
            }

            public void setDetailStatus(Integer detailStatus) {
                this.detailStatus = detailStatus;
            }
        }
    }
}
