package com.werkflow.business.procurement.integration;

import com.werkflow.business.common.sequence.NumberGenerationService;
import com.werkflow.business.common.sequence.SequenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class PurchaseOrderNumberGenerationIntegrationTest {

    private NumberGenerationService numberGenerationService;

    @Mock
    private SequenceService sequenceService;

    private AtomicLong sequenceCounter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sequenceCounter = new AtomicLong(0);

        // Mock the sequence service to return incrementing values
        when(sequenceService.nextValue(anyString(), anyString()))
            .thenAnswer(inv -> sequenceCounter.incrementAndGet());

        numberGenerationService = new NumberGenerationService(sequenceService);
    }

    @Test
    void testGeneratePoNumber_UniqueFormat() {
        // Act
        String poNumber = numberGenerationService.generatePoNumber("DEFAULT");

        // Assert
        assertNotNull(poNumber);

        // Verify format: PO-{TENANT}-{TIMESTAMP}-{SEQ:05d}
        Pattern pattern = Pattern.compile("^PO-[A-Z_]+-\\d{13}-\\d{5}$");
        assertTrue(
            pattern.matcher(poNumber).matches(),
            "PO number format should match PO-TENANT-TIMESTAMP-SEQ05d, got: " + poNumber
        );
    }

    @Test
    void testGeneratePoNumber_NoCollisions() {
        // Act
        String po1 = numberGenerationService.generatePoNumber("DEFAULT");
        String po2 = numberGenerationService.generatePoNumber("DEFAULT");

        // Assert
        assertNotEquals(po1, po2,
            "Generated PO numbers should be unique");
    }

    @Test
    void testGeneratePoNumber_IncreasingSequence() {
        // Act
        String num1 = numberGenerationService.generatePoNumber("DEFAULT");
        String num2 = numberGenerationService.generatePoNumber("DEFAULT");

        // Assert
        // Extract sequence parts (last 5 digits)
        String seq1 = num1.substring(num1.length() - 5);
        String seq2 = num2.substring(num2.length() - 5);

        long seqNum1 = Long.parseLong(seq1);
        long seqNum2 = Long.parseLong(seq2);

        assertTrue(seqNum1 < seqNum2, "Sequence should increase monotonically");
    }
}
