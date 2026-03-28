package com.lw.backend.modules.mes.service.impl;

import com.lw.backend.modules.mes.dto.TaskCompleteRequest;
import com.lw.backend.modules.mes.entity.ProcessTask;
import com.lw.backend.modules.mes.exception.BizException;
import com.lw.backend.modules.mes.mapper.ProcessTaskMapper;
import com.lw.backend.modules.mes.service.TaskReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class TaskReportServiceImpl implements TaskReportService {

    /**
     * 工序待接收
     */
    private static final int TASK_STATUS_PENDING = 0;

    /**
     * 工序已完成（此处用 3=已闭环）
     */
    private static final int TASK_STATUS_COMPLETED = 3;

    /**
     * 标准工艺路线最后一道工序
     */
    private static final int LAST_PROCESS_TYPE = 5;

    private final ProcessTaskMapper processTaskMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> completeTask(String taskId, TaskCompleteRequest request) {
        validate(taskId, request);

        ProcessTask current = processTaskMapper.selectById(taskId);
        if (current == null) {
            throw new BizException("任务不存在: " + taskId);
        }

        current.setOperatorId(request.getOperatorId());
        current.setOutputData(request.getOutputData());
        current.setEndTime(LocalDateTime.now());
        current.setStatus(TASK_STATUS_COMPLETED);
        int updated = processTaskMapper.updateById(current);
        if (updated != 1) {
            throw new BizException("更新当前工序任务失败: " + taskId);
        }

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
}

