package com.anhtran.secure_mdm.advice;

import com.anhtran.secure_mdm.common.CommonException;
import com.anhtran.secure_mdm.common.ErrorMessage;
import com.anhtran.secure_mdm.common.ValidateException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ErrorMessage> handleCommonException(CommonException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorMessage(ex.getCode(), ex.getMessage(), ex.getStatus()));
    }

    @ExceptionHandler(ValidateException.class)
    public ResponseEntity<Map<String, String>> handleValidateException(ValidateException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ex.getMessageMap());
    }

    @Override
    protected Mono<ResponseEntity<Object>> handleWebExchangeBindException(
            WebExchangeBindException ex, HttpHeaders headers, HttpStatusCode status,
            ServerWebExchange exchange) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getAllErrors().forEach(error -> {
            String key = error instanceof FieldError field ? field.getField() : error.getObjectName();
            String message = error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value";
            errors.merge(key, message, (first, next) -> first + "; " + next);
        });
        return handleExceptionInternal(ex, errors, headers, status, exchange);
    }

    @Override
    protected Mono<ResponseEntity<Object>> handleExceptionInternal(
            Exception ex, @Nullable Object body, @Nullable HttpHeaders headers,
            HttpStatusCode status, ServerWebExchange exchange) {
        if (status.is5xxServerError()) {
            log.error("HTTP server error on {} {}", exchange.getRequest().getMethod(),
                    exchange.getRequest().getPath(), ex);
            // Retain Spring's HTTP status and headers, without exposing exception details.
            body = ProblemDetail.forStatus(status);
        }
        return super.handleExceptionInternal(ex, body, headers, status, exchange);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleUnexpectedException(Exception ex, ServerWebExchange exchange) {
        log.error("Unexpected error on {} {}", exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorMessage("9999", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
