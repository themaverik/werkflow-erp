package com.werkflow.business.common.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for UserRepository using H2 in-memory database.
 *
 * Note: upsertUser() uses a PostgreSQL-specific ON CONFLICT native query and
 * is therefore not exercised here. It is covered by integration tests against
 * a real PostgreSQL instance in CI/CD.
 */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveUser_persistsAllFields() {
        User user = User.builder()
                .keycloakId("550e8400-e29b-41d4-a716-446655440000")
                .displayName("Jane Smith")
                .email("jane.smith@example.com")
                .updatedAt(LocalDateTime.now())
                .build();

        User saved = userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        assertThat(saved.getKeycloakId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(saved.getDisplayName()).isEqualTo("Jane Smith");
        assertThat(saved.getEmail()).isEqualTo("jane.smith@example.com");
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void findByKeycloakId_returnsUserWhenExists() {
        User user = User.builder()
                .keycloakId("auth0|5f7a1c82-test")
                .displayName("John Doe")
                .email("john.doe@example.com")
                .updatedAt(LocalDateTime.now())
                .build();
        entityManager.persistAndFlush(user);
        entityManager.clear();

        Optional<User> found = userRepository.findByKeycloakId("auth0|5f7a1c82-test");

        assertThat(found).isPresent();
        assertThat(found.get().getDisplayName()).isEqualTo("John Doe");
        assertThat(found.get().getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    void findByKeycloakId_returnsEmptyWhenNotExists() {
        Optional<User> found = userRepository.findByKeycloakId("non-existent-id");

        assertThat(found).isEmpty();
    }

    @Test
    void save_updatesExistingUser() {
        LocalDateTime initial = LocalDateTime.of(2026, 1, 1, 10, 0);
        User user = User.builder()
                .keycloakId("update-test-id")
                .displayName("Original Name")
                .email("original@example.com")
                .updatedAt(initial)
                .build();
        entityManager.persistAndFlush(user);
        entityManager.clear();

        User toUpdate = userRepository.findByKeycloakId("update-test-id").orElseThrow();
        toUpdate.setDisplayName("Updated Name");
        toUpdate.setEmail("updated@example.com");
        userRepository.save(toUpdate);
        entityManager.flush();
        entityManager.clear();

        User updated = userRepository.findByKeycloakId("update-test-id").orElseThrow();
        assertThat(updated.getDisplayName()).isEqualTo("Updated Name");
        assertThat(updated.getEmail()).isEqualTo("updated@example.com");
    }

    @Test
    void usersTable_hasCorrectPrimaryKey() {
        // Verify keycloak_id is treated as the primary key (ID for JpaRepository<User, String>)
        User user = User.builder()
                .keycloakId("pk-test-id")
                .displayName("PK Test User")
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        Optional<User> found = userRepository.findById("pk-test-id");
        assertThat(found).isPresent();
        assertThat(found.get().getKeycloakId()).isEqualTo("pk-test-id");
    }
}
