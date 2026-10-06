package com.dtthuan3.ecommerce.fileservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.antivirus.clamav")
public record ClamAvProperties(
        String host,
        int port,
        int connectTimeout,
        int readTimeout,
        boolean enabled
) {
}