package com.werkflow.business.common.filter;

import com.werkflow.business.common.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that extracts tenantId from JWT and stores in TenantContext (ThreadLocal)
 * Must be registered in SecurityFilterChain after authentication filters
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantContextFilter extends OncePerRequestFilter {

    private final TenantContext tenantContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
        try {
            // Extract authentication from SecurityContext (set by OAuth2 filters)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()) {
                try {
                    String tenantId = tenantContext.extractTenantIdFromAuthentication(authentication);
                    tenantContext.setTenantId(tenantId);
                    log.debug("Set tenant context to: {}", tenantId);
                } catch (Exception e) {
                    log.warn("Failed to extract tenant ID from authentication: {}", e.getMessage());
                    // If tenantId extraction fails, request is rejected by SecurityConfig anyway
                }
            } else {
                log.debug("No authentication found in SecurityContext");
            }

            // Continue filter chain
            filterChain.doFilter(request, response);

        } finally {
            // Always clear tenant context after request completes
            tenantContext.clear();
            log.debug("Cleared tenant context");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Don't process for public endpoints (swagger, actuator, etc)
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") ||
               path.startsWith("/api-docs") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs");
    }
}
