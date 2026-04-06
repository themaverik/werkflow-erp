package com.werkflow.business.common.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    private TenantContext tenantContext;

    @BeforeEach
    void setUp() {
        tenantContext = new TenantContext();
        tenantContext.clear(); // Clear any previous state
    }

    @Test
    void testSetAndGetTenantId() {
        tenantContext.setTenantId("tenant-123");
        assertEquals("tenant-123", tenantContext.getTenantId());
    }

    @Test
    void testGetTenantIdThrowsWhenNotSet() {
        assertThrows(IllegalStateException.class, () -> tenantContext.getTenantId());
    }

    @Test
    void testClear() {
        tenantContext.setTenantId("tenant-123");
        tenantContext.clear();
        assertThrows(IllegalStateException.class, () -> tenantContext.getTenantId());
    }

    @Test
    void testExtractFromJwtClaim() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("organization_id", "acme-corp");
        claims.put("sub", "user-123");

        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600),
            Collections.singletonMap("alg", "HS256"), claims);

        String tenantId = tenantContext.extractTenantIdFromJwt(jwt);
        assertEquals("acme-corp", tenantId);
    }

    @Test
    void testExtractFromJwtThrowsWhenClaimMissing() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user-123");

        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600),
            Collections.singletonMap("alg", "HS256"), claims);

        assertThrows(IllegalArgumentException.class, () -> tenantContext.extractTenantIdFromJwt(jwt));
    }

    @Test
    void testExtractFromAuthentication() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("organization_id", "acme-corp");
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600),
            Collections.singletonMap("alg", "HS256"), claims);

        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt,
            Collections.emptyList());

        String tenantId = tenantContext.extractTenantIdFromAuthentication(auth);
        assertEquals("acme-corp", tenantId);
    }
}
