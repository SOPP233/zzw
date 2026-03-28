package com.lw.backend.modules.mes.dto;

import lombok.Data;

import java.util.Map;

@Data
public class TaskCompleteRequest {

    /**
     * 操作员工号
     */
    private String operatorId;

    /**
     * 报工业务数据（JSON）
     */
    private Map<String, Object> outputData;
}

