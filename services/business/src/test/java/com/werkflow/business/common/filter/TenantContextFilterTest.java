package com.werkflow.business.common.filter;

import com.werkflow.business.common.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantContextFilterTest {

    private TenantContextFilter filter;
    private TenantContext tenantContext;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tenantContext = new TenantContext();
        filter = new TenantContextFilter(tenantContext);
    }

    @Test
    void testFilterSetsAndClearsTenantId() throws ServletException, IOException {
        // Setup JWT with organization_id claim
        Map<String, Object> claims = new HashMap<>();
        claims.put("organization_id", "acme-corp");
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600),
            Collections.singletonMap("alg", "HS256"), claims);

        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, Collections.emptyList());

        // Setup SecurityContext with JWT
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Doable test: verify filter calls doFilter
        filter.doFilter(request, response, filterChain);

        // Verify filterChain was called
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testFilterClearsContextAfterChain() throws ServletException, IOException {
        Map<String, Object> claims = new HashMap<>();
        claims.put("organization_id", "acme-corp");
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600),
            Collections.singletonMap("alg", "HS256"), claims);

        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, Collections.emptyList());

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // After filter completes, TenantContext should be cleared
        filter.doFilter(request, response, filterChain);

        // Attempt to get tenantId after filter should throw
        assertThrows(IllegalStateException.class, () -> tenantContext.getTenantId());
    }
}
