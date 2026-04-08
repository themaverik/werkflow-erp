package com.werkflow.business.common.sequence;

import com.werkflow.business.common.exception.SequenceCreationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Manages database sequences for tenant-scoped document numbering.
 *
 * Uses lazy creation: sequences are created on first request for a tenant.
 * Thread-safe: PostgreSQL sequences are inherently atomic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SequenceService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Ensures a sequence exists for the given tenant and document type.
     * Safe to call multiple times (uses CREATE SEQUENCE IF NOT EXISTS).
     *
     * @param tenantId the tenant ID (case-insensitive, will be uppercase in DB)
     * @param docType the document type abbreviation (pr, po, grn)
     * @throws SequenceCreationException if sequence creation fails
     */
    public void ensureSequenceExists(String tenantId, String docType) {
        String sequenceName = buildSequenceName(tenantId, docType);
        String sql = String.format(
            "CREATE SEQUENCE IF NOT EXISTS %s START WITH 1",
            sequenceName
        );

        try {
            jdbcTemplate.execute(sql);
            log.debug("Ensured sequence exists: {}", sequenceName);
        } catch (Exception e) {
            String errorMsg = String.format(
                "Failed to create sequence %s for tenant %s, docType %s",
                sequenceName, tenantId, docType
            );
            log.error(errorMsg, e);
            throw new SequenceCreationException(errorMsg, e);
        }
    }

    /**
     * Gets the next value from the sequence for the given tenant and document type.
     * Ensures sequence exists before calling nextval().
     *
     * @param tenantId the tenant ID
     * @param docType the document type abbreviation (pr, po, grn)
     * @return the next sequence value
     * @throws SequenceCreationException if sequence access fails
     */
    public long nextValue(String tenantId, String docType) {
        ensureSequenceExists(tenantId, docType);

        String sequenceName = buildSequenceName(tenantId, docType);
        String sql = String.format("SELECT nextval('%s')", sequenceName);

        try {
            Long result = jdbcTemplate.queryForObject(sql, Long.class);
            long value = result != null ? result : 0L;
            log.debug("Got next value {} for sequence {}", value, sequenceName);
            return value;
        } catch (Exception e) {
            String errorMsg = String.format(
                "Failed to get next value from sequence %s",
                sequenceName
            );
            log.error(errorMsg, e);
            throw new SequenceCreationException(errorMsg, e);
        }
    }

    /**
     * Builds the sequence name from tenant ID and document type.
     * Format: {docType}_seq_{TENANT_ID_UPPERCASE}
     * Example: pr_seq_ACME
     *
     * @param tenantId the tenant ID (will be normalized to uppercase)
     * @param docType the document type abbreviation
     * @return the sequence name
     */
    private String buildSequenceName(String tenantId, String docType) {
        return String.format("%s_seq_%s", docType.toLowerCase(), tenantId.toUpperCase());
    }
}
