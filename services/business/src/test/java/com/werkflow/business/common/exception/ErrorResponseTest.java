package com.werkflow.business.common.exception;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

public class ErrorResponseTest {

    @Test
    void testErrorResponseStructure() throws Exception {
        ErrorResponse response = ErrorResponse.builder()
            .code("DEPARTMENT_NOT_FOUND")
            .message("Department with ID 1 not found")
            .timestamp("2026-04-08T10:30:00Z")
            .details(null)
            .build();

        assertNotNull(response);
        assertEquals("DEPARTMENT_NOT_FOUND", response.getCode());
        assertEquals("Department with ID 1 not found", response.getMessage());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testErrorResponseWithDetails() throws Exception {
        ErrorResponse response = ErrorResponse.builder()
            .code("VALIDATION_FAILED")
            .message("Request validation failed")
            .timestamp("2026-04-08T10:30:00Z")
            .details(new ErrorResponse.ErrorDetails())
            .build();

        assertNotNull(response.getDetails());
    }

    @Test
    void testErrorResponseSerialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ErrorResponse response = ErrorResponse.builder()
            .code("ERROR_CODE")
            .message("Error message")
            .timestamp("2026-04-08T10:30:00Z")
            .details(null)
            .build();

        String json = mapper.writeValueAsString(response);
        assertTrue(json.contains("ERROR_CODE"));
        assertTrue(json.contains("Error message"));
    }
}
