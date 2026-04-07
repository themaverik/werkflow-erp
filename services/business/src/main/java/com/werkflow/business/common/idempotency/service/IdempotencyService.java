package com.werkflow.business.common.idempotency.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.werkflow.business.common.idempotency.dto.CachedResponse;
import com.werkflow.business.common.idempotency.entity.IdempotencyRecord;
import com.werkflow.business.common.idempotency.exception.IdempotencyException;
import com.werkflow.business.common.idempotency.repository.IdempotencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class IdempotencyService {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String CACHE_NAME = "idempotency";
    private static final long TTL_HOURS = 24L;

    private final IdempotencyRepository repository;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRepository repository, CacheManager cacheManager, ObjectMapper objectMapper) {
        this.repository = repository;
        this.cacheManager = cacheManager;
        this.objectMapper = objectMapper;
    }

    public Optional<CachedResponse> getIfPresent(String tenantId, String key, String currentPayload) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        String cacheKey = buildCacheKey(tenantId, key);

        // 1. Check Caffeine cache
        IdempotencyRecord cached = cache.get(cacheKey, IdempotencyRecord.class);
        if (cached != null) {
            // Lazy cleanup: check if expired
            if (isExpired(cached)) {
                repository.delete(cached);
                return Optional.empty();
            }

            // Strict validation: payload must match
            validatePayload(cached.getRequestPayload(), currentPayload);
            return Optional.of(toCachedResponse(cached));
        }

        // 2. Fall back to database
        Optional<IdempotencyRecord> dbRecord = repository.findByTenantIdAndIdempotencyKey(tenantId, key);
        if (dbRecord.isPresent()) {
            IdempotencyRecord record = dbRecord.get();

            // Lazy cleanup
            if (isExpired(record)) {
                repository.delete(record);
                return Optional.empty();
            }

            // Populate cache and validate
            validatePayload(record.getRequestPayload(), currentPayload);
            cache.put(cacheKey, record);
            return Optional.of(toCachedResponse(record));
        }

        return Optional.empty();
    }

    public void store(String tenantId, String key, String requestPayload, CachedResponse response) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setTenantId(tenantId);
        record.setIdempotencyKey(key);
        record.setRequestPayload(requestPayload);
        record.setResponseBody(response.getBody());
        record.setResponseHeaders(serializeHeaders(response.getHeaders()));
        record.setStatusCode(response.getStatusCode());

        // Write-through: save to DB, then cache
        IdempotencyRecord saved;
        try {
            saved = repository.save(record);
        } catch (DataAccessException e) {
            logger.error("Failed to persist idempotency record for key: {}", key, e);
            throw e;
        }

        Cache cache = cacheManager.getCache(CACHE_NAME);
        String cacheKey = buildCacheKey(tenantId, key);
        try {
            cache.put(cacheKey, saved);
        } catch (Exception e) {
            logger.warn("Failed to cache idempotency record for key: {}", key, e);
        }
    }

    private void validatePayload(String storedPayload, String currentPayload) {
        if (!storedPayload.equals(currentPayload)) {
            throw new IdempotencyException(
                "Idempotency-Key reused with different payload. Use a new key for different requests."
            );
        }
    }

    private boolean isExpired(IdempotencyRecord record) {
        LocalDateTime expiresAt = record.getCreatedAt().plusHours(TTL_HOURS);
        return LocalDateTime.now(ZoneOffset.UTC).isAfter(expiresAt);
    }

    private String buildCacheKey(String tenantId, String key) {
        return tenantId + ":" + key;
    }

    private CachedResponse toCachedResponse(IdempotencyRecord record) {
        CachedResponse response = new CachedResponse();
        response.setBody(record.getResponseBody());
        response.setStatusCode(record.getStatusCode());
        response.setHeaders(deserializeHeaders(record.getResponseHeaders()));
        return response;
    }

    private String serializeHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(headers);
        } catch (Exception e) {
            logger.warn("Failed to serialize headers", e);
            return "{}";
        }
    }

    private Map<String, String> deserializeHeaders(String headersJson) {
        if (headersJson == null || headersJson.isEmpty() || headersJson.equals("{}")) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(headersJson, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            logger.warn("Failed to deserialize headers", e);
            return new HashMap<>();
        }
    }
}
