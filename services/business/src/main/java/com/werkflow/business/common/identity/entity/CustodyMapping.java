package com.werkflow.business.common.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Custody mapping entity — maps a custody owner key to a set of Flowable candidate groups
 * within a tenant (ADR-004). ERP is the authoritative source; werkflow-enterprise reads via API.
 */
@Entity
@Table(
    name = "custody_mappings",
    schema = "identity_service",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_custody_owner_tenant",
        columnNames = {"tenant_id", "custody_owner"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"tenantId", "custodyOwner"})
public class CustodyMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tenant identifier — all queries are scoped to this value. */
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    /**
     * Custody owner key — identifies who or what owns this custody group mapping.
     * Typically maps to an asset category or process domain key.
     */
    @Column(name = "custody_owner", nullable = false, length = 255)
    private String custodyOwner;

    /**
     * Flowable candidate group names assigned to this custody owner.
     * Stored as a PostgreSQL TEXT[] array.
     */
    @Column(name = "candidate_groups", columnDefinition = "text[]", nullable = false)
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] candidateGroups;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
