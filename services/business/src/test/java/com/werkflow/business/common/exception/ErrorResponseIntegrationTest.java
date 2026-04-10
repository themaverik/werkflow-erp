package com.werkflow.business.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import jakarta.persistence.EntityNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration-style tests for {@link GlobalExceptionHandler} verifying standardised
 * error response structure.  Runs as a plain unit test — no Spring context required
 * since {@code GlobalExceptionHandler} has no injected dependencies.
 */
public class ErrorResponseIntegrationTest {

    private GlobalExceptionHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testEntityNotFoundExceptionReturnsStandardizedFormat() {
        EntityNotFoundException ex = new EntityNotFoundException("Department with ID 999 not found");

        ResponseEntity<ErrorResponse> response = handler.handleEntityNotFoundException(ex);

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCode());
        assertNotNull(response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
        assertTrue(response.getBody().getTimestamp().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
    }

    @Test
    void testValidationExceptionReturnsStandardizedFormat() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid employee data");

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_FAILED", response.getBody().getCode());
        assertNotNull(response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
        assertTrue(response.getBody().getTimestamp().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
    }

    @Test
    void testGenericExceptionReturnsStandardizedFormat() {
        Exception ex = new Exception("Unexpected error");

        ResponseEntity<ErrorResponse> response = handler.handleGlobalException(ex);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getCode());
        assertNotNull(response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
        assertTrue(response.getBody().getTimestamp().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
    }

    @Test
    void testErrorResponseSerializationToJson() throws Exception {
        EntityNotFoundException ex = new EntityNotFoundException("Test error");

        ResponseEntity<ErrorResponse> response = handler.handleEntityNotFoundException(ex);
        String json = objectMapper.writeValueAsString(response.getBody());

        assertTrue(json.contains("code"));
        assertTrue(json.contains("message"));
        assertTrue(json.contains("timestamp"));
        assertTrue(json.contains("ENTITY_NOT_FOUND"));
    }

    @Test
    void testErrorResponseHasRequiredFields() {
        Exception ex = new RuntimeException("Test error");

        ResponseEntity<ErrorResponse> response = handler.handleGlobalException(ex);
        ErrorResponse errorResponse = response.getBody();

        assertNotNull(errorResponse);
        assertNotNull(errorResponse.getCode());
        assertFalse(errorResponse.getCode().isEmpty());
        assertNotNull(errorResponse.getMessage());
        assertFalse(errorResponse.getMessage().isEmpty());
        assertNotNull(errorResponse.getTimestamp());
        assertFalse(errorResponse.getTimestamp().isEmpty());
    }
}
