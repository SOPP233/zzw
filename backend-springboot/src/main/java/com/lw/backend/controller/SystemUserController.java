package com.lw.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lw.backend.modules.mes.entity.SysRole;
import com.lw.backend.modules.mes.entity.SysUser;
import com.lw.backend.modules.mes.entity.SysUserRole;
import com.lw.backend.modules.mes.exception.BizException;
import com.lw.backend.modules.mes.mapper.SysRoleMapper;
import com.lw.backend.modules.mes.mapper.SysUserMapper;
import com.lw.backend.modules.mes.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemUserController {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/roles")
    public Map<String, Object> roleOptions() {
        List<SysRole> roles = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getRoleCode));
        List<Map<String, Object>> data = roles.stream().map(role -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("roleCode", role.getRoleCode());
            row.put("roleName", role.getRoleName());
            return row;
        }).toList();
        return success(data);
    }

    @GetMapping("/users")
    public Map<String, Object> page(@RequestParam(defaultValue = "1") long pageNo,
                                    @RequestParam(defaultValue = "10") long pageSize,
                                    @RequestParam(required = false) String username,
                                    @RequestParam(required = false) String realName,
                                    @RequestParam(required = false) Integer status,
                                    @RequestParam(required = false) String roleCode) {
        long safeNo = Math.max(pageNo, 1);
        long safeSize = Math.max(pageSize, 1);
        long offset = (safeNo - 1) * safeSize;

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> countArgs = new ArrayList<>();
        if (StringUtils.hasText(username)) {
            where.append(" AND u.username LIKE ? ");
            countArgs.add("%" + username.trim() + "%");
        }
        if (StringUtils.hasText(realName)) {
            where.append(" AND u.real_name LIKE ? ");
            countArgs.add("%" + realName.trim() + "%");
        }
        if (status != null) {
            where.append(" AND u.is_active = ? ");
            countArgs.add(status);
        }
        if (StringUtils.hasText(roleCode)) {
            where.append(" AND EXISTS (SELECT 1 FROM sys_user_role x WHERE x.user_id = u.user_id AND x.role_code = ?) ");
            countArgs.add(roleCode.trim());
        }

        String countSql = "SELECT COUNT(1) FROM sys_user u" + where;
        Long total = queryCount(countSql, countArgs);

        String dataSql = "SELECT u.user_id, u.username, u.real_name, u.phone, u.dept_id, u.open_id, u.union_id, u.is_active AS status, u.created_at, " +
                "GROUP_CONCAT(ur.role_code ORDER BY ur.role_code SEPARATOR ',') AS role_codes " +
                "FROM sys_user u LEFT JOIN sys_user_role ur ON ur.user_id = u.user_id " +
                where +
                " GROUP BY u.user_id, u.username, u.real_name, u.is_active, u.created_at " +
                " ORDER BY u.created_at DESC LIMIT ? OFFSET ?";
        List<Object> dataArgs = new ArrayList<>(countArgs);
        dataArgs.add(safeSize);
        dataArgs.add(offset);
        List<Map<String, Object>> rows = queryList(dataSql, dataArgs);

        Map<String, String> roleNameMap = sysRoleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getRoleCode, r -> Optional.ofNullable(r.getRoleName()).orElse(r.getRoleCode()), (a, b) -> a));

        List<Map<String, Object>> records = rows.stream().map(row -> {
            String codesRaw = asString(row.get("role_codes"));
            List<String> roleCodes = splitCsv(codesRaw);
            List<String> roleNames = roleCodes.stream().map(code -> roleNameMap.getOrDefault(code, code)).toList();
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("userId", asString(row.get("user_id")));
            out.put("username", asString(row.get("username")));
            out.put("realName", asString(row.get("real_name")));
            out.put("phone", asString(row.get("phone")));
            out.put("deptId", asString(row.get("dept_id")));
            out.put("openId", asString(row.get("open_id")));
            out.put("unionId", asString(row.get("union_id")));
            out.put("status", row.get("status"));
            out.put("createdAt", row.get("created_at"));
            out.put("roleCodes", roleCodes);
            out.put("roleNames", roleNames);
            return out;
        }).toList();

        Map<String, Object> page = new LinkedHashMap<>();
        page.put("records", records);
        page.put("total", total == null ? 0 : total);
        page.put("current", safeNo);
        page.put("size", safeSize);
        return success(page);
    }

    @PostMapping("/users")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(@RequestBody UserSaveRequest request) {
        validateForCreate(request);
        ensureUsernameUnique(request.getUsername(), null);

        SysUser user = new SysUser();
        user.setUserId(StringUtils.hasText(request.getUserId()) ? request.getUserId().trim() : generateUserId());
        user.setUsername(request.getUsername().trim());
        user.setPasswordHash(PASSWORD_ENCODER.encode(request.getPassword()));
        user.setRealName(request.getRealName().trim());
        user.setPhone(trimToNull(request.getPhone()));
        user.setDeptId(trimToNull(request.getDeptId()));
        user.setOpenId(trimToNull(request.getOpenId()));
        user.setUnionId(trimToNull(request.getUnionId()));
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        sysUserMapper.insert(user);

        saveUserRoles(user.getUserId(), request.getRoleCodes());
        return success(Map.of("userId", user.getUserId()));
    }

    @PutMapping("/users/{userId}")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(@PathVariable String userId,
                                      @RequestBody UserSaveRequest request) {
        SysUser existing = sysUserMapper.selectById(userId);
        if (existing == null) {
            throw new BizException("用户不存在");
        }
        if (!StringUtils.hasText(request.getUsername())) {
            throw new BizException("username不能为空");
        }
        if (!StringUtils.hasText(request.getRealName())) {
            throw new BizException("realName不能为空");
        }
        ensureUsernameUnique(request.getUsername(), userId);

        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getUserId, userId)
                .set(SysUser::getUsername, request.getUsername().trim())
                .set(SysUser::getRealName, request.getRealName().trim())
                .set(SysUser::getPhone, trimToNull(request.getPhone()))
                .set(SysUser::getDeptId, trimToNull(request.getDeptId()))
                .set(SysUser::getOpenId, trimToNull(request.getOpenId()))
                .set(SysUser::getUnionId, trimToNull(request.getUnionId()))
                .set(SysUser::getStatus, request.getStatus() == null ? 1 : request.getStatus());
        if (StringUtils.hasText(request.getPassword())) {
            wrapper.set(SysUser::getPasswordHash, PASSWORD_ENCODER.encode(request.getPassword()));
        }
        sysUserMapper.update(null, wrapper);

        saveUserRoles(userId, request.getRoleCodes());
        return success(true);
    }

    @DeleteMapping("/users/{userId}")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> delete(@PathVariable String userId) {
        SysUser existing = sysUserMapper.selectById(userId);
        if (existing == null) {
            return success(true);
        }
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        sysUserMapper.deleteById(userId);
        return success(true);
    }

    private void validateForCreate(UserSaveRequest request) {
        if (request == null) {
            throw new BizException("请求体不能为空");
        }
        if (!StringUtils.hasText(request.getUsername())) {
            throw new BizException("username不能为空");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BizException("password不能为空");
        }
        if (!StringUtils.hasText(request.getRealName())) {
            throw new BizException("realName不能为空");
        }
    }

    private void ensureUsernameUnique(String username, String excludeUserId) {
        LambdaQueryWrapper<SysUser> q = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username.trim());
        if (StringUtils.hasText(excludeUserId)) {
            q.ne(SysUser::getUserId, excludeUserId);
        }
        Long count = sysUserMapper.selectCount(q);
        if (count != null && count > 0) {
            throw new BizException("用户名已存在");
        }
    }

    private void saveUserRoles(String userId, List<String> roleCodes) {
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        List<String> distinct = roleCodes == null ? Collections.emptyList() :
                roleCodes.stream().filter(StringUtils::hasText).map(String::trim).distinct().toList();
        if (distinct.isEmpty()) {
            return;
        }
        Set<String> validRoleCodes = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getRoleCode, distinct))
                .stream().map(SysRole::getRoleCode).collect(Collectors.toSet());
        for (String roleCode : distinct) {
            if (!validRoleCodes.contains(roleCode)) {
                throw new BizException("角色不存在: " + roleCode);
            }
            SysUserRole rel = new SysUserRole();
            rel.setUserId(userId);
            rel.setRoleCode(roleCode);
            sysUserRoleMapper.insert(rel);
        }
    }

    private String generateUserId() {
        return "U" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private List<String> splitCsv(String csv) {
        if (!StringUtils.hasText(csv)) {
            return Collections.emptyList();
        }
        return Arrays.stream(csv.split(",")).filter(StringUtils::hasText).map(String::trim).toList();
    }

    private Long queryCount(String sql, List<Object> args) {
        return jdbcTemplate.queryForObject(sql, args.toArray(), Long.class);
    }

    private List<Map<String, Object>> queryList(String sql, List<Object> args) {
        return jdbcTemplate.queryForList(sql, args.toArray());
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", data);
        return result;
    }

    public static class UserSaveRequest {
        private String userId;
        private String username;
        private String password;
        private String realName;
        private String phone;
        private String deptId;
        private String openId;
        private String unionId;
        private Integer status;
        private List<String> roleCodes;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getRealName() {
            return realName;
        }

        public void setRealName(String realName) {
            this.realName = realName;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getDeptId() {
            return deptId;
        }

        public void setDeptId(String deptId) {
            this.deptId = deptId;
        }

        public String getOpenId() {
            return openId;
        }

        public void setOpenId(String openId) {
            this.openId = openId;
        }

        public String getUnionId() {
            return unionId;
        }

        public void setUnionId(String unionId) {
            this.unionId = unionId;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public List<String> getRoleCodes() {
            return roleCodes;
        }

        public void setRoleCodes(List<String> roleCodes) {
            this.roleCodes = roleCodes;
        }
    }
}
