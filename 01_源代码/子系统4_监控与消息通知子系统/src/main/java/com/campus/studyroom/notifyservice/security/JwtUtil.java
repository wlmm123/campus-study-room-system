package com.campus.studyroom.notifyservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] kb = secret.getBytes(StandardCharsets.UTF_8);   // 关键：直接取 UTF-8 字节
        if (kb.length < 32) {
            throw new IllegalStateException("jwt.secret 不足 32 字节");
        }
        this.key = Keys.hmacShaKeyFor(kb);                     // 不要 Base64 解码
    }

    /** 验签并返回 Claims；签名错抛 SignatureException，过期抛 ExpiredJwtException */
    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)      // 传纯 token，不能带 "Bearer "
                .getBody();
    }

    /** 取 userId：数字 claim 反序列化后是 Integer，必须用 Number 转 */
    public Long getUserId(Claims c) {
        Object v = c.get("userId");
        if (v == null) {
            String sub = c.getSubject();                       // 兜底用 sub
            return sub == null ? null : Long.valueOf(sub.trim());
        }
        return ((Number) v).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(Claims c) {
        Object raw = c.get("roles");
        if (!(raw instanceof List)) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (Object o : (List<Object>) raw) {
            if (o != null) out.add(String.valueOf(o));
        }
        return out;
    }

    public boolean isAdmin(Claims c) {
        return getRoles(c).contains("ADMIN");                  // 只看数组，不看 role
    }
}