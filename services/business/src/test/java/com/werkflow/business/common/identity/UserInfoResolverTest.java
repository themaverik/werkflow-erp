package com.werkflow.business.common.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.werkflow.business.common.entity.UserRepository;
import com.werkflow.business.common.identity.dto.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Unit tests for {@link UserInfoResolver}.
 *
 * HTTP calls are mocked via {@link MockRestServiceServer} bound to a real {@link RestTemplate}.
 * {@link UserRepository} is mocked via Mockito.
 * Caffeine caches are injected directly to allow isolation between tests.
 */
@ExtendWith(MockitoExtension.class)
class UserInfoResolverTest {

    private static final String ISSUER = "https://auth.example.com/realms/werkflow";
    private static final String DISCOVERY_URL = ISSUER + "/.well-known/openid-configuration";
    private static final String USERINFO_URL = ISSUER + "/protocol/openid-connect/userinfo";

    private static final String SUB = "550e8400-e29b-41d4-a716-446655440000";
    private static final String DISPLAY_NAME = "Jane Smith";
    private static final String EMAIL = "jane.smith@example.com";

    @Mock
    private UserRepository userRepository;

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private UserInfoResolver resolver;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);

        resolver = new UserInfoResolver(userRepository, restTemplate, objectMapper);
        resolver.initCaches(); // trigger @PostConstruct
    }

    // --- Helpers ---

    /** Builds a minimal JWT (not signed — signature not validated by resolver). */
    private String buildJwt(String sub, String issuer) throws Exception {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes());
        Map<String, String> claims = Map.of("sub", sub, "iss", issuer);
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(claims));
        return header + "." + payload + ".fakesignature";
    }

    private String discoveryResponse() throws Exception {
        return objectMapper.writeValueAsString(Map.of("userinfo_endpoint", USERINFO_URL));
    }

    private String userinfoResponse() throws Exception {
        return objectMapper.writeValueAsString(
                Map.of("sub", SUB, "name", DISPLAY_NAME, "email", EMAIL));
    }

    private void expectDiscovery() throws Exception {
        mockServer.expect(once(), requestTo(DISCOVERY_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(discoveryResponse(), MediaType.APPLICATION_JSON));
    }

    private void expectUserinfo() throws Exception {
        mockServer.expect(once(), requestTo(USERINFO_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(userinfoResponse(), MediaType.APPLICATION_JSON));
    }

    // --- Tests ---

    /**
     * Test 1: Cache miss on first call → fetches from /userinfo, upserts to DB.
     */
    @Test
    void cacheMiss_firstCall_fetchesFromUserinfoAndUpserts() throws Exception {
        String jwt = buildJwt(SUB, ISSUER);
        expectDiscovery();
        expectUserinfo();

        UserInfo result = resolver.resolveUserInfo(jwt);

        assertThat(result.getKeycloakId()).isEqualTo(SUB);
        assertThat(result.getDisplayName()).isEqualTo(DISPLAY_NAME);
        assertThat(result.getEmail()).isEqualTo(EMAIL);

        verify(userRepository, times(1))
                .upsertUser(eq(SUB), eq(DISPLAY_NAME), eq(EMAIL), any());
        mockServer.verify();
    }

    /**
     * Test 2: Cache hit on second call → no additional HTTP calls.
     */
    @Test
    void cacheHit_secondCall_doesNotCallUserinfoAgain() throws Exception {
        String jwt = buildJwt(SUB, ISSUER);
        // Only one round of HTTP expectations — second call must not trigger them
        expectDiscovery();
        expectUserinfo();

        UserInfo first = resolver.resolveUserInfo(jwt);
        UserInfo second = resolver.resolveUserInfo(jwt);

        assertThat(second).isEqualTo(first);
        // upsertUser called only once (on first call)
        verify(userRepository, times(1)).upsertUser(any(), any(), any(), any());
        mockServer.verify(); // verifies no unexpected calls were made
    }

    /**
     * Test 3: OIDC issuer discovery — extracts userinfo_endpoint from .well-known config.
     */
    @Test
    void issuerDiscovery_extractsUserinfoEndpointCorrectly() throws Exception {
        String customUserinfoUrl = "https://auth.example.com/custom/userinfo";
        String customDiscovery = objectMapper.writeValueAsString(
                Map.of("userinfo_endpoint", customUserinfoUrl, "issuer", ISSUER));

        mockServer.expect(once(), requestTo(DISCOVERY_URL))
                .andRespond(withSuccess(customDiscovery, MediaType.APPLICATION_JSON));

        // Inject an empty discovery cache so discovery is triggered
        String discovered = resolver.discoverUserinfoEndpoint(ISSUER);

        assertThat(discovered).isEqualTo(customUserinfoUrl);
        mockServer.verify();
    }

    /**
     * Test 4: Malformed JWT (not 3 parts) → graceful degradation, sub = "unknown".
     */
    @Test
    void malformedJwt_returnsOpaqueUserInfo() {
        UserInfo result = resolver.resolveUserInfo("not.a.valid.jwt.format.with.too.many.parts.here");

        // Malformed JWT: extractSub returns null → keycloakId = "unknown"
        assertThat(result.getKeycloakId()).isEqualTo("unknown");
        assertThat(result.getDisplayName()).isNull();
        assertThat(result.getEmail()).isNull();
        verifyNoInteractions(userRepository);
    }

    /**
     * Test 5: Null JWT → graceful degradation.
     */
    @Test
    void nullJwt_returnsOpaqueUserInfo() {
        UserInfo result = resolver.resolveUserInfo(null);

        assertThat(result.getKeycloakId()).isEqualTo("unknown");
        assertThat(result.getDisplayName()).isNull();
        verifyNoInteractions(userRepository);
    }

    /**
     * Test 6: /userinfo returns 401 → graceful degradation to opaque sub.
     */
    @Test
    void userinfoReturns401_degradesGracefully() throws Exception {
        String jwt = buildJwt(SUB, ISSUER);
        expectDiscovery();

        mockServer.expect(once(), requestTo(USERINFO_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withUnauthorizedRequest());

        UserInfo result = resolver.resolveUserInfo(jwt);

        assertThat(result.getKeycloakId()).isEqualTo(SUB);
        assertThat(result.getDisplayName()).isNull();
        assertThat(result.getEmail()).isNull();
        verifyNoInteractions(userRepository);
        mockServer.verify();
    }

    /**
     * Test 7: /userinfo returns 403 → graceful degradation to opaque sub.
     */
    @Test
    void userinfoReturns403_degradesGracefully() throws Exception {
        String jwt = buildJwt(SUB, ISSUER);
        expectDiscovery();

        mockServer.expect(once(), requestTo(USERINFO_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        UserInfo result = resolver.resolveUserInfo(jwt);

        assertThat(result.getKeycloakId()).isEqualTo(SUB);
        assertThat(result.getDisplayName()).isNull();
        verifyNoInteractions(userRepository);
        mockServer.verify();
    }

    /**
     * Test 8: OIDC discovery endpoint unreachable → graceful degradation.
     */
    @Test
    void discoveryFails_degradesGracefully() throws Exception {
        String jwt = buildJwt(SUB, ISSUER);

        mockServer.expect(once(), requestTo(DISCOVERY_URL))
                .andRespond(withServerError());

        UserInfo result = resolver.resolveUserInfo(jwt);

        assertThat(result.getKeycloakId()).isEqualTo(SUB);
        assertThat(result.getDisplayName()).isNull();
        verifyNoInteractions(userRepository);
        mockServer.verify();
    }

    /**
     * Test 9: display name resolution — "given_name" + "family_name" fallback when "name" absent.
     */
    @Test
    void displayNameResolution_givenAndFamilyName() throws Exception {
        Map<String, Object> claims = Map.of(
                "sub", SUB,
                "given_name", "Jane",
                "family_name", "Smith",
                "email", EMAIL);

        UserInfo result = resolver.buildUserInfo(SUB, claims);

        assertThat(result.getDisplayName()).isEqualTo("Jane Smith");
        assertThat(result.getEmail()).isEqualTo(EMAIL);
    }

    /**
     * Test 10: display name resolution — falls back to email when no name claims present.
     */
    @Test
    void displayNameResolution_emailFallback() {
        Map<String, Object> claims = Map.of("sub", SUB, "email", EMAIL);

        UserInfo result = resolver.buildUserInfo(SUB, claims);

        assertThat(result.getDisplayName()).isEqualTo(EMAIL);
    }

    /**
     * Test 11: display name resolution — falls back to sub when no name or email.
     */
    @Test
    void displayNameResolution_subFallback() {
        Map<String, Object> claims = Map.of("sub", SUB);

        UserInfo result = resolver.buildUserInfo(SUB, claims);

        assertThat(result.getDisplayName()).isEqualTo(SUB);
    }

    /**
     * Test 12: Concurrent requests for the same user trigger only one /userinfo call.
     *
     * Uses a latch to release all threads simultaneously, then verifies upsertUser
     * was called exactly once — Caffeine's atomic get() coalesces concurrent loads.
     */
    @Test
    void concurrentRequestsSameUser_onlyOneUserinfoCall() throws Exception {
        String jwt = buildJwt(SUB, ISSUER);

        // Pre-populate discovery cache to isolate the test to userinfo calls
        resolver.getDiscoveryCache().put(ISSUER, USERINFO_URL);

        // Only one userinfo expectation — concurrent threads must share the result
        mockServer.expect(once(), requestTo(USERINFO_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(userinfoResponse(), MediaType.APPLICATION_JSON));

        int threadCount = 8;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    UserInfo info = resolver.resolveUserInfo(jwt);
                    if (SUB.equals(info.getKeycloakId())) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // count stays zero
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // release all threads simultaneously
        assertThat(doneLatch.await(5, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(threadCount);
        verify(userRepository, times(1)).upsertUser(any(), any(), any(), any());
        mockServer.verify();
    }

    /**
     * Test 13: JWT with only sub (no iss) → graceful degradation (no issuer, can't discover).
     */
    @Test
    void jwtWithoutIssuer_degradesGracefully() throws Exception {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(Map.of("sub", SUB)));
        String jwt = header + "." + payload + ".sig";

        UserInfo result = resolver.resolveUserInfo(jwt);

        assertThat(result.getKeycloakId()).isEqualTo(SUB);
        assertThat(result.getDisplayName()).isNull();
        verifyNoInteractions(userRepository);
    }

    /**
     * Test 14: DB upsert failure does not propagate — UserInfo is still returned.
     */
    @Test
    void databaseUpsertFailure_doesNotPropagateError() throws Exception {
        String jwt = buildJwt(SUB, ISSUER);
        expectDiscovery();
        expectUserinfo();

        doThrow(new RuntimeException("DB unavailable"))
                .when(userRepository).upsertUser(any(), any(), any(), any());

        UserInfo result = resolver.resolveUserInfo(jwt);

        // UserInfo still returned despite DB failure
        assertThat(result.getKeycloakId()).isEqualTo(SUB);
        assertThat(result.getDisplayName()).isEqualTo(DISPLAY_NAME);
        mockServer.verify();
    }
}
