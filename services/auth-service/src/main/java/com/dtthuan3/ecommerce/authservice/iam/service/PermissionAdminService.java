package com.dtthuan3.ecommerce.authservice.iam.service;

import com.dtthuan3.ecommerce.authservice.iam.entity.Action;
import com.dtthuan3.ecommerce.authservice.iam.entity.Menu;
import com.dtthuan3.ecommerce.authservice.iam.entity.Permission;
import com.dtthuan3.ecommerce.authservice.iam.repository.ActionRepository;
import com.dtthuan3.ecommerce.authservice.iam.repository.MenuRepository;
import com.dtthuan3.ecommerce.authservice.iam.repository.PermissionRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PermissionAdminService {

    private final PermissionRepository permissionRepository;
    private final MenuRepository menuRepository;
    private final ActionRepository actionRepository;

    @Transactional
    public Map<String, Object> create(Long menuId, Long actionId, String description) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu not found: " + menuId));
        Action action = actionRepository.findById(actionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Action not found: " + actionId));
        String code = menu.getCode() + "." + action.getCode();
        if (permissionRepository.existsByCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Permission already exists: " + code);
        }
        Permission permission = Permission.builder().menu(menu).action(action).code(code)
                .description(description).isDeleted(false).build();
        return toMap(permissionRepository.save(permission));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return permissionRepository.findAllByIsDeletedFalse().stream()
                .map(this::toMap).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Permission not found: " + id));
        return toMap(permission);
    }

    @Transactional
    public void delete(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Permission not found: " + id));
        permission.setIsDeleted(true);
        permissionRepository.save(permission);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> findByMenu(Long menuId) {
        return permissionRepository.findByMenuIdAndIsDeletedFalse(menuId).stream()
                .map(this::toMap).collect(Collectors.toList());
    }

    /**
     * Actions tren menu ma user hien tai duoc phep (loc theo effective permission codes).
     * Action da toggle-off van tra ve kem active=false de FE hien denied.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> actionsForUser(String menuCode, Set<String> callerPermissions) {
        Menu menu = menuRepository.findByCode(menuCode.trim().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Menu not found: " + menuCode));
        Set<String> grants = callerPermissions == null ? Set.of() : callerPermissions;
        return permissionRepository.findByMenuIdAndIsDeletedFalse(menu.getId()).stream()
                .filter(p -> grants.contains(p.getCode()))
                .map(p -> {
                    Action action = p.getAction();
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", action.getId());
                    map.put("code", action.getCode());
                    map.put("name", action.getName());
                    map.put("active", Boolean.TRUE.equals(action.getIsActive()));
                    map.put("permissionCode", p.getCode());
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * Tat ca actions dinh nghia cho menu (danh cho sysadmin) - kem active flag that
     * de FE phan biet action dang tat.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> actionsForSysadmin(String menuCode) {
        Menu menu = menuRepository.findByCode(menuCode.trim().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Menu not found: " + menuCode));
        return permissionRepository.findByMenuIdAndIsDeletedFalse(menu.getId()).stream()
                .map(p -> {
                    Action action = p.getAction();
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", action.getId());
                    map.put("code", action.getCode());
                    map.put("name", action.getName());
                    map.put("active", Boolean.TRUE.equals(action.getIsActive()));
                    map.put("permissionCode", p.getCode());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> toMap(Permission permission) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", permission.getId());
        map.put("code", permission.getCode());
        map.put("menuId", permission.getMenu().getId());
        map.put("menuCode", permission.getMenu().getCode());
        map.put("menuName", permission.getMenu().getName());
        map.put("actionId", permission.getAction().getId());
        map.put("actionCode", permission.getAction().getCode());
        map.put("actionName", permission.getAction().getName());
        map.put("description", permission.getDescription());
        return map;
    }
}
