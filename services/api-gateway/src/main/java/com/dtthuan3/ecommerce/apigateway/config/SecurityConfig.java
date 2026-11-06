package com.dtthuan3.ecommerce.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(new JwtRoleConverter());
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        // Preflight CORS (OPTIONS) luon cho qua: CorsWebFilter xu ly
                        // truoc, rule nay la luoi an toan neu thu tu filter doi.
                        .pathMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()
                        // P0: /internal/** la API noi bo giua cac service, KHONG expose public qua gateway.
                        // Chan truoc moi rule permitAll de /api/auth/internal/** khong lot qua rule /api/auth/**.
                        .pathMatchers("/api/*/internal/**")
                        .denyAll()
                        // Dat rule admin TRUOC rule /api/auth/** permitAll ben duoi: neu khong
                        // /api/auth/admin/me se lot qua permitAll o gateway (van bi service chan
                        // 403, nhung gateway nen chan som + thong nhat voi moi service).
                        .pathMatchers("/api/*/admin/**")
                        .hasRole("ADMIN")
                        // IPN/callback tu gateway thanh toan (VNPAY/ZaloPay) va nha xe
                        // (carrier webhook): KHONG co JWT — bao ve bang HMAC o service layer.
                        // Gateway phai cho qua, service verify chu ky + timestamp + dedup.
                        .pathMatchers("/api/payment/payments/webhooks/**",
                                      "/api/shipping/shipments/webhooks/**")
                        .permitAll()
                        // Marketplace guest browse (2026-11-05 fe-marketplace D3):
                        // GET cong khai de khach xem san pham/danh muc/review truoc khi
                        // login (kieu Shopee/Tiki). CHI GET — moi write van roi vao
                        // rule /api/** authenticated ben duoi. Rule phai dung SAU
                        // denyAll internal + admin de khong mo lo /api/*/internal/**.
                        .pathMatchers(HttpMethod.GET,
                                      "/api/catalog/**",
                                      "/api/product/**",
                                      "/api/review/**",
                                      // vi-search 2026-11-06: guest tim kiem
                                      // (search-service GET /products permitAll;
                                      // /admin/reindex van roi vao rule admin o tren).
                                      "/api/search/**")
                        .permitAll()
                        .pathMatchers("/api/auth/**", "/actuator/**")
                        .permitAll()
                        .pathMatchers("/api/**")
                        .authenticated()
                        .anyExchange()
                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                        jwt -> jwt.jwtAuthenticationConverter(new ReactiveJwtAuthenticationConverterAdapter(jwtConverter))));
        return http.build();
    }
}
