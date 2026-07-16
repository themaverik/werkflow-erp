package com.werkflow.business.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.RejectedExecutionException;

/**
 * General application infrastructure beans.
 */
@Slf4j
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

    /**
     * Dedicated bounded executor for {@link com.werkflow.business.common.webhook.WebhookEventPublisher}.
     *
     * <p>Webhook publishing retries up to 24 times with a 5-minute fixed backoff (a ~2h window
     * per failing event). If those dispatches ran on the shared {@code applicationTaskExecutor}
     * (core 8, unbounded queue), a single webhook-receiver outage would park shared pool threads
     * for hours and starve the app-wide executor. Isolating them here caps the blast radius to
     * this small pool.</p>
     *
     * <p>The queue is bounded and the rejection policy is <strong>abort + log</strong>, never
     * {@code CallerRunsPolicy}: caller-runs would execute the parking dispatch on the request
     * thread that submitted it, reintroducing the exact request-thread-starvation bug this pool
     * exists to prevent. When the pool saturates we drop the event loudly instead.</p>
     */
    @Bean
    public TaskExecutor webhookTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("webhook-pub-");
        executor.setRejectedExecutionHandler((runnable, poolExecutor) -> {
            log.warn("webhookTaskExecutor saturated (activeThreads={}, queuedTasks={}) — rejecting "
                            + "webhook dispatch; receiver is likely down and the retry backlog is full",
                    poolExecutor.getActiveCount(), poolExecutor.getQueue().size());
            throw new RejectedExecutionException("webhookTaskExecutor saturated; webhook dispatch rejected");
        });
        executor.initialize();
        return executor;
    }
}
