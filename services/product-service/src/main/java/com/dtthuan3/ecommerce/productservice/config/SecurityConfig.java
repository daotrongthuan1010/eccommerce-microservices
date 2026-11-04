package com.dtthuan3.ecommerce.productservice.config;

import com.dtthuan3.ecommerce.productservice.security.JwtRoleConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new JwtRoleConverter());
        return converter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/**")
                        .permitAll()
                        // Marketplace guest browse (2026-11-05 fe-marketplace D3):
                        // GET cong khai de khach xem san pham khong can login.
                        // Nhom A (/products, /skus, ...) + nhom B (/api/v1/... doubled
                        // prefix qua gateway). Write giu nguyen rule role ben duoi.
                        .requestMatchers(HttpMethod.GET, "/products", "/products/**",
                                        "/skus", "/skus/**",
                                        "/products-variant", "/products-variant/**",
                                        "/products-media", "/products-media/**",
                                        "/attribute-templates", "/attribute-templates/**",
                                        "/api/v1/**")
                        .permitAll()
                        .requestMatchers("/products/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/products", "/products/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/products/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/products/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }
}
