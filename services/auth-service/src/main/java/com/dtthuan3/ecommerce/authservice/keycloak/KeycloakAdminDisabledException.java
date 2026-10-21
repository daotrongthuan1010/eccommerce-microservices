package com.dtthuan3.ecommerce.authservice.keycloak;

/**
 * Nem khi thao tac can Keycloak Admin API (register, forgot/change password,
 * verify email) nhung service account chua duoc cau hinh
 * ({@code auth.keycloak.admin.client-secret} de trong).
 */
public class KeycloakAdminDisabledException extends RuntimeException {

    public KeycloakAdminDisabledException() {
        super("Keycloak admin service account is not configured");
    }
}
