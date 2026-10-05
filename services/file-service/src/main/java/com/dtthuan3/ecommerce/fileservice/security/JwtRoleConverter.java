package com.dtthuan3.ecommerce.fileservice.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Map Keycloak JWT claims thanh GrantedAuthority (RBAC + scope).
 * Giu dong bo voi cac service khac de moi service hieu role giong nhau.
 */
public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection) {
            for (Object role : (Collection) realmAccess.get("roles")) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
        }
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            for (Object client : resourceAccess.values()) {
                if (client instanceof Map) {
                    Object clientRoles = ((Map) client).get("roles");
                    if (clientRoles instanceof Collection) {
                        for (Object role : (Collection) clientRoles) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                        }
                    }
                }
            }
        }
        List<String> scopes = new ArrayList<>();
        if (jwt.hasClaim("scope")) {
            String scope = jwt.getClaimAsString("scope");
            if (scope != null && !scope.isBlank()) {
                scopes.addAll(List.of(scope.split(" ")));
            }
        }
        if (jwt.hasClaim("scp")) {
            List<String> scp = jwt.getClaimAsStringList("scp");
            if (scp != null) {
                scopes.addAll(scp);
            }
        }
        for (String scope : scopes) {
            if (!scope.isBlank()) {
                authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
            }
        }
        return authorities;
    }
}
