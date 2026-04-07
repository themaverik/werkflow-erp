package com.werkflow.business.common.idempotency.repository;

import com.werkflow.business.common.idempotency.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);

    void deleteByTenantIdAndCreatedAtBefore(String tenantId, LocalDateTime cutoff);
}
