package com.dtthuan3.ecommerce.authservice.security;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Tien ich doc SecurityContext chuan doanh nghiep: moi service deu lay
 * CurrentUser qua class nay thay vi tu parse Jwt o controller.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<CurrentUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        Object roles = realmAccess != null ? realmAccess.getOrDefault("roles", List.of()) : List.of();
        @SuppressWarnings("unchecked")
        List<String> roleList = roles instanceof List<?> list ? (List<String>) list : List.of();
        return Optional.of(new CurrentUser(
                jwt.getSubject(),
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("email"),
                List.copyOf(roleList)));
    }

    public static CurrentUser requireUser() {
        return currentUser().orElseThrow(() -> new AccessDeniedException("Unauthenticated"));
    }

    public static void requireRole(String role) {
        CurrentUser user = requireUser();
        if (!user.hasRole(role)) {
            throw new AccessDeniedException("Missing role: " + role);
        }
    }
}
