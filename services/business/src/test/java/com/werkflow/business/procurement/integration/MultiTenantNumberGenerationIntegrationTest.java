package com.werkflow.business.procurement.integration;

import com.werkflow.business.common.sequence.NumberGenerationService;
import com.werkflow.business.common.sequence.SequenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class MultiTenantNumberGenerationIntegrationTest {

    private NumberGenerationService numberGenerationService;

    @Mock
    private SequenceService sequenceService;

    private ConcurrentHashMap<String, AtomicLong> tenantSequences;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tenantSequences = new ConcurrentHashMap<>();

        // Mock the sequence service to return incrementing values per tenant/docType
        when(sequenceService.nextValue(anyString(), anyString()))
            .thenAnswer(inv -> {
                String tenantId = (String) inv.getArgument(0);
                String docType = (String) inv.getArgument(1);
                String key = tenantId + "_" + docType;
                return tenantSequences.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
            });

        numberGenerationService = new NumberGenerationService(sequenceService);
    }

    @Test
    void testNumberGenerationIsolation_TwoTenants() {
        // Generate PR numbers for ACME
        String acmePr1 = numberGenerationService.generatePrNumber("ACME");
        String acmePr2 = numberGenerationService.generatePrNumber("ACME");

        // Extract tenant from PR number (second component)
        String acmeTenant1 = acmePr1.split("-")[1];
        String acmeTenant2 = acmePr2.split("-")[1];

        assertEquals("ACME", acmeTenant1);
        assertEquals("ACME", acmeTenant2);

        // Generate PR numbers for TECH
        String techPr1 = numberGenerationService.generatePrNumber("TECH");
        String techPr2 = numberGenerationService.generatePrNumber("TECH");

        String techTenant1 = techPr1.split("-")[1];
        String techTenant2 = techPr2.split("-")[1];

        assertEquals("TECH", techTenant1);
        assertEquals("TECH", techTenant2);

        // Verify all numbers are unique
        assertNotEquals(acmePr1, techPr1);
        assertNotEquals(acmePr2, techPr2);
    }

    @Test
    void testNumberGenerationIsolation_SameSequenceValueDifferentTenant() {
        // Both tenants might have sequence value 00001, but different tenant prefixes make them unique

        String acmePr = numberGenerationService.generatePrNumber("ACME");
        String techPr = numberGenerationService.generatePrNumber("TECH");

        // Numbers should be different due to tenant ID
        assertNotEquals(acmePr, techPr);

        // But both should start with correct tenant IDs
        assertTrue(acmePr.contains("ACME"));
        assertTrue(techPr.contains("TECH"));
    }
}
