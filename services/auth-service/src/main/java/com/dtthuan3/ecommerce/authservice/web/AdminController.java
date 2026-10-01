package com.dtthuan3.ecommerce.authservice.web;

import com.dtthuan3.ecommerce.authservice.security.SecurityUtils;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/admin")
public class AdminController {

    @GetMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> adminMe() {
        var user = SecurityUtils.requireUser();
        return Map.of(
                "subject", String.valueOf(user.subject()),
                "username", String.valueOf(user.username()),
                "roles", user.roles() == null ? java.util.List.of() : user.roles());
    }
}
