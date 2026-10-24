package com.dtthuan3.ecommerce.authservice.iam.web;

import com.dtthuan3.ecommerce.authservice.iam.dto.AuthContext;
import com.dtthuan3.ecommerce.authservice.iam.service.AuthContextService;
import com.dtthuan3.ecommerce.authservice.iam.service.UserGroupAdminService;
import com.dtthuan3.ecommerce.authservice.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/admin/groups")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserGroupAdminController {

    private final UserGroupAdminService groupAdminService;
    private final AuthContextService authContextService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupAdminService.create(
                request.tenantId(), request.name(), request.description(),
                request.groupTypeId(), request.parentId()));
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id,
            @Valid @RequestBody UpdateGroupRequest request) {
        return groupAdminService.update(id, request.name(), request.description(), request.active());
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        groupAdminService.delete(id);
        return Map.of("deleted", true);
    }

    @GetMapping
    public List<Map<String, Object>> list(
            @RequestParam(value = "tenantId", required = false) Long tenantId) {
        return groupAdminService.list(tenantId);
    }

    @GetMapping("/tree")
    public List<Map<String, Object>> tree(
            @RequestParam(value = "tenantId", required = false) Long tenantId) {
        return groupAdminService.tree(tenantId);
    }

    @GetMapping("/accessible")
    public List<Map<String, Object>> accessible() {
        return groupAdminService.accessible(SecurityUtils.requireUser().subject());
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        return groupAdminService.get(id);
    }

    /** Gan role cho group (endpoint moi - ca hai codebase deu chua co). */
    @PostMapping("/{id}/roles/{roleId}")
    public Map<String, Object> assignRole(@PathVariable Long id, @PathVariable Long roleId) {
        groupAdminService.assignRole(id, roleId);
        return Map.of("assigned", true);
    }

    /** Go role khoi group - co self-lockout guard. */
    @DeleteMapping("/{id}/roles/{roleId}")
    public Map<String, Object> revokeRole(@PathVariable Long id, @PathVariable Long roleId) {
        var caller = SecurityUtils.requireUser();
        groupAdminService.revokeRole(caller.subject(), callerGroupId(caller.subject()), id, roleId);
        return Map.of("revoked", true);
    }

    /** Gan user vao group (dat tenant + group + position tuy chon). */
    @PostMapping("/{id}/users")
    public Map<String, Object> assignUser(@PathVariable Long id,
            @Valid @RequestBody AssignUserRequest request) {
        var caller = SecurityUtils.requireUser();
        groupAdminService.assignUser(caller.subject(), callerGroupId(caller.subject()),
                request.subject(), request.tenantId(), id, request.positionId());
        return Map.of("assigned", true);
    }

    private Long callerGroupId(String subject) {
        AuthContext ctx = authContextService.getContext(subject);
        return ctx == null ? null : ctx.getGroupId();
    }

    public record CreateGroupRequest(
            @NotNull Long tenantId,
            @NotBlank String name,
            String description,
            Long groupTypeId,
            Long parentId) {}

    public record UpdateGroupRequest(
            String name,
            String description,
            Boolean active) {}

    public record AssignUserRequest(
            @NotNull String subject,
            Long tenantId,
            Long positionId) {}
}
