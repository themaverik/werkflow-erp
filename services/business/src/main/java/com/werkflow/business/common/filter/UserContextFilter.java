package com.werkflow.business.common.filter;

import com.werkflow.business.common.context.UserContext;
import com.werkflow.business.common.identity.UserInfoResolver;
import com.werkflow.business.common.identity.dto.UserInfo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that resolves user identity from the Bearer token and stores it in
 * {@link UserContext} for the duration of the request.
 *
 * <p>Execution order (relative to other filters):
 * <ol>
 *   <li>{@code BearerTokenAuthenticationFilter} — validates JWT signature and populates
 *       {@code SecurityContext}.</li>
 *   <li>{@code TenantContextFilter} — extracts tenant from validated JWT claims.</li>
 *   <li><strong>This filter</strong> — resolves full user profile via
 *       {@link UserInfoResolver} and stores it in {@link UserContext}.</li>
 * </ol>
 *
 * <p>Graceful degradation: if the Authorization header is missing, malformed, or the
 * resolver fails, the filter logs a warning and continues the filter chain without
 * setting the user context. Downstream code that requires user identity should call
 * {@link UserContext#getUserInfo()} which will throw {@link IllegalStateException}
 * if the context is unset — letting the security layer handle the rejection.
 *
 * <p>{@link UserContext#clear()} is always called in the {@code finally} block to
 * prevent ThreadLocal leaks in thread-pool environments.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserContextFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserInfoResolver userInfoResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractBearerToken(request);

            if (token != null) {
                try {
                    UserInfo userInfo = userInfoResolver.resolveUserInfo(token);
                    UserContext.setUserInfo(userInfo);
                    log.debug("UserContext set for keycloakId={}", userInfo.getKeycloakId());
                } catch (Exception e) {
                    // Resolver is designed never to throw, but guard defensively
                    log.warn("UserContextFilter: failed to resolve user info; degrading. cause={}", e.getMessage());
                }
            } else {
                log.debug("UserContextFilter: no Bearer token found in Authorization header");
            }

            filterChain.doFilter(request, response);

        } finally {
            UserContext.clear();
            log.debug("UserContext cleared");
        }
    }

    /**
     * Extracts the raw JWT token from the {@code Authorization: Bearer {token}} header.
     *
     * @return the token string, or null if header is absent or not a Bearer token
     */
    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            return token.isBlank() ? null : token;
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") ||
               path.startsWith("/api-docs") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs");
    }
}
