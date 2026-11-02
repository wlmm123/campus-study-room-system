package com.campus.studyroom.auth.security;

import com.campus.studyroom.auth.common.BizCode;
import com.campus.studyroom.auth.common.BusinessException;

import java.util.Collections;
import java.util.List;

/**
 * 当前登录用户上下文（ThreadLocal），由 AuthInterceptor 写入、请求结束时清理。
 *
 * @author 成员1
 */
public final class UserContext {

    private static final ThreadLocal<JwtPayload> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(JwtPayload payload) {
        HOLDER.set(payload);
    }

    public static JwtPayload get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** 取当前登录用户 ID，未登录直接判 401（正常情况下拦截器已保证） */
    public static Long getUserId() {
        JwtPayload payload = HOLDER.get();
        if (payload == null || payload.getUserId() == null) {
            throw new BusinessException(BizCode.UNAUTHORIZED, "Token 无效或已过期");
        }
        return payload.getUserId();
    }

    public static List<String> getRoles() {
        JwtPayload payload = HOLDER.get();
        return payload == null ? Collections.emptyList() : payload.getRoles();
    }

    public static boolean isAdmin() {
        JwtPayload payload = HOLDER.get();
        return payload != null && payload.isAdmin();
    }
}
