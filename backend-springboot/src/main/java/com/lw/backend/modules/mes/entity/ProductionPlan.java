package com.lw.backend.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("production_plan")
public class ProductionPlan {

    @TableId(value = "batch_id", type = IdType.INPUT)
    private String batchId;

    private String machineId;

    private LocalDate planStartDate;

    private LocalDateTime actualStartTime;

    private Integer planStatus;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

