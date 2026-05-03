package com.werkflow.business.common.identity;

import com.werkflow.business.common.identity.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing user profile data for enterprise identity resolution.
 *
 * Used by werkflow-enterprise to resolve department, employee ID, cost center,
 * and POC status from Keycloak user IDs (ADR-003, ADR-005).
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Profiles", description = "Enterprise user profile APIs")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/{keycloakId}/profile")
    @Operation(
        summary = "Get user profile",
        description = "Returns OIDC identity and enterprise HR profile fields for a given Keycloak user ID."
    )
    public ResponseEntity<UserProfileResponse> getProfile(
            @Parameter(description = "Keycloak user ID (JWT sub claim)")
            @PathVariable String keycloakId) {
        return ResponseEntity.ok(userProfileService.getProfile(keycloakId));
    }
}
