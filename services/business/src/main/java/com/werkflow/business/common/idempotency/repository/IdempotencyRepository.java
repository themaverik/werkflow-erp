package com.werkflow.business.common.idempotency.repository;

import com.werkflow.business.common.idempotency.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);

    @Modifying
    @Transactional
    int deleteByTenantIdAndCreatedAtBefore(String tenantId, LocalDateTime cutoff);
}
