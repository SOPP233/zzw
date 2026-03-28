package com.lw.backend.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "process_task", autoResultMap = true)
public class ProcessTask {

    @TableId(value = "task_id", type = IdType.INPUT)
    private String taskId;

    private String batchId;

    private Integer processType;

    private String operatorId;

    private Integer status;

    @TableField(value = "output_data", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> outputData;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

