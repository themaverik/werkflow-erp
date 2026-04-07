package com.werkflow.business.common.idempotency.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.werkflow.business.common.idempotency.dto.CachedResponse;
import com.werkflow.business.common.idempotency.entity.IdempotencyRecord;
import com.werkflow.business.common.idempotency.exception.IdempotencyException;
import com.werkflow.business.common.idempotency.repository.IdempotencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRepository repository;

    private ConcurrentMapCache cache;
    private IdempotencyService service;

    private static final String TENANT_ID = "tenant-1";
    private static final String KEY = "key-abc";
    private static final String PAYLOAD = "{\"amount\":100}";

    @BeforeEach
    void setUp() {
        cache = new ConcurrentMapCache("idempotency");
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(cache));
        cacheManager.afterPropertiesSet();
        service = new IdempotencyService(repository, cacheManager, new ObjectMapper());
    }

    @Test
    void testGetIfPresent_CacheHit_PayloadMatches() {
        IdempotencyRecord record = buildRecord(TENANT_ID, KEY, PAYLOAD, 200,
                LocalDateTime.now().minusHours(1));
        cache.put(TENANT_ID + ":" + KEY, record);

        Optional<CachedResponse> result = service.getIfPresent(TENANT_ID, KEY, PAYLOAD);

        assertTrue(result.isPresent());
        assertEquals(200, result.get().getStatusCode());
        verifyNoInteractions(repository);
    }

    /**
     * Idempotency-Key reused with different payload returns 409 Conflict.
     * The IdempotencyException thrown is caught by a @ExceptionHandler
     * in the IdempotencyFilter that maps it to a 409 response.
     */
    @Test
    void testGetIfPresent_CacheHit_PayloadMismatch_ThrowsException() {
        IdempotencyRecord record = buildRecord(TENANT_ID, KEY, PAYLOAD, 200,
                LocalDateTime.now().minusHours(1));
        cache.put(TENANT_ID + ":" + KEY, record);

        IdempotencyException ex = assertThrows(IdempotencyException.class,
                () -> service.getIfPresent(TENANT_ID, KEY, "{\"amount\":999}"));

        assertTrue(ex.getMessage().contains("different payload"));
        verifyNoInteractions(repository);
    }

    @Test
    void testGetIfPresent_CacheMiss_DatabaseHit() {
        IdempotencyRecord record = buildRecord(TENANT_ID, KEY, PAYLOAD, 201,
                LocalDateTime.now().minusHours(2));
        when(repository.findByTenantIdAndIdempotencyKey(TENANT_ID, KEY))
                .thenReturn(Optional.of(record));

        Optional<CachedResponse> result = service.getIfPresent(TENANT_ID, KEY, PAYLOAD);

        assertTrue(result.isPresent());
        assertEquals(201, result.get().getStatusCode());
        // Verify cache was populated
        assertNotNull(cache.get(TENANT_ID + ":" + KEY));
        verify(repository).findByTenantIdAndIdempotencyKey(TENANT_ID, KEY);
    }

    @Test
    void testGetIfPresent_ExpiredRecord_Deleted() {
        IdempotencyRecord record = buildRecord(TENANT_ID, KEY, PAYLOAD, 200,
                LocalDateTime.now(ZoneOffset.UTC).minusHours(25));
        cache.put(TENANT_ID + ":" + KEY, record);

        Optional<CachedResponse> result = service.getIfPresent(TENANT_ID, KEY, PAYLOAD);

        assertFalse(result.isPresent());
        verify(repository).delete(record);
    }

    @Test
    void testStore_WriteThroughCache() {
        IdempotencyRecord saved = buildRecord(TENANT_ID, KEY, PAYLOAD, 200,
                LocalDateTime.now());
        when(repository.save(any(IdempotencyRecord.class))).thenReturn(saved);

        CachedResponse response = new CachedResponse();
        response.setBody("{\"id\":\"123\"}");
        response.setStatusCode(200);
        response.setHeaders(new HashMap<>());

        service.store(TENANT_ID, KEY, PAYLOAD, response);

        verify(repository).save(any(IdempotencyRecord.class));
        assertNotNull(cache.get(TENANT_ID + ":" + KEY));
    }

    @Test
    void testGetIfPresent_CacheMiss_ExpiredRecord_Deleted() {
        // Arrange
        String key = "key-db-expired";
        String payload = "{\"vendorId\":\"v1\"}";

        IdempotencyRecord expiredRecord = buildRecord(TENANT_ID, key, payload, 200,
                LocalDateTime.now(ZoneOffset.UTC).minusHours(25));
        expiredRecord.setId(3L);

        when(repository.findByTenantIdAndIdempotencyKey(TENANT_ID, key))
                .thenReturn(Optional.of(expiredRecord));

        // Act
        Optional<CachedResponse> result = service.getIfPresent(TENANT_ID, key, payload);

        // Assert
        assertTrue(result.isEmpty());
        verify(repository).delete(expiredRecord);
    }

    // --- helpers ---

    private IdempotencyRecord buildRecord(String tenantId, String key, String payload,
                                          int statusCode, LocalDateTime createdAt) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setTenantId(tenantId);
        record.setIdempotencyKey(key);
        record.setRequestPayload(payload);
        record.setResponseBody("{\"result\":\"ok\"}");
        record.setResponseHeaders("{}");
        record.setStatusCode(statusCode);
        record.setCreatedAt(createdAt);
        return record;
    }
}
