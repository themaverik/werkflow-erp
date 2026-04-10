package com.werkflow.business.common.filter;

import com.werkflow.business.common.context.UserContext;
import com.werkflow.business.common.identity.UserInfoResolver;
import com.werkflow.business.common.identity.dto.UserInfo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserContextFilterTest {

    private UserContextFilter filter;

    @Mock
    private UserInfoResolver userInfoResolver;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new UserContextFilter(userInfoResolver);
        UserContext.clear();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void validToken_callsResolver_setsUserContext() throws ServletException, IOException {
        UserInfo userInfo = UserInfo.builder()
            .keycloakId("kc-user-123")
            .displayName("Jane Smith")
            .email("jane@example.com")
            .build();

        when(userInfoResolver.resolveUserInfo("valid.jwt.token")).thenReturn(userInfo);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Capture UserContext state during filter chain execution
        doAnswer(invocation -> {
            UserInfo contextUserInfo = UserContext.getUserInfo();
            assertEquals("kc-user-123", contextUserInfo.getKeycloakId());
            assertEquals("Jane Smith", contextUserInfo.getDisplayName());
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);

        verify(userInfoResolver).resolveUserInfo("valid.jwt.token");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void userContext_clearedInFinallyBlock_evenOnException() throws ServletException, IOException {
        UserInfo userInfo = UserInfo.builder().keycloakId("kc-xyz").build();
        when(userInfoResolver.resolveUserInfo(anyString())).thenReturn(userInfo);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer some.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Simulate exception thrown during filter chain
        doThrow(new RuntimeException("downstream failure"))
            .when(filterChain).doFilter(request, response);

        assertThrows(RuntimeException.class, () -> filter.doFilter(request, response, filterChain));

        // UserContext must be cleared despite the exception
        assertThrows(IllegalStateException.class, UserContext::getUserInfo);
    }

    @Test
    void missingAuthorizationHeader_degradesGracefully_filterChainContinues() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // No Authorization header
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        // Resolver must not be called
        verifyNoInteractions(userInfoResolver);
        // Filter chain must still be invoked
        verify(filterChain).doFilter(request, response);
        // UserContext is clear after filter
        assertThrows(IllegalStateException.class, UserContext::getUserInfo);
    }

    @Test
    void malformedAuthorizationHeader_notBearer_degradesGracefully() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(userInfoResolver);
        verify(filterChain).doFilter(request, response);
        assertThrows(IllegalStateException.class, UserContext::getUserInfo);
    }

    @Test
    void resolverReturnsDegradedUserInfo_contextSetWithDegradedInfo() throws ServletException, IOException {
        // Degraded UserInfo: keycloakId only, displayName and email null
        UserInfo degraded = UserInfo.builder().keycloakId("kc-degraded").build();
        when(userInfoResolver.resolveUserInfo("degraded.jwt.token")).thenReturn(degraded);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer degraded.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            UserInfo ctx = UserContext.getUserInfo();
            assertEquals("kc-degraded", ctx.getKeycloakId());
            assertNull(ctx.getDisplayName());
            assertNull(ctx.getEmail());
            return null;
        }).when(filterChain).doFilter(request, response);

        // Must not throw — degraded info is acceptable
        assertDoesNotThrow(() -> filter.doFilter(request, response, filterChain));

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void resolverThrowsUnexpectedly_filterChainStillInvoked_contextCleared() throws ServletException, IOException {
        when(userInfoResolver.resolveUserInfo(anyString()))
            .thenThrow(new RuntimeException("unexpected resolver failure"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer some.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Filter must not propagate the resolver exception
        assertDoesNotThrow(() -> filter.doFilter(request, response, filterChain));

        verify(filterChain).doFilter(request, response);
        assertThrows(IllegalStateException.class, UserContext::getUserInfo);
    }

    @Test
    void shouldNotFilter_actuatorPaths() throws ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/actuator/health");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldFilter_normalApiPaths() throws ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/employees");
        assertFalse(filter.shouldNotFilter(request));
    }
}
