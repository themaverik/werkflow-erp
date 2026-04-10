package com.werkflow.business.security;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.werkflow.business.common.entity.UserRepository;
import com.werkflow.business.common.identity.UserInfoResolver;
import com.werkflow.business.common.identity.dto.UserInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Security tests verifying that PII and raw JWT tokens do not appear in application logs.
 *
 * <p>ADR-002 security contract:
 * <ul>
 *   <li>JWT tokens (Bearer values) must never be logged in full.</li>
 *   <li>PII (given_name, family_name, email, department) must not appear in logs,
 *       even if a misconfigured provider injects them into the JWT payload.</li>
 *   <li>UserInfoResolver log messages must not expose raw JWT values or userinfo endpoint
 *       response bodies — only safe identifiers (sub claim, status codes) are permitted.</li>
 * </ul>
 *
 * <p>Log capture is implemented via a Logback {@link ListAppender} injected into the
 * {@link UserInfoResolver} logger. This avoids the need for mockStatic and works reliably
 * across test isolation boundaries.
 */
@ExtendWith(MockitoExtension.class)
class JwtPiiLoggingSecurityTest {

    private static final String ISSUER = "https://auth.example.com/realms/werkflow";
    private static final String SUB = "550e8400-e29b-41d4-a716-446655440000";

    @Mock
    private UserRepository userRepository;

    private UserInfoResolver resolver;
    private MockRestServiceServer mockServer;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger resolverLogger;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        resolver = new UserInfoResolver(userRepository, restTemplate, objectMapper);

        // initCaches() is package-private; call via reflection to avoid forcing public visibility
        Method initCaches = ReflectionUtils.findMethod(UserInfoResolver.class, "initCaches");
        assert initCaches != null;
        ReflectionUtils.makeAccessible(initCaches);
        initCaches.invoke(resolver);

        // Attach a ListAppender to the UserInfoResolver logger to capture log output
        resolverLogger = (Logger) LoggerFactory.getLogger(UserInfoResolver.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        resolverLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        resolverLogger.detachAppender(logAppender);
        logAppender.stop();
    }

    // --- Helpers ---

    /**
     * Builds a minimal JWT with the given payload claims. The token value itself is a
     * recognizable sentinel so we can assert it never appears in logs verbatim.
     */
    private String buildJwt(Map<String, Object> payloadClaims) throws Exception {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(payloadClaims));
        // Use a recognizable fake signature so we can grep for it
        return header + "." + payload + ".FAKESIG_SENTINEL_VALUE";
    }

    private List<String> capturedLogMessages() {
        return logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    // --- Tests ---

    /**
     * Test 1: When OIDC discovery fails (no reachable server), the logged warning
     * must not contain the raw JWT Bearer value. Only the sub or issuer may appear.
     */
    @Test
    void onDiscoveryFailure_loggedMessages_doNotContainRawJwtToken() throws Exception {
        String jwt = buildJwt(Map.of("sub", SUB, "iss", ISSUER));
        String discoveryUrl = ISSUER + "/.well-known/openid-configuration";

        // Discovery returns 503 — triggers the warning log path
        mockServer.expect(once(), requestTo(discoveryUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        resolver.resolveUserInfo(jwt);

        List<String> logs = capturedLogMessages();

        // The full JWT (all three segments) must never appear in any log line
        String fullJwtToken = jwt;
        assertThat(logs)
                .as("Raw JWT token value must not appear in any log line")
                .noneMatch(line -> line.contains(fullJwtToken));

        // The fake signature sentinel must not appear in logs
        assertThat(logs)
                .as("JWT signature segment must not appear in logs")
                .noneMatch(line -> line.contains("FAKESIG_SENTINEL_VALUE"));
    }

    /**
     * Test 2: A JWT whose payload contains PII (simulating a misconfigured provider) —
     * the PII values must not appear in UserInfoResolver log output.
     * The resolver only ever logs the sub claim, not the decoded payload.
     */
    @Test
    void jwtWithPiiInPayload_loggedMessages_doNotExposePiiValues() throws Exception {
        // Simulate a misconfigured provider that puts PII in the JWT payload
        Map<String, Object> piiPayload = Map.of(
                "sub", SUB,
                "iss", ISSUER,
                "given_name", "Jane",
                "family_name", "Smith",
                "email", "jane.smith@acme.com",
                "department", "Finance"
        );

        String jwt = buildJwt(piiPayload);
        String discoveryUrl = ISSUER + "/.well-known/openid-configuration";

        // Discovery returns 503 — triggers warning log path; PII must still not leak
        mockServer.expect(once(), requestTo(discoveryUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        resolver.resolveUserInfo(jwt);

        List<String> logs = capturedLogMessages();

        assertThat(logs)
                .as("given_name value must not appear in logs")
                .noneMatch(line -> line.contains("Jane"));

        assertThat(logs)
                .as("family_name value must not appear in logs")
                .noneMatch(line -> line.contains("Smith"));

        assertThat(logs)
                .as("email value must not appear in logs")
                .noneMatch(line -> line.contains("jane.smith@acme.com"));

        assertThat(logs)
                .as("department value must not appear in logs")
                .noneMatch(line -> line.contains("Finance"));
    }

    /**
     * Test 3: On a malformed JWT (wrong number of segments), the warning log must
     * not echo back the malformed token value — only a generic structural error is logged.
     */
    @Test
    void malformedJwt_loggedWarning_doesNotEchoTokenValue() {
        String malformedJwt = "not-a-valid.jwt";

        resolver.resolveUserInfo(malformedJwt);

        List<String> logs = capturedLogMessages();

        assertThat(logs)
                .as("Malformed JWT value must not be echoed in log messages")
                .noneMatch(line -> line.contains("not-a-valid.jwt"));
    }

    /**
     * Test 4: On a null JWT, the logged warning must contain no sensitive data — only
     * a safe diagnostic message about the missing sub claim.
     */
    @Test
    void nullJwt_loggedWarning_containsNoSensitiveData() {
        resolver.resolveUserInfo(null);

        List<String> logs = capturedLogMessages();

        // At least one warning should be logged (can't extract sub)
        assertThat(logs).isNotEmpty();

        // No Bearer, no PII, no token fragments in the message
        assertThat(logs)
                .as("Null JWT log must not contain 'Bearer'")
                .noneMatch(line -> line.toLowerCase().contains("bearer"));

        assertThat(logs)
                .as("Null JWT log must not contain token-like content")
                .noneMatch(line -> line.contains("eyJ"));
    }

    /**
     * Test 5: Logs from the resolver only contain safe structural information.
     * Specifically, the sub claim (a UUID/opaque ID, not PII) may appear, but
     * no full name, email, or other GDPR-regulated data should.
     */
    @Test
    void resolverLogs_onError_containOnlySafeIdentifiers() throws Exception {
        String jwt = buildJwt(Map.of("sub", SUB, "iss", ISSUER));
        String discoveryUrl = ISSUER + "/.well-known/openid-configuration";

        // Discovery returns 503 — triggers warning log path
        mockServer.expect(once(), requestTo(discoveryUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        resolver.resolveUserInfo(jwt);

        List<String> logs = capturedLogMessages();

        // Sub (opaque UUID) may appear — it's a stable non-PII identifier in ADR-002
        // PII must not appear
        assertThat(logs)
                .as("Email PII must not appear in resolver logs")
                .noneMatch(line -> line.contains("@") && line.contains(".com"));

        assertThat(logs)
                .as("Hire date patterns must not appear in logs")
                .noneMatch(line -> line.matches(".*\\d{4}-\\d{2}-\\d{2}.*hire.*"));
    }

    /**
     * Test 6: On a cache hit, the resolver produces no log output — the display name
     * is retrieved locally without any HTTP calls or log events. The second call to
     * resolveUserInfo for the same sub must be silent (no warnings, no endpoint URLs logged).
     */
    @Test
    void cacheHit_producesNoLogOutput_noEndpointUrlInLogs() throws Exception {
        String discoveryUrl = ISSUER + "/.well-known/openid-configuration";
        String userinfoUrl = ISSUER + "/protocol/openid-connect/userinfo";

        // Seed the cache via a first successful call
        mockServer.expect(once(), requestTo(discoveryUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        objectMapper.writeValueAsString(Map.of("userinfo_endpoint", userinfoUrl)),
                        MediaType.APPLICATION_JSON));
        mockServer.expect(once(), requestTo(userinfoUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        objectMapper.writeValueAsString(Map.of(
                                "sub", SUB, "name", "Jane Smith", "email", "jane@example.com")),
                        MediaType.APPLICATION_JSON));

        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(Map.of("sub", SUB, "iss", ISSUER)));
        String jwt = header + "." + payload + ".sig";

        // First call — populates cache (may log discovery/userinfo activity)
        resolver.resolveUserInfo(jwt);
        mockServer.verify();

        // Clear captured logs — we only care about the second call
        logAppender.list.clear();

        // Second call — cache hit, must produce zero log output
        UserInfo result = resolver.resolveUserInfo(jwt);

        assertThat(result.getDisplayName()).isEqualTo("Jane Smith");

        List<String> logsAfterCacheHit = capturedLogMessages();
        assertThat(logsAfterCacheHit)
                .as("Cache hit must produce no log messages (no endpoint URLs, no PII)")
                .isEmpty();
    }
}
