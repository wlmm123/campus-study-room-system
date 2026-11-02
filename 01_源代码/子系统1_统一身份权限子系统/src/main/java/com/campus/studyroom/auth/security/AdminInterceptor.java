package com.campus.studyroom.auth.security;

import com.campus.studyroom.auth.common.BizCode;
import com.campus.studyroom.auth.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/**
 * 管理员权限拦截器：/api/admin/** 必须携带 ADMIN 角色，否则 HTTP 403（契约 4.1 模块鉴权补充说明）。
 *
 * <p>判定依据是 JWT 的 roles 数组，不是单个 role 字段。
 *
 * @author 成员1
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    public AdminInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (UserContext.isAdmin()) {
            return true;
        }
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.fail(BizCode.FORBIDDEN, "无权限")));
        return false;
    }
}
