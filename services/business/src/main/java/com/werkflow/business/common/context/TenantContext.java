package com.werkflow.business.common.context;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Manages tenant context for the current request.
 * Stores tenantId in ThreadLocal for request-scoped access.
 */
@Component
public class TenantContext {

    private static final ThreadLocal<String> tenantIdHolder = new ThreadLocal<>();

    /**
     * Set the current tenant ID
     */
    public void setTenantId(String tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId cannot be null");
        }
        tenantIdHolder.set(tenantId);
    }

    /**
     * Get the current tenant ID
     * @throws IllegalStateException if tenantId not set for this request
     */
    public String getTenantId() {
        String tenantId = tenantIdHolder.get();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID not set. " +
                "Ensure TenantContextFilter is registered in SecurityConfig");
        }
        return tenantId;
    }

    /**
     * Extract tenant ID from JWT token
     * Looks for "organization_id" claim in JWT
     */
    public String extractTenantIdFromJwt(Jwt jwt) {
        Object orgId = jwt.getClaim("organization_id");
        if (orgId == null) {
            throw new IllegalArgumentException("JWT claim 'organization_id' not found");
        }
        return (String) orgId;
    }

    /**
     * Extract tenant ID from current Authentication
     */
    public String extractTenantIdFromAuthentication(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
            Jwt jwt = (Jwt) jwtAuth.getPrincipal();
            return extractTenantIdFromJwt(jwt);
        }
        throw new IllegalArgumentException("Authentication is not JWT-based");
    }

    /**
     * Clear the tenant ID (called on request completion)
     */
    public void clear() {
        tenantIdHolder.remove();
    }
}
