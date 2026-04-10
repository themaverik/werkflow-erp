package com.werkflow.business.common.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal DTO representing a resolved user profile from the OIDC /userinfo endpoint.
 *
 * This is NOT a request/response DTO — it is used exclusively within {@link com.werkflow.business.common.identity.UserInfoResolver}
 * for in-memory Caffeine caching and upsert coordination.
 *
 * Fields:
 * - keycloakId: JWT sub claim (the stable user identifier across sessions)
 * - displayName: human-readable name ("Jane Smith"), nullable on graceful degradation
 * - email: user email from OIDC provider, nullable per OIDC spec and on degradation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfo {

    /** JWT sub claim — stable user identifier from OIDC provider. Never null. */
    private String keycloakId;

    /** Display name from /userinfo (name, given_name + family_name, or email fallback). Null on degradation. */
    private String displayName;

    /** Email address from OIDC provider. Nullable — not all providers expose email via /userinfo. */
    private String email;
}
