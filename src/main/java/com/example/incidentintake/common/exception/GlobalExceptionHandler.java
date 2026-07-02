package com.example.incidentintake.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(), "message", defaultMessage(fe)))
                .collect(Collectors.toList());

        log.warn("event=VALIDATION_FAILURE path={} fields={}", request.getRequestURI(),
                fieldErrors.stream().map(m -> m.get("field")).collect(Collectors.joining(",")));

        return ResponseEntity.badRequest().body(errorBody(
                HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), fieldErrors));
    }

    @ExceptionHandler(IncidentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            IncidentNotFoundException ex, HttpServletRequest request) {
        log.warn("event=INCIDENT_NOT_FOUND path={} message=\"{}\"", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(
                HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), List.of()));
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTransition(
            InvalidStatusTransitionException ex, HttpServletRequest request) {
        log.warn("event=INVALID_STATUS_TRANSITION path={} message=\"{}\"", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorBody(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI(), List.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("event=UNHANDLED_EXCEPTION path={} errorClass={} message=\"{}\"",
                request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return ResponseEntity.internalServerError().body(errorBody(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred",
                request.getRequestURI(), List.of()));
    }

    private Map<String, Object> errorBody(HttpStatus status, String message,
                                           String path, List<?> fieldErrors) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message,
                "path", path,
                "fieldErrors", fieldErrors
        );
    }

    private String defaultMessage(FieldError fe) {
        return fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid value";
    }
}
