package com.campus.studyroom.order.common;

public class ErrorCode {
    // HTTP 状态码
    public static final int SUCCESS = 200;
    public static final int BAD_REQUEST = 400; 
    public static final int UNAUTHORIZED = 401; 
    public static final int FORBIDDEN = 403; 
    public static final int INTERNAL_ERROR = 500; 

    // 业务错误码 (契约 1.2)
    public static final int SEAT_LOCKED = 1001;
    public static final int PARAM_INVALID = 1002;
    public static final int STATE_NOT_ALLOWED = 1003;
    public static final int TEMP_LEAVE_TIMEOUT = 1004;
    public static final int INTERNAL_AUTH_FAIL = 1005;
    public static final int SEAT_UNAVAILABLE = 1006;
    public static final int REMOTE_SERVICE_ERROR = 1007;
    public static final int TIME_OVERLAP = 1008;

    public static final int ACCOUNT_OR_PWD_ERROR = 2001;
    public static final int ACCOUNT_DISABLED = 2002;
    public static final int ACCOUNT_EXISTS = 2003;
}