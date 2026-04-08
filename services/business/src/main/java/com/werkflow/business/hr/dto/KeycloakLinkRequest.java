package com.werkflow.business.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakLinkRequest {
    @NotBlank(message = "Keycloak user ID is required")
    @Size(max = 255, message = "Keycloak user ID cannot exceed 255 characters")
    private String keycloakUserId;
}
