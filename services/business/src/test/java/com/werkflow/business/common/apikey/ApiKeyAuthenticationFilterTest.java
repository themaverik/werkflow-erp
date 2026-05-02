package com.werkflow.business.common.apikey;

import com.werkflow.business.common.apikey.entity.ApiKey;
import com.werkflow.business.common.apikey.filter.ApiKeyAuthenticationFilter;
import com.werkflow.business.common.apikey.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiKeyAuthenticationFilterTest {

    private static final String VALID_RAW_KEY = "test-api-key-secret-12345";
    private static final String VALID_HASH     = sha256Hex(VALID_RAW_KEY);
    private static final String TENANT_ID      = "default";
    private static final String KEY_NAME       = "werkflow-enterprise";

    private ApiKeyAuthenticationFilter filter;

    @Mock private ApiKeyRepository apiKeyRepository;
    @Mock private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new ApiKeyAuthenticationFilter(apiKeyRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- Pass-through cases ---

    @Test
    void noApiKeyHeader_passesThroughWithoutTouchingSecurityContext() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, filterChain);

        verify(filterChain).doFilter(req, res);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(apiKeyRepository);
    }

    // --- Success ---

    @Test
    void validActiveKey_setsAuthenticationWithCorrectTenantAndRole() throws ServletException, IOException {
        when(apiKeyRepository.findByKeyHashAndActiveTrue(VALID_HASH))
                .thenReturn(Optional.of(activeKey()));

        MockHttpServletRequest req = request(VALID_RAW_KEY);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, filterChain);

        verify(filterChain).doFilter(req, res);
        assertEquals(200, res.getStatus());

        ApiKeyAuthenticationToken auth =
                (ApiKeyAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());
        assertEquals(KEY_NAME, auth.getPrincipal());
        assertEquals(TENANT_ID, auth.getTenantId());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_API_CLIENT")));
    }

    // --- Rejection cases ---

    @Test
    void unknownKey_returns401AndDoesNotCallChain() throws ServletException, IOException {
        when(apiKeyRepository.findByKeyHashAndActiveTrue(anyString())).thenReturn(Optional.empty());

        MockHttpServletRequest req = request("unknown-key");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, filterChain);

        assertEquals(401, res.getStatus());
        verifyNoInteractions(filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void expiredKey_returns401() throws ServletException, IOException {
        ApiKey expired = activeKey();
        expired.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
        when(apiKeyRepository.findByKeyHashAndActiveTrue(VALID_HASH)).thenReturn(Optional.of(expired));

        MockHttpServletRequest req = request(VALID_RAW_KEY);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, filterChain);

        assertEquals(401, res.getStatus());
        verifyNoInteractions(filterChain);
    }

    @Test
    void blankKey_returns401WithoutHittingDb() throws ServletException, IOException {
        MockHttpServletRequest req = request("   ");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, filterChain);

        assertEquals(401, res.getStatus());
        verifyNoInteractions(apiKeyRepository);
        verifyNoInteractions(filterChain);
    }

    @Test
    void emptyStringKey_returns401WithoutHittingDb() throws ServletException, IOException {
        MockHttpServletRequest req = request("");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, filterChain);

        assertEquals(401, res.getStatus());
        verifyNoInteractions(apiKeyRepository);
    }

    @Test
    void oversizedKey_returns401WithoutHittingDb() throws ServletException, IOException {
        String bigKey = "x".repeat(513);
        MockHttpServletRequest req = request(bigKey);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, filterChain);

        assertEquals(401, res.getStatus());
        verifyNoInteractions(apiKeyRepository);
    }

    // --- Path exclusions (verify via doFilter — shouldNotFilter is protected) ---

    @Test
    void actuatorPath_skipsRepoLookupEvenWithApiKeyHeader() throws ServletException, IOException {
        // On excluded paths the filter calls filterChain.doFilter directly without touching the repo
        MockHttpServletRequest req = request(VALID_RAW_KEY);
        req.setRequestURI("/actuator/health");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, filterChain);

        verify(filterChain).doFilter(req, res);
        verifyNoInteractions(apiKeyRepository);
    }

    @Test
    void apiPath_withApiKeyHeader_hitsRepository() throws ServletException, IOException {
        when(apiKeyRepository.findByKeyHashAndActiveTrue(VALID_HASH))
                .thenReturn(Optional.of(activeKey()));

        MockHttpServletRequest req = request(VALID_RAW_KEY);
        req.setRequestURI("/api/v1/employees");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, filterChain);

        verify(apiKeyRepository).findByKeyHashAndActiveTrue(VALID_HASH);
    }

    // --- Helpers ---

    private MockHttpServletRequest request(String apiKey) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-API-Key", apiKey);
        return req;
    }

    private ApiKey activeKey() {
        ApiKey key = new ApiKey();
        // Use reflection to set immutable fields for test purposes
        try {
            var keyHashField = ApiKey.class.getDeclaredField("keyHash");
            keyHashField.setAccessible(true);
            keyHashField.set(key, VALID_HASH);

            var tenantField = ApiKey.class.getDeclaredField("tenantId");
            tenantField.setAccessible(true);
            tenantField.set(key, TENANT_ID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        key.setName(KEY_NAME);
        key.setActive(true);
        return key;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
