package com.lw.backend.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_detail")
public class OrderDetail {

    @TableId(value = "detail_id", type = IdType.INPUT)
    private String detailId;

    private String orderId;

    private String productModel;

    private Integer airPermeability;

    private Integer lengthReq;

    private Integer widthReq;

    private String craftReq;

    private Integer detailStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
