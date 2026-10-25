package com.dtthuan3.ecommerce.authservice.iam.web;

import com.dtthuan3.ecommerce.authservice.iam.service.IamQueryService;
import com.dtthuan3.ecommerce.authservice.security.SecurityUtils;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sider dong: chi tra menu ma caller duoc phep VIEW (sysadmin thay tat ca).
 * FE render sider truc tiep tu endpoint nay, khong hardcode menu.
 */
@RestController
@RequestMapping("/auth/me")
@RequiredArgsConstructor
public class MeMenuController {

    private final IamQueryService iamQueryService;

    @GetMapping("/menus")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> menus() {
        var caller = SecurityUtils.requireUser();
        List<Map<String, Object>> menus =
                iamQueryService.menusForUser(caller.subject(), caller.isAdmin());
        return Map.of("menus", menus, "total", menus.size());
    }

    @GetMapping("/permissions")
    @PreAuthorize("isAuthenticated()")
    public List<String> permissions() {
        var caller = SecurityUtils.requireUser();
        if (caller.isAdmin() || iamQueryService.isDbSystemAdmin(caller.subject())) {
            return iamQueryService.allPermissionCodes();
        }
        return iamQueryService.dbPermissionCodes(caller.subject()).stream().sorted().toList();
    }
}
