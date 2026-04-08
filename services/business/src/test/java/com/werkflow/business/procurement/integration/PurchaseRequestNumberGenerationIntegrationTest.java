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

class PurchaseRequestNumberGenerationIntegrationTest {

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
    void testGeneratePrNumber_UniqueFormat() {
        // Act
        String prNumber = numberGenerationService.generatePrNumber("DEFAULT");

        // Assert
        assertNotNull(prNumber);

        // Verify format: PR-{TENANT}-{TIMESTAMP}-{SEQ:05d}
        Pattern pattern = Pattern.compile("^PR-[A-Z_]+-\\d{13}-\\d{5}$");
        assertTrue(
            pattern.matcher(prNumber).matches(),
            "PR number format should match PR-TENANT-TIMESTAMP-SEQ05d, got: " + prNumber
        );
    }

    @Test
    void testGeneratePrNumber_NoCollisions() {
        // Act
        String pr1 = numberGenerationService.generatePrNumber("DEFAULT");
        String pr2 = numberGenerationService.generatePrNumber("DEFAULT");

        // Assert
        assertNotEquals(pr1, pr2,
            "Generated PR numbers should be unique");
    }

    @Test
    void testGeneratePrNumber_IncreasingSequence() {
        // Act
        String num1 = numberGenerationService.generatePrNumber("DEFAULT");
        String num2 = numberGenerationService.generatePrNumber("DEFAULT");

        // Assert
        // Extract sequence parts (last 5 digits)
        String seq1 = num1.substring(num1.length() - 5);
        String seq2 = num2.substring(num2.length() - 5);

        long seqNum1 = Long.parseLong(seq1);
        long seqNum2 = Long.parseLong(seq2);

        assertTrue(seqNum1 < seqNum2, "Sequence should increase monotonically");
    }
}
