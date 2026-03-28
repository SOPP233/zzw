package com.lw.backend.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId(value = "menu_id", type = IdType.INPUT)
    private String menuId;

    private String parentId;

    private String menuName;

    private Integer menuType;

    private String path;

    private String component;

    private String permissionCode;

    private Integer sortNo;

    private Integer visible;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

