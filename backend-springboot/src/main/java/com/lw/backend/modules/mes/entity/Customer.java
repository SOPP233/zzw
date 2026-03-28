package com.lw.backend.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer")
public class Customer {

    @TableId(value = "customer_id", type = IdType.INPUT)
    private String customerId;

    private String customerName;

    private String contactName;

    private String contactPhone;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

