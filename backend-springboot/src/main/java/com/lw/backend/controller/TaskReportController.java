package com.lw.backend.controller;

import com.lw.backend.modules.mes.dto.TaskCompleteRequest;
import com.lw.backend.modules.mes.service.TaskReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskReportController {

    private final TaskReportService taskReportService;

    /**
     * 工序报工并完成当前任务，同时按标准工艺链路激活下一工序。
     */
    @PostMapping("/{taskId}/complete")
    public Map<String, Object> complete(@PathVariable String taskId, @RequestBody TaskCompleteRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", taskReportService.completeTask(taskId, request));
        return result;
    }
}

