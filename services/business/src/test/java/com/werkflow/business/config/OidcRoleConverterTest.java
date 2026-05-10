package com.werkflow.business.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OidcRoleConverterTest {

    private final OidcRoleConverter converter = new OidcRoleConverter();

    private Jwt buildJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(c -> c.putAll(claims))
                .build();
    }

    @Test
    void extractRoles_fromRealmAccessRoles_keycloakStandard() {
        Jwt jwt = buildJwt(Map.of("realm_access", Map.of("roles", List.of("super_admin", "hr_manager"))));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_SUPER_ADMIN", "ROLE_HR_MANAGER");
    }

    @Test
    void missingRealmAccess_returnsEmptyList() {
        Jwt jwt = buildJwt(Map.of("sub", "user-123"));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void realmAccessWithoutRoles_returnsEmptyList() {
        Jwt jwt = buildJwt(Map.of("realm_access", Map.of("other", "value")));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void blankRolesInList_areFilteredOut() {
        Jwt jwt = buildJwt(Map.of("realm_access", Map.of("roles", List.of("admin", "", "  "))));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void emptyRolesList_returnsEmptyAuthorities() {
        Jwt jwt = buildJwt(Map.of("realm_access", Map.of("roles", List.of())));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void singleRole_isPrefixedAndUppercased() {
        Jwt jwt = buildJwt(Map.of("realm_access", Map.of("roles", List.of("viewer"))));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_VIEWER");
    }

    @Test
    void realWorldKeycloakPayload_mapsAllRoles() {
        Jwt jwt = buildJwt(Map.of("realm_access", Map.of(
                "roles", List.of("doa_approver_level4", "super_admin", "admin"))));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_DOA_APPROVER_LEVEL4", "ROLE_SUPER_ADMIN", "ROLE_ADMIN");
    }
}
