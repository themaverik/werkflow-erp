package com.werkflow.business.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * User entity representing a cached user profile from OIDC provider.
 * Maps keycloak_id (from JWT sub claim) to display_name and email.
 *
 * This entity is populated by UserInfoResolver via OIDC /userinfo endpoint.
 * Serves as the single source of truth for display names in API responses.
 *
 * OIDC Compliance:
 * - JWT contains only: sub, tenant_id, roles, iss, aud, exp, iat, scope
 * - User profile (name, email) is fetched from /userinfo endpoint
 * - Cached locally with TTL 5-15 minutes via Caffeine
 * - This prevents N+1 lookups and keeps PII out of JWT/logs
 */
@Entity
@Table(name = "users", schema = "identity_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "keycloakId")
public class User {

    /**
     * Primary key: keycloak_id / sub claim from JWT.
     * Format: provider-specific (e.g., "550e8400-e29b-41d4-a716-446655440000" for Keycloak,
     * "auth0|5f7a1c82" for Auth0, "3f7a1c82-4b9e-4c1a-8f3d" for Azure AD).
     */
    @Id
    @Column(name = "keycloak_id", nullable = false, length = 255)
    private String keycloakId;

    /**
     * Display name: "Jane Smith" or "John Doe".
     * Populated from OIDC /userinfo endpoint (name, given_name + family_name, or email).
     * Never null after UserInfoResolver caches the user.
     */
    @Column(name = "display_name", nullable = false)
    private String displayName;

    /**
     * Email address from OIDC provider.
     * May be masked in logs per GDPR requirements.
     */
    @Column(name = "email")
    private String email;

    /**
     * Timestamp of first cache population from OIDC provider.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp of last cache refresh from OIDC provider.
     * Used for cache invalidation (TTL: 5-15 minutes).
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
