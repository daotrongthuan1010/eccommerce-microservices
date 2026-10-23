package com.dtthuan3.ecommerce.authservice.iam.web;

import com.dtthuan3.ecommerce.authservice.iam.service.MenuAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/admin/menus")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class MenuAdminController {

    private final MenuAdminService menuAdminService;

    @GetMapping
    public List<Map<String, Object>> list() {
        return menuAdminService.list();
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        return menuAdminService.get(id);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateMenuRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(menuAdminService.create(
                request.code(), request.name(), request.path(), request.icon(),
                request.orderNo(), request.parentId()));
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id,
            @Valid @RequestBody UpdateMenuRequest request) {
        return menuAdminService.update(id, request.name(), request.path(), request.icon(),
                request.orderNo());
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        menuAdminService.delete(id);
        return Map.of("deleted", true);
    }

    public record CreateMenuRequest(
            @NotBlank String code,
            @NotBlank String name,
            String path,
            String icon,
            Integer orderNo,
            Long parentId) {}

    public record UpdateMenuRequest(
            String name,
            String path,
            String icon,
            Integer orderNo) {}
}
