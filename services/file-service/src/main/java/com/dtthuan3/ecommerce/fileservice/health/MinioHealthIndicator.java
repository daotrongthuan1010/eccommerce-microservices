package com.dtthuan3.ecommerce.fileservice.health;

import com.dtthuan3.ecommerce.fileservice.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class MinioHealthIndicator implements HealthIndicator {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public MinioHealthIndicator(
            @Qualifier("minioClient") MinioClient minioClient,
            MinioProperties minioProperties
    ) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    @Override
    public Health health() {

        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioProperties.bucket())
                            .build()
            );

            if (!bucketExists) {
                return Health.down()
                        .withDetail("minio", "MinIO connected")
                        .withDetail("bucket", minioProperties.bucket())
                        .withDetail("bucketExists", false)
                        .withDetail("message", "Bucket không tồn tại")
                        .build();
            }

            return Health.up()
                    .withDetail("minio", "MinIO connected")
                    .withDetail("bucket", minioProperties.bucket())
                    .withDetail("bucketExists", true)
                    .build();

        } catch (Exception e) {

            return Health.down()
                    .withDetail("minio", "Cannot connect to MinIO")
                    .withDetail("bucket", minioProperties.bucket())
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}