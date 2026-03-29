package com.lw.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lw.backend.modules.mes.entity.ProcessCuttingReport;
import com.lw.backend.modules.mes.entity.ProcessJointingReport;
import com.lw.backend.modules.mes.entity.ProcessReshapingReport;
import com.lw.backend.modules.mes.entity.ProcessSettingReport;
import com.lw.backend.modules.mes.entity.ProcessTask;
import com.lw.backend.modules.mes.entity.ProcessWeavingReport;
import com.lw.backend.modules.mes.mapper.ProcessCuttingReportMapper;
import com.lw.backend.modules.mes.mapper.ProcessJointingReportMapper;
import com.lw.backend.modules.mes.mapper.ProcessReshapingReportMapper;
import com.lw.backend.modules.mes.mapper.ProcessSettingReportMapper;
import com.lw.backend.modules.mes.mapper.ProcessTaskMapper;
import com.lw.backend.modules.mes.mapper.ProcessWeavingReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/process-tasks")
@RequiredArgsConstructor
public class ProcessTaskController {

    private final ProcessTaskMapper mapper;
    private final ProcessWeavingReportMapper processWeavingReportMapper;
    private final ProcessSettingReportMapper processSettingReportMapper;
    private final ProcessCuttingReportMapper processCuttingReportMapper;
    private final ProcessJointingReportMapper processJointingReportMapper;
    private final ProcessReshapingReportMapper processReshapingReportMapper;

    @PostMapping
    public Map<String, Object> create(@RequestBody ProcessTask data) {
        mapper.insert(data);
        return successWithData(data);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable String id, @RequestBody ProcessTask data) {
        data.setTaskId(id);
        return affected(mapper.updateById(data));
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable String id) {
        return successWithData(mapper.selectById(id));
    }

    @GetMapping("/{id}/report")
    public Map<String, Object> report(@PathVariable String id) {
        ProcessTask task = mapper.selectById(id);
        if (task == null) {
            return successWithData(null);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskId", task.getTaskId());
        data.put("processType", task.getProcessType());
        data.put("report", fetchProcessReport(task.getProcessType(), task.getTaskId()));
        return successWithData(data);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        return affected(mapper.deleteById(id));
    }

    @GetMapping
    public Map<String, Object> page(@RequestParam(defaultValue = "1") long pageNo,
                                    @RequestParam(defaultValue = "10") long pageSize) {
        IPage<ProcessTask> result = mapper.selectPage(new Page<>(pageNo, pageSize), new QueryWrapper<ProcessTask>().orderByDesc("created_at"));
        return successWithData(result);
    }

    private Map<String, Object> successWithData(Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", data);
        return result;
    }

    private Object fetchProcessReport(Integer processType, String taskId) {
        if (processType == null || taskId == null) {
            return null;
        }
        if (processType == 1) {
            ProcessWeavingReport report = processWeavingReportMapper.selectById(taskId);
            if (report == null) {
                report = backfillWeavingReport(taskId);
            }
            return report;
        }
        if (processType == 2) {
            ProcessSettingReport report = processSettingReportMapper.selectById(taskId);
            if (report == null) {
                report = backfillSettingReport(taskId);
            }
            return report;
        }
        if (processType == 3) {
            ProcessCuttingReport report = processCuttingReportMapper.selectById(taskId);
            if (report == null) {
                report = backfillCuttingReport(taskId);
            }
            return report;
        }
        if (processType == 4) {
            ProcessJointingReport report = processJointingReportMapper.selectById(taskId);
            if (report == null) {
                report = backfillJointingReport(taskId);
            }
            return report;
        }
        if (processType == 5) {
            ProcessReshapingReport report = processReshapingReportMapper.selectById(taskId);
            if (report == null) {
                report = backfillReshapingReport(taskId);
            }
            return report;
        }
        return null;
    }

    private ProcessWeavingReport backfillWeavingReport(String taskId) {
        ProcessTask task = mapper.selectById(taskId);
        if (task == null || task.getOutputData() == null || task.getOutputData().isEmpty()) {
            return null;
        }
        Map<String, Object> output = task.getOutputData();
        ProcessWeavingReport report = new ProcessWeavingReport();
        report.setTaskId(taskId);
        report.setOperatorId(task.getOperatorId());
        report.setWarpWeftDensity(asString(output.get("warpWeftDensity")));
        report.setTensionParam(asString(output.get("tensionParam")));
        report.setMachineNo(asString(output.get("machineNo")));
        report.setMaterialBatchNo(asString(output.get("materialBatchNo")));
        report.setActualOutputMeters(asDecimal(output.get("actualOutputMeters")));
        processWeavingReportMapper.insert(report);
        clearLegacyOutputData(task);
        return processWeavingReportMapper.selectById(taskId);
    }

    private ProcessSettingReport backfillSettingReport(String taskId) {
        ProcessTask task = mapper.selectById(taskId);
        if (task == null || task.getOutputData() == null || task.getOutputData().isEmpty()) {
            return null;
        }
        Map<String, Object> output = task.getOutputData();
        ProcessSettingReport report = new ProcessSettingReport();
        report.setTaskId(taskId);
        report.setOperatorId(task.getOperatorId());
        report.setTemperatureCurve(asString(output.get("temperatureCurve")));
        report.setShapingDuration(asInteger(output.get("shapingDuration")));
        report.setShrinkageRate(asDecimal(output.get("shrinkageRate")));
        processSettingReportMapper.insert(report);
        clearLegacyOutputData(task);
        return processSettingReportMapper.selectById(taskId);
    }

    private ProcessCuttingReport backfillCuttingReport(String taskId) {
        ProcessTask task = mapper.selectById(taskId);
        if (task == null || task.getOutputData() == null || task.getOutputData().isEmpty()) {
            return null;
        }
        Map<String, Object> output = task.getOutputData();
        ProcessCuttingReport report = new ProcessCuttingReport();
        report.setTaskId(taskId);
        report.setOperatorId(task.getOperatorId());
        report.setCutLength(asDecimal(output.get("cutLength")));
        report.setCutWidth(asDecimal(output.get("cutWidth")));
        report.setLossArea(asDecimal(output.get("lossArea")));
        processCuttingReportMapper.insert(report);
        clearLegacyOutputData(task);
        return processCuttingReportMapper.selectById(taskId);
    }

    private ProcessJointingReport backfillJointingReport(String taskId) {
        ProcessTask task = mapper.selectById(taskId);
        if (task == null || task.getOutputData() == null || task.getOutputData().isEmpty()) {
            return null;
        }
        Map<String, Object> output = task.getOutputData();
        ProcessJointingReport report = new ProcessJointingReport();
        report.setTaskId(taskId);
        report.setOperatorId(task.getOperatorId());
        report.setJointType(asString(output.get("jointType")));
        report.setJointStrengthFlag(asString(output.get("jointStrengthFlag")));
        processJointingReportMapper.insert(report);
        clearLegacyOutputData(task);
        return processJointingReportMapper.selectById(taskId);
    }

    private ProcessReshapingReport backfillReshapingReport(String taskId) {
        ProcessTask task = mapper.selectById(taskId);
        if (task == null || task.getOutputData() == null || task.getOutputData().isEmpty()) {
            return null;
        }
        Map<String, Object> output = task.getOutputData();
        ProcessReshapingReport report = new ProcessReshapingReport();
        report.setTaskId(taskId);
        report.setOperatorId(task.getOperatorId());
        report.setFinalSize(asString(output.get("finalSize")));
        report.setDefectParam(asString(output.get("defectParam")));
        processReshapingReportMapper.insert(report);
        clearLegacyOutputData(task);
        return processReshapingReportMapper.selectById(taskId);
    }

    private void clearLegacyOutputData(ProcessTask task) {
        task.setOutputData(null);
        mapper.updateById(task);
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

    private Map<String, Object> affected(int affected) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", affected > 0);
        result.put("affected", affected);
        return result;
    }
}
