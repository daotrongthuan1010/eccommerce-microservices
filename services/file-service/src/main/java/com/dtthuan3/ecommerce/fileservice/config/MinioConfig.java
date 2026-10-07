package com.dtthuan3.ecommerce.fileservice.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {
    //dùng để upload delete stat download
    @Bean
    public MinioClient minioClient(MinioProperties props) {
        return MinioClient.builder()
                .endpoint(props.endpoint())
                .credentials(props.accessKey(), props.secretKey())
                .region(props.region())
                .build();
    }
    //dùng để tạo URL tạm thời.
    @Bean
    public MinioClient presignMinioClient(MinioProperties props) {
        return MinioClient.builder()
                .endpoint(props.effectivePublicEndpoint())
                .credentials(props.accessKey(), props.secretKey())
                .region(props.region())
                .build();
    }
}
