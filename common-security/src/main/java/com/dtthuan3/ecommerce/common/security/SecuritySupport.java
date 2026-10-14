package com.dtthuan3.ecommerce.common.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Helper để service tự dựng SecurityFilterChain nhanh mà vẫn thống nhất.
 * Ví dụ trong service:
 * <pre>
 *  @Bean
 *  SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationConverter conv) throws Exception {
 *      return SecuritySupport.defaultChain(http, conv)
 *          .authorizeHttpRequests(a -> a.requestMatchers(HttpMethod.DELETE, "/files/**").hasRole("ADMIN").anyRequest().authenticated())
 *          .build();
 *  }
 * </pre>
 */
public final class SecuritySupport {

    private SecuritySupport() {}

    public static HttpSecurity defaultChain(HttpSecurity http, JwtAuthenticationConverter converter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.requestMatchers("/actuator/**").permitAll().anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter)));
        return http;
    }

    public static HttpSecurity permitAllActuator(HttpSecurity http, JwtAuthenticationConverter converter) throws Exception {
        return defaultChain(http, converter);
    }

    public static HttpSecurity withDeleteAdminOnly(HttpSecurity http, JwtAuthenticationConverter converter, String... patterns) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> {
                a.requestMatchers("/actuator/**").permitAll();
                if (patterns != null && patterns.length > 0) {
                    a.requestMatchers(HttpMethod.DELETE, patterns).hasRole("ADMIN");
                }
                a.anyRequest().authenticated();
            })
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter)));
        return http;
    }
}
