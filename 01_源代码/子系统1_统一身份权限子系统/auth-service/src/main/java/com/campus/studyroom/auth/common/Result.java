package com.campus.studyroom.auth.common;

/**
 * 统一 JSON 返回结构（契约 1.1）：{"code":200,"message":"操作成功","data":{}}
 *
 * @param <T> 业务数据类型
 * @author 成员1
 */
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(BizCode.SUCCESS, "操作成功", data);
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(BizCode.SUCCESS, message, data);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
