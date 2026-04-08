package com.werkflow.business.hr.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class KeycloakLinkRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidKeycloakLinkRequest() {
        KeycloakLinkRequest request = new KeycloakLinkRequest("keycloak-uuid-123");
        Set<ConstraintViolation<KeycloakLinkRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testBlankKeycloakUserId() {
        KeycloakLinkRequest request = new KeycloakLinkRequest("");
        Set<ConstraintViolation<KeycloakLinkRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("required")));
    }

    @Test
    void testNullKeycloakUserId() {
        KeycloakLinkRequest request = new KeycloakLinkRequest(null);
        Set<ConstraintViolation<KeycloakLinkRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("required")));
    }

    @Test
    void testExcessivelyLongKeycloakUserId() {
        String longId = "a".repeat(256);
        KeycloakLinkRequest request = new KeycloakLinkRequest(longId);
        Set<ConstraintViolation<KeycloakLinkRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("255")));
    }
}
