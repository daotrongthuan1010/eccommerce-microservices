package com.dtthuan3.ecommerce.fileservice.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Tu tao bucket khi khoi dong. Loi MinIO chi log canh bao, khong lam service sap. */
@Component
public class BucketInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BucketInitializer.class);

    private final MinioClient minioClient;
    private final MinioProperties props;

    public BucketInitializer(@Qualifier("minioClient") MinioClient minioClient, MinioProperties props) {
        this.minioClient = minioClient;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!props.autoCreateBucket()) {
            return;
        }
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(props.bucket()).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(props.bucket()).build());
                log.info("Created MinIO bucket '{}'", props.bucket());
            }
        } catch (Exception e) {
            log.warn("Cannot verify/create MinIO bucket '{}': {}", props.bucket(), e.getMessage());
        }
    }
}
