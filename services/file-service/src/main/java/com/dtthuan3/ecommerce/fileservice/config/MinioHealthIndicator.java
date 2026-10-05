package com.dtthuan3.ecommerce.fileservice.config;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("minio")
public class MinioHealthIndicator implements HealthIndicator {

    private final MinioClient minioClient;
    private final MinioProperties props;

    public MinioHealthIndicator(@Qualifier("minioClient") MinioClient minioClient, MinioProperties props) {
        this.minioClient = minioClient;
        this.props = props;
    }

    @Override
    public Health health() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(props.bucket()).build());
            return exists ? Health.up().build()
                    : Health.down().withDetail("reason", "bucket not found").build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
