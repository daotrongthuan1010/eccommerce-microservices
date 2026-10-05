package com.dtthuan3.ecommerce.fileservice.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    /** Client chinh: upload/download/xoa, noi toi MinIO qua endpoint noi bo. */
    @Bean
    public MinioClient minioClient(MinioProperties props) {
        return MinioClient.builder()
                .endpoint(props.endpoint())
                .credentials(props.accessKey(), props.secretKey())
                .region(props.region())
                .build();
    }

    /**
     * Client chi dung de ky presigned URL. Chu ky S3 gan voi host nen URL phai duoc ky bang
     * endpoint ma client ben ngoai truy cap duoc. Da set region nen khong goi mang khi ky.
     */
    @Bean
    public MinioClient presignMinioClient(MinioProperties props) {
        return MinioClient.builder()
                .endpoint(props.effectivePublicEndpoint())
                .credentials(props.accessKey(), props.secretKey())
                .region(props.region())
                .build();
    }
}
