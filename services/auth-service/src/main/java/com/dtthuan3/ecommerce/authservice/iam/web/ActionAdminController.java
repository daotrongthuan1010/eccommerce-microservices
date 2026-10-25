package com.dtthuan3.ecommerce.authservice.iam.web;

import com.dtthuan3.ecommerce.authservice.iam.service.ActionAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/admin/actions")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ActionAdminController {

    private final ActionAdminService actionAdminService;

    @GetMapping
    public List<Map<String, Object>> list() {
        return actionAdminService.list();
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        return actionAdminService.get(id);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateActionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                actionAdminService.create(request.code(), request.name(), request.description()));
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id,
            @Valid @RequestBody UpdateActionRequest request) {
        return actionAdminService.update(id, request.name(), request.description());
    }

    @PatchMapping("/{id}/toggle")
    public Map<String, Object> toggle(@PathVariable Long id) {
        return actionAdminService.toggle(id);
    }

    public record CreateActionRequest(
            @NotBlank String code,
            @NotBlank String name,
            String description) {}

    public record UpdateActionRequest(
            String name,
            String description) {}
}
