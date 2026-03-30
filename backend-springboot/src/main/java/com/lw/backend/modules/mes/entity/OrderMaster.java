package com.lw.backend.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("order_master")
public class OrderMaster {

    @TableId(value = "order_no", type = IdType.INPUT)
    private String orderNo;

    private String contractNo;

    @TableField(exist = false)
    private String customerId;

    @TableField(exist = false)
    private BigDecimal totalAmount;

    private LocalDate expectedDate;

    private Integer orderStatus;

    @TableField(exist = false)
    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

