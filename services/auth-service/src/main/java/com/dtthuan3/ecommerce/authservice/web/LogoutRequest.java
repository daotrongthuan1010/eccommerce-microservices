package com.dtthuan3.ecommerce.authservice.web;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank(message = "refreshToken is required") String refreshToken,
        // Tuy chon: Keycloak logout chap nhan id_token_hint thay/dong thoi voi refresh_token.
        String idTokenHint) {}
