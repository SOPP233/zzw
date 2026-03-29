package com.lw.backend.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("process_jointing_report")
public class ProcessJointingReport {

    @TableId(value = "task_id", type = IdType.INPUT)
    private String taskId;

    private String operatorId;

    private String jointType;

    private String jointStrengthFlag;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
