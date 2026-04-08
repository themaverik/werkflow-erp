package com.werkflow.business.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        .withZone(ZoneId.of("UTC"));

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex) {
        String timestamp = ISO_FORMATTER.format(Instant.now());

        ErrorResponse response = ErrorResponse.builder()
            .code("DEPARTMENT_NOT_FOUND")
            .message(ex.getMessage())
            .timestamp(timestamp)
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleValidationException(Exception ex) {
        String timestamp = ISO_FORMATTER.format(Instant.now());

        ErrorResponse response = ErrorResponse.builder()
            .code("VALIDATION_FAILED")
            .message(ex.getMessage())
            .timestamp(timestamp)
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        String timestamp = ISO_FORMATTER.format(Instant.now());

        ErrorResponse response = ErrorResponse.builder()
            .code("INTERNAL_SERVER_ERROR")
            .message(ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred")
            .timestamp(timestamp)
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
