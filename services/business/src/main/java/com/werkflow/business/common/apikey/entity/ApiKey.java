package com.werkflow.business.common.apikey.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "api_keys")
@Getter
@NoArgsConstructor
public class ApiKey {

    public static ApiKey of(String keyHash, String tenantId, String name) {
        ApiKey key = new ApiKey();
        key.keyHash = keyHash;
        key.tenantId = tenantId;
        key.name = name;
        return key;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    private String keyHash;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Setter
    @Column(nullable = false)
    private String name;

    @Setter
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Setter
    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    public boolean isExpired() {
        return expiresAt != null && OffsetDateTime.now(ZoneOffset.UTC).isAfter(expiresAt);
    }
}
