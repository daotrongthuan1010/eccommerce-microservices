package com.dtthuan3.ecommerce.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Auto-config mặc định cho service muốn dùng ngay mà không cần viết SecurityConfig.
 * Nếu service tự khai SecurityFilterChain thì bean này không tạo (ConditionalOnMissingBean).
 * Bật method security toàn cục.
 */
@Configuration
@EnableMethodSecurity
public class DefaultSecurityAutoConfiguration {

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(SecurityFilterChain.class)
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(JwtAuthenticationConverter.class)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http, JwtAuthenticationConverter converter) throws Exception {
        http.csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.requestMatchers("/actuator/**").permitAll().anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter)));
        return http.build();
    }
}
