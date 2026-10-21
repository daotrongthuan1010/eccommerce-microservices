package com.dtthuan3.ecommerce.authservice.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "username is required") String username,
        @NotBlank(message = "password is required") @Size(min = 1, max = 256) String password) {}
