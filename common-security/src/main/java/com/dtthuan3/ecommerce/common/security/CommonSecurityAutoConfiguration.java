package com.dtthuan3.ecommerce.common.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * Auto-config cho resource-server chung. Service chỉ cần pull common-security
 * là có sẵn JwtAuthenticationConverter dùng JwtRoleConverter chuẩn Keycloak.
 */
@Configuration
@ConditionalOnClass(Jwt.class)
public class CommonSecurityAutoConfiguration {

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
    public JwtRoleConverter jwtRoleConverter() {
        return new JwtRoleConverter();
    }

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
    public JwtAuthenticationConverter jwtAuthenticationConverter(JwtRoleConverter jwtRoleConverter) {
        JwtAuthenticationConverter c = new JwtAuthenticationConverter();
        c.setJwtGrantedAuthoritiesConverter(jwtRoleConverter);
        return c;
    }
}
