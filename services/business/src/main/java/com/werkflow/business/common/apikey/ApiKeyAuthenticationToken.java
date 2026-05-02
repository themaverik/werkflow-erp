package com.werkflow.business.common.apikey;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Authentication token for API key-authenticated requests.
 * Carries the key's display name as principal and tenant ID for context propagation.
 */
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final String principal;
    private final String tenantId;

    public ApiKeyAuthenticationToken(String name, String tenantId,
                                     Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = name;
        this.tenantId  = tenantId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public String getTenantId() {
        return tenantId;
    }
}
