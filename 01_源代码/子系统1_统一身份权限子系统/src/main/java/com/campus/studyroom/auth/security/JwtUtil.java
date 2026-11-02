package com.campus.studyroom.auth.security;

import com.campus.studyroom.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * JWT 签发与解析工具。
 *
 * <p>Claims 口径（契约 1.3，全员对齐，8082/8083/8084 本地验签时按此取值）：
 * <ul>
 *   <li>{@code sub} = userId 字符串形式</li>
 *   <li>{@code userId} = 同一个 userId</li>
 *   <li>{@code roles} = 角色数组，如 ["STUDENT"]、["STUDENT","ADMIN"]</li>
 *   <li>{@code role} = 主角色（多角色时 ADMIN &gt; STUDENT）</li>
 *   <li>{@code exp} = 过期时间</li>
 * </ul>
 * 接口拦截一律看 roles 数组，不看单个 role。
 *
 * @author 成员1
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 启动即校验密钥是否已注入：缺失时直接启动失败，避免带着空密钥上线后全员 401。
     */
    @PostConstruct
    public void init() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException(
                    "jwt.secret 未配置。请在 jar 同级 config/application.yml 中配置，"
                            + "并确保四个子系统取值完全一致（契约 1.3 / 开发契约 6.3）");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("jwt.secret 长度不足 32 字节，HS256 要求至少 256 位密钥");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT 签名密钥已加载（长度 {} 字节），有效期 {} 秒", keyBytes.length, jwtProperties.getExpireSeconds());
    }

    /**
     * 签发 Token。
     *
     * @param userId 用户 ID
     * @param roles  角色编码数组，至少一个
     * @return 紧凑格式 JWT
     */
    public String generateToken(Long userId, List<String> roles) {
        List<String> safeRoles = (roles == null || roles.isEmpty())
                ? Collections.singletonList(RoleCode.STUDENT)
                : roles;
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getExpireSeconds() * 1000L);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("roles", safeRoles)
                .claim("role", primaryRole(safeRoles))
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析并验签。
     *
     * @param token 不含 "Bearer " 前缀的 JWT
     * @return 解析结果
     * @throws JwtException 签名无效、过期、格式非法
     */
    public JwtPayload parseToken(String token) throws JwtException {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Long userId = resolveUserId(claims);
        return new JwtPayload(userId, resolveRoles(claims), claims.get("role", String.class));
    }

    /** 多角色时按 ADMIN &gt; STUDENT 取主角色（契约 4.1.2） */
    public static String primaryRole(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return RoleCode.STUDENT;
        }
        return roles.contains(RoleCode.ADMIN) ? RoleCode.ADMIN : roles.get(0);
    }

    private Long resolveUserId(Claims claims) {
        String subject = claims.getSubject();
        if (subject == null || subject.trim().isEmpty()) {
            throw new JwtException("Token 缺少 sub 声明");
        }
        try {
            return Long.valueOf(subject.trim());
        } catch (NumberFormatException e) {
            throw new JwtException("Token 的 sub 不是合法用户 ID");
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> resolveRoles(Claims claims) {
        Object raw = claims.get("roles");
        if (!(raw instanceof List)) {
            return Collections.emptyList();
        }
        List<String> roles = new ArrayList<>();
        for (Object item : (List<Object>) raw) {
            if (item != null) {
                roles.add(String.valueOf(item));
            }
        }
        return roles;
    }
}
