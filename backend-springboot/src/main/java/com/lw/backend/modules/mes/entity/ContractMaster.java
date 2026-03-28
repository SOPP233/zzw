package com.lw.backend.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("contract_master")
public class ContractMaster {

    @TableId(value = "contract_id", type = IdType.INPUT)
    private String contractId;

    private String customerId;

    private BigDecimal contractAmount;

    private LocalDate signDate;

    private String deliveryAddress;

    private Integer contractStatus;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
