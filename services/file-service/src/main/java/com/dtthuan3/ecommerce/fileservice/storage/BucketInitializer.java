package com.dtthuan3.ecommerce.fileservice.storage.minio;

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

        // Nếu không cho phép tự động tạo bucket thì bỏ qua
        if (!minioProperties.autoCreateBucket()) {
            log.info("MinIO auto create bucket is disabled");
            return;
        }

        try {

            String bucket = minioProperties.bucket();

            // Kiểm tra bucket đã tồn tại chưa
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucket)
                            .build()
            );

            // Nếu chưa tồn tại thì tạo mới
            if (!exists) {

                MakeBucketArgs.Builder builder =
                        MakeBucketArgs.builder()
                                .bucket(bucket);

                // Nếu có region thì thêm region
                if (minioProperties.region() != null
                        && !minioProperties.region().isBlank()) {

                    builder.region(
                            minioProperties.region()
                    );
                }

                minioClient.makeBucket(
                        builder.build()
                );

                log.info(
                        "Created MinIO bucket: {}",
                        bucket
                );

            } else {

                log.info(
                        "MinIO bucket already exists: {}",
                        bucket
                );
            }

        } catch (Exception e) {

            log.error(
                    "Cannot initialize MinIO bucket: {}",
                    minioProperties.bucket(),
                    e
            );

            throw new IllegalStateException(
                    "Không thể khởi tạo MinIO bucket",
                    e
            );
        }
    }
}