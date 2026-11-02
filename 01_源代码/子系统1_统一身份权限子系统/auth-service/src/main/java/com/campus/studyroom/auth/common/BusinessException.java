package com.campus.studyroom.auth.common;

/**
 * 业务异常：由全局异常处理器转换为 HTTP 200 + 业务错误码（2001/2002/2003 等）。
 *
 * @author 成员1
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
