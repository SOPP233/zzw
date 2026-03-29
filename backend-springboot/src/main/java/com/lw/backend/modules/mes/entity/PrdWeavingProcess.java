package com.lw.backend.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("prd_weaving_process")
public class PrdWeavingProcess {

    @TableId(value = "weaving_batch_no", type = IdType.INPUT)
    private String weavingBatchNo;

    private String machineId;

    private String operatorId;

    private BigDecimal actualLength;

    private BigDecimal actualWidth;

    private Integer processStatus;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
