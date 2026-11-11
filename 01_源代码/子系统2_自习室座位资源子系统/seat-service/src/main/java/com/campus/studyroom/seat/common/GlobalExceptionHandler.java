package com.campus.studyroom.seat.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 捕获参数非法异常（比如状态传错了），返回 400
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgument(IllegalArgumentException e) {
        return Result.error(400, e.getMessage());
    }

    // 捕获业务异常，把 "1006:请先取消..." 拆开，返回规范的 JSON
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        String msg = e.getMessage();
        if (msg != null && msg.contains(":")) {
            String[] parts = msg.split(":", 2);
            try {
                // 如果是契约里的业务码（如 1006），就返回对应 code 和 message
                return Result.error(Integer.parseInt(parts[0]), parts[1]);
            } catch (NumberFormatException ignored) {}
        }
        return Result.error(500, msg == null ? "服务器内部错误" : msg);
    }

    // 捕获其他系统异常
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        e.printStackTrace();
        return Result.error(500, "服务器内部错误");
    }
}