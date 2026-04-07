package com.werkflow.business.common.idempotency.entity;

import com.werkflow.business.hr.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Persisted record for idempotent request deduplication.
 *
 * <p>Each row represents a completed or in-flight request identified by the
 * (tenantId, idempotencyKey) pair. Fields {@code tenantId}, {@code idempotencyKey},
 * and {@code statusCode} are non-nullable at both the JPA and database level.
 * {@code requestPayload}, {@code responseBody}, and {@code responseHeaders} are
 * optional TEXT columns and may be {@code null}.
 */
@Entity
@Table(
    name = "idempotency_record",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "idempotency_key"}),
    indexes = {
        @Index(name = "idx_idempotency_tenant_created", columnList = "tenant_id, created_at")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord extends BaseEntity {

    @NotNull
    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;

    @NotBlank
    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "response_headers", columnDefinition = "TEXT")
    private String responseHeaders;

    @Column(name = "status_code", nullable = false)
    private int statusCode;
}
