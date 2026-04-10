package com.werkflow.business.common.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.persistence.EntityNotFoundException;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Test
    void testHandleEntityNotFoundException() {
        EntityNotFoundException ex = new EntityNotFoundException("Department with ID 1 not found");
        ResponseEntity<ErrorResponse> response = handler.handleEntityNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ENTITY_NOT_FOUND", response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("not found"));
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testErrorResponseHasTimestamp() {
        EntityNotFoundException ex = new EntityNotFoundException("Test error");
        ResponseEntity<ErrorResponse> response = handler.handleEntityNotFoundException(ex);

        assertNotNull(response.getBody().getTimestamp());
        // Timestamp should be ISO 8601 format
        assertTrue(response.getBody().getTimestamp().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
    }

    @Test
    void testHandleNullPointerException() {
        NullPointerException ex = new NullPointerException("Unexpected null value");
        ResponseEntity<ErrorResponse> response = handler.handleGlobalException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getCode());
    }

    @Test
    void testHandleConstraintViolationException() {
        String violationMessage = "Department not found";
        IllegalArgumentException ex = new IllegalArgumentException(violationMessage);
        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_FAILED", response.getBody().getCode());
        assertEquals(violationMessage, response.getBody().getMessage());
    }

    @Test
    void testValidationExceptionWithFieldDetails() {
        IllegalArgumentException ex = new IllegalArgumentException("Validation failed: invalid department ID");
        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testHandleDataAccessException() {
        org.springframework.dao.DataAccessException ex =
            new org.springframework.dao.DataIntegrityViolationException("Duplicate key");
        ResponseEntity<ErrorResponse> response = handler.handleDataAccessException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("DATA_INTEGRITY_VIOLATION", response.getBody().getCode());
    }

    @Test
    void testHandleGenericDatabaseException() {
        org.springframework.dao.DataAccessException ex =
            new org.springframework.dao.DataAccessResourceFailureException("Database unavailable");
        ResponseEntity<ErrorResponse> response = handler.handleDataAccessException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("DATABASE_ERROR", response.getBody().getCode());
    }
}
