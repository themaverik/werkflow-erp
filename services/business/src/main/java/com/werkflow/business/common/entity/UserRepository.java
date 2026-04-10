package com.werkflow.business.common.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for User entity.
 *
 * Provides standard CRUD operations and upsert functionality for caching
 * user profiles fetched from OIDC /userinfo endpoint.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find user by keycloak_id (sub claim from JWT).
     * @param keycloakId user ID from JWT sub claim
     * @return Optional containing user if found
     */
    Optional<User> findByKeycloakId(String keycloakId);

    /**
     * Upsert (insert or update) a user record.
     * If user exists, updates display_name and email; if not, inserts new record.
     *
     * @param keycloakId user ID from JWT sub claim
     * @param displayName display name from /userinfo response
     * @param email email from /userinfo response
     * @param updatedAt timestamp of refresh
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO identity_service.users (keycloak_id, display_name, email, updated_at)
        VALUES (:keycloakId, :displayName, :email, :updatedAt)
        ON CONFLICT (keycloak_id) DO UPDATE
        SET display_name = :displayName,
            email = :email,
            updated_at = :updatedAt
        """, nativeQuery = true)
    void upsertUser(
        @Param("keycloakId") String keycloakId,
        @Param("displayName") String displayName,
        @Param("email") String email,
        @Param("updatedAt") LocalDateTime updatedAt
    );
}
