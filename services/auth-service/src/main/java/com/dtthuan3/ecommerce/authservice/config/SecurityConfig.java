package com.dtthuan3.ecommerce.authservice.config;

import com.dtthuan3.ecommerce.authservice.security.JwtRoleConverter;
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
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            com.dtthuan3.ecommerce.authservice.iam.web.InternalServiceAuthFilter internalServiceAuthFilter)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(
                        internalServiceAuthFilter,
                        org.springframework.security.web.access.intercept.AuthorizationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // P0 (giu o P2): /internal/** chi dung noi bo giua cac service.
                        // P2 mo them ROLE_INTERNAL: gateway goi kem X-Internal-Token (shared secret
                        // trong cluster). Public van bi chan vi gateway denyAll /api/*/internal/**.
                        .requestMatchers("/internal/**")
                        .hasAnyRole("ADMIN", "INTERNAL")
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/refresh", "/auth/logout",
                                        "/auth/register", "/auth/forgot-password")
                                .permitAll()
                                .requestMatchers(HttpMethod.GET, "/auth/me")
                                .authenticated()
                                .requestMatchers(HttpMethod.POST, "/auth/change-password", "/auth/send-verify-email")
                                .authenticated()
                        .requestMatchers("/actuator/**")
                        .permitAll()
                        .requestMatchers("/auth/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/auth/**")
                        .authenticated()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }
}
