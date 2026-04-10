package com.werkflow.business.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * General application infrastructure beans.
 */
@Configuration
public class AppConfig {

    /**
     * RestTemplate used by {@link com.werkflow.business.common.identity.UserInfoResolver}
     * for OIDC /userinfo and discovery endpoint calls.
     *
     * Timeouts are set conservatively: auth server calls must not block request threads.
     * UserInfoResolver degrades gracefully on timeout.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }
}
