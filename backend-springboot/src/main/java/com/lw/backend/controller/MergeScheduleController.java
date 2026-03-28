package com.lw.backend.controller;

import com.lw.backend.modules.mes.dto.MergeScheduleRequest;
import com.lw.backend.modules.mes.service.MergeScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class MergeScheduleController {

    private final MergeScheduleService mergeScheduleService;

    /**
     * 合并排产：
     * 1) 校验明细型号一致
     * 2) 创建生产批次
     * 3) 更新明细状态并建立批次映射
     * 4) 激活首道织造工序任务
     */
    @PostMapping("/merge")
    public Map<String, Object> merge(@RequestBody MergeScheduleRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", mergeScheduleService.mergeSchedule(request));
        return result;
    }
}

