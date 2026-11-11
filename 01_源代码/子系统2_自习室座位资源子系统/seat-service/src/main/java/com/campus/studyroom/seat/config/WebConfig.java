package com.campus.studyroom.seat.config;

import com.campus.studyroom.seat.interceptor.InternalAuthInterceptor;
import com.campus.studyroom.seat.interceptor.UserAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private InternalAuthInterceptor internalAuthInterceptor;
    @Autowired
    private UserAuthInterceptor userAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 内部接口拦截
        registry.addInterceptor(internalAuthInterceptor)
                .addPathPatterns("/api/internal/**", "/api/seats/*/lock");

        // 用户接口拦截 (3.1, 3.2, 3.6)
        registry.addInterceptor(userAuthInterceptor)
                .addPathPatterns("/api/rooms/**", "/api/admin/**");
    }
}