package com.campus.studyroom.order.controller;

import com.campus.studyroom.order.common.BizException;
import com.campus.studyroom.order.common.ErrorCode;
import com.campus.studyroom.order.common.Result;
import com.campus.studyroom.order.service.OrderService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.Key;

@RestController
@CrossOrigin
public class ConfigAndInternalController {

    @Autowired
    private OrderService orderService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    // ================== 全局配置接口 (供前端) ==================

    @GetMapping("/api/config/business")
    public Result getBusinessConfig(HttpServletRequest request) {
        // 【修复点 4】依据契约 2.1，用户接口需校验 Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BizException(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Token 缺失");
        }
        String token = authHeader.substring(7);
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token); // 解析验签
        
        return orderService.getBusinessConfig();
    }

    // ================== 内部接口 (供8084调用) ==================

    @GetMapping("/api/internal/config/business")
    public Result getInternalBusinessConfig() {
        return orderService.getInternalBusinessConfig();
    }

    @PostMapping("/api/internal/orders/batch-handle-timeout")
    public Result batchHandleTimeout() {
        return orderService.batchHandleTimeout();
    }
}