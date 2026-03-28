package com.lw.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lw.backend.modules.mes.entity.SysUserRole;
import com.lw.backend.modules.mes.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sys-user-roles")
@RequiredArgsConstructor
public class SysUserRoleController {

    private final SysUserRoleMapper mapper;

    @PostMapping
    public Map<String, Object> create(@RequestBody SysUserRole data) {
        mapper.insert(data);
        return successWithData(data);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody SysUserRole data) {
        data.setId(id);
        return affected(mapper.updateById(data));
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        return successWithData(mapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        return affected(mapper.deleteById(id));
    }

    @GetMapping
    public Map<String, Object> page(@RequestParam(defaultValue = "1") long pageNo,
                                    @RequestParam(defaultValue = "10") long pageSize) {
        IPage<SysUserRole> result = mapper.selectPage(new Page<>(pageNo, pageSize), new QueryWrapper<SysUserRole>().orderByDesc("created_at"));
        return successWithData(result);
    }

    private Map<String, Object> successWithData(Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", data);
        return result;
    }

    private Map<String, Object> affected(int affected) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", affected > 0);
        result.put("affected", affected);
        return result;
    }
}

