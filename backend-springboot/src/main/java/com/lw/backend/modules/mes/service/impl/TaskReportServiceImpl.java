package com.lw.backend.modules.mes.service.impl;

import com.lw.backend.modules.mes.dto.TaskCompleteRequest;
import com.lw.backend.modules.mes.entity.ProcessCuttingReport;
import com.lw.backend.modules.mes.entity.ProcessJointingReport;
import com.lw.backend.modules.mes.entity.ProcessReshapingReport;
import com.lw.backend.modules.mes.entity.ProcessSettingReport;
import com.lw.backend.modules.mes.entity.ProcessTask;
import com.lw.backend.modules.mes.entity.ProcessWeavingReport;
import com.lw.backend.modules.mes.exception.BizException;
import com.lw.backend.modules.mes.mapper.ProcessCuttingReportMapper;
import com.lw.backend.modules.mes.mapper.ProcessJointingReportMapper;
import com.lw.backend.modules.mes.mapper.ProcessReshapingReportMapper;
import com.lw.backend.modules.mes.mapper.ProcessSettingReportMapper;
import com.lw.backend.modules.mes.mapper.ProcessTaskMapper;
import com.lw.backend.modules.mes.mapper.ProcessWeavingReportMapper;
import com.lw.backend.modules.mes.service.TaskReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class TaskReportServiceImpl implements TaskReportService {

    private static final int TASK_STATUS_PENDING = 0;
    private static final int TASK_STATUS_COMPLETED = 3;
    private static final int LAST_PROCESS_TYPE = 5;

    private final ProcessTaskMapper processTaskMapper;
    private final ProcessWeavingReportMapper processWeavingReportMapper;
    private final ProcessSettingReportMapper processSettingReportMapper;
    private final ProcessCuttingReportMapper processCuttingReportMapper;
    private final ProcessJointingReportMapper processJointingReportMapper;
    private final ProcessReshapingReportMapper processReshapingReportMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> completeTask(String taskId, TaskCompleteRequest request) {
        validate(taskId, request);

        ProcessTask current = processTaskMapper.selectById(taskId);
        if (current == null) {
            throw new BizException("任务不存在: " + taskId);
        }

        current.setOperatorId(request.getOperatorId());
        // 工序参数主存储改为五张工序报工表，process_task 仅保留任务流转信息
        current.setOutputData(null);
        current.setEndTime(LocalDateTime.now());
        current.setStatus(TASK_STATUS_COMPLETED);
        int updated = processTaskMapper.updateById(current);
        if (updated != 1) {
            throw new BizException("更新当前工序任务失败: " + taskId);
        }

        persistProcessReport(current.getProcessType(), taskId, request);

        String nextTaskId = null;
        if (current.getProcessType() < LAST_PROCESS_TYPE) {
            int nextProcessType = current.getProcessType() + 1;
            ProcessTask nextTask = new ProcessTask();
            nextTaskId = generateTaskId();
            nextTask.setTaskId(nextTaskId);
            nextTask.setBatchId(current.getBatchId());
            nextTask.setProcessType(nextProcessType);
            nextTask.setStatus(TASK_STATUS_PENDING);
            int inserted = processTaskMapper.insert(nextTask);
            if (inserted != 1) {
                throw new BizException("激活下一工序失败，batchId=" + current.getBatchId());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", taskId);
        result.put("status", TASK_STATUS_COMPLETED);
        result.put("batchId", current.getBatchId());
        result.put("nextTaskId", nextTaskId);
        result.put("nextProcessType", current.getProcessType() < LAST_PROCESS_TYPE ? current.getProcessType() + 1 : null);
        return result;
    }

    private void validate(String taskId, TaskCompleteRequest request) {
        if (!StringUtils.hasText(taskId)) {
            throw new BizException("taskId 不能为空");
        }
        if (request == null) {
            throw new BizException("请求体不能为空");
        }
        if (!StringUtils.hasText(request.getOperatorId())) {
            throw new BizException("operatorId 不能为空");
        }
    }

    private String generateTaskId() {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(100, 1000);
        return "TASK" + timePart + random;
    }

    private void persistProcessReport(Integer processType, String taskId, TaskCompleteRequest request) {
        Map<String, Object> output = request.getOutputData() == null ? Map.of() : request.getOutputData();
        if (processType == null) {
            return;
        }
        if (processType == 1) {
            ProcessWeavingReport report = new ProcessWeavingReport();
            report.setTaskId(taskId);
            report.setOperatorId(request.getOperatorId());
            report.setWarpWeftDensity(asString(output.get("warpWeftDensity")));
            report.setTensionParam(asString(output.get("tensionParam")));
            report.setMachineNo(asString(output.get("machineNo")));
            report.setMaterialBatchNo(asString(output.get("materialBatchNo")));
            report.setActualOutputMeters(asDecimal(output.get("actualOutputMeters")));
            processWeavingReportMapper.insert(report);
            return;
        }
        if (processType == 2) {
            ProcessSettingReport report = new ProcessSettingReport();
            report.setTaskId(taskId);
            report.setOperatorId(request.getOperatorId());
            report.setTemperatureCurve(asString(output.get("temperatureCurve")));
            report.setShapingDuration(asInteger(output.get("shapingDuration")));
            report.setShrinkageRate(asDecimal(output.get("shrinkageRate")));
            processSettingReportMapper.insert(report);
            return;
        }
        if (processType == 3) {
            ProcessCuttingReport report = new ProcessCuttingReport();
            report.setTaskId(taskId);
            report.setOperatorId(request.getOperatorId());
            report.setCutLength(asDecimal(output.get("cutLength")));
            report.setCutWidth(asDecimal(output.get("cutWidth")));
            report.setLossArea(asDecimal(output.get("lossArea")));
            processCuttingReportMapper.insert(report);
            return;
        }
        if (processType == 4) {
            ProcessJointingReport report = new ProcessJointingReport();
            report.setTaskId(taskId);
            report.setOperatorId(request.getOperatorId());
            report.setJointType(asString(output.get("jointType")));
            report.setJointStrengthFlag(asString(output.get("jointStrengthFlag")));
            processJointingReportMapper.insert(report);
            return;
        }
        if (processType == 5) {
            ProcessReshapingReport report = new ProcessReshapingReport();
            report.setTaskId(taskId);
            report.setOperatorId(request.getOperatorId());
            report.setFinalSize(asString(output.get("finalSize")));
            report.setDefectParam(asString(output.get("defectParam")));
            processReshapingReportMapper.insert(report);
        }
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
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private BigDecimal asDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }
}
