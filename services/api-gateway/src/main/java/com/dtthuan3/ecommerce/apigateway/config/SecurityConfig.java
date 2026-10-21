package com.dtthuan3.ecommerce.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                        // P0: /internal/** la API noi bo giua cac service, KHONG expose public qua gateway.
                        // Chan truoc moi rule permitAll de /api/auth/internal/** khong lot qua rule /api/auth/**.
                        .pathMatchers("/api/*/internal/**")
                        .denyAll()
                        // Dat rule admin TRUOC rule /api/auth/** permitAll ben duoi: neu khong
                        // /api/auth/admin/me se lot qua permitAll o gateway (van bi service chan
                        // 403, nhung gateway nen chan som + thong nhat voi moi service).
                        .pathMatchers("/api/*/admin/**")
                        .hasRole("ADMIN")
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
