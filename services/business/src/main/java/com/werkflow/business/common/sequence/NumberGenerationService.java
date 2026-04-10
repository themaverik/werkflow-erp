package com.werkflow.business.common.sequence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Generates document numbers in the format:
 * {PREFIX}-{TENANT_ID}-{TIMESTAMP_MS}-{SEQ:05d}
 *
 * Example: PR-ACME-1712345678901-00042
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NumberGenerationService {

    private final SequenceService sequenceService;

    /**
     * Generates a unique purchase request number.
     *
     * @param tenantId the tenant ID (will be uppercase in format)
     * @return formatted number: PR-{tenantId}-{timestamp}-{seq:05d}
     */
    public String generatePrNumber(String tenantId) {
        return generateNumber("PR", "pr", tenantId);
    }

    /**
     * Generates a unique purchase order number.
     *
     * @param tenantId the tenant ID (will be uppercase in format)
     * @return formatted number: PO-{tenantId}-{timestamp}-{seq:05d}
     */
    public String generatePoNumber(String tenantId) {
        return generateNumber("PO", "po", tenantId);
    }

    /**
     * Generates a unique goods receipt number.
     *
     * @param tenantId the tenant ID (will be uppercase in format)
     * @return formatted number: GRN-{tenantId}-{timestamp}-{seq:05d}
     */
    public String generateGrnNumber(String tenantId) {
        return generateNumber("GRN", "grn", tenantId);
    }

    /**
     * Internal method to generate a formatted number.
     *
     * @param prefix the document prefix (PR, PO, GRN)
     * @param docType the sequence document type (pr, po, grn)
     * @param tenantId the tenant ID
     * @return formatted number
     */
    private String generateNumber(String prefix, String docType, String tenantId) {
        // Get next sequence value from SequenceService (lazy creation)
        long seqValue = sequenceService.nextValue(tenantId, docType);

        // Capture timestamp at creation time
        long timestamp = System.currentTimeMillis();

        // Format: {PREFIX}-{TENANT_ID_UPPER}-{TIMESTAMP}-{SEQ:05d}
        String number = String.format(
            "%s-%s-%d-%05d",
            prefix,
            tenantId.toUpperCase(),
            timestamp,
            seqValue
        );

        log.debug("Generated {} number for tenant {}: {}", prefix, tenantId, number);
        return number;
    }
}
