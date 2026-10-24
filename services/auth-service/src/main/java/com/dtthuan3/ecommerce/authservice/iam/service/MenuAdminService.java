package com.dtthuan3.ecommerce.authservice.iam.service;

import com.dtthuan3.ecommerce.authservice.iam.entity.Menu;
import com.dtthuan3.ecommerce.authservice.iam.entity.Permission;
import com.dtthuan3.ecommerce.authservice.iam.repository.MenuRepository;
import com.dtthuan3.ecommerce.authservice.iam.repository.PermissionRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MenuAdminService {

    private final MenuRepository menuRepository;
    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return menuRepository.findAllByIsDeletedFalseOrderByOrderNoAscIdAsc().stream()
                .map(this::toMap).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> create(String code, String name, String path, String icon,
            Integer orderNo, Long parentId) {
        String upper = code.trim().toUpperCase();
        if (menuRepository.existsByCode(upper)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Menu code already exists: " + upper);
        }
        Menu menu = Menu.builder().code(upper).name(name).path(path).icon(icon)
                .orderNo(orderNo).isActive(true).isDeleted(false).isDefault(false).build();
        if (parentId != null) {
            Menu parent = menuRepository.findById(parentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Parent menu not found: " + parentId));
            menu.setParent(parent);
        }
        return toMap(menuRepository.save(menu));
    }

    @Transactional
    public Map<String, Object> update(Long id, String name, String path, String icon, Integer orderNo,
            Long parentId) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu not found: " + id));
        if (Boolean.TRUE.equals(menu.getIsDeleted())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu not found: " + id);
        }
        if (name != null) {
            menu.setName(name);
        }
        if (path != null) {
            menu.setPath(path);
        }
        if (icon != null) {
            menu.setIcon(icon);
        }
        if (orderNo != null) {
            menu.setOrderNo(orderNo);
        }
        if (parentId != null) {
            if (parentId.equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Menu cannot be its own parent");
            }
            Menu parent = menuRepository.findById(parentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Parent menu not found: " + parentId));
            if (Boolean.TRUE.equals(parent.getIsDeleted())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Parent menu not found: " + parentId);
            }
            Menu ancestor = parent;
            while (ancestor != null) {
                if (ancestor.getId() != null && ancestor.getId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Cannot set parent: would create a cycle");
                }
                ancestor = ancestor.getParent();
            }
            menu.setParent(parent);
        } else {
            menu.setParent(null);
        }
        return toMap(menuRepository.save(menu));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu not found: " + id));
        return toMap(menu);
    }

    @Transactional
    public void delete(Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu not found: " + id));
        if (Boolean.TRUE.equals(menu.getIsDeleted())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu not found: " + id);
        }
        if (Boolean.TRUE.equals(menu.getIsDefault())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete a default system menu");
        }
        if (!menuRepository.findByParentIdAndIsDeletedFalse(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete menu with children. Delete or move child menus first");
        }
        for (Permission permission : permissionRepository.findByMenuIdAndIsDeletedFalse(id)) {
            permission.setIsDeleted(true);
            permissionRepository.save(permission);
        }
        menu.setIsDeleted(true);
        menu.setIsActive(false);
        menuRepository.save(menu);
    }

    public Map<String, Object> toMap(Menu menu) {
        return Map.of(
                "id", menu.getId(),
                "code", menu.getCode(),
                "name", menu.getName(),
                "path", menu.getPath() == null ? "" : menu.getPath(),
                "icon", menu.getIcon() == null ? "" : menu.getIcon(),
                "parentId", menu.getParent() == null ? (Object) "" : menu.getParent().getId(),
                "orderNo", menu.getOrderNo() == null ? (Object) "" : menu.getOrderNo(),
                "active", Boolean.TRUE.equals(menu.getIsActive()),
                "default", Boolean.TRUE.equals(menu.getIsDefault()));
    }
}
