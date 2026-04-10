package com.werkflow.business.common.context;

import com.werkflow.business.common.identity.dto.UserInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserContextTest {

    @BeforeEach
    void setUp() {
        UserContext.clear();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void setUserInfo_andGetUserInfo_roundtrip() {
        UserInfo userInfo = UserInfo.builder()
            .keycloakId("kc-user-123")
            .displayName("Jane Smith")
            .email("jane@example.com")
            .build();

        UserContext.setUserInfo(userInfo);

        UserInfo retrieved = UserContext.getUserInfo();
        assertSame(userInfo, retrieved);
        assertEquals("kc-user-123", retrieved.getKeycloakId());
        assertEquals("Jane Smith", retrieved.getDisplayName());
        assertEquals("jane@example.com", retrieved.getEmail());
    }

    @Test
    void getKeycloakId_returnsSubClaim() {
        UserContext.setUserInfo(UserInfo.builder()
            .keycloakId("kc-abc-999")
            .displayName("Bob")
            .build());

        assertEquals("kc-abc-999", UserContext.getKeycloakId());
    }

    @Test
    void getDisplayName_returnsDisplayName() {
        UserContext.setUserInfo(UserInfo.builder()
            .keycloakId("kc-xyz")
            .displayName("Alice Wonderland")
            .build());

        assertEquals("Alice Wonderland", UserContext.getDisplayName());
    }

    @Test
    void getDisplayName_returnsNullOnDegradedUserInfo() {
        // Degraded UserInfo has only keycloakId; displayName is null
        UserContext.setUserInfo(UserInfo.builder()
            .keycloakId("kc-degraded")
            .build());

        assertNull(UserContext.getDisplayName());
    }

    @Test
    void clear_removesUserInfo_subsequentGetThrows() {
        UserContext.setUserInfo(UserInfo.builder()
            .keycloakId("kc-to-clear")
            .build());

        UserContext.clear();

        assertThrows(IllegalStateException.class, UserContext::getUserInfo);
    }

    @Test
    void getUserInfo_throwsWhenNeverSet() {
        assertThrows(IllegalStateException.class, UserContext::getUserInfo);
    }

    @Test
    void setUserInfo_withNull_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> UserContext.setUserInfo(null));
    }

    @Test
    void multipleUsersInSequence_isolatedCorrectly() {
        UserInfo first = UserInfo.builder().keycloakId("user-first").displayName("First User").build();
        UserContext.setUserInfo(first);
        assertEquals("user-first", UserContext.getKeycloakId());

        UserContext.clear();
        assertThrows(IllegalStateException.class, UserContext::getUserInfo);

        UserInfo second = UserInfo.builder().keycloakId("user-second").displayName("Second User").build();
        UserContext.setUserInfo(second);
        assertEquals("user-second", UserContext.getKeycloakId());
        assertEquals("Second User", UserContext.getDisplayName());
    }
}
