package com.dtthuan3.ecommerce.authservice.iam.web;

import com.dtthuan3.ecommerce.authservice.iam.dto.AuthContext;
import com.dtthuan3.ecommerce.authservice.iam.service.AuthContextService;
import com.dtthuan3.ecommerce.authservice.iam.service.DbRoleService;
import com.dtthuan3.ecommerce.authservice.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/admin/roles/db")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DbRoleController {

    private final DbRoleService dbRoleService;
    private final AuthContextService authContextService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @Valid @RequestBody CreateDbRoleRequest request,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dbRoleService.create(
                request.code(), request.name(), request.description(),
                request.permissionIds(), tenantId));
    }

    @GetMapping
    public List<Map<String, Object>> list(
            @RequestParam(value = "tenantId", required = false) Long tenantId) {
        return dbRoleService.list(tenantId);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        return dbRoleService.get(id);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        var caller = SecurityUtils.requireUser();
        dbRoleService.delete(caller.subject(), callerGroupId(caller.subject()), id);
        return Map.of("deleted", true);
    }

    @PostMapping("/{id}/assign")
    public Map<String, Object> assign(@PathVariable Long id,
            @Valid @RequestBody SubjectRequest request) {
        var caller = SecurityUtils.requireUser();
        dbRoleService.assign(caller.subject(), request.subject(), id);
        return Map.of("assigned", true);
    }

    @PostMapping("/{id}/revoke")
    public Map<String, Object> revoke(@PathVariable Long id,
            @Valid @RequestBody SubjectRequest request) {
        var caller = SecurityUtils.requireUser();
        dbRoleService.revoke(caller.subject(), callerGroupId(caller.subject()), request.subject(), id);
        return Map.of("revoked", true);
    }

    @PutMapping("/{id}/permissions")
    public Map<String, Object> setPermissions(@PathVariable Long id,
            @RequestBody Set<Long> permissionIds) {
        var caller = SecurityUtils.requireUser();
        return dbRoleService.setPermissions(caller.subject(), callerGroupId(caller.subject()), id,
                permissionIds == null ? Set.of() : permissionIds);
    }

    private Long callerGroupId(String subject) {
        AuthContext ctx = authContextService.getContext(subject);
        return ctx == null ? null : ctx.getGroupId();
    }

    public record CreateDbRoleRequest(
            @NotBlank String code,
            @NotBlank String name,
            String description,
            Set<Long> permissionIds) {}

    public record SubjectRequest(
            @NotNull String subject) {}
}
