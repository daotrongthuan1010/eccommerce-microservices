package com.dtthuan3.ecommerce.authservice.iam.web;

import com.dtthuan3.ecommerce.authservice.iam.service.IamQueryService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class EffectivePermissionController {

    private final IamQueryService iamQueryService;

    /**
     * Hop quyen cua user: Keycloak realm roles UNION DB direct UNION DB group-inherited,
     * moi entry kem source tag (KEYCLOAK | DB_DIRECT | DB_GROUP).
     */
    @GetMapping("/{subject}/effective-permissions")
    public Map<String, Object> effectivePermissions(@PathVariable String subject) {
        List<Map<String, String>> grants = iamQueryService.effectivePermissions(subject);
        return Map.of("subject", subject, "permissions", grants, "total", grants.size());
    }
}
