package com.lw.backend.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("order_master")
public class OrderMaster {

    @TableId(value = "order_id", type = IdType.INPUT)
    private String orderId;

    private String contractId;

    private String customerId;

    private BigDecimal totalAmount;

    private LocalDate expectedDate;

    private Integer orderStatus;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

