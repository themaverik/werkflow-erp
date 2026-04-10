package com.werkflow.business.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OIDC-compliant role converter that extracts granted authorities from a configurable
 * JWT claim name, enabling compatibility with any OIDC provider.
 *
 * <p>Different OIDC providers place roles in different JWT claims:</p>
 * <ul>
 *   <li><b>Keycloak</b>: {@code roles} (flat list at token root, after realm-access flattening)</li>
 *   <li><b>Auth0</b>: {@code scope} or a custom namespace claim (e.g. {@code https://example.com/roles})</li>
 *   <li><b>Azure AD</b>: {@code roles} (app role assignments)</li>
 *   <li><b>AWS Cognito</b>: {@code cognito:groups}</li>
 * </ul>
 *
 * <p>The claim name is configurable via {@code werkflow.security.roles-claim} in
 * {@code application.yml} (default: {@code "roles"}). Each extracted role is prefixed
 * with {@code ROLE_} and uppercased to conform to Spring Security conventions.</p>
 *
 * <p>Null, blank, or missing claims are handled gracefully — an empty list is returned without
 * throwing an exception.</p>
 */
public class OidcRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final String rolesClaim;

    /**
     * Constructs an OidcRoleConverter for the given claim name.
     *
     * @param rolesClaim the JWT claim name that contains the list of roles
     *                   (e.g. {@code "roles"}, {@code "scope"}, {@code "permissions"})
     */
    public OidcRoleConverter(String rolesClaim) {
        this.rolesClaim = rolesClaim;
    }

    /**
     * Converts a {@link Jwt} to a collection of {@link GrantedAuthority} objects by
     * reading the configured claim and mapping each role to a {@code ROLE_}-prefixed authority.
     *
     * @param jwt the JWT token to extract roles from
     * @return list of granted authorities, or empty list if the claim is absent or null
     */
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList(rolesClaim);

        if (roles == null) {
            return List.of();
        }

        return roles.stream()
                .filter(role -> role != null && !role.isBlank())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toUnmodifiableList());
    }
}
