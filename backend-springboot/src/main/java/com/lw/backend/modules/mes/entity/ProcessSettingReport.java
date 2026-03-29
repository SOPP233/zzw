package com.lw.backend.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("process_setting_report")
public class ProcessSettingReport {

    @TableId(value = "task_id", type = IdType.INPUT)
    private String taskId;

    private String operatorId;

    private String temperatureCurve;

    private Integer shapingDuration;

    private BigDecimal shrinkageRate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
