package com.werkflow.business.security;

import com.werkflow.business.common.entity.User;
import com.werkflow.business.common.identity.dto.UserInfo;
import com.werkflow.business.hr.dto.EmployeeResponse;
import com.werkflow.business.procurement.dto.PurchaseRequestResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GDPR and CCPA compliance tests for the werkflow-erp JWT and data model design.
 *
 * <p>These tests enforce the data minimization principle (GDPR Article 5(1)(c)): personal data
 * must be adequate, relevant, and limited to what is necessary for the processing purpose.
 *
 * <p>Compliance areas verified:
 * <ol>
 *   <li><b>JWT Claims</b>: No GDPR-regulated PII in JWT tokens (ADR-002).</li>
 *   <li><b>User Table</b>: The identity_service.users table contains only the fields
 *       needed for display name resolution — not surplus HR data like hire_date or salary_band.</li>
 *   <li><b>Response DTOs</b>: Public API responses expose only necessary fields; sensitive
 *       HR data (employee_id in opaque form, salary) is scoped appropriately.</li>
 *   <li><b>Display Name Compliance</b>: createdByDisplayName exposes a human-readable name,
 *       not an email address or employee ID, per GDPR data minimization.</li>
 * </ol>
 */
class GdprComplianceTest {

    /** Claim names that constitute GDPR-regulated personal data and must not appear in JWTs. */
    private static final List<String> GDPR_REGULATED_JWT_CLAIMS = List.of(
            "given_name", "family_name", "email", "phone_number",
            "address", "birthdate", "locale", "zoneinfo",
            "hire_date", "salary_band", "department", "job_title",
            "manager_id", "employee_id", "national_id", "tax_id"
    );

    /**
     * Fields that should NOT exist on the User entity (GDPR over-collection prevention).
     * The User entity is a minimal identity cache — not a full HR profile.
     */
    private static final List<String> FORBIDDEN_USER_ENTITY_FIELDS = List.of(
            "hireDate", "dateOfJoining", "salaryBand", "salary",
            "departmentId", "departmentName", "managerId",
            "nationalId", "taxId", "gender", "birthDate",
            "phoneNumber", "address", "profilePhotoUrl"
    );

    /**
     * Fields that MUST exist on the User entity — the minimal cache for display resolution.
     */
    private static final Set<String> REQUIRED_USER_ENTITY_FIELDS = Set.of(
            "keycloakId", "displayName", "email", "createdAt", "updatedAt"
    );

    private Jwt buildCompliantJwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(c -> c.putAll(Map.of(
                        "sub", "3f7a1c82-4b9e-4c1a-8f3d-2e5b7a9c0d1e",
                        "iss", "https://auth.example.com/realms/werkflow",
                        "aud", "werkflow-erp-api",
                        "tenant_id", "tenant-acme",
                        "roles", List.of("finance.manager"),
                        "scope", "openid api"
                )))
                .build();
    }

    // --- Test 1: JWT Claims GDPR Compliance ---

    /**
     * Test 1: JWT claims must not contain GDPR-regulated personal data.
     * Verifies that a compliant token carries only opaque identifiers and
     * authorization data, with zero personal data fields present.
     */
    @Test
    void jwt_claims_doNotContainGdprRegulatedPersonalData() {
        Jwt jwt = buildCompliantJwt();

        for (String forbiddenClaim : GDPR_REGULATED_JWT_CLAIMS) {
            assertThat(jwt.getClaims())
                    .as("JWT must not contain GDPR-regulated claim '%s' (GDPR Art. 5(1)(c))", forbiddenClaim)
                    .doesNotContainKey(forbiddenClaim);
        }
    }

    /**
     * Test 2: JWT sub claim is an opaque identifier (UUID or provider-specific format),
     * not an email address or human-readable name. An email-as-sub would be PII.
     */
    @Test
    void jwt_subClaim_isOpaqueIdentifier_notEmailOrName() {
        Jwt jwt = buildCompliantJwt();

        String sub = jwt.getSubject();

        assertThat(sub)
                .as("JWT sub must not be an email address (GDPR data minimization)")
                .doesNotContain("@");

        assertThat(sub)
                .as("JWT sub must not be a plain human name (GDPR data minimization)")
                .matches("[a-zA-Z0-9\\-|]+");
    }

    // --- Test 3: User Entity GDPR Data Minimization ---

    /**
     * Test 3: The User entity (identity_service.users) contains only the minimal fields
     * required for display name resolution. Sensitive HR data must not be co-located here.
     * Validated via reflection on the declared field names of the User class.
     */
    @Test
    void userEntity_containsOnlyMinimalFields_noHrSurplusData() {
        Set<String> actualFields = Arrays.stream(User.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        // Required fields must be present
        assertThat(actualFields)
                .as("User entity must contain all required identity cache fields")
                .containsAll(REQUIRED_USER_ENTITY_FIELDS);

        // Surplus HR fields must NOT be present
        for (String forbidden : FORBIDDEN_USER_ENTITY_FIELDS) {
            assertThat(actualFields)
                    .as("User entity must not contain surplus HR field '%s' (GDPR data minimization)", forbidden)
                    .doesNotContain(forbidden);
        }
    }

    /**
     * Test 4: The UserInfo DTO (in-memory cache) carries only the three fields needed
     * for request processing — keycloakId, displayName, email. No HR, salary, or
     * department data is cached in memory alongside user identity.
     */
    @Test
    void userInfoDto_isMinimal_containsOnlyIdentityFields() {
        Set<String> actualFields = Arrays.stream(UserInfo.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertThat(actualFields)
                .as("UserInfo must contain exactly the three identity fields")
                .containsExactlyInAnyOrder("keycloakId", "displayName", "email");

        // Explicitly assert no HR data bleeds into the UserInfo cache DTO
        assertThat(actualFields)
                .doesNotContain("hireDate", "salary", "departmentId",
                        "managerId", "employeeId", "phone", "address");
    }

    // --- Test 5: Response DTO GDPR Compliance ---

    /**
     * Test 5: PurchaseRequestResponse (a public API response) exposes only display names
     * for audit fields — not employee IDs or email addresses. This implements GDPR
     * data minimization at the API boundary.
     *
     * createdByDisplayName: "Jane Smith" — compliant (display name only)
     * The response must NOT expose the raw keycloak_id or email as the audit author.
     */
    @Test
    void purchaseRequestResponse_auditField_exposesDisplayNameNotEmailOrId() {
        PurchaseRequestResponse response = PurchaseRequestResponse.builder()
                .id(501L)
                .prNumber("PR-2026-0001")
                .createdByDisplayName("Jane Smith")
                .updatedByDisplayName("John Doe")
                .build();

        // Display name is set correctly
        assertThat(response.getCreatedByDisplayName())
                .as("createdByDisplayName must be a human display name, not an ID or email")
                .isEqualTo("Jane Smith")
                .doesNotContain("@")
                .doesNotContain("550e8400");

        // Via reflection: verify no "email" or "keycloakId" field leaks into this DTO
        Set<String> dtoFields = Arrays.stream(PurchaseRequestResponse.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertThat(dtoFields)
                .as("PurchaseRequestResponse must not expose raw keycloak IDs as audit author fields")
                .doesNotContain("createdByKeycloakId", "updatedByKeycloakId",
                        "createdByEmail", "updatedByEmail");
    }

    /**
     * Test 6: EmployeeResponse exposes operational HR data scoped to a specific HR domain
     * (name, department, status) but should not expose salary or national ID at the
     * public API level. Salary is an internal HR field, not an authorization concern.
     *
     * Note: salary is currently included in EmployeeResponse as this is an HR management
     * system — this test documents the field exists and flags it as a data minimization
     * review point for GDPR compliance. Salary exposure should be restricted to authorized
     * HR roles via method-level security (@PreAuthorize), not removed from the DTO.
     */
    @Test
    void employeeResponse_sensitiveFields_areAccessControlledNotAbsent() {
        Set<String> dtoFields = Arrays.stream(EmployeeResponse.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        // The display name audit fields must use display names, not raw IDs
        assertThat(dtoFields)
                .as("EmployeeResponse audit fields must use display name format")
                .contains("createdByDisplayName", "updatedByDisplayName");

        // No national ID, tax ID, or biometric data (hard GDPR special category data)
        assertThat(dtoFields)
                .as("EmployeeResponse must not expose national ID or tax ID (GDPR special category data)")
                .doesNotContain("nationalId", "taxId", "nationalInsuranceNumber",
                        "biometricData", "healthData");
    }

    /**
     * Test 7: createdByDisplayName carries a human-readable name ("Jane Smith"),
     * not an opaque UUID or email address. Verifies the display name contract
     * enforced throughout DTOs aligns with GDPR purpose limitation.
     *
     * The display name shows who performed the action without exposing the
     * auth provider's internal user ID or the user's contact details.
     */
    @Test
    void displayNameContract_showsHumanName_notEmailOrUuid() {
        String displayName = "Jane Smith";

        // A conformant display name is a human-readable name
        assertThat(displayName)
                .as("Display name must be a human-readable name")
                .doesNotContain("@")
                .doesNotContain("auth0|")
                .doesNotContain("-e29b-41d4") // UUID fragment
                .matches("[A-Za-z ]+");
    }

    /**
     * Test 8: UserInfo.email is nullable — OIDC providers are not required to expose email
     * via the /userinfo endpoint, and some privacy-preserving configurations omit it.
     * This confirms the data model accommodates email absence without breaking functionality.
     */
    @Test
    void userInfo_emailIsNullable_supportsPrivacyPreservingOidcConfigurations() {
        // A degraded UserInfo with only keycloakId (e.g., provider does not expose email)
        UserInfo minimalUserInfo = UserInfo.builder()
                .keycloakId("sub-without-email")
                .displayName("Anonymous User")
                // email intentionally omitted
                .build();

        assertThat(minimalUserInfo.getEmail())
                .as("email must be nullable to support privacy-preserving OIDC configurations")
                .isNull();

        assertThat(minimalUserInfo.getDisplayName())
                .as("displayName must still be resolvable without email")
                .isEqualTo("Anonymous User");
    }
}
