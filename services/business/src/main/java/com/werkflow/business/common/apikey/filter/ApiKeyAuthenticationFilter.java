package com.werkflow.business.common.apikey.filter;

import com.werkflow.business.common.apikey.ApiKeyAuthenticationToken;
import com.werkflow.business.common.apikey.entity.ApiKey;
import com.werkflow.business.common.apikey.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final int MAX_KEY_LENGTH = 512;

    // Reuse MessageDigest per thread — getInstance() is expensive under load
    private static final ThreadLocal<MessageDigest> SHA256 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    });

    private final ApiKeyRepository apiKeyRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String rawKey = request.getHeader(API_KEY_HEADER);
        if (rawKey == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Reject blank or oversized keys before hitting the DB
        if (rawKey.isBlank() || rawKey.length() > MAX_KEY_LENGTH) {
            reject(response, "Invalid API key");
            return;
        }

        byte[] computedHash = sha256Bytes(rawKey);
        String computedHex  = toHex(computedHash);

        Optional<ApiKey> found = apiKeyRepository.findByKeyHashAndActiveTrue(computedHex);

        // Constant-time hash comparison to prevent application-layer timing oracle
        if (found.isEmpty() || !MessageDigest.isEqual(computedHash, fromHex(found.get().getKeyHash()))
                || found.get().isExpired()) {
            log.warn("API key auth failed for request to {}", request.getRequestURI());
            reject(response, "Invalid or expired API key");
            return;
        }

        ApiKey apiKey = found.get();
        ApiKeyAuthenticationToken auth = new ApiKeyAuthenticationToken(
                apiKey.getName(),
                apiKey.getTenantId(),
                List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        log.debug("API key auth succeeded: name='{}' tenant='{}'", apiKey.getName(), apiKey.getTenantId());

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") ||
               path.startsWith("/api-docs") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs");
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

    private byte[] sha256Bytes(String input) {
        MessageDigest digest = SHA256.get();
        digest.reset();
        return digest.digest(input.getBytes(StandardCharsets.UTF_8));
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(64);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private byte[] fromHex(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
