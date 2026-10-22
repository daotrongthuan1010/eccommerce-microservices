package com.dtthuan3.ecommerce.authservice.web;

import com.dtthuan3.ecommerce.authservice.iam.service.AuthContextService;
import com.dtthuan3.ecommerce.authservice.security.SecurityUtils;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthContextService authContextService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> adminMe() {
        var user = SecurityUtils.requireUser();
        return Map.of(
                "subject", String.valueOf(user.subject()),
                "username", String.valueOf(user.username()),
                "roles", user.roles() == null ? java.util.List.of() : user.roles());
    }

    /**
     * P2: moc xoa cache iam:ctx sau khi gan/thu hoi role hoac doi quyen.
     * Moi thay doi gan quyen (qua Keycloak mapper, SQL truc tiep, hay API tuong lai)
     * phai goi endpoint nay de gateway khong dung context cu (TTL 30 phut).
     */
    @PostMapping("/users/{userId}/context/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> refreshUserContext(@PathVariable String userId) {
        authContextService.refreshContext(userId);
        return Map.of("userId", userId, "refreshed", true);
    }

    /** Xoa cache hang loat sau khi doi role cua group hoac permission cua role. */
    @PostMapping("/users/context/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> refreshUsersContext(@RequestBody List<String> userIds) {
        authContextService.invalidateAll(userIds);
        return Map.of("refreshed", userIds == null ? 0 : userIds.size());
    }
}
