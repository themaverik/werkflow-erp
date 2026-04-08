package com.werkflow.business.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for linking a Keycloak user to an employee.
 * Called by Admin Service after user provisioning in Keycloak.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakLinkRequest {
    @NotBlank(message = "Keycloak user ID is required")
    @Size(max = 255, message = "Keycloak user ID cannot exceed 255 characters")
    private String keycloakUserId;
}
