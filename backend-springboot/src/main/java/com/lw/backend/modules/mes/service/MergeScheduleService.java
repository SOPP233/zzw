package com.lw.backend.modules.mes.service;

import com.lw.backend.modules.mes.dto.MergeScheduleRequest;

import java.util.Map;

public interface MergeScheduleService {

    Map<String, Object> mergeSchedule(MergeScheduleRequest request);
}

