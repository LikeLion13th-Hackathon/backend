// // src/main/java/com/example/hackathon/common/GlobalExceptionHandler.java
// package com.example.hackathon.common;

// import jakarta.servlet.http.HttpServletRequest;
// import org.springframework.http.HttpStatus;
// import org.springframework.security.authentication.BadCredentialsException;
// import org.springframework.web.bind.annotation.*;

// import java.time.Instant;
// import java.util.Map;

// @RestControllerAdvice
// public class GlobalExceptionHandler {

//     private Map<String, Object> body(HttpServletRequest req, HttpStatus status, String message) {
//         return Map.of(
//             "timestamp", Instant.now().toString(),
//             "status", status.value(),
//             "error", status.getReasonPhrase(),
//             "path", req.getRequestURI(),
//             "message", message
//         );
//     }

//     @ExceptionHandler(BadCredentialsException.class)
//     @ResponseStatus(HttpStatus.UNAUTHORIZED) // 401
//     public Map<String, Object> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
//         return body(req, HttpStatus.UNAUTHORIZED, "invalid_credentials");
//     }

//     @ExceptionHandler(IllegalArgumentException.class)
//     @ResponseStatus(HttpStatus.BAD_REQUEST) // 400 (원한다면 401로 통일해도 됨)
//     public Map<String, Object> handleIllegalArg(IllegalArgumentException ex, HttpServletRequest req) {
//         return body(req, HttpStatus.BAD_REQUEST, ex.getMessage());
//     }

//     @ExceptionHandler(ForbiddenException.class)
//     @ResponseStatus(HttpStatus.FORBIDDEN) // 403
//     public Map<String, Object> handleForbidden(ForbiddenException ex, HttpServletRequest req) {
//         return body(req, HttpStatus.FORBIDDEN, ex.getMessage());
//     }

//     @ExceptionHandler(NotFoundException.class)
//     @ResponseStatus(HttpStatus.NOT_FOUND) // 404
//     public Map<String, Object> handleNotFound(NotFoundException ex, HttpServletRequest req) {
//         return body(req, HttpStatus.NOT_FOUND, ex.getMessage());
//     }

//     @ExceptionHandler(InsufficientBalanceException.class)
//     @ResponseStatus(HttpStatus.BAD_REQUEST) // 400
//     public Map<String, Object> handleInsufficient(InsufficientBalanceException ex, HttpServletRequest req) {
//         return body(req, HttpStatus.BAD_REQUEST, "insufficient_balance");
//     }

//     @ExceptionHandler(Exception.class)
//     @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // 안전망
//     public Map<String, Object> handleOthers(Exception ex, HttpServletRequest req) {
//         return body(req, HttpStatus.INTERNAL_SERVER_ERROR, "internal_error");
//     }
// }
