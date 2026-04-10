package com.werkflow.business.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ErrorResponseIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private GlobalExceptionHandler getGlobalExceptionHandler() {
        return applicationContext.getBean(GlobalExceptionHandler.class);
    }

    @Test
    void testEntityNotFoundExceptionReturnsStandardizedFormat() {
        GlobalExceptionHandler handler = getGlobalExceptionHandler();
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
        GlobalExceptionHandler handler = getGlobalExceptionHandler();
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
        GlobalExceptionHandler handler = getGlobalExceptionHandler();
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
        GlobalExceptionHandler handler = getGlobalExceptionHandler();
        EntityNotFoundException ex = new EntityNotFoundException("Test error");

        ResponseEntity<ErrorResponse> response = handler.handleEntityNotFoundException(ex);
        String json = objectMapper.writeValueAsString(response.getBody());

        assertTrue(json.contains("code"));
        assertTrue(json.contains("message"));
        assertTrue(json.contains("timestamp"));
        assertTrue(json.contains("DEPARTMENT_NOT_FOUND"));
    }

    @Test
    void testErrorResponseHasRequiredFields() {
        GlobalExceptionHandler handler = getGlobalExceptionHandler();
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
