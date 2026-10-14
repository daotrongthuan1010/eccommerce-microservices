package com.dtthuan3.ecommerce.fileservice.storage;

import com.dtthuan3.ecommerce.fileservice.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BucketInitializer implements ApplicationRunner {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public BucketInitializer(
            @Qualifier("minioClient") MinioClient minioClient,
            MinioProperties minioProperties
    ) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!minioProperties.autoCreateBucket()) {
            log.info("MinIO auto create bucket is disabled");
            return;
        }
        int maxRetries = 3;
        long backoffMs = 5000;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String bucket = minioProperties.bucket();
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(bucket).build()
                );
                if (!exists) {
                    MakeBucketArgs.Builder builder = MakeBucketArgs.builder().bucket(bucket);
                    if (minioProperties.region() != null && !minioProperties.region().isBlank()) {
                        builder.region(minioProperties.region());
                    }
                    minioClient.makeBucket(builder.build());
                    log.info("Created MinIO bucket: {}", bucket);
                } else {
                    log.info("MinIO bucket already exists: {}", bucket);
                }
                return;
            } catch (Exception e) {
                log.warn("Bucket init attempt {}/{} failed: {}", attempt, maxRetries, e.getMessage());
                if (attempt == maxRetries) {
                    log.error("Cannot initialize MinIO bucket {} after {} attempts — service continues in degraded mode", minioProperties.bucket(), maxRetries, e);
                    // do NOT throw — let service start, health indicator will report DOWN
                    return;
                }
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Bucket init interrupted");
                    return;
                }
            }
        }
    }
}
