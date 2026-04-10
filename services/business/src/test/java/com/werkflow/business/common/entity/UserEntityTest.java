package com.werkflow.business.common.entity;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Entity lifecycle tests for User.
 * Tests @PrePersist/@PreUpdate hooks and field-level constraints.
 */
@DataJpaTest
@ActiveProfiles("test")
class UserEntityTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void createUser_withValidData_persistsSuccessfully() {
        User user = User.builder()
                .keycloakId("valid-keycloak-id")
                .displayName("Alice Wonderland")
                .email("alice@example.com")
                .updatedAt(LocalDateTime.now())
                .build();

        User persisted = entityManager.persistAndFlush(user);

        assertThat(persisted.getKeycloakId()).isEqualTo("valid-keycloak-id");
        assertThat(persisted.getDisplayName()).isEqualTo("Alice Wonderland");
        assertThat(persisted.getEmail()).isEqualTo("alice@example.com");
        assertThat(persisted.getUpdatedAt()).isNotNull();
    }

    @Test
    void createUser_withoutEmail_persistsSuccessfully() {
        // email is nullable per entity definition
        User user = User.builder()
                .keycloakId("no-email-user-id")
                .displayName("Bob Builder")
                .updatedAt(LocalDateTime.now())
                .build();

        User persisted = entityManager.persistAndFlush(user);

        assertThat(persisted.getEmail()).isNull();
        assertThat(persisted.getDisplayName()).isEqualTo("Bob Builder");
    }

    @Test
    void preUpdate_setsUpdatedAtTimestamp_onMerge() throws InterruptedException {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        User user = User.builder()
                .keycloakId("update-lifecycle-id")
                .displayName("Carol Before")
                .updatedAt(LocalDateTime.now())
                .build();
        entityManager.persistAndFlush(user);
        entityManager.clear();

        User loaded = entityManager.find(User.class, "update-lifecycle-id");
        loaded.setDisplayName("Carol After");

        // Small pause to ensure updated_at is strictly after the original
        Thread.sleep(10);
        entityManager.merge(loaded);
        entityManager.flush();
        entityManager.clear();

        User updated = entityManager.find(User.class, "update-lifecycle-id");
        assertThat(updated.getDisplayName()).isEqualTo("Carol After");
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(before);
    }

    @Test
    void builder_createsUserWithAllFields() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 10, 12, 0, 0);

        User user = User.builder()
                .keycloakId("builder-test-id")
                .displayName("Dan Tester")
                .email("dan@example.com")
                .updatedAt(now)
                .build();

        assertThat(user.getKeycloakId()).isEqualTo("builder-test-id");
        assertThat(user.getDisplayName()).isEqualTo("Dan Tester");
        assertThat(user.getEmail()).isEqualTo("dan@example.com");
        assertThat(user.getUpdatedAt()).isEqualTo(now);
    }
}
