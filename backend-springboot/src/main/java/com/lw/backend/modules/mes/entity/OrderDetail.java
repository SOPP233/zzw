package com.lw.backend.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_detail")
public class OrderDetail {

    @TableId(value = "detail_id", type = IdType.INPUT)
    private String detailId;

    private String orderNo;

    private String productModel;

    private Integer airPermeability;

    private BigDecimal reqLength;

    private BigDecimal reqWidth;

    private Integer detailStatus;

    private Integer weavingModeStatus;

    private Integer deliveredQty;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
