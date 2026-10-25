package com.dtthuan3.ecommerce.authservice.iam.web;

import com.dtthuan3.ecommerce.authservice.iam.dto.AuthContext;
import com.dtthuan3.ecommerce.authservice.iam.dto.ProvisionRequest;
import com.dtthuan3.ecommerce.authservice.iam.service.AuthContextService;
import com.dtthuan3.ecommerce.authservice.iam.service.IamProvisioningProperties;
import com.dtthuan3.ecommerce.authservice.iam.service.IamProvisioningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API internal cho api-gateway lay AuthContext. Chi goi noi bo (service-to-service qua
 * X-Internal-Token, hoac JWT co ROLE_ADMIN), khong expose public - gateway da denyAll
 * moi path internal dang /api/{service}/internal/... (P0, giu nguyen o P2).
 */
@RestController
@RequestMapping("/internal/auth")
@RequiredArgsConstructor
public class InternalAuthController {

    private final AuthContextService authContextService;
    private final IamProvisioningService provisioningService;
    private final IamProvisioningProperties provisioningProperties;

    /**
     * Lay AuthContext. Neu user chua co trong IAM DB va request kem claims (gateway truyen
     * tu JWT: sub tren path, username/email/fullName tren query) thi JIT provision truoc
     * khi tra context - nen lan goi dau tien da co du lieu thay vi 404.
     */
    @GetMapping("/context/{userId}")
    public ResponseEntity<AuthContext> getContext(
            @PathVariable String userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String email) {
        AuthContext ctx = authContextService.getContext(userId);
        if (ctx == null && provisioningProperties.isEnabled() && username != null && !username.isBlank()) {
            provisioningService.provisionIfAbsent(userId, username, fullName, email);
            ctx = authContextService.refreshContext(userId);
        }
        if (ctx == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ctx);
    }

    @PostMapping("/provision")
    public ResponseEntity<AuthContext> provision(@RequestBody ProvisionRequest request) {
        if (request.userId() == null || request.userId().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        provisioningService.provisionIfAbsent(
                request.userId(), request.username(), request.fullName(), request.email());
        AuthContext ctx = authContextService.refreshContext(request.userId());
        if (ctx == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ctx);
    }

    @PostMapping("/context/{userId}/refresh")
    public ResponseEntity<AuthContext> refreshContext(@PathVariable String userId) {
        AuthContext ctx = authContextService.refreshContext(userId);
        if (ctx == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ctx);
    }

    @DeleteMapping("/context/{userId}")
    public ResponseEntity<Void> invalidateContext(@PathVariable String userId) {
        authContextService.invalidate(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth Internal is healthy");
    }
}
