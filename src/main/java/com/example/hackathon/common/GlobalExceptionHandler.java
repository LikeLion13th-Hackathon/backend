// src/main/java/com/example/hackathon/common/GlobalExceptionHandler.java
package com.example.hackathon.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private Map<String, Object> body(HttpServletRequest req, HttpStatus status, String message) {
        return Map.of(
            "timestamp", Instant.now().toString(),
            "status", status.value(),
            "error", status.getReasonPhrase(),
            "path", req.getRequestURI(),
            "message", message
        );
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        return body(req, HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, Object> handleForbidden(ForbiddenException ex, HttpServletRequest req) {
        return body(req, HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 402를 쓰기도 하지만 실무에선 400/409가 일반적
    public Map<String, Object> handleInsufficient(InsufficientBalanceException ex, HttpServletRequest req) {
        return body(req, HttpStatus.BAD_REQUEST, "insufficient_balance");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
        return body(req, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 마지막 안전망(예상 밖 예외는 500으로)
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleOthers(Exception ex, HttpServletRequest req) {
        return body(req, HttpStatus.INTERNAL_SERVER_ERROR, "internal_error");
    }
}
