package com.lw.backend.modules.mes.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lw.backend.modules.mes.dto.MergeScheduleRequest;
import com.lw.backend.modules.mes.entity.OrderDetail;
import com.lw.backend.modules.mes.entity.PlanDetailRelation;
import com.lw.backend.modules.mes.entity.ProcessTask;
import com.lw.backend.modules.mes.entity.ProductionPlan;
import com.lw.backend.modules.mes.exception.BizException;
import com.lw.backend.modules.mes.mapper.OrderDetailMapper;
import com.lw.backend.modules.mes.mapper.PlanDetailRelationMapper;
import com.lw.backend.modules.mes.mapper.ProcessTaskMapper;
import com.lw.backend.modules.mes.mapper.ProductionPlanMapper;
import com.lw.backend.modules.mes.service.MergeScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MergeScheduleServiceImpl implements MergeScheduleService {

    /**
     * 约定状态：3=生产中（与 order_master 中“生产中”状态保持一致）
     */
    private static final int DETAIL_STATUS_IN_PRODUCTION = 3;

    /**
     * process_task 状态：0=待接收
     */
    private static final int PROCESS_TASK_STATUS_PENDING = 0;

    /**
     * process_type：1=织造（首道工序）
     */
    private static final int PROCESS_TYPE_WEAVING = 1;

    /**
     * production_plan 状态：1=加工中（排产成功即投入生产）
     */
    private static final int PLAN_STATUS_PROCESSING = 1;

    private final OrderDetailMapper orderDetailMapper;
    private final ProductionPlanMapper productionPlanMapper;
    private final PlanDetailRelationMapper planDetailRelationMapper;
    private final ProcessTaskMapper processTaskMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> mergeSchedule(MergeScheduleRequest request) {
        validateRequest(request);

        List<OrderDetail> details = fetchAndValidateDetails(request.getDetailIds());
        validateSameProductModel(details);

        String batchId = createProductionPlan(request.getMachineId());
        bindDetailsToBatchAndUpdateStatus(batchId, details);
        String taskId = createFirstProcessTask(batchId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batchId", batchId);
        result.put("taskId", taskId);
        result.put("detailCount", details.size());
        result.put("machineId", request.getMachineId());
        result.put("detailIds", details.stream().map(OrderDetail::getDetailId).toList());
        result.put("productModel", details.get(0).getProductModel());
        return result;
    }

    private void validateRequest(MergeScheduleRequest request) {
        if (request == null || request.getDetailIds() == null || request.getDetailIds().isEmpty()) {
            throw new BizException("请选择至少一条订单明细进行排产");
        }
        if (!StringUtils.hasText(request.getMachineId())) {
            throw new BizException("machineId 不能为空");
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
        return details;
    }

    private void validateSameProductModel(List<OrderDetail> details) {
        Set<String> models = details.stream().map(OrderDetail::getProductModel).collect(Collectors.toSet());
        if (models.size() != 1) {
            throw new BizException("所选明细型号不一致，无法合批");
        }
    }

    private String createProductionPlan(String machineId) {
        String batchId = generateBatchId();
        ProductionPlan plan = new ProductionPlan();
        plan.setBatchId(batchId);
        plan.setMachineId(machineId);
        plan.setPlanStartDate(LocalDate.now());
        plan.setPlanStatus(PLAN_STATUS_PROCESSING);
        int inserted = productionPlanMapper.insert(plan);
        if (inserted != 1) {
            throw new BizException("创建生产批次失败");
        }
        return batchId;
    }

    private void bindDetailsToBatchAndUpdateStatus(String batchId, List<OrderDetail> details) {
        for (OrderDetail detail : details) {
            // 建立批次与明细的映射关系，支持后续追溯与拆批分析
            PlanDetailRelation relation = new PlanDetailRelation();
            relation.setBatchId(batchId);
            relation.setDetailId(detail.getDetailId());
            int relationInserted = planDetailRelationMapper.insert(relation);
            if (relationInserted != 1) {
                throw new BizException("写入批次明细关联失败，detailId=" + detail.getDetailId());
            }

            // 明细状态置为“生产中”
            OrderDetail update = new OrderDetail();
            update.setDetailId(detail.getDetailId());
            update.setDetailStatus(DETAIL_STATUS_IN_PRODUCTION);
            int updated = orderDetailMapper.updateById(update);
            if (updated != 1) {
                throw new BizException("更新明细状态失败，detailId=" + detail.getDetailId());
            }
        }
    }

    private String createFirstProcessTask(String batchId) {
        String taskId = generateTaskId();
        ProcessTask task = new ProcessTask();
        task.setTaskId(taskId);
        task.setBatchId(batchId);
        task.setProcessType(PROCESS_TYPE_WEAVING);
        task.setStatus(PROCESS_TASK_STATUS_PENDING);
        int inserted = processTaskMapper.insert(task);
        if (inserted != 1) {
            throw new BizException("创建首道工序任务失败");
        }
        return taskId;
    }

    private String generateBatchId() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        int random = ThreadLocalRandom.current().nextInt(100, 1000);
        return "BAT" + datePart + random;
    }

    private String generateTaskId() {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(100, 1000);
        return "TASK" + timePart + random;
    }
}

