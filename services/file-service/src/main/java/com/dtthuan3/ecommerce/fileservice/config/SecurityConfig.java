package com.dtthuan3.ecommerce.fileservice.config;

import com.dtthuan3.ecommerce.common.security.SecuritySupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Mẫu cho các service khác học theo: dùng common-security (JwtRoleConverter auto-config)
 * + SecuritySupport để dựng chain. Chỉ còn rule đặc thù DELETE /files -> ADMIN.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {
        SecuritySupport.withDeleteAdminOnly(http, jwtAuthenticationConverter, "/files", "/files/**");
        return http.build();
    }
}
