package com.campus.studyroom.auth.security;

import java.util.Collections;
import java.util.List;

/**
 * JWT 解析结果（契约 1.3）：sub=userId、userId、roles=角色数组、role=主角色。
 *
 * @author 成员1
 */
public class JwtPayload {

    private final Long userId;
    private final List<String> roles;
    private final String role;

    public JwtPayload(Long userId, List<String> roles, String role) {
        this.userId = userId;
        this.roles = roles == null ? Collections.emptyList() : roles;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getRole() {
        return role;
    }

    /** 管理员判定一律看 roles 数组，不看单个 role（契约 4.1.3 多角色规则） */
    public boolean isAdmin() {
        return roles.contains(RoleCode.ADMIN);
    }
}
