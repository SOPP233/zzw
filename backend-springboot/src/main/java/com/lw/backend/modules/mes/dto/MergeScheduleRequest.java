package com.lw.backend.modules.mes.dto;

import lombok.Data;

import java.util.List;

@Data
public class MergeScheduleRequest {

    /**
     * 被勾选的订单明细ID列表
     */
    private List<String> detailIds;

    /**
     * 分配机器号
     */
    private String machineId;
}

