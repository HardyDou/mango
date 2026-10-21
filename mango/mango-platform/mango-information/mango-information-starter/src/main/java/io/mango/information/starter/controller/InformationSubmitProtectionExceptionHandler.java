package io.mango.information.starter.controller;

import io.mango.common.result.R;
import io.mango.information.starter.security.InformationSubmitProtectionException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 将公开提交防刷拒绝映射为稳定的 HTTP 错误响应。 */
@RestControllerAdvice
public class InformationSubmitProtectionExceptionHandler {

    @ExceptionHandler(InformationSubmitProtectionException.class)
    public ResponseEntity<R<Void>> handle(InformationSubmitProtectionException exception) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(exception.getStatus());
        if (exception.getRetryAfterSeconds() > 0) {
            builder.header("Retry-After", String.valueOf(exception.getRetryAfterSeconds()));
        }
        return builder.body(R.fail(exception.getCode().getCode(), exception.getMessage()));
    }
}
