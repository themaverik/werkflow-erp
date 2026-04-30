package com.werkflow.business.common.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API response DTO for GET /api/v1/users/{keycloakId}/profile.
 *
 * Combines OIDC-sourced identity fields with enterprise HR profile data
 * as required by ADR-003 (Keycloak role simplification) and ADR-005
 * (department-scoped routing and visibility).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    /** Keycloak sub claim — stable user identifier across sessions. */
    private String keycloakId;

    /** Human-readable display name from OIDC /userinfo. */
    private String displayName;

    /** Email address from OIDC provider. Nullable per OIDC spec. */
    private String email;

    /** HR department code linking this user to a department (ADR-005). Nullable until set by admin. */
    private String departmentCode;

    /** Employee ID from HR system (ADR-003). Nullable until linked. */
    private String employeeId;

    /** Cost center code for financial routing. Nullable until set. */
    private String costCenter;

    /** Whether this user is a Point of Contact for their department (ADR-003). */
    private boolean isPoc;
}
