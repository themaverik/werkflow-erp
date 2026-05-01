package com.werkflow.business.common.identity.repository;

import com.werkflow.business.common.identity.entity.CustodyMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Repository for CustodyMapping.
 * Provides standard CRUD and an idempotent upsert keyed on (tenant_id, custody_owner).
 */
@Repository
public interface CustodyMappingRepository extends JpaRepository<CustodyMapping, Long> {

    Page<CustodyMapping> findByTenantIdOrderByCustodyOwner(String tenantId, Pageable pageable);

    boolean existsByTenantIdAndCustodyOwner(String tenantId, String custodyOwner);

    java.util.Optional<CustodyMapping> findByTenantIdAndCustodyOwner(String tenantId, String custodyOwner);

    /**
     * Idempotent upsert: inserts or updates on (tenant_id, custody_owner) conflict.
     * Safe to call multiple times with the same key — only updates when the payload changes.
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO identity_service.custody_mappings
            (tenant_id, custody_owner, candidate_groups, created_at, updated_at)
        VALUES (:tenantId, :custodyOwner, :candidateGroups, :now, :now)
        ON CONFLICT (tenant_id, custody_owner) DO UPDATE
        SET candidate_groups = :candidateGroups,
            updated_at       = :now
        """, nativeQuery = true)
    void upsert(
        @Param("tenantId") String tenantId,
        @Param("custodyOwner") String custodyOwner,
        @Param("candidateGroups") String[] candidateGroups,
        @Param("now") LocalDateTime now
    );
}
