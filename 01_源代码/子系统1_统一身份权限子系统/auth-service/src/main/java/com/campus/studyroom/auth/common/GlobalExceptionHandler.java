package com.campus.studyroom.auth.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理：保证任何异常都返回契约 1.1 规定的统一 JSON 结构。
 *
 * <p>状态码口径（契约 1.2）：
 * <ul>
 *   <li>参数缺失 / 类型错误 / 格式错误 → HTTP 400，code=400</li>
 *   <li>业务拒绝（2001/2002/2003）→ HTTP 200，code 为业务码</li>
 *   <li>未捕获异常 → HTTP 500，code=500，不返回堆栈</li>
 * </ul>
 *
 * @author 成员1
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：业务码 400 走 HTTP 400，其余（2001/2002/2003 等）走 HTTP 200 + 业务码 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        log.info("业务拒绝: code={}, message={}", e.getCode(), e.getMessage());
        if (e.getCode() == BizCode.BAD_REQUEST) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Result.fail(BizCode.BAD_REQUEST, e.getMessage()));
        }
        return ResponseEntity.ok(Result.fail(e.getCode(), e.getMessage()));
    }

    /** @RequestBody 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleInvalidBody(MethodArgumentNotValidException e) {
        return badRequest(extractFieldMessage(e.getBindingResult().getFieldError()));
    }

    /** 表单/Query 绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBind(BindException e) {
        return badRequest(extractFieldMessage(e.getBindingResult().getFieldError()));
    }

    /** 必填 Query 参数缺失 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        return badRequest("缺少必填参数：" + e.getParameterName());
    }

    /** 参数类型不匹配（如 pageNum 传了字母） */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return badRequest("参数类型错误：" + e.getName());
    }

    /** 请求体不是合法 JSON */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleUnreadable(HttpMessageNotReadableException e) {
        return badRequest("请求体格式错误");
    }

    /** 兜底：不向调用方暴露堆栈 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e) {
        log.error("未捕获异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(BizCode.INTERNAL_ERROR, "服务器内部错误"));
    }

    private ResponseEntity<Result<Void>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(BizCode.BAD_REQUEST, message));
    }

    private String extractFieldMessage(FieldError fieldError) {
        if (fieldError == null) {
            return "参数校验失败";
        }
        return fieldError.getDefaultMessage();
    }
}
