package com.dtthuan3.ecommerce.authservice.iam.web;

import com.dtthuan3.ecommerce.authservice.iam.service.AuthContextService;
import com.dtthuan3.ecommerce.authservice.iam.service.IamQueryService;
import com.dtthuan3.ecommerce.authservice.iam.service.PermissionAdminService;
import com.dtthuan3.ecommerce.authservice.security.SecurityUtils;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/admin/permissions")
@RequiredArgsConstructor
public class PermissionAdminController {

    private final PermissionAdminService permissionAdminService;
    private final AuthContextService authContextService;
    private final IamQueryService iamQueryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreatePermissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(permissionAdminService.create(
                request.menuId(), request.actionId(), request.description()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> list() {
        return permissionAdminService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> get(@PathVariable Long id) {
        return permissionAdminService.get(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> delete(@PathVariable Long id) {
        permissionAdminService.delete(id);
        return Map.of("deleted", true);
    }

    @GetMapping("/menu/{menuId}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> byMenu(@PathVariable Long menuId) {
        return permissionAdminService.findByMenu(menuId);
    }

    /**
     * Actions tren menu ma user hien tai duoc phep - loc theo effective permissions
     * cua caller (sysadmin tra ve het ke ca khi chua co grant cu the).
     */
    @GetMapping("/actions")
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> actions(@RequestParam String menuCode) {
        var caller = SecurityUtils.requireUser();
        boolean sysadmin = caller.isAdmin() || iamQueryService.isDbSystemAdmin(caller.subject());
        if (sysadmin) {
            return permissionAdminService.actionsForSysadmin(menuCode);
        }
        var ctx = authContextService.getContext(caller.subject());
        Set<String> grants = ctx == null || ctx.getPermissions() == null ? Set.of() : ctx.getPermissions();
        return permissionAdminService.actionsForUser(menuCode, grants);
    }

    public record CreatePermissionRequest(
            @NotNull Long menuId,
            @NotNull Long actionId,
            String description) {}
}
