package com.campus.studyroom.seat.interceptor;

import com.campus.studyroom.seat.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Component
public class UserAuthInterceptor implements HandlerInterceptor {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return reject(response, 401, "Token缺失或格式错误");
        }

        String token = authHeader.substring(7);
        try {
            // 解析 JWT
            Claims claims = Jwts.parser().setSigningKey(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8)).parseClaimsJws(token).getBody();
            List<String> roles = (List<String>) claims.get("roles");
            Long userId = Long.valueOf(claims.get("userId").toString());

            // 把 userId 存到 request 里，方便 Controller 取
            request.setAttribute("userId", userId);
            request.setAttribute("roles", roles);

            // 如果是管理员接口，校验 roles
            if (request.getRequestURI().startsWith("/api/admin/")) {
                if (roles == null || !roles.contains("ADMIN")) {
                    return reject(response, 403, "无权限");
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return reject(response, 401, "Token 无效或已过期");
        }
    }

    private boolean reject(HttpServletResponse response, int code, String msg) throws Exception {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        Result<?> result = Result.error(code, msg);
        response.getWriter().write(new ObjectMapper().writeValueAsString(result));
        return false;
    }
}