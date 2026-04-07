package com.werkflow.business.common.idempotency.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.werkflow.business.common.idempotency.dto.CachedResponse;
import com.werkflow.business.common.idempotency.exception.IdempotencyException;
import com.werkflow.business.common.idempotency.service.IdempotencyService;
import com.werkflow.business.common.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyFilter.class);

    private final IdempotencyService idempotencyService;
    private final TenantContext tenantContext;
    private final ObjectMapper objectMapper;

    public IdempotencyFilter(IdempotencyService idempotencyService, TenantContext tenantContext,
                             ObjectMapper objectMapper) {
        this.idempotencyService = idempotencyService;
        this.tenantContext = tenantContext;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String method = request.getMethod();

        // Only process POST and PUT
        if (!("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method))) {
            chain.doFilter(request, response);
            return;
        }

        // Check for Idempotency-Key header (optional)
        String idempotencyKey = request.getHeader("Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        // Get tenant ID from TenantContext
        String tenantId = tenantContext.getTenantId();
        if (tenantId == null) {
            logger.warn("IdempotencyFilter: TenantContext.getTenantId() returned null; bypassing idempotency check");
            chain.doFilter(request, response);
            return;
        }

        // Wrap FIRST so downstream can still read the body via getInputStream/getReader
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);

        // Read body from the wrapper — downstream can re-read via the cached wrapper
        String requestPayload;
        try {
            StringBuilder sb = new StringBuilder();
            try (java.io.BufferedReader reader = wrappedRequest.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            requestPayload = sb.toString();
        } catch (Exception e) {
            logger.warn("Failed to capture request body", e);
            chain.doFilter(wrappedRequest, response);
            return;
        }

        // Check cache
        try {
            Optional<CachedResponse> cached = idempotencyService.getIfPresent(tenantId, idempotencyKey, requestPayload);
            if (cached.isPresent()) {
                CachedResponse cachedResp = cached.get();
                response.setStatus(cachedResp.getStatusCode());

                // Set headers
                if (cachedResp.getHeaders() != null) {
                    cachedResp.getHeaders().forEach(response::addHeader);
                }

                // Write body
                response.getWriter().write(cachedResp.getBody());
                return;  // Skip controller
            }
        } catch (IdempotencyException e) {
            // Payload mismatch — return 409 Conflict
            logger.info("Idempotency validation failed: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.setContentType("application/json");
            Map<String, String> errorBody = Map.of("error", e.getMessage());
            response.getWriter().write(objectMapper.writeValueAsString(errorBody));
            return;
        }

        // Cache miss: proceed to controller, capture response
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            // After controller execution, store response if successful (2xx)
            if (wrappedResponse.getStatus() >= 200 && wrappedResponse.getStatus() < 300) {
                try {
                    CachedResponse responseToCache = new CachedResponse();
                    responseToCache.setBody(new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8));
                    responseToCache.setStatusCode(wrappedResponse.getStatus());
                    responseToCache.setHeaders(extractHeaders(wrappedResponse));

                    idempotencyService.store(tenantId, idempotencyKey, requestPayload, responseToCache);
                } catch (Exception e) {
                    logger.warn("Failed to store idempotency record", e);
                    // Don't fail the request if caching fails
                }
            }

            wrappedResponse.copyBodyToResponse();
        }
    }

    private Map<String, String> extractHeaders(HttpServletResponse response) {
        Map<String, String> headers = new HashMap<>();
        for (String headerName : response.getHeaderNames()) {
            Collection<String> values = response.getHeaders(headerName);
            if (!values.isEmpty()) {
                headers.put(headerName, String.join(",", values));
            }
        }
        return headers;
    }
}
