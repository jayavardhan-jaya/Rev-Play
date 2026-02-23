package com.revature.revplaydemo.auth.advice;

import com.revature.revplaydemo.auth.exception.AuthConflictException;
import com.revature.revplaydemo.auth.exception.AuthForbiddenException;
import com.revature.revplaydemo.auth.exception.AuthNotFoundException;
import com.revature.revplaydemo.auth.exception.AuthUnauthorizedException;
import com.revature.revplaydemo.auth.exception.AuthValidationException;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.revature.revplaydemo.auth.controller")
public class AuthApiExceptionHandler {

    @ExceptionHandler(AuthValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(AuthValidationException exception) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleBeanValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraint(ConstraintViolationException exception) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage());
    }

    @ExceptionHandler(AuthConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(AuthConflictException exception) {
        return build(HttpStatus.CONFLICT, "CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(AuthNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(AuthNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler({AuthUnauthorizedException.class})
    public ResponseEntity<Map<String, Object>> handleUnauthorized(AuthUnauthorizedException exception) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", exception.getMessage());
    }

    @ExceptionHandler({AuthForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<Map<String, Object>> handleForbidden(Exception exception) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(
                Map.of(
                        "timestamp", Instant.now().toString(),
                        "error", error,
                        "message", message
                )
        );
    }
}
