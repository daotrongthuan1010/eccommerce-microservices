package com.dtthuan3.ecommerce.fileservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "storage.minio")
public record MinioProperties(
        String endpoint,
        String publicEndpoint,
        String accessKey,
        String secretKey,
        String bucket,
        @DefaultValue("us-east-1") String region,
        @DefaultValue("true") boolean autoCreateBucket) {

    public String effectivePublicEndpoint() {
        return (publicEndpoint == null || publicEndpoint.isBlank()) ? endpoint : publicEndpoint;
    }
}
