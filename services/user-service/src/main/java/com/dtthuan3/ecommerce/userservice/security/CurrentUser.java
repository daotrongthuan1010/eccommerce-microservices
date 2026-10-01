package com.dtthuan3.ecommerce.userservice.security;

import java.util.List;

/** Thong tin user hien tai trich tu JWT, dung chung cho user-service. */
public record CurrentUser(String subject, String username, String email, List<String> roles) {

    public boolean hasRole(String role) {
        String normalized = role.startsWith("ROLE_") ? role.substring("ROLE_".length()) : role;
        return roles != null && roles.contains(normalized);
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
}
