package com.werkflow.business.common.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes domain events to the Werkflow engine webhook receiver.
 *
 * <p>Active only when {@code werkflow.webhook.base-url} is set. When absent the
 * bean is not registered and ERP operates fully standalone (ADR-001 compliant).</p>
 *
 * <p>Each publish attempt:
 * <ul>
 *   <li>Signs the payload with HMAC-SHA256 using {@code WERKFLOW_ERP_WEBHOOK_SECRET}</li>
 *   <li>Attaches {@code X-Idempotency-Key} for replay protection</li>
 *   <li>Retries up to 24 times with 5-minute fixed backoff (~2h total window)</li>
 * </ul>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "werkflow.webhook.base-url")
public class WebhookEventPublisher {

    private static final String ALGO = "HmacSHA256";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String secret;

    public WebhookEventPublisher(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${werkflow.webhook.base-url}") String baseUrl,
            @Value("${WERKFLOW_ERP_WEBHOOK_SECRET:}") String secret) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl      = baseUrl;
        this.secret       = secret;
    }

    /**
     * Publishes a domain event asynchronously with up to 24 retries at 5-minute intervals.
     *
     * @param tenantCode  Flowable tenant code
     * @param connectorKey connector key registered in the engine (e.g. "werkflow-erp-events")
     * @param payload      event payload map — will be serialised to JSON
     */
    @Async
    @Retryable(
        maxAttempts = 24,
        backoff = @Backoff(delay = 300_000, multiplier = 1.0)  // 5 minutes, fixed
    )
    public void publish(String tenantCode, String connectorKey, Map<String, Object> payload) {
        String idempotencyKey = UUID.randomUUID().toString();
        String url = baseUrl + "/api/v1/webhooks/" + tenantCode + "/" + connectorKey;

        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Idempotency-Key", idempotencyKey);
            if (!secret.isBlank()) {
                headers.set("X-Werkflow-Signature", hmacHex(secret, body));
            }

            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Void.class);
            log.info("WebhookEventPublisher: published event to connector='{}' tenant='{}'",
                    connectorKey, tenantCode);
        } catch (Exception e) {
            log.warn("WebhookEventPublisher: publish failed for connector='{}' tenant='{}' — {} — retrying",
                    connectorKey, tenantCode, e.getMessage());
            throw new RuntimeException("Webhook publish failed: " + e.getMessage(), e);
        }
    }

    private String hmacHex(String secret, String body) {
        try {
            Mac mac = Mac.getInstance(ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGO));
            return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("WebhookEventPublisher: HMAC computation failed", e);
        }
    }
}
