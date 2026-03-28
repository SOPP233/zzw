package com.lw.backend.controller;

import com.lw.backend.modules.mes.exception.BizException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Map<String, Object> handleBizException(BizException e) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("message", e.getMessage());
        return result;
    }
}

