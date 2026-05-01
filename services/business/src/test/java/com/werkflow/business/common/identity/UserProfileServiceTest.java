package com.werkflow.business.common.identity;

import com.werkflow.business.common.entity.User;
import com.werkflow.business.common.entity.UserRepository;
import com.werkflow.business.common.identity.dto.UserProfileResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .keycloakId("user-123")
                .displayName("Jane Smith")
                .email("jane@example.com")
                .departmentCode("FIN")
                .employeeId("EMP-001")
                .costCenter("CC-100")
                .isPoc(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getProfile_returnsFullProfile_whenUserExists() {
        when(userRepository.findByKeycloakId("user-123")).thenReturn(Optional.of(user));

        UserProfileResponse response = userProfileService.getProfile("user-123");

        assertThat(response.getKeycloakId()).isEqualTo("user-123");
        assertThat(response.getDisplayName()).isEqualTo("Jane Smith");
        assertThat(response.getEmail()).isEqualTo("jane@example.com");
        assertThat(response.getDepartmentCode()).isEqualTo("FIN");
        assertThat(response.getEmployeeId()).isEqualTo("EMP-001");
        assertThat(response.getCostCenter()).isEqualTo("CC-100");
        assertThat(response.isPoc()).isTrue();
    }

    @Test
    void getProfile_throwsEntityNotFoundException_whenUserNotFound() {
        when(userRepository.findByKeycloakId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getProfile("missing"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void getProfile_handlesNullableEnterpriseFields() {
        User minimalUser = User.builder()
                .keycloakId("user-456")
                .displayName("John Doe")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(userRepository.findByKeycloakId("user-456")).thenReturn(Optional.of(minimalUser));

        UserProfileResponse response = userProfileService.getProfile("user-456");

        assertThat(response.getDepartmentCode()).isNull();
        assertThat(response.getEmployeeId()).isNull();
        assertThat(response.getCostCenter()).isNull();
        assertThat(response.isPoc()).isFalse();
    }

    @Test
    void getProfile_callsRepositoryWithCorrectId() {
        when(userRepository.findByKeycloakId("user-123")).thenReturn(Optional.of(user));

        userProfileService.getProfile("user-123");

        verify(userRepository).findByKeycloakId("user-123");
    }
}
