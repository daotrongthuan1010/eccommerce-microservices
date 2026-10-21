package com.dtthuan3.ecommerce.authservice.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "currentPassword is required") String currentPassword,
        @NotBlank(message = "newPassword is required")
                @Size(min = 8, max = 128, message = "newPassword must be at least 8 chars")
                String newPassword) {}
