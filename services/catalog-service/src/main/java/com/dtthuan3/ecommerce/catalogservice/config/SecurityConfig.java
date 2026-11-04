package com.dtthuan3.ecommerce.catalogservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Marketplace guest browse (2026-11-05 fe-marketplace D3): GET cong khai de
 * khach xem danh muc/brand/collection truoc khi login (kieu Shopee/Tiki).
 * Write van can JWT + @PreAuthorize tren controller (ADMIN/CATALOG_MANAGER).
 * Chain nay thay the default cua common-security (anyRequest authenticated).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        // JwtRoleConverter tu common-security (bean auto-config cung cap).
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                new com.dtthuan3.ecommerce.common.security.JwtRoleConverter());
        return converter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**")
                        .permitAll()
                        // GET moi thu trong catalog deu cong khai (chi la du lieu
                        // danh muc, khong PII). Admin path van chuyen ve rule role.
                        .requestMatchers(HttpMethod.GET, "/**")
                        .permitAll()
                        // Write: controller da co @PreAuthorize chi tiet; o day chi
                        // dam bao khong cho anonymous ghi.
                        .requestMatchers(HttpMethod.POST, "/**")
                        .authenticated()
                        .requestMatchers(HttpMethod.PUT, "/**")
                        .authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/**")
                        .authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/**")
                        .authenticated()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }
}
