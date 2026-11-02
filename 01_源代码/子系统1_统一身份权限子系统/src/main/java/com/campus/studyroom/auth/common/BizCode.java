package com.campus.studyroom.auth.common;

/**
 * 全局业务状态码（与《REST接口契约》1.2 保持一致）。
 *
 * @author 成员1
 */
public final class BizCode {

    /** 成功 */
    public static final int SUCCESS = 200;
    /** 参数缺失、类型错误、格式错误 */
    public static final int BAD_REQUEST = 400;
    /** Token 缺失 / 格式非法 / 签名无效 / 过期 */
    public static final int UNAUTHORIZED = 401;
    /** 无权限 */
    public static final int FORBIDDEN = 403;
    /** 服务器内部错误 */
    public static final int INTERNAL_ERROR = 500;

    /** 账号或密码错误 */
    public static final int ACCOUNT_OR_PASSWORD_ERROR = 2001;
    /** 账号已被禁用 */
    public static final int ACCOUNT_DISABLED = 2002;
    /** 账号（学号）已存在 */
    public static final int USERNAME_EXISTS = 2003;

    private BizCode() {
    }
}
