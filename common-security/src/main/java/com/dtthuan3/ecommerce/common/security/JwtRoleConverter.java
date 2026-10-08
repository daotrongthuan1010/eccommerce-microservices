package com.dtthuan3.ecommerce.common.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Chuẩn Keycloak: realm_access + resource_access + scope/scp -> GrantedAuthority.
 * Dùng chung cho mọi service (đã thay 12 bản copy).
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
            String s = jwt.getClaimAsString("scope");
            if (s != null && !s.isBlank()) scopes.addAll(List.of(s.split(" ")));
        }
        if (jwt.hasClaim("scp")) {
            List<String> scp = jwt.getClaimAsStringList("scp");
            if (scp != null) scopes.addAll(scp);
        }
        for (String scope : scopes) {
            if (!scope.isBlank()) authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
        }
        return authorities;
    }
}
