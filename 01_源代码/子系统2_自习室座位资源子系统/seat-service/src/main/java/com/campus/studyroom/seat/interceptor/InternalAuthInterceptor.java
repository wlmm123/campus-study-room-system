package com.campus.studyroom.seat.interceptor;

import com.campus.studyroom.seat.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class InternalAuthInterceptor implements HandlerInterceptor {

    @Value("${internal.token}")
    private String internalToken;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("X-Internal-Token");
        if (token != null && token.equals(internalToken)) {
            return true; // 放行
        }
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        Result<?> result = Result.error(1005, "内部服务鉴权失败");
        response.getWriter().write(new ObjectMapper().writeValueAsString(result));
        return false;
    }
}