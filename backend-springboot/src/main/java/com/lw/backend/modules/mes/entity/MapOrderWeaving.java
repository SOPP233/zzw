package com.lw.backend.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("map_order_weaving")
public class MapOrderWeaving {

    @TableId(value = "map_id", type = IdType.AUTO)
    private Long mapId;

    private String detailId;

    private String weavingBatchNo;

    private Integer mapQty;

    private LocalDateTime createdAt;
}
