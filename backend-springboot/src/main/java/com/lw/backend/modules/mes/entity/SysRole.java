package com.lw.backend.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_role")
public class SysRole {

    @TableId(value = "role_code", type = IdType.INPUT)
    private String roleCode;

    private String roleName;

    private String roleDesc;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

