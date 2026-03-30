package com.lw.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lw.backend.modules.mes.entity.ContractMaster;
import com.lw.backend.modules.mes.entity.Customer;
import com.lw.backend.modules.mes.entity.OrderDetail;
import com.lw.backend.modules.mes.entity.OrderMaster;
import com.lw.backend.modules.mes.exception.BizException;
import com.lw.backend.modules.mes.mapper.ContractMasterMapper;
import com.lw.backend.modules.mes.mapper.CustomerMapper;
import com.lw.backend.modules.mes.mapper.OrderDetailMapper;
import com.lw.backend.modules.mes.mapper.OrderMasterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String customerName) {
        CustomerContractContext context = buildCustomerContractContext(customerName);
        if (context.contractIds().isEmpty()) {
            return success(Collections.emptyList());
        }

        List<OrderMaster> masters = orderMasterMapper.selectList(new LambdaQueryWrapper<OrderMaster>()
                .in(OrderMaster::getContractNo, context.contractIds())
                .orderByDesc(OrderMaster::getCreatedAt));
        if (masters.isEmpty()) {
            return success(Collections.emptyList());
        }

        Map<String, List<OrderDetail>> detailGroup = buildDetailGroup(masters);
        List<Map<String, Object>> rows = masters.stream()
                .map(master -> toOrderRow(master, detailGroup.getOrDefault(master.getOrderNo(), Collections.emptyList()), context))
                .toList();
        return success(rows);
    }

    @GetMapping("/full")
    public Map<String, Object> fullPage(@RequestParam(defaultValue = "1") long pageNo,
                                        @RequestParam(defaultValue = "10") long pageSize,
                                        @RequestParam(required = false) String orderId,
                                        @RequestParam(required = false) String customerName,
                                        @RequestParam(required = false) Integer orderStatus) {
        CustomerContractContext context = buildCustomerContractContext(customerName);
        if (context.contractIds().isEmpty()) {
            return success(emptyPage(pageNo, pageSize));
        }

        LambdaQueryWrapper<OrderMaster> wrapper = new LambdaQueryWrapper<OrderMaster>()
                .in(OrderMaster::getContractNo, context.contractIds())
                .like(StringUtils.hasText(orderId), OrderMaster::getOrderNo, orderId)
                .eq(orderStatus != null, OrderMaster::getOrderStatus, orderStatus)
                .orderByDesc(OrderMaster::getCreatedAt);
        IPage<OrderMaster> pageResult = orderMasterMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        if (pageResult.getRecords().isEmpty()) {
            return success(emptyPage(pageNo, pageSize, pageResult.getTotal()));
        }

        Map<String, List<OrderDetail>> detailGroup = buildDetailGroup(pageResult.getRecords());
        List<Map<String, Object>> records = pageResult.getRecords().stream()
                .map(master -> toOrderRow(master, detailGroup.getOrDefault(master.getOrderNo(), Collections.emptyList()), context))
                .toList();

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
        String contractNo = firstNonBlank(request.getContractNo(), request.getContractId());
        if (!StringUtils.hasText(contractNo)) {
            throw new BizException("contractNo不能为空");
        }
        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new BizException("至少需要一条订单明细");
        }

        ContractMaster contract = contractMasterMapper.selectById(contractNo);
        if (contract == null) {
            throw new BizException("合同不存在");
        }

        OrderMaster existingMaster = orderMasterMapper.selectOne(new LambdaQueryWrapper<OrderMaster>()
                .eq(OrderMaster::getContractNo, contractNo)
                .last("limit 1"));

        String orderNo;
        if (existingMaster == null) {
            // 订单主表号由合同号派生，规则：合同号 + D
            orderNo = contractNo + "D";

            OrderMaster master = new OrderMaster();
            master.setOrderNo(orderNo);
            master.setContractNo(contractNo);
            master.setExpectedDate(request.getExpectedDate());
            master.setOrderStatus(request.getOrderStatus() == null ? 1 : request.getOrderStatus());
            orderMasterMapper.insert(master);
        } else {
            orderNo = existingMaster.getOrderNo();
        }

        Long existedDetailCount = orderDetailMapper.selectCount(new LambdaQueryWrapper<OrderDetail>()
                .eq(OrderDetail::getOrderNo, orderNo));
        int nextIndex = (existedDetailCount == null ? 0 : existedDetailCount.intValue()) + 1;

        for (OrderFullCreateRequest.OrderDetailCreateRequest item : request.getDetails()) {
            if (!StringUtils.hasText(item.getProductModel())) {
                throw new BizException("明细产品型号不能为空");
            }
            BigDecimal reqLength = item.getReqLength() != null ? item.getReqLength() : toDecimal(item.getLengthReq());
            BigDecimal reqWidth = item.getReqWidth() != null ? item.getReqWidth() : toDecimal(item.getWidthReq());
            if (reqLength == null || reqWidth == null) {
                throw new BizException("明细长度和宽度不能为空");
            }

            OrderDetail detail = new OrderDetail();
            detail.setDetailId(resolveDetailId(item.getDetailId(), orderNo, nextIndex++));
            detail.setOrderNo(orderNo);
            detail.setProductModel(item.getProductModel());
            detail.setAirPermeability(item.getAirPermeability() == null ? 0 : item.getAirPermeability());
            detail.setReqLength(reqLength);
            detail.setReqWidth(reqWidth);
            detail.setDetailStatus(item.getDetailStatus() == null ? 0 : item.getDetailStatus());
            detail.setWeavingModeStatus(0);
            detail.setDeliveredQty(item.getDeliveredQty() == null ? 0 : item.getDeliveredQty());
            orderDetailMapper.insert(detail);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", orderNo);
        data.put("orderId", orderNo);
        data.put("contractNo", contractNo);
        return success(data);
    }

    private Map<String, List<OrderDetail>> buildDetailGroup(List<OrderMaster> masters) {
        Set<String> orderNos = masters.stream().map(OrderMaster::getOrderNo).collect(Collectors.toSet());
        List<OrderDetail> details = orderDetailMapper.selectList(new LambdaQueryWrapper<OrderDetail>()
                .in(OrderDetail::getOrderNo, orderNos)
                .orderByAsc(OrderDetail::getCreatedAt));
        return details.stream().collect(Collectors.groupingBy(OrderDetail::getOrderNo));
    }

    private CustomerContractContext buildCustomerContractContext(String customerName) {
        List<Customer> customers = customerMapper.selectList(new LambdaQueryWrapper<Customer>()
                .like(StringUtils.hasText(customerName), Customer::getCustomerName, customerName));
        if (customers.isEmpty()) {
            return new CustomerContractContext(Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet());
        }

        Map<String, String> customerNameMap = customers.stream()
                .collect(Collectors.toMap(Customer::getCustomerId, Customer::getCustomerName, (a, b) -> a));
        Set<String> customerIds = customers.stream().map(Customer::getCustomerId).collect(Collectors.toSet());

        List<ContractMaster> contracts = contractMasterMapper.selectList(new LambdaQueryWrapper<ContractMaster>()
                .in(ContractMaster::getCustomerId, customerIds)
                .orderByDesc(ContractMaster::getUpdatedAt));
        Map<String, ContractMaster> contractMap = contracts.stream()
                .collect(Collectors.toMap(ContractMaster::getContractId, c -> c, (a, b) -> a));
        return new CustomerContractContext(customerNameMap, contractMap, contractMap.keySet());
    }

    private Map<String, Object> toOrderRow(OrderMaster master, List<OrderDetail> details, CustomerContractContext context) {
        ContractMaster contract = context.contractMap().get(master.getContractNo());
        String customerId = contract == null ? null : contract.getCustomerId();
        String customerName = customerId == null ? null : context.customerNameMap().get(customerId);
        Map<String, ProcessProgress> progressMap = resolveProcessProgress(details);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("orderNo", master.getOrderNo());
        row.put("orderId", master.getOrderNo());
        row.put("contractNo", master.getContractNo());
        row.put("contractId", master.getContractNo());
        row.put("customerId", customerId);
        row.put("customerName", customerName);
        row.put("signDate", contract == null ? null : contract.getSignDate());
        row.put("deliveryAddress", contract == null ? null : contract.getDeliveryAddress());
        row.put("totalAmount", contract == null ? null : contract.getContractAmount());
        row.put("expectedDate", master.getExpectedDate());
        row.put("orderStatus", resolveOrderStatus(master.getOrderStatus(), details));
        row.put("details", details.stream().map(detail -> toDetailRow(detail, progressMap.get(detail.getDetailId()))).toList());
        return row;
    }

    private Map<String, Object> toDetailRow(OrderDetail detail, ProcessProgress progress) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("detailId", detail.getDetailId());
        row.put("orderNo", detail.getOrderNo());
        row.put("orderId", detail.getOrderNo());
        row.put("productModel", detail.getProductModel());
        row.put("airPermeability", detail.getAirPermeability());
        row.put("reqLength", detail.getReqLength());
        row.put("reqWidth", detail.getReqWidth());
        row.put("lengthReq", detail.getReqLength());
        row.put("widthReq", detail.getReqWidth());
        row.put("detailStatus", detail.getDetailStatus());
        row.put("weavingModeStatus", detail.getWeavingModeStatus());
        row.put("deliveredQty", detail.getDeliveredQty());
        row.put("currentProcessType", progress == null ? null : progress.processType());
        row.put("currentTaskStatus", progress == null ? null : progress.taskStatus());
        return row;
    }

    private Map<String, ProcessProgress> resolveProcessProgress(List<OrderDetail> details) {
        if (details == null || details.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> detailIds = details.stream().map(OrderDetail::getDetailId).collect(Collectors.toSet());
        String detailPh = placeholders(detailIds.size());
        List<Map<String, Object>> mappingRows = jdbcTemplate.queryForList(
                "SELECT detail_id, weaving_batch_no FROM map_order_weaving WHERE detail_id IN (" + detailPh + ")",
                detailIds.toArray()
        );
        if (mappingRows.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> detailToWeaving = new LinkedHashMap<>();
        Set<String> weavingBatches = new LinkedHashSet<>();
        for (Map<String, Object> row : mappingRows) {
            String detailId = String.valueOf(row.get("detail_id"));
            String weavingBatch = String.valueOf(row.get("weaving_batch_no"));
            detailToWeaving.put(detailId, weavingBatch);
            weavingBatches.add(weavingBatch);
        }

        Map<String, Integer> weavingStatus = queryStatusMap("prd_weaving_process", "weaving_batch_no", weavingBatches);
        Map<String, Map<String, Object>> settingByWeaving = queryLinkAndStatus("prd_setting_process", "weaving_batch_no", "setting_batch_no", weavingBatches);
        Set<String> settingBatches = settingByWeaving.values().stream()
                .map(v -> asString(v.get("next")))
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, Map<String, Object>> cuttingByDetail = queryDetailCutting(detailIds);
        Set<String> cutBatches = cuttingByDetail.values().stream()
                .map(v -> asString(v.get("next")))
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, Map<String, Object>> splicingByCut = queryLinkAndStatus("prd_splicing_process", "cut_batch_no", "splice_batch_no", cutBatches);
        Set<String> spliceBatches = splicingByCut.values().stream()
                .map(v -> asString(v.get("next")))
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Integer> secStatusBySplice = queryStatusMap("prd_sec_setting_process", "splice_batch_no", spliceBatches);

        Map<String, ProcessProgress> result = new LinkedHashMap<>();
        for (OrderDetail detail : details) {
            String detailId = detail.getDetailId();
            String weavingBatch = detailToWeaving.get(detailId);
            if (!StringUtils.hasText(weavingBatch)) {
                continue;
            }
            Integer processType = 1;
            Integer taskStatus = weavingStatus.get(weavingBatch);

            Map<String, Object> setting = settingByWeaving.get(weavingBatch);
            if (setting != null) {
                processType = 2;
                taskStatus = asInteger(setting.get("status"));
            }

            Map<String, Object> cutting = cuttingByDetail.get(detailId);
            if (cutting != null) {
                processType = 3;
                taskStatus = asInteger(cutting.get("status"));
            }

            String cutBatch = cutting == null ? null : asString(cutting.get("next"));
            Map<String, Object> splicing = StringUtils.hasText(cutBatch) ? splicingByCut.get(cutBatch) : null;
            if (splicing != null) {
                processType = 4;
                taskStatus = asInteger(splicing.get("status"));
            }

            String spliceBatch = splicing == null ? null : asString(splicing.get("next"));
            Integer secStatus = StringUtils.hasText(spliceBatch) ? secStatusBySplice.get(spliceBatch) : null;
            if (secStatus != null) {
                processType = 5;
                taskStatus = secStatus;
            }
            result.put(detailId, new ProcessProgress(processType, taskStatus));
        }
        return result;
    }

    private Map<String, Integer> queryStatusMap(String table, String keyColumn, Set<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }
        String ph = placeholders(keys.size());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT " + keyColumn + " AS k, process_status AS s FROM " + table + " WHERE " + keyColumn + " IN (" + ph + ")",
                keys.toArray()
        );
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            map.put(asString(row.get("k")), asInteger(row.get("s")));
        }
        return map;
    }

    private Map<String, Map<String, Object>> queryLinkAndStatus(String table, String keyColumn, String nextColumn, Set<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }
        String ph = placeholders(keys.size());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT " + keyColumn + " AS k, " + nextColumn + " AS n, process_status AS s FROM " + table + " WHERE " + keyColumn + " IN (" + ph + ")",
                keys.toArray()
        );
        Map<String, Map<String, Object>> map = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("next", asString(row.get("n")));
            v.put("status", asInteger(row.get("s")));
            map.put(asString(row.get("k")), v);
        }
        return map;
    }

    private Map<String, Map<String, Object>> queryDetailCutting(Set<String> detailIds) {
        if (detailIds == null || detailIds.isEmpty()) {
            return Collections.emptyMap();
        }
        String ph = placeholders(detailIds.size());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT detail_id AS k, cut_batch_no AS n, process_status AS s FROM prd_cutting_record WHERE detail_id IN (" + ph + ")",
                detailIds.toArray()
        );
        Map<String, Map<String, Object>> map = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("next", asString(row.get("n")));
            v.put("status", asInteger(row.get("s")));
            map.put(asString(row.get("k")), v);
        }
        return map;
    }

    private String placeholders(int size) {
        if (size <= 0) {
            return "";
        }
        return String.join(",", Collections.nCopies(size, "?"));
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private Integer resolveOrderStatus(Integer masterStatus, List<OrderDetail> details) {
        if (details == null || details.isEmpty()) {
            return masterStatus;
        }
        List<Integer> statuses = details.stream().map(OrderDetail::getDetailStatus).filter(v -> v != null).toList();
        if (statuses.isEmpty()) {
            return masterStatus;
        }
        if (statuses.stream().anyMatch(v -> v >= 2)) {
            return 3;
        }
        return 1;
    }

    private String resolveDetailId(String requestDetailId, String orderNo, int startIndex) {
        if (StringUtils.hasText(requestDetailId)) {
            return requestDetailId;
        }
        int index = Math.max(startIndex, 1);
        while (true) {
            String candidate = orderNo + "-" + String.format("%03d", index);
            if (orderDetailMapper.selectById(candidate) == null) {
                return candidate;
            }
            index++;
        }
    }

    private BigDecimal toDecimal(Number value) {
        return value == null ? null : new BigDecimal(String.valueOf(value));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
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

    private record CustomerContractContext(Map<String, String> customerNameMap,
                                           Map<String, ContractMaster> contractMap,
                                           Set<String> contractIds) {
    }

    private record ProcessProgress(Integer processType, Integer taskStatus) {
    }

    public static class OrderFullCreateRequest {
        private String orderNo;
        private String orderId;
        private String contractNo;
        private String contractId;
        private LocalDate expectedDate;
        private Integer orderStatus;
        private List<OrderDetailCreateRequest> details = new ArrayList<>();

        public String getOrderNo() {
            return orderNo;
        }

        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo;
        }

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public String getContractNo() {
            return contractNo;
        }

        public void setContractNo(String contractNo) {
            this.contractNo = contractNo;
        }

        public String getContractId() {
            return contractId;
        }

        public void setContractId(String contractId) {
            this.contractId = contractId;
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
            private BigDecimal reqLength;
            private BigDecimal reqWidth;
            private Number lengthReq;
            private Number widthReq;
            private Integer detailStatus;
            private Integer deliveredQty;

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

            public Integer getAirPermeability() {
                return airPermeability;
            }

            public void setAirPermeability(Integer airPermeability) {
                this.airPermeability = airPermeability;
            }

            public BigDecimal getReqLength() {
                return reqLength;
            }

            public void setReqLength(BigDecimal reqLength) {
                this.reqLength = reqLength;
            }

            public BigDecimal getReqWidth() {
                return reqWidth;
            }

            public void setReqWidth(BigDecimal reqWidth) {
                this.reqWidth = reqWidth;
            }

            public Number getLengthReq() {
                return lengthReq;
            }

            public void setLengthReq(Number lengthReq) {
                this.lengthReq = lengthReq;
            }

            public Number getWidthReq() {
                return widthReq;
            }

            public void setWidthReq(Number widthReq) {
                this.widthReq = widthReq;
            }

            public Integer getDetailStatus() {
                return detailStatus;
            }

            public void setDetailStatus(Integer detailStatus) {
                this.detailStatus = detailStatus;
            }

            public Integer getDeliveredQty() {
                return deliveredQty;
            }

            public void setDeliveredQty(Integer deliveredQty) {
                this.deliveredQty = deliveredQty;
            }
        }
    }
}
