package com.campus.studyroom.auth.security;

import com.campus.studyroom.auth.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JWT 签发与解析单元测试：重点验证 Claims 里必须带 roles 数组（契约 1.3）。
 *
 * @author 成员1
 */
class JwtUtilTest {

    private static final String SECRET = "CampusStudyRoom2026JwtSecret9f3a7c1e5b2d4a6f8e0c1b3d5a7e2f4c6";

    private JwtUtil newJwtUtil() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setExpireSeconds(7200L);
        JwtUtil jwtUtil = new JwtUtil(properties);
        jwtUtil.init();
        return jwtUtil;
    }

    @Test
    @DisplayName("签发的 Token 解析后能看到 userId、roles 数组、主角色")
    void shouldCarryUserIdAndRolesArray() {
        JwtUtil jwtUtil = newJwtUtil();
        String token = jwtUtil.generateToken(1L, Collections.singletonList(RoleCode.STUDENT));

        JwtPayload payload = jwtUtil.parseToken(token);

        assertEquals(1L, payload.getUserId());
        assertEquals(1, payload.getRoles().size());
        assertTrue(payload.getRoles().contains("STUDENT"));
        assertEquals("STUDENT", payload.getRole());
        assertFalse(payload.isAdmin());
    }

    @Test
    @DisplayName("多角色时主角色按 ADMIN > STUDENT，且 roles 数组两个都在")
    void shouldPreferAdminAsPrimaryRole() {
        JwtUtil jwtUtil = newJwtUtil();
        String token = jwtUtil.generateToken(2L, Arrays.asList("STUDENT", "ADMIN"));

        JwtPayload payload = jwtUtil.parseToken(token);

        assertEquals("ADMIN", payload.getRole());
        assertEquals(2, payload.getRoles().size());
        assertTrue(payload.isAdmin());
        assertEquals("ADMIN", JwtUtil.primaryRole(Arrays.asList("STUDENT", "ADMIN")));
    }

    @Test
    @DisplayName("换一个密钥签发的 Token 必须验签失败（防止四端密钥不一致）")
    void shouldRejectTokenSignedByOtherSecret() {
        JwtUtil issuer = newJwtUtil();
        String token = issuer.generateToken(1L, Collections.singletonList(RoleCode.STUDENT));

        JwtProperties other = new JwtProperties();
        other.setSecret("AnotherSecretAnotherSecret12AnotherSecret12X");
        JwtUtil verifier = new JwtUtil(other);
        verifier.init();

        assertThrows(JwtException.class, () -> verifier.parseToken(token));
    }

    @Test
    @DisplayName("未配置 jwt.secret 时启动即失败，不允许空密钥上线")
    void shouldFailFastWhenSecretMissing() {
        JwtProperties properties = new JwtProperties();
        JwtUtil jwtUtil = new JwtUtil(properties);
        assertThrows(IllegalStateException.class, jwtUtil::init);
    }
}
