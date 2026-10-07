package com.dtthuan3.ecommerce.catalogservice.service.impl;

import com.dtthuan3.ecommerce.catalogservice.service.MinioService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;

    @Value("${storage.minio.bucket}")
    private String bucket;

    @Override
    public String upload(MultipartFile file, String folder) {

        try {

            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File không được để trống");
            }

            // Lấy extension của file
            String originalFilename = file.getOriginalFilename();

            String extension = "";

            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(
                        originalFilename.lastIndexOf(".")
                );
            }

            // Ngày hiện tại
            LocalDate today = LocalDate.now();

            String year = today.format(
                    DateTimeFormatter.ofPattern("yyyy")
            );

            String month = today.format(
                    DateTimeFormatter.ofPattern("MM")
            );

            String day = today.format(
                    DateTimeFormatter.ofPattern("dd")
            );

            // Tạo object key
            String objectKey =
                    folder + "/"
                            + year + "/"
                            + month + "/"
                            + day + "/"
                            + UUID.randomUUID()
                            + extension;

            // Upload lên MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(
                                    file.getInputStream(),
                                    file.getSize(),
                                    -1
                            )
                            .contentType(file.getContentType())
                            .build()
            );

            return objectKey;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Upload file lên MinIO thất bại",
                    e
            );
        }
    }

    @Override
    public String getPresignedUrl(String objectKey) {

        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        try {

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Không thể tạo URL MinIO",
                    e
            );
        }
    }

    @Override
    public void delete(String objectKey) {

        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        try {

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Xóa file trên MinIO thất bại",
                    e
            );
        }
    }
}