package com.dtthuan3.ecommerce.authservice.security;

import java.util.List;

/** Thong tin user hien tai trich tu JWT, dung chung cho moi service. */
public record CurrentUser(String subject, String username, String email, List<String> roles) {

    public boolean hasRole(String role) {
        String normalized = role.startsWith("ROLE_") ? role.substring("ROLE_".length()) : role;
        return roles != null && roles.contains(normalized);
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Role cao nhat theo thu bac ADMIN > MODERATOR > USER — dung quyet dinh
     * role nao duoc gan cho cap duoi (chong leo thang quyen).
     */
    public String highestRole() {
        if (hasRole("ADMIN")) {
            return "ADMIN";
        }
        if (hasRole("MODERATOR")) {
            return "MODERATOR";
        }
        return "USER";
    }
}
