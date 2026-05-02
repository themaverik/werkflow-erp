package com.werkflow.business.config;

import com.werkflow.business.common.apikey.repository.ApiKeyRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test security configuration that replaces the production JWT-based SecurityConfig.
 * Allows all authenticated requests through without requiring a real JWT token,
 * making it compatible with @WithMockUser in WebMvcTest slices.
 *
 * MockBean for ApiKeyRepository is declared here so @WebMvcTest slices can
 * instantiate ApiKeyAuthenticationFilter (which requires the repository).
 */
@TestConfiguration
@EnableMethodSecurity(prePostEnabled = true)
public class TestSecurityConfig {

    @MockBean
    ApiKeyRepository apiKeyRepository;

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
