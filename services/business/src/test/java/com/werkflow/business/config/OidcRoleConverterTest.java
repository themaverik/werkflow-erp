package com.werkflow.business.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OidcRoleConverter}.
 *
 * Uses {@link Jwt#withTokenValue(String)} builder to construct lightweight mocked JWTs
 * without requiring a running Spring context.
 */
class OidcRoleConverterTest {

    private Jwt buildJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(c -> c.putAll(claims))
                .build();
    }

    /**
     * Test 1: Extracts roles from the default "roles" claim, applying ROLE_ prefix and uppercase.
     */
    @Test
    void extractRoles_fromDefaultRolesClaim() {
        OidcRoleConverter converter = new OidcRoleConverter("roles");
        Jwt jwt = buildJwt(Map.of("roles", List.of("admin", "hr_manager")));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_HR_MANAGER");
    }

    /**
     * Test 2: Extracts roles from a custom "scope" claim (Auth0 use case).
     */
    @Test
    void extractRoles_fromCustomScopeClaim_auth0UseCase() {
        OidcRoleConverter converter = new OidcRoleConverter("scope");
        Jwt jwt = buildJwt(Map.of("scope", List.of("read:employees", "write:payroll")));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_READ:EMPLOYEES", "ROLE_WRITE:PAYROLL");
    }

    /**
     * Test 3: Extracts roles from a "permissions" claim (used by some OIDC providers).
     */
    @Test
    void extractRoles_fromPermissionsClaim() {
        OidcRoleConverter converter = new OidcRoleConverter("permissions");
        Jwt jwt = buildJwt(Map.of("permissions", List.of("reports:view", "users:manage")));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_REPORTS:VIEW", "ROLE_USERS:MANAGE");
    }

    /**
     * Test 4: Missing claim returns an empty list without throwing an exception.
     */
    @Test
    void missingClaim_returnsEmptyList() {
        OidcRoleConverter converter = new OidcRoleConverter("roles");
        Jwt jwt = buildJwt(Map.of("sub", "user-123"));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    /**
     * Test 5: Null claim value returns an empty list without throwing an exception.
     * Spring's Jwt.getClaimAsStringList returns null when the claim is absent.
     */
    @Test
    void nullClaimValue_returnsEmptyList() {
        OidcRoleConverter converter = new OidcRoleConverter("roles");
        // Build JWT with no "roles" key at all — getClaimAsStringList returns null
        Jwt jwt = buildJwt(Map.of("email", "user@example.com"));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isNotNull().isEmpty();
    }

    /**
     * Test 6: Blank and null roles in the list are filtered out, not mapped to authorities.
     * Protects against providers that include empty strings in the roles claim.
     */
    @Test
    void blankRolesInList_areFilteredOut() {
        OidcRoleConverter converter = new OidcRoleConverter("roles");
        Jwt jwt = buildJwt(Map.of("roles", List.of("admin", "", "  ")));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    /**
     * Test 7: Empty roles list returns an empty authorities collection.
     */
    @Test
    void emptyRolesList_returnsEmptyAuthorities() {
        OidcRoleConverter converter = new OidcRoleConverter("roles");
        Jwt jwt = buildJwt(Map.of("roles", List.of()));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    /**
     * Test 8: Single role is correctly prefixed and uppercased.
     */
    @Test
    void singleRole_isPrefixedAndUppercased() {
        OidcRoleConverter converter = new OidcRoleConverter("roles");
        Jwt jwt = buildJwt(Map.of("roles", List.of("viewer")));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_VIEWER");
    }

    /**
     * Test 9: Azure AD "roles" claim behaviour — same claim name as Keycloak default,
     * confirming the converter works for both providers with the same configuration.
     */
    @Test
    void azureAdRolesClaim_worksWithDefaultConfiguration() {
        OidcRoleConverter converter = new OidcRoleConverter("roles");
        Jwt jwt = buildJwt(Map.of("roles", List.of("Task.Read", "Employee.Write")));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_TASK.READ", "ROLE_EMPLOYEE.WRITE");
    }
}
