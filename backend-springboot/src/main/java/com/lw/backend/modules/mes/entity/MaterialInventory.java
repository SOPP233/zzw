package com.lw.backend.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("material_inventory")
public class MaterialInventory {

    @TableId(value = "material_id", type = IdType.INPUT)
    private String materialId;

    private String materialType;

    private BigDecimal currentStock;

    private BigDecimal frozenStock;

    private Integer minStock;

    private Integer versionNo;

    private String updatedBy;

    private LocalDateTime updatedAt;

    private LocalDateTime createdAt;
}

