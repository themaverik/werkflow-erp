package com.werkflow.business.common.idempotency.job;

import com.werkflow.business.common.idempotency.repository.IdempotencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class IdempotencyCleanupJob {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyCleanupJob.class);
    private static final long TTL_HOURS = 24L;

    private final IdempotencyRepository repository;

    public IdempotencyCleanupJob(IdempotencyRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 0 2 * * *")  // 2 AM UTC daily
    public void cleanupExpiredRecords() {
        try {
            LocalDateTime cutoff = LocalDateTime.now(ZoneOffset.UTC).minusHours(TTL_HOURS);
            logger.info("Starting idempotency record cleanup: deleting records before {}", cutoff);

            long deleted = repository.deleteByCreatedAtBefore(cutoff);
            logger.info("Idempotency cleanup job completed: deleted {} records", deleted);
        } catch (Exception e) {
            logger.error("Failed to cleanup expired idempotency records", e);
        }
    }
}
