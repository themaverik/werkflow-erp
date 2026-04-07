package com.werkflow.business.common.idempotency.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.werkflow.business.common.context.TenantContext;
import com.werkflow.business.common.idempotency.dto.CachedResponse;
import com.werkflow.business.common.idempotency.exception.IdempotencyException;
import com.werkflow.business.common.idempotency.service.IdempotencyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyFilterTest {

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private TenantContext tenantContext;

    @Mock
    private FilterChain filterChain;

    private IdempotencyFilter filter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        filter = new IdempotencyFilter(idempotencyService, tenantContext, objectMapper);
    }

    @Test
    void testDoFilterInternal_GetRequest_SkipsCache() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(idempotencyService, tenantContext);
    }

    @Test
    void testDoFilterInternal_PostWithoutIdempotencyKey_SkipsCache() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.setContent("{\"name\":\"test\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(idempotencyService, tenantContext);
    }

    @Test
    void testDoFilterInternal_PostWithIdempotencyKey_CacheHit_ReturnsCached() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.addHeader("Idempotency-Key", "key-123");
        request.setContent("{\"name\":\"test\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tenantContext.getTenantId()).thenReturn("tenant-1");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        CachedResponse cached = CachedResponse.builder()
                .body("{\"id\":\"order-1\"}")
                .statusCode(201)
                .headers(headers)
                .build();

        when(idempotencyService.getIfPresent(eq("tenant-1"), eq("key-123"), any()))
                .thenReturn(Optional.of(cached));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getContentAsString()).isEqualTo("{\"id\":\"order-1\"}");
        assertThat(response.getHeader("Content-Type")).isEqualTo("application/json");
        verifyNoInteractions(filterChain);
    }

    @Test
    void testDoFilterInternal_PostWithIdempotencyKey_PayloadMismatch_Returns409() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.addHeader("Idempotency-Key", "key-123");
        request.setContent("{\"name\":\"different\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tenantContext.getTenantId()).thenReturn("tenant-1");
        when(idempotencyService.getIfPresent(eq("tenant-1"), eq("key-123"), any()))
                .thenThrow(new IdempotencyException("Idempotency-Key reused with different payload. Use a new key for different requests."));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_CONFLICT);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("error");
        verifyNoInteractions(filterChain);
    }

    @Test
    void testDoFilterInternal_PostWithIdempotencyKey_CacheMiss_ProceedsAndCaches() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.addHeader("Idempotency-Key", "key-456");
        request.setContent("{\"name\":\"new-order\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tenantContext.getTenantId()).thenReturn("tenant-1");
        when(idempotencyService.getIfPresent(eq("tenant-1"), eq("key-456"), any()))
                .thenReturn(Optional.empty());

        // Simulate controller writing a 201 response
        doAnswer(invocation -> {
            HttpServletResponse resp = invocation.getArgument(1);
            resp.setStatus(201);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"id\":\"order-99\"}");
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
        verify(idempotencyService).store(eq("tenant-1"), eq("key-456"), any(), any(CachedResponse.class));
        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getContentAsString()).contains("order-99");
    }
}
