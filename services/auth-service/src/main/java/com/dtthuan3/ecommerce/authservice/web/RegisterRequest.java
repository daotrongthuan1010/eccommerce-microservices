package com.dtthuan3.ecommerce.authservice.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username is required")
                @Size(min = 3, max = 64, message = "username must be 3-64 chars")
                @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "username has invalid characters")
                String username,
        @NotBlank(message = "email is required") @Email(message = "email is invalid") String email,
        @NotBlank(message = "password is required")
                @Size(min = 8, max = 128, message = "password must be at least 8 chars")
                String password,
        @Size(max = 64) String firstName,
        @Size(max = 64) String lastName,
        /** seller=true: gan them realm role SELLER (role only, chua co bang seller_profiles/CCCD). */
        Boolean seller) {}
