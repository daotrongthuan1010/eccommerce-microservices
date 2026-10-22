package com.dtthuan3.ecommerce.authservice.iam.dto;

/** Body cho POST /internal/auth/provision - gateway goi khi can tao user tuong minh. */
public record ProvisionRequest(String userId, String username, String fullName, String email) {}
