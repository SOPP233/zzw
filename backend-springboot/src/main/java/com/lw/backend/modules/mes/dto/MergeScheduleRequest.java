package com.lw.backend.modules.mes.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MergeScheduleRequest {

    private List<String> detailIds;

    private String machineId;

    private String weavingBatchNo;

    private BigDecimal weavingLength;

    private BigDecimal weavingWidth;
}