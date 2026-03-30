package com.lw.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lw.backend.modules.mes.entity.SysUser;
import com.lw.backend.modules.mes.entity.SysUserRole;
import com.lw.backend.modules.mes.exception.BizException;
import com.lw.backend.modules.mes.mapper.SysUserMapper;
import com.lw.backend.modules.mes.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private static final ConcurrentHashMap<String, LoginSession> SESSION_STORE = new ConcurrentHashMap<>();

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new BizException("用户名和密码不能为空");
        }

        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername().trim())
                .last("limit 1"));
        if (user == null) {
            throw new BizException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException("账号已禁用");
        }
        if (!passwordMatches(request.getPassword(), user.getPasswordHash())) {
            throw new BizException("用户名或密码错误");
        }

        String roleCode = resolveRoleCode(user.getUserId());
        String displayName = StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();

        String token = "MES-" + UUID.randomUUID();
        SESSION_STORE.put(token, new LoginSession(user.getUserId(), user.getUsername(), displayName, roleCode, LocalDateTime.now()));

        user.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(user);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("roleCode", roleCode);
        data.put("username", displayName);
        data.put("userId", user.getUserId());
        return success(data);
    }

    @GetMapping("/me")
    public Map<String, Object> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractToken(authorization);
        LoginSession session = SESSION_STORE.get(token);
        if (session == null) {
            throw new BizException("登录已失效，请重新登录");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("roleCode", session.roleCode());
        data.put("username", session.displayName());
        data.put("userId", session.userId());
        return success(data);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractToken(authorization);
        if (StringUtils.hasText(token)) {
            SESSION_STORE.remove(token);
        }
        return success(true);
    }

    private boolean passwordMatches(String rawPassword, String passwordHash) {
        if (!StringUtils.hasText(passwordHash)) {
            return false;
        }
        String hash = passwordHash.trim();
        if (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$")) {
            return PASSWORD_ENCODER.matches(rawPassword, hash);
        }
        return rawPassword.equals(hash);
    }

    private String resolveRoleCode(String userId) {
        List<SysUserRole> roles = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .orderByAsc(SysUserRole::getCreatedAt));
        if (roles == null || roles.isEmpty()) {
            throw new BizException("账号未分配角色");
        }
        return roles.get(0).getRoleCode();
    }

    private String extractToken(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        String value = authorization.trim();
        if (value.startsWith("Bearer ")) {
            return value.substring(7).trim();
        }
        return value;
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", data);
        return result;
    }

    public static class LoginRequest {
        private String username;
        private String password;

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
    }

    private record LoginSession(String userId,
                                String username,
                                String displayName,
                                String roleCode,
                                LocalDateTime loginAt) {
    }
}
