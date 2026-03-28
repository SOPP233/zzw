package com.lw.backend.modules.mes.service;

import com.lw.backend.modules.mes.dto.TaskCompleteRequest;

import java.util.Map;

public interface TaskReportService {

    Map<String, Object> completeTask(String taskId, TaskCompleteRequest request);
}

