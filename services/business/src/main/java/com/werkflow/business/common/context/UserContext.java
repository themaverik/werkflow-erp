package com.werkflow.business.common.context;

import com.werkflow.business.common.identity.dto.UserInfo;

/**
 * Manages user identity context for the current request thread.
 *
 * <p>Stores the resolved {@link UserInfo} in a static {@link ThreadLocal}, providing
 * request-scoped access to user identity across controllers, services, and repositories
 * without requiring explicit parameter passing.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>{@link com.werkflow.business.common.filter.UserContextFilter} calls
 *       {@link #setUserInfo(UserInfo)} after resolving identity from the JWT.</li>
 *   <li>Application code calls {@link #getUserInfo()}, {@link #getKeycloakId()}, or
 *       {@link #getDisplayName()} during request processing.</li>
 *   <li>{@link com.werkflow.business.common.filter.UserContextFilter} calls {@link #clear()}
 *       in its {@code finally} block to prevent ThreadLocal leaks between requests.</li>
 * </ol>
 *
 * <p>This class intentionally does not call {@link com.werkflow.business.common.identity.UserInfoResolver}.
 * Resolution is the filter's responsibility. This class is purely storage.
 */
public final class UserContext {

    private static final ThreadLocal<UserInfo> userInfoHolder = new ThreadLocal<>();

    private UserContext() {
        // Utility class — not instantiable
    }

    /**
     * Stores {@link UserInfo} for the current request thread.
     *
     * @param userInfo the resolved user info; must not be null
     * @throws IllegalArgumentException if userInfo is null
     */
    public static void setUserInfo(UserInfo userInfo) {
        if (userInfo == null) {
            throw new IllegalArgumentException("userInfo cannot be null");
        }
        userInfoHolder.set(userInfo);
    }

    /**
     * Retrieves the {@link UserInfo} for the current request thread.
     *
     * @return the current user info
     * @throws IllegalStateException if no user info has been set for this thread
     */
    public static UserInfo getUserInfo() {
        UserInfo userInfo = userInfoHolder.get();
        if (userInfo == null) {
            throw new IllegalStateException("UserInfo not set for current thread. " +
                "Ensure UserContextFilter is registered in SecurityConfig.");
        }
        return userInfo;
    }

    /**
     * Convenience method returning the Keycloak subject (JWT {@code sub} claim).
     *
     * @return the keycloakId of the current user
     * @throws IllegalStateException if user context has not been set
     */
    public static String getKeycloakId() {
        return getUserInfo().getKeycloakId();
    }

    /**
     * Convenience method returning the display name of the current user.
     * May return {@code null} if resolution degraded gracefully.
     *
     * @return the display name, or null on degraded UserInfo
     * @throws IllegalStateException if user context has not been set
     */
    public static String getDisplayName() {
        return getUserInfo().getDisplayName();
    }

    /**
     * Clears the user info from the current thread's ThreadLocal storage.
     * Must be called in the {@code finally} block of
     * {@link com.werkflow.business.common.filter.UserContextFilter} to prevent
     * memory leaks in thread-pool environments.
     */
    public static void clear() {
        userInfoHolder.remove();
    }
}
