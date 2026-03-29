package com.lw.backend.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("process_weaving_report")
public class ProcessWeavingReport {

    @TableId(value = "task_id", type = IdType.INPUT)
    private String taskId;

    private String operatorId;

    private String warpWeftDensity;

    private String tensionParam;

    private String machineNo;

    private String materialBatchNo;

    private BigDecimal actualOutputMeters;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
