package com.campus.studyroom.order.common;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final int code;
    private final int httpStatus;

    public BizException(int code, int httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
    
    // 默认 HTTP 200 的业务异常（如 1001, 1003, 1004 等）
    public BizException(int code, String message) {
        this(code, 200, message);
    }
}