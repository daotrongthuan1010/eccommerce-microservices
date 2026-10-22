package com.dtthuan3.ecommerce.authservice.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
        @NotBlank(message = "username is required") @Size(max = 256) String username) {}
