package com.dtthuan3.ecommerce.notificationservice.provider;

/**
 * Ket qua resolve nguoi nhan — phan biet ro tung case de xu ly dung (NOTI-06):
 * khong phai case nao cung la "retry".
 */
public sealed interface RecipientResolveResult {

    /** Tim thay user co email hop le. */
    record Found(String email, String displayName) implements RecipientResolveResult {}

    /** User khong ton tai trong Keycloak (404): PERMANENT — tao tac vụ kiem tra lien he, khong thu lai. */
    record UserNotFound() implements RecipientResolveResult {}

    /** User ton tai nhung khong co email: PERMANENT — can bo sung lien he, khong thu lai mù. */
    record NoEmail(String displayName) implements RecipientResolveResult {}

    /** Chua cau hinh Keycloak admin / Keycloak down / timeout: RETRYABLE — thu lai sau. */
    record Unavailable(String reason) implements RecipientResolveResult {}
}
