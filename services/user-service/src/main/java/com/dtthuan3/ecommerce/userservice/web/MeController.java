package com.dtthuan3.ecommerce.userservice.web;

import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        Object roles = realmAccess != null ? realmAccess.getOrDefault("roles", List.of()) : List.of();
        return Map.of(
                "subject", jwt.getSubject(),
                "username", String.valueOf(jwt.getClaimAsString("preferred_username")),
                "email", String.valueOf(jwt.getClaimAsString("email")),
                "roles", roles);
    }
}
