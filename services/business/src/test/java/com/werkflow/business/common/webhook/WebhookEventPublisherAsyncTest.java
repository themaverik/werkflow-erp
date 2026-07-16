package com.werkflow.business.common.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.werkflow.business.BusinessServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * "@Async no-op" regression test. Without {@code @EnableAsync} on the application
 * config, {@code @Async} on {@link WebhookEventPublisher#publish} is silently
 * ignored by Spring and the retry-with-5-minute-backoff loop runs on the calling
 * (request) thread instead of a background executor.
 */
class WebhookEventPublisherAsyncTest {

    /**
     * BusinessServiceApplication must enable async execution — otherwise the
     * @Async annotation on WebhookEventPublisher.publish is a silent no-op.
     */
    @Test
    void businessServiceApplication_enablesAsyncMethodExecution() {
        assertTrue(BusinessServiceApplication.class.isAnnotationPresent(EnableAsync.class),
                "BusinessServiceApplication must be annotated with @EnableAsync for " +
                "WebhookEventPublisher.publish's @Async to take effect");
    }

    @Configuration
    @EnableAsync
    @EnableRetry
    static class AsyncTestConfig {

        @Bean
        CountDownLatch callLatch() {
            return new CountDownLatch(1);
        }

        @Bean
        AtomicReference<Thread> callingThread() {
            return new AtomicReference<>();
        }

        @Bean
        RestTemplate restTemplate(CountDownLatch callLatch, AtomicReference<Thread> callingThread) {
            RestTemplate restTemplate = mock(RestTemplate.class);
            when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                    .thenAnswer(invocation -> {
                        callingThread.set(Thread.currentThread());
                        callLatch.countDown();
                        return ResponseEntity.ok().build();
                    });
            return restTemplate;
        }

        @Bean
        WebhookEventPublisher webhookEventPublisher(RestTemplate restTemplate) {
            return new WebhookEventPublisher(restTemplate, new ObjectMapper(), "http://engine.test", "");
        }
    }

    @Test
    void publish_dispatchesToBackgroundThread_whenAsyncIsEnabled() throws InterruptedException {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(AsyncTestConfig.class)) {
            WebhookEventPublisher publisher = ctx.getBean(WebhookEventPublisher.class);
            CountDownLatch callLatch = ctx.getBean(CountDownLatch.class);
            AtomicReference<Thread> callingThread = ctx.getBean(AtomicReference.class);
            Thread testThread = Thread.currentThread();

            publisher.publish("tenant-1", "connector-1", Map.of("k", "v"), "idem-key-1");

            boolean completed = callLatch.await(3, TimeUnit.SECONDS);
            assertTrue(completed, "async publish should complete within the timeout");
            assertNotSame(testThread, callingThread.get(),
                    "publish should run on a background executor thread, not the caller thread");
        }
    }
}
