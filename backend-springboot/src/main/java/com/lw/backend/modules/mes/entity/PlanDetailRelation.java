package com.lw.backend.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("plan_detail_relation")
public class PlanDetailRelation {

    @TableId(value = "relation_id", type = IdType.AUTO)
    private Long relationId;

    private String batchId;

    private String detailId;

    private BigDecimal allocatedQty;

    private LocalDateTime createdAt;
}

