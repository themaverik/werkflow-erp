package com.werkflow.business.common.idempotency.entity;

import com.werkflow.business.hr.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "idempotency_record",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "idempotency_key"}),
    indexes = {
        @Index(name = "idx_tenant_created", columnList = "tenant_id, created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "response_headers", columnDefinition = "TEXT")
    private String responseHeaders;

    @Column(name = "status_code", nullable = false)
    private Integer statusCode;
}
