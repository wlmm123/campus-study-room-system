package com.campus.studyroom.order.config;
import com.campus.studyroom.order.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class InternalAuthInterceptor implements HandlerInterceptor {
    @Value("${internal.token}")
    private String internalToken;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("X-Internal-Token");
        if (internalToken.equals(token)) {
            return true;
        }
        // 依据契约 1.3 与 1.2，校验失败返回 HTTP 401 + 业务码 1005
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        Result<?> result = Result.error(1005, "内部服务鉴权失败");
        response.getWriter().write(objectMapper.writeValueAsString(result));
        return false; 
    }
}