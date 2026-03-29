package com.lw.backend.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("process_cutting_report")
public class ProcessCuttingReport {

    @TableId(value = "task_id", type = IdType.INPUT)
    private String taskId;

    private String operatorId;

    private BigDecimal cutLength;

    private BigDecimal cutWidth;

    private BigDecimal lossArea;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
