package com.dtthuan3.ecommerce.orderservice.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Forward JWT nguoi dung khi order goi inventory/payment (lanh truyen quyen:
 * service sau check role/ownership tren principal that, khong tin caller).
 */
@Configuration
public class FeignSecurityConfig {

    @Bean
    public RequestInterceptor bearerTokenRequestInterceptor() {
        return requestTemplate -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwt) {
                requestTemplate.header("Authorization", "Bearer " + jwt.getToken().getTokenValue());
            }
        };
    }
}
