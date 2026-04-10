package com.werkflow.business.common.identity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.werkflow.business.common.entity.UserRepository;
import com.werkflow.business.common.identity.dto.UserInfo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Resolves user profile information from the OIDC /userinfo endpoint.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>Parses the JWT sub claim without re-validating the signature — Spring Security has already
 *       validated the token before this service is called.</li>
 *   <li>Discovers the userinfo_endpoint URL via OIDC issuer discovery
 *       ({issuer}/.well-known/openid-configuration) on first cache miss. The discovered URL is
 *       cached separately to avoid repeated discovery calls.</li>
 *   <li>Caches resolved profiles in-memory with a 10-minute TTL via Caffeine to prevent N+1
 *       /userinfo calls across requests.</li>
 *   <li>On any error (network timeout, 401, 403, discovery failure, malformed JWT), degrades
 *       gracefully: logs the error and returns a {@link UserInfo} containing only the sub claim.
 *       This ensures downstream code always gets a non-null result.</li>
 * </ul>
 *
 * <p>Thread safety: Caffeine's {@code Cache.get(key, loader)} is atomic per key — concurrent
 * callers for the same sub will block on the first caller's load and share the result.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserInfoResolver {

    static final int CACHE_TTL_MINUTES = 10;

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** sub → UserInfo: primary profile cache with 10-minute TTL. */
    private Cache<String, UserInfo> userInfoCache;

    /** issuer → userinfo_endpoint URL: avoids repeated OIDC discovery calls. */
    private Cache<String, String> discoveryCache;

    @PostConstruct
    void initCaches() {
        userInfoCache = Caffeine.newBuilder()
                .expireAfterWrite(CACHE_TTL_MINUTES, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();

        discoveryCache = Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.MINUTES) // OIDC config rarely changes
                .maximumSize(50)
                .build();
    }

    /**
     * Resolves user profile for the given JWT bearer token.
     *
     * <p>Returns a cached {@link UserInfo} on hit. On cache miss, calls the OIDC /userinfo
     * endpoint, upserts the result into the users table, caches it, and returns it.
     *
     * <p>Never throws — degrades to an opaque {@link UserInfo} with only keycloakId on any error.
     *
     * @param jwtToken raw JWT bearer token (already validated by Spring Security)
     * @return resolved UserInfo; keycloakId is always present, displayName/email may be null on degradation
     */
    public UserInfo resolveUserInfo(String jwtToken) {
        String sub = extractSub(jwtToken);
        if (sub == null) {
            log.warn("UserInfoResolver: could not extract sub from JWT — returning opaque UserInfo");
            return UserInfo.builder().keycloakId("unknown").build();
        }

        // Cache.get(key, loader) is atomic: concurrent callers for the same sub share one load
        return userInfoCache.get(sub, key -> fetchAndStore(key, jwtToken));
    }

    // --- Internal helpers ---

    /**
     * Fetches user profile from /userinfo, upserts to DB, and returns UserInfo.
     * On any error, returns a degraded UserInfo with sub only.
     */
    private UserInfo fetchAndStore(String sub, String jwtToken) {
        try {
            String issuer = extractIssuer(jwtToken);
            if (issuer == null) {
                log.warn("UserInfoResolver: could not extract issuer from JWT for sub={}; degrading", sub);
                return degraded(sub);
            }

            String userinfoEndpoint = discoverUserinfoEndpoint(issuer);
            if (userinfoEndpoint == null) {
                log.warn("UserInfoResolver: OIDC discovery failed for issuer={}; degrading for sub={}", issuer, sub);
                return degraded(sub);
            }

            Map<String, Object> userinfoClaims = callUserinfoEndpoint(userinfoEndpoint, jwtToken);
            if (userinfoClaims == null) {
                return degraded(sub);
            }

            UserInfo userInfo = buildUserInfo(sub, userinfoClaims);
            upsertToDatabase(userInfo);
            return userInfo;

        } catch (Exception e) {
            log.error("UserInfoResolver: unexpected error resolving sub={}; degrading. cause={}", sub, e.getMessage(), e);
            return degraded(sub);
        }
    }

    /**
     * Discovers the userinfo_endpoint via {issuer}/.well-known/openid-configuration.
     * Result is cached in discoveryCache for 60 minutes.
     *
     * @return userinfo_endpoint URL, or null on failure
     */
    String discoverUserinfoEndpoint(String issuer) {
        return discoveryCache.get(issuer, key -> {
            String discoveryUrl = key.replaceAll("/$", "") + "/.well-known/openid-configuration";
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(discoveryUrl, String.class);
                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    log.warn("UserInfoResolver: OIDC discovery returned non-2xx for url={}", discoveryUrl);
                    return null;
                }
                Map<String, Object> config = objectMapper.readValue(
                        response.getBody(), new TypeReference<>() {});
                Object endpoint = config.get("userinfo_endpoint");
                if (endpoint == null) {
                    log.warn("UserInfoResolver: 'userinfo_endpoint' missing in OIDC config from url={}", discoveryUrl);
                    return null;
                }
                return endpoint.toString();
            } catch (ResourceAccessException e) {
                log.warn("UserInfoResolver: network error during OIDC discovery url={}; cause={}", discoveryUrl, e.getMessage());
                return null;
            } catch (Exception e) {
                log.error("UserInfoResolver: unexpected error during OIDC discovery url={}; cause={}", discoveryUrl, e.getMessage(), e);
                return null;
            }
        });
    }

    /**
     * Calls GET {userinfoEndpoint} with Bearer token.
     *
     * @return parsed claims map, or null on any error
     */
    private Map<String, Object> callUserinfoEndpoint(String userinfoEndpoint, String jwtToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    userinfoEndpoint, HttpMethod.GET, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("UserInfoResolver: /userinfo returned non-2xx status={}", response.getStatusCode());
                return null;
            }
            return objectMapper.readValue(response.getBody(), new TypeReference<>() {});

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.warn("UserInfoResolver: /userinfo rejected token with status={}", e.getStatusCode());
            } else {
                log.warn("UserInfoResolver: /userinfo HTTP error status={}; cause={}", e.getStatusCode(), e.getMessage());
            }
            return null;
        } catch (ResourceAccessException e) {
            log.warn("UserInfoResolver: /userinfo timeout or network error; cause={}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("UserInfoResolver: unexpected error calling /userinfo; cause={}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Builds a UserInfo DTO from /userinfo claims.
     * Display name resolution order: "name" → "given_name" + "family_name" → "email" → sub.
     */
    UserInfo buildUserInfo(String sub, Map<String, Object> claims) {
        String displayName = resolveDisplayName(claims, sub);
        String email = claims.containsKey("email") ? (String) claims.get("email") : null;

        return UserInfo.builder()
                .keycloakId(sub)
                .displayName(displayName)
                .email(email)
                .build();
    }

    private String resolveDisplayName(Map<String, Object> claims, String sub) {
        if (claims.containsKey("name") && claims.get("name") != null) {
            return (String) claims.get("name");
        }
        String given = (String) claims.get("given_name");
        String family = (String) claims.get("family_name");
        if (given != null && family != null) {
            return given + " " + family;
        }
        if (given != null) return given;
        if (family != null) return family;
        if (claims.containsKey("email") && claims.get("email") != null) {
            return (String) claims.get("email");
        }
        return sub;
    }

    private void upsertToDatabase(UserInfo userInfo) {
        try {
            userRepository.upsertUser(
                    userInfo.getKeycloakId(),
                    userInfo.getDisplayName(),
                    userInfo.getEmail(),
                    LocalDateTime.now());
        } catch (Exception e) {
            // DB upsert failure must not prevent the caller from getting UserInfo
            log.error("UserInfoResolver: failed to upsert user sub={}; cause={}", userInfo.getKeycloakId(), e.getMessage(), e);
        }
    }

    /**
     * Extracts the "sub" claim from the JWT payload without signature validation.
     * Spring Security has already validated the token upstream.
     */
    String extractSub(String jwtToken) {
        return extractClaim(jwtToken, "sub");
    }

    /**
     * Extracts the "iss" claim from the JWT payload without signature validation.
     */
    String extractIssuer(String jwtToken) {
        return extractClaim(jwtToken, "iss");
    }

    /**
     * Decodes the JWT payload (second segment) and extracts the given claim value.
     *
     * @return claim value as String, or null if not found or JWT is malformed
     */
    private String extractClaim(String jwtToken, String claim) {
        if (jwtToken == null || jwtToken.isBlank()) {
            return null;
        }
        try {
            String[] parts = jwtToken.split("\\.");
            if (parts.length < 2) {
                log.warn("UserInfoResolver: malformed JWT — expected 3 parts, got {}", parts.length);
                return null;
            }
            byte[] payloadBytes = Base64.getUrlDecoder().decode(padBase64(parts[1]));
            Map<String, Object> payload = objectMapper.readValue(payloadBytes, new TypeReference<>() {});
            Object value = payload.get(claim);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.warn("UserInfoResolver: failed to extract '{}' from JWT; cause={}", claim, e.getMessage());
            return null;
        }
    }

    private static String padBase64(String base64) {
        int padding = base64.length() % 4;
        if (padding == 2) return base64 + "==";
        if (padding == 3) return base64 + "=";
        return base64;
    }

    private static UserInfo degraded(String sub) {
        return UserInfo.builder().keycloakId(sub).build();
    }

    // Package-visible for testing cache injection
    Cache<String, UserInfo> getUserInfoCache() {
        return userInfoCache;
    }

    Cache<String, String> getDiscoveryCache() {
        return discoveryCache;
    }
}
