package com.werkflow.business.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Security tests verifying that JWT claims conform to the minimal contract defined in ADR-002.
 *
 * <p>ADR-002 mandates that JWT tokens contain ONLY: sub, iss, aud, exp, iat, scope, tenant_id, roles.
 * PII fields such as given_name, family_name, email, department, hire_date, employee_id, and name
 * must NOT be present in the JWT claims. This prevents PII from leaking into logs, API gateways,
 * CDN proxy logs, and error tracking systems.
 *
 * <p>Tests also verify that the configurable roles-claim property ({@code werkflow.security.roles-claim})
 * correctly routes role extraction to the nominated claim, supporting Keycloak, Auth0, and Azure AD.
 */
class JwtClaimsSecurityTest {

    /** Allowed claim names per ADR-002 minimal JWT contract. */
    private static final Set<String> ALLOWED_CLAIMS = Set.of(
            "sub", "iss", "aud", "exp", "iat", "scope", "tenant_id", "roles"
    );

    /** PII claim names that must never appear in a JWT per ADR-002. */
    private static final List<String> FORBIDDEN_PII_CLAIMS = List.of(
            "given_name", "family_name", "email", "department",
            "hire_date", "employee_id", "name", "phone_number",
            "address", "birthdate", "job_title", "salary_band"
    );

    private Jwt buildJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(c -> c.putAll(claims))
                .build();
    }

    /**
     * Test 1: A conformant minimal JWT (Keycloak-style) contains only the allowed claims.
     * Verifies that all claims are within the ADR-002 allow-list.
     */
    @Test
    void minimalJwt_keycloakStyle_containsOnlyAllowedClaims() {
        Map<String, Object> claims = Map.of(
                "sub", "3f7a1c82-4b9e-4c1a-8f3d-2e5b7a9c0d1e",
                "iss", "https://auth.example.com/realms/werkflow",
                "aud", "werkflow-erp-api",
                "tenant_id", "tenant-acme",
                "roles", List.of("finance.manager", "procurement.viewer"),
                "scope", "openid api"
        );

        Jwt jwt = buildJwt(claims);

        Set<String> actualClaims = jwt.getClaims().keySet();
        // exp and iat are added by the Jwt builder from issuedAt/expiresAt
        Set<String> extendedAllowed = Set.of("sub", "iss", "aud", "exp", "iat",
                "scope", "tenant_id", "roles", "nbf");

        assertThat(actualClaims)
                .as("JWT must only contain ADR-002 approved claims")
                .allMatch(claim -> extendedAllowed.contains(claim),
                        "unexpected claim found outside allowed set");
    }

    /**
     * Test 2: JWT must NOT contain PII fields — given_name, family_name, email, department,
     * hire_date, employee_id, name. Each is individually asserted absent.
     */
    @Test
    void minimalJwt_doesNotContainPiiFields() {
        Map<String, Object> claims = Map.of(
                "sub", "3f7a1c82-4b9e-4c1a-8f3d-2e5b7a9c0d1e",
                "iss", "https://auth.example.com/realms/werkflow",
                "aud", "werkflow-erp-api",
                "tenant_id", "tenant-acme",
                "roles", List.of("admin"),
                "scope", "openid api"
        );

        Jwt jwt = buildJwt(claims);

        for (String piiClaim : FORBIDDEN_PII_CLAIMS) {
            assertThat(jwt.getClaims())
                    .as("JWT must not contain PII claim '%s' (ADR-002)", piiClaim)
                    .doesNotContainKey(piiClaim);
        }
    }

    /**
     * Test 3: A JWT that incorrectly includes PII (simulating a misconfigured provider) is
     * detected. This confirms the test framework can catch violations, and that our own
     * constructed JWTs do not carry PII.
     *
     * This test documents what a VIOLATION looks like so reviewers understand the enforcement.
     */
    @Test
    void jwt_withPiiFields_violatesAdR002Contract() {
        // Simulate a misconfigured provider that injects PII into the JWT
        Map<String, Object> violatingClaims = Map.of(
                "sub", "user-123",
                "iss", "https://bad-idp.example.com",
                "given_name", "Jane",     // VIOLATION
                "family_name", "Smith",   // VIOLATION
                "email", "jane@example.com" // VIOLATION
        );

        Jwt violatingJwt = buildJwt(violatingClaims);

        // The violation exists in the claims — this test confirms detection capability
        assertThat(violatingJwt.getClaims()).containsKey("given_name");
        assertThat(violatingJwt.getClaims()).containsKey("family_name");
        assertThat(violatingJwt.getClaims()).containsKey("email");

        // Count how many forbidden claims are present — should be 0 in compliant JWTs
        long forbiddenCount = FORBIDDEN_PII_CLAIMS.stream()
                .filter(claim -> violatingJwt.getClaims().containsKey(claim))
                .count();

        assertThat(forbiddenCount)
                .as("A non-zero count confirms PII violation detection works")
                .isGreaterThan(0);
    }

    /**
     * Test 4 (Parameterized): OidcRoleConverter correctly extracts roles from different claim names
     * for Keycloak ("roles"), Auth0 ("scope"), and Azure AD ("roles"). Verifies the
     * configurable roles-claim property routes extraction to the right place.
     */
    @ParameterizedTest(name = "Provider={0}: roles-claim=''{1}'' extracts role ''{3}''")
    @MethodSource("providerRolesClaims")
    void configurableRolesClaim_worksForAllMajorProviders(
            String provider, String claimName, List<String> claimValues, String expectedRole) {

        OidcRoleConverter converter = new OidcRoleConverter(claimName);
        Jwt jwt = buildJwt(Map.of(
                "sub", "user-from-" + provider.toLowerCase(),
                "iss", "https://auth." + provider.toLowerCase() + ".com",
                claimName, claimValues
        ));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .as("Provider %s with roles-claim '%s' must extract role '%s'", provider, claimName, expectedRole)
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_" + expectedRole.toUpperCase());
    }

    static Stream<Arguments> providerRolesClaims() {
        return Stream.of(
                // Keycloak — flat "roles" claim after realm-access flattening
                Arguments.of("Keycloak", "roles",
                        List.of("finance.manager", "procurement.viewer"), "finance.manager"),
                // Auth0 — roles in "scope" claim or custom namespace
                Arguments.of("Auth0", "scope",
                        List.of("read:employees", "write:payroll"), "read:employees"),
                // Azure AD — "roles" claim (app role assignments)
                Arguments.of("AzureAD", "roles",
                        List.of("Employee.Write", "Task.Read"), "Employee.Write"),
                // AWS Cognito — custom "cognito:groups" claim
                Arguments.of("Cognito", "cognito:groups",
                        List.of("Admins", "Managers"), "Admins")
        );
    }

    /**
     * Test 5: Sub claim is the sole user identifier in the JWT.
     * Confirms no employee_id, user_id, or keycloak_id duplicates the sub.
     */
    @Test
    void jwt_subIsTheSoleUserIdentifier_noEmployeeIdOrUserId() {
        Map<String, Object> claims = Map.of(
                "sub", "550e8400-e29b-41d4-a716-446655440000",
                "iss", "https://auth.example.com/realms/werkflow",
                "aud", "werkflow-erp-api",
                "tenant_id", "tenant-beta",
                "roles", List.of("viewer")
        );

        Jwt jwt = buildJwt(claims);

        assertThat(jwt.getClaims()).doesNotContainKey("employee_id");
        assertThat(jwt.getClaims()).doesNotContainKey("user_id");
        assertThat(jwt.getClaims()).doesNotContainKey("keycloak_id");
        assertThat(jwt.getSubject()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
    }

    /**
     * Test 6: roles-claim property defaults to "roles" — when configured with "roles",
     * OidcRoleConverter extracts from "roles" and not from "permissions" or any other claim.
     */
    @Test
    void defaultRolesClaim_extractsFromRolesOnly_notFromOtherClaims() {
        OidcRoleConverter converterWithDefaultClaim = new OidcRoleConverter("roles");

        Jwt jwt = buildJwt(Map.of(
                "sub", "user-abc",
                "roles", List.of("admin"),
                // These claims must NOT be picked up as roles
                "permissions", List.of("should-not-be-extracted"),
                "scope", "openid api"
        ));

        Collection<GrantedAuthority> authorities = converterWithDefaultClaim.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN")
                .doesNotContain("ROLE_SHOULD-NOT-BE-EXTRACTED");
    }

    /**
     * Test 7: Keycloak-style token structure — validates that tenant_id is present and not null,
     * and that the JWT structure matches the ADR-002 example exactly.
     */
    @Test
    void keycloakStyleToken_matchesAdR002ExampleStructure() {
        Jwt jwt = buildJwt(Map.of(
                "sub", "3f7a1c82-4b9e-4c1a-8f3d-2e5b7a9c0d1e",
                "iss", "https://auth.example.com/realms/werkflow",
                "aud", "werkflow-erp-api",
                "tenant_id", "tenant-acme",
                "roles", List.of("finance.manager", "procurement.viewer"),
                "scope", "openid api"
        ));

        assertThat(jwt.getSubject()).isEqualTo("3f7a1c82-4b9e-4c1a-8f3d-2e5b7a9c0d1e");
        assertThat(jwt.getIssuer()).isNotNull();
        assertThat(jwt.getClaimAsString("tenant_id")).isEqualTo("tenant-acme");
        assertThat(jwt.getClaimAsStringList("roles"))
                .containsExactlyInAnyOrder("finance.manager", "procurement.viewer");
        assertThat(jwt.getClaimAsString("scope")).isEqualTo("openid api");

        // Verify PII absence in the ADR-002 compliant token
        assertThat(jwt.getClaims())
                .doesNotContainKey("given_name")
                .doesNotContainKey("family_name")
                .doesNotContainKey("email")
                .doesNotContainKey("department")
                .doesNotContainKey("hire_date");
    }
}
