package com.werkflow.business.common.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * D5 idempotency regression test.
 *
 * <p>Spring's {@code @Retryable} interceptor re-invokes the target method with the
 * SAME arguments on every retry attempt — argument values are evaluated once by the
 * caller. So the idempotency key must be supplied by the caller (once) rather than
 * generated inside the retried method body, or every retry attempt of one logical
 * event would carry a different key and the engine's replay guard could never dedupe.</p>
 */
class WebhookEventPublisherTest {

    private RestTemplate restTemplate;
    private WebhookEventPublisher publisher;
    private org.mockito.ArgumentCaptor<HttpEntity> requestCaptor;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        requestCaptor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.postForEntity(anyString(), requestCaptor.capture(), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());
        publisher = new WebhookEventPublisher(restTemplate, new ObjectMapper(), "http://engine.test", "");
    }

    @Test
    void publish_repeatedAttemptsForSameEvent_useTheSameIdempotencyKey() {
        String idempotencyKey = UUID.randomUUID().toString();
        Map<String, Object> payload = Map.of("k", "v");

        // Simulate two attempts Spring's @Retryable interceptor would make for the
        // SAME logical event — it re-invokes the target method with identical arguments.
        publisher.publish("tenant-1", "connector-1", payload, idempotencyKey);
        publisher.publish("tenant-1", "connector-1", payload, idempotencyKey);

        List<HttpEntity> requests = requestCaptor.getAllValues();
        String keyAttempt1 = requests.get(0).getHeaders().getFirst("X-Idempotency-Key");
        String keyAttempt2 = requests.get(1).getHeaders().getFirst("X-Idempotency-Key");

        assertEquals(keyAttempt1, keyAttempt2,
                "retry attempts of the same event must carry the same idempotency key");
        assertEquals(idempotencyKey, keyAttempt1,
                "the header must carry the caller-supplied idempotency key, not one generated internally");
    }
}
