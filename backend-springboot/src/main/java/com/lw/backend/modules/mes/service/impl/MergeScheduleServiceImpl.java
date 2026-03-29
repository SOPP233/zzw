package com.lw.backend.modules.mes.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lw.backend.modules.mes.dto.MergeScheduleRequest;
import com.lw.backend.modules.mes.entity.MapOrderWeaving;
import com.lw.backend.modules.mes.entity.OrderDetail;
import com.lw.backend.modules.mes.entity.OrderMaster;
import com.lw.backend.modules.mes.entity.PrdWeavingProcess;
import com.lw.backend.modules.mes.exception.BizException;
import com.lw.backend.modules.mes.mapper.MapOrderWeavingMapper;
import com.lw.backend.modules.mes.mapper.OrderDetailMapper;
import com.lw.backend.modules.mes.mapper.OrderMasterMapper;
import com.lw.backend.modules.mes.mapper.PrdWeavingProcessMapper;
import com.lw.backend.modules.mes.service.MergeScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MergeScheduleServiceImpl implements MergeScheduleService {

    private static final int DETAIL_STATUS_WAIT_MERGE = 1;
    private static final int DETAIL_STATUS_MERGED = 2;
    private static final int ORDER_STATUS_WAIT_SCHEDULE = 2;
    private static final int WEAVING_STATUS_PENDING = 1;

    private final OrderDetailMapper orderDetailMapper;
    private final OrderMasterMapper orderMasterMapper;
    private final PrdWeavingProcessMapper prdWeavingProcessMapper;
    private final MapOrderWeavingMapper mapOrderWeavingMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> mergeSchedule(MergeScheduleRequest request) {
        validateRequest(request);

        List<OrderDetail> details = fetchAndValidateDetails(request.getDetailIds());
        validateMergeEligibility(details);

        String weavingBatchNo = resolveWeavingBatchNo(request.getWeavingBatchNo());
        createWeavingProcess(request, weavingBatchNo);
        bindDetailsAndUpdateStatus(details, weavingBatchNo);
        updateOrderMasterStatus(details);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("weavingBatchNo", weavingBatchNo);
        result.put("detailCount", details.size());
        result.put("machineId", request.getMachineId());
        result.put("weavingLength", request.getWeavingLength());
        result.put("weavingWidth", request.getWeavingWidth());
        result.put("productModel", details.get(0).getProductModel());
        result.put("airPermeability", details.get(0).getAirPermeability());
        result.put("detailIds", details.stream().map(OrderDetail::getDetailId).toList());
        return result;
    }

    private void validateRequest(MergeScheduleRequest request) {
        if (request == null || request.getDetailIds() == null || request.getDetailIds().isEmpty()) {
            throw new BizException("请选择至少一条明细进行排产");
        }
        if (!StringUtils.hasText(request.getMachineId())) {
            throw new BizException("请选择机台");
        }
        if (request.getWeavingLength() == null || request.getWeavingLength().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("织造长度必须大于0");
        }
        if (request.getWeavingWidth() == null || request.getWeavingWidth().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("织造宽度必须大于0");
        }
    }

    private List<OrderDetail> fetchAndValidateDetails(List<String> detailIds) {
        List<OrderDetail> details = orderDetailMapper.selectList(
                new LambdaQueryWrapper<OrderDetail>().in(OrderDetail::getDetailId, detailIds)
        );
        if (details.size() != detailIds.size()) {
            Set<String> foundIds = details.stream().map(OrderDetail::getDetailId).collect(Collectors.toSet());
            List<String> missingIds = detailIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new BizException("存在无效明细ID: " + String.join(",", missingIds));
        }
        boolean hasInvalidStatus = details.stream().anyMatch(d -> d.getDetailStatus() == null || d.getDetailStatus() != DETAIL_STATUS_WAIT_MERGE);
        if (hasInvalidStatus) {
            throw new BizException("仅允许对待合批明细执行排产");
        }
        return details;
    }

    private void validateMergeEligibility(List<OrderDetail> details) {
        Set<String> models = details.stream().map(OrderDetail::getProductModel).collect(Collectors.toSet());
        if (models.size() != 1) {
            throw new BizException("所选明细型号不一致，无法合并排产");
        }

        Set<Integer> permeabilities = details.stream().map(OrderDetail::getAirPermeability).collect(Collectors.toSet());
        if (permeabilities.contains(null)) {
            throw new BizException("存在未维护透气量的明细，无法合并排产");
        }
        if (permeabilities.size() != 1) {
            throw new BizException("所选明细透气量不一致，无法合并排产");
        }
    }

    private String resolveWeavingBatchNo(String requestNo) {
        String batchNo = StringUtils.hasText(requestNo) ? requestNo.trim() : generateWeavingBatchNo();
        if (prdWeavingProcessMapper.selectById(batchNo) != null) {
            throw new BizException("织造单号已存在: " + batchNo);
        }
        return batchNo;
    }

    private void createWeavingProcess(MergeScheduleRequest request, String weavingBatchNo) {
        PrdWeavingProcess process = new PrdWeavingProcess();
        process.setWeavingBatchNo(weavingBatchNo);
        process.setMachineId(request.getMachineId());
        process.setActualLength(request.getWeavingLength());
        process.setActualWidth(request.getWeavingWidth());
        process.setProcessStatus(WEAVING_STATUS_PENDING);
        int inserted = prdWeavingProcessMapper.insert(process);
        if (inserted != 1) {
            throw new BizException("创建织造单失败");
        }
    }

    private void bindDetailsAndUpdateStatus(List<OrderDetail> details, String weavingBatchNo) {
        for (OrderDetail detail : details) {
            MapOrderWeaving map = new MapOrderWeaving();
            map.setDetailId(detail.getDetailId());
            map.setWeavingBatchNo(weavingBatchNo);
            map.setMapQty(1);
            int mapInserted = mapOrderWeavingMapper.insert(map);
            if (mapInserted != 1) {
                throw new BizException("写入合批映射失败，detailId=" + detail.getDetailId());
            }

            log.info("准备更新明细状态");
            int rows = orderDetailMapper.update(
                    null,
                    new LambdaUpdateWrapper<OrderDetail>()
                            .eq(OrderDetail::getDetailId, detail.getDetailId())
                            .set(OrderDetail::getDetailStatus, DETAIL_STATUS_MERGED)
            );
            log.info("明细状态更新完成，影响行数: {}", rows);
            if (rows != 1) {
                throw new BizException("更新明细状态失败，detailId=" + detail.getDetailId());
            }
        }
    }

    private void updateOrderMasterStatus(List<OrderDetail> details) {
        Set<String> orderNos = details.stream().map(OrderDetail::getOrderNo).collect(Collectors.toSet());
        if (orderNos.isEmpty()) {
            return;
        }
        orderMasterMapper.update(
                null,
                new LambdaUpdateWrapper<OrderMaster>()
                        .in(OrderMaster::getOrderNo, orderNos)
                        .set(OrderMaster::getOrderStatus, ORDER_STATUS_WAIT_SCHEDULE)
        );
    }

    private String generateWeavingBatchNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "WV" + datePart + random;
    }
}