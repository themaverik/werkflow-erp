package com.werkflow.business.common.sequence;

import com.werkflow.business.common.exception.SequenceCreationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SequenceServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private SequenceService sequenceService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sequenceService = new SequenceService(jdbcTemplate);
    }

    @Test
    void testEnsureSequenceExists_CreatesSequence() {
        // Arrange
        String tenantId = "ACME";
        String docType = "pr";
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        sequenceService.ensureSequenceExists(tenantId, docType);

        // Assert
        verify(jdbcTemplate, times(1)).execute(sqlCaptor.capture());
        String executedSql = sqlCaptor.getValue();
        assertTrue(executedSql.contains("CREATE SEQUENCE IF NOT EXISTS"));
    }

    @Test
    void testEnsureSequenceExists_TenantIdNormalized() {
        // Arrange
        String tenantId = "acme";  // lowercase
        String docType = "po";
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        sequenceService.ensureSequenceExists(tenantId, docType);

        // Assert
        verify(jdbcTemplate, times(1)).execute(sqlCaptor.capture());
        String executedSql = sqlCaptor.getValue();
        assertTrue(executedSql.contains("po_seq_ACME"));  // uppercase in sequence name
    }

    @Test
    void testEnsureSequenceExists_SequenceCreationFailure() {
        // Arrange
        String tenantId = "ACME";
        String docType = "pr";
        doThrow(new DataAccessException("Permission denied") {})
            .when(jdbcTemplate).execute(anyString());

        // Act & Assert
        assertThrows(
            SequenceCreationException.class,
            () -> sequenceService.ensureSequenceExists(tenantId, docType)
        );
    }

    @Test
    void testEnsureSequenceExists_NullTenantId() {
        // Act & Assert
        assertThrows(
            IllegalArgumentException.class,
            () -> sequenceService.ensureSequenceExists(null, "pr")
        );
    }

    @Test
    void testEnsureSequenceExists_BlankTenantId() {
        // Act & Assert
        assertThrows(
            IllegalArgumentException.class,
            () -> sequenceService.ensureSequenceExists("  ", "pr")
        );
    }

    @Test
    void testEnsureSequenceExists_NullDocType() {
        // Act & Assert
        assertThrows(
            IllegalArgumentException.class,
            () -> sequenceService.ensureSequenceExists("ACME", null)
        );
    }

    @Test
    void testEnsureSequenceExists_BlankDocType() {
        // Act & Assert
        assertThrows(
            IllegalArgumentException.class,
            () -> sequenceService.ensureSequenceExists("ACME", "  ")
        );
    }

    @Test
    void testNextValue_ReturnsIncrementingValues() {
        // Arrange
        String tenantId = "ACME";
        String docType = "pr";
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(1L)
            .thenReturn(2L)
            .thenReturn(3L);

        // Act
        long val1 = sequenceService.nextValue(tenantId, docType);
        long val2 = sequenceService.nextValue(tenantId, docType);
        long val3 = sequenceService.nextValue(tenantId, docType);

        // Assert
        assertEquals(1L, val1);
        assertEquals(2L, val2);
        assertEquals(3L, val3);
        verify(jdbcTemplate, atLeast(3)).execute(anyString());
    }

    @Test
    void testNextValue_SequenceAccessFailure() {
        // Arrange
        String tenantId = "ACME";
        String docType = "pr";
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenThrow(new DataAccessException("Connection lost") {});

        // Act & Assert
        assertThrows(
            SequenceCreationException.class,
            () -> sequenceService.nextValue(tenantId, docType)
        );
    }

    @Test
    void testNextValue_NullTenantId() {
        // Act & Assert
        assertThrows(
            IllegalArgumentException.class,
            () -> sequenceService.nextValue(null, "pr")
        );
    }

    @Test
    void testNextValue_BlankTenantId() {
        // Act & Assert
        assertThrows(
            IllegalArgumentException.class,
            () -> sequenceService.nextValue("  ", "pr")
        );
    }

    @Test
    void testNextValue_MultipleDocTypes() {
        // Arrange
        String tenantId = "ACME";
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(1L)
            .thenReturn(1L);  // Each doc type starts from 1
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        long prVal = sequenceService.nextValue(tenantId, "pr");
        long poVal = sequenceService.nextValue(tenantId, "po");

        // Assert
        assertEquals(1L, prVal);
        assertEquals(1L, poVal);  // Different sequences for different doc types
        verify(jdbcTemplate, atLeast(2)).execute(sqlCaptor.capture());
        String executedSql = sqlCaptor.getValue();
        assertTrue(executedSql.contains("po_seq_ACME") || executedSql.contains("pr_seq_ACME"));
    }

    @Test
    void testNextValue_MultiTenantIsolation() {
        // Arrange
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(1L)  // ACME gets 1
            .thenReturn(1L); // TECH gets 1
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        long acmeVal = sequenceService.nextValue("ACME", "pr");
        long techVal = sequenceService.nextValue("TECH", "pr");

        // Assert
        assertEquals(1L, acmeVal);
        assertEquals(1L, techVal);  // Independent sequences per tenant
        verify(jdbcTemplate, atLeast(2)).execute(sqlCaptor.capture());
        String lastExecutedSql = sqlCaptor.getValue();
        assertTrue(lastExecutedSql.contains("pr_seq_TECH") || lastExecutedSql.contains("pr_seq_ACME"));
    }

    @Test
    void testNextValue_SequenceNameQuoted() {
        // Arrange
        String tenantId = "ACME";
        String docType = "pr";
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(1L);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        sequenceService.nextValue(tenantId, docType);

        // Assert - verify sequence name is properly quoted with double quotes
        verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), eq(Long.class));
        String executedSql = sqlCaptor.getValue();
        assertTrue(executedSql.contains("\"pr_seq_ACME\""));  // Quoted for PostgreSQL safety
    }
}
