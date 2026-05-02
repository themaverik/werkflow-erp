package com.werkflow.business.config;

import com.werkflow.business.common.apikey.filter.ApiKeyAuthenticationFilter;
import com.werkflow.business.common.filter.TenantContextFilter;
import com.werkflow.business.common.filter.UserContextFilter;
import com.werkflow.business.common.idempotency.filter.IdempotencyFilter;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${werkflow.security.roles-claim:roles}")
    private String rolesClaim;

    /** C-3/MED-01: when false (default), actuator/Swagger require authentication. */
    @Value("${werkflow.security.expose-management-endpoints:false}")
    private boolean exposeManagementEndpoints;

    @Value("${werkflow.security.cors.allowed-origins:http://localhost:4000,http://localhost:4001,http://localhost:3000}")
    private String[] corsAllowedOrigins;

    @PostConstruct
    public void validateRolesClaim() {
        Assert.hasText(rolesClaim, "werkflow.security.roles-claim must not be blank");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
                                                   TenantContextFilter tenantContextFilter,
                                                   UserContextFilter userContextFilter,
                                                   IdempotencyFilter idempotencyFilter,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                // C-3: health endpoint always public; all other management endpoints require auth
                auth.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
                if (exposeManagementEndpoints) {
                    auth.requestMatchers("/actuator/**", "/api-docs/**", "/v3/api-docs/**",
                            "/swagger-ui/**", "/swagger-ui.html").permitAll();
                } else {
                    auth.requestMatchers("/actuator/**").authenticated()
                        .requestMatchers("/api-docs/**", "/v3/api-docs/**",
                            "/swagger-ui/**", "/swagger-ui.html").authenticated();
                }
                auth.anyRequest().authenticated();
            })
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter)
                )
            )
            // API key filter runs first — sets SecurityContext so BearerTokenAuthenticationFilter skips JWT
            .addFilterBefore(apiKeyAuthenticationFilter, BearerTokenAuthenticationFilter.class)
            // Add TenantContextFilter AFTER OAuth2 authentication filters
            .addFilterAfter(tenantContextFilter, BearerTokenAuthenticationFilter.class)
            // Add UserContextFilter AFTER TenantContextFilter (JWT already validated upstream)
            .addFilterAfter(userContextFilter, TenantContextFilter.class)
            .addFilterAfter(idempotencyFilter, UserContextFilter.class);

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Bean
    public OidcRoleConverter oidcRoleConverter() {
        return new OidcRoleConverter(rolesClaim);
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(OidcRoleConverter oidcRoleConverter) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(oidcRoleConverter);
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // M-8: CORS origins driven from config (werkflow.security.cors.allowed-origins)
        configuration.setAllowedOrigins(java.util.Arrays.asList(corsAllowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
