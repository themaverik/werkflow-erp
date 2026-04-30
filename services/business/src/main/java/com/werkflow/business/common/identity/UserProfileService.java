package com.werkflow.business.common.identity;

import com.werkflow.business.common.entity.User;
import com.werkflow.business.common.entity.UserRepository;
import com.werkflow.business.common.identity.dto.UserProfileResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user profile read and update operations.
 *
 * The users table is populated by {@link UserInfoResolver} from OIDC /userinfo.
 * Enterprise profile fields (departmentCode, employeeId, costCenter, isPoc) are
 * written by admin workflows and consumed by ADR-003 / ADR-005 routing logic.
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;

    /**
     * Returns the full profile for the given Keycloak user ID.
     *
     * @param keycloakId JWT sub claim
     * @return profile response with OIDC + enterprise fields
     * @throws EntityNotFoundException if no cached user record exists
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String keycloakId) {
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User profile not found for keycloakId: " + keycloakId));
        return toResponse(user);
    }

    private UserProfileResponse toResponse(User user) {
        return UserProfileResponse.builder()
                .keycloakId(user.getKeycloakId())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .departmentCode(user.getDepartmentCode())
                .employeeId(user.getEmployeeId())
                .costCenter(user.getCostCenter())
                .isPoc(user.isPoc())
                .build();
    }
}
