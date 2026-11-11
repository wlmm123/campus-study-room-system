package com.campus.studyroom.order.common;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 处理自定义业务异常（HTTP 状态码由异常自带）
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<?>> handleBizException(BizException e) {
        return ResponseEntity.status(e.getHttpStatus()).body(Result.error(e.getCode(), e.getMessage()));
    }

    // 捕获 JWT 相关异常，返回 HTTP 401
    @ExceptionHandler({ExpiredJwtException.class, MalformedJwtException.class, SignatureException.class})
    public ResponseEntity<Result<?>> handleJwtException(Exception e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.error(ErrorCode.UNAUTHORIZED, "Token 无效或已过期"));
    }

    // 捕获参数校验异常 (如 @Valid 失败)，返回 HTTP 400
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result<?>> handleValidationException(Exception e) {
        String msg = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException) {
            FieldError fieldError = ((MethodArgumentNotValidException) e).getBindingResult().getFieldError();
            if (fieldError != null) msg = fieldError.getDefaultMessage();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(ErrorCode.BAD_REQUEST, msg));
    }

    // 兜底捕获所有异常，返回 HTTP 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> handleException(Exception e) {
        e.printStackTrace(); 
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(ErrorCode.INTERNAL_ERROR, "服务器内部错误"));
    }
}