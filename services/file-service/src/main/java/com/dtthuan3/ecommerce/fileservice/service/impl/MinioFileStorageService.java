package com.dtthuan3.ecommerce.fileservice.service.impl;

import com.dtthuan3.ecommerce.fileservice.config.MinioProperties;
import com.dtthuan3.ecommerce.fileservice.config.UploadProperties;
import com.dtthuan3.ecommerce.fileservice.dto.request.PresignUploadRequest;
import com.dtthuan3.ecommerce.fileservice.dto.response.*;
import com.dtthuan3.ecommerce.fileservice.exception.FileNotFoundInStorageException;
import com.dtthuan3.ecommerce.fileservice.exception.FileValidationException;
import com.dtthuan3.ecommerce.fileservice.exception.StorageException;
import com.dtthuan3.ecommerce.fileservice.scanner.VirusScanner;
import com.dtthuan3.ecommerce.fileservice.service.FileStorageService;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class MinioFileStorageService
        implements FileStorageService {


    // =========================================================
    // DEPENDENCY
    // =========================================================

    @Qualifier("minioClient")
    private final MinioClient minioClient;


    @Qualifier("presignMinioClient")
    private final MinioClient presignMinioClient;


    private final MinioProperties minioProperties;


    private final UploadProperties uploadProperties;


    private final VirusScanner virusScanner;


    @Qualifier("fileUploadExecutor")
    private final Executor fileUploadExecutor;


    // =========================================================
    // UPLOAD 1 FILE
    // =========================================================

    @Override
    public FileResponse upload(
            MultipartFile file,
            String folder
    ) {

        // 1. Validate
        validateFile(file);


        String contentType =
                file.getContentType();


        // 2. Quét virus ảnh / video
        scanVirusIfNeeded(
                file,
                contentType
        );


        // 3. Tạo key
        String key = generateKey(folder, file.getOriginalFilename());


        // 4. Upload MinIO
        try (
                InputStream inputStream = file.getInputStream()
        ) {

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(key)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );


            // 5. Thời gian URL
            long expiry = uploadProperties.defaultPresignExpirySeconds();


            // 6. Sinh URL
            String url = generateGetUrl(key, (int) expiry);


            // 7. Response
            return new FileResponse(
                    key,
                    minioProperties.bucket(),
                    file.getOriginalFilename(),
                    contentType,
                    file.getSize(),
                    url,
                    expiry
            );


        } catch (Exception e) {
            throw new StorageException("Không thể upload file lên MinIO", e);
        }
    }

    @Override
    public FileResponse upload(
            byte[] data,
            String filename,
            String contentType,
            String folder
    ) {

        if (data == null || data.length == 0) {
            throw new FileValidationException(
                    "Dữ liệu file không được rỗng"
            );
        }

        com.dtthuan3.ecommerce.fileservice.util.ByteArrayMultipartFile multipartFile =
                new com.dtthuan3.ecommerce.fileservice.util.ByteArrayMultipartFile(
                        data,
                        filename,
                        contentType
                );

        return upload(
                multipartFile,
                folder
        );
    }


    // =========================================================
    // UPLOAD NHIỀU FILE - ĐA LUỒNG
    // =========================================================

    @Override
    public BatchUploadResponse uploadBatch(List<MultipartFile> files, String folder) {

        // 1. Validate danh sách
        if (files == null || files.isEmpty()) {
            throw new FileValidationException("Danh sách file không được rỗng");
        }


        /*
         * Mỗi file được biến thành một CompletableFuture.
         *
         * Ví dụ:
         *
         * anh1.jpg  -> task 1
         * anh2.jpg  -> task 2
         * video.mp4 -> task 3
         */
        List<CompletableFuture<FileResponse>> futures = files.stream()
                .map(file ->
                        CompletableFuture.supplyAsync(
                                () -> upload(
                                        file,
                                        folder
                                ),
                                fileUploadExecutor
                        )
                )
                .toList();


        try {

            // =================================================
            // 2. CHỜ TẤT CẢ TASK XONG
            // =================================================

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();


            // =================================================
            // 3. LẤY RESPONSE CỦA TỪNG FILE
            // =================================================

            List<FileResponse> responses =
                    futures.stream()
                            .map(CompletableFuture::join)
                            .toList();


            // =================================================
            // 4. TRẢ RESPONSE
            // =================================================

            return new BatchUploadResponse(responses);
        } catch (CompletionException e) {

            /*
             * CompletableFuture bọc exception thật
             * trong CompletionException.
             *
             * Lấy exception thật ra.
             */
            Throwable cause = e.getCause();


            if (
                    cause instanceof RuntimeException runtimeException
            ) {
                throw runtimeException;
            }
            throw new StorageException("Không thể upload nhiều file", cause);
        }
    }


    // =========================================================
    // GET FILE INFO
    // =========================================================

    @Override
    public FileInfoResponse getInfo(String key) {
        validateKey(key);
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(key)
                            .build()
            );


            return new FileInfoResponse(
                    key,
                    minioProperties.bucket(),
                    stat.contentType(),
                    stat.size(),
                    stat.etag(),

                    stat.lastModified() != null
                            ? stat.lastModified().toInstant()
                            : null
            );


        } catch (ErrorResponseException e) {
            if (isFileNotFound(e)) {
                throw new FileNotFoundInStorageException("Không tìm thấy file: " + key);
            }


            throw new StorageException("Không thể lấy thông tin file", e);


        } catch (Exception e) {
            throw new StorageException("Không thể lấy thông tin file", e
            );
        }
    }


    // =========================================================
    // GET PRESIGNED URL
    // =========================================================

    @Override
    public UrlResponse getUrl(String key, Integer expirySeconds) {
        validateKey(key);
        getInfo(key);

        int expiry = normalizeExpiry(expirySeconds);
        String url = generateGetUrl(key, expiry);

        return new UrlResponse(key, url, expiry
        );
    }


    // =========================================================
    // PRESIGNED UPLOAD
    // =========================================================

    @Override
    public PresignUploadResponse presignUpload(PresignUploadRequest request) {

        if (request == null) {
            throw new FileValidationException("Request không được null");
        }
        if (request.getFilename() == null || request.getFilename().isBlank()) {
            throw new FileValidationException("Filename không được rỗng");
        }


        if (request.getContentType() == null || request.getContentType().isBlank()) {
            throw new FileValidationException("Content-Type không được rỗng");
        }
        validateContentType(request.getContentType());
        String key = generateKey(request.getFolder(), request.getFilename());

        int expiry = normalizeExpiry(request.getExpirySeconds());


        try {

            String uploadUrl = presignMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs
                            .builder()
                            .method(Method.PUT)
                            .bucket(minioProperties.bucket())
                            .object(key)
                            .expiry(expiry)
                            .build()
            );


            return new PresignUploadResponse(key, uploadUrl, request.getContentType(), expiry);
        } catch (Exception e) {
            throw new StorageException("Không thể tạo presigned upload URL", e);
        }
    }


    // =========================================================
    // DOWNLOAD FILE
    // =========================================================

    @Override
    public InputStream download(String key) {
        validateKey(key);
        // Kiểm tra file tồn tại
        getInfo(key);


        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(key)
                            .build()
            );


        } catch (Exception e) {
            throw new StorageException("Không thể download file: " + key, e);
        }
    }


    // =========================================================
    // DELETE FILE
    // =========================================================

    @Override
    public void delete(String key) {
        validateKey(key);

        // Kiểm tra tồn tại
        getInfo(key);


        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(key)
                            .build()
            );


        } catch (Exception e) {
            throw new StorageException("Không thể xóa file: " + key, e);
        }
    }


    // =========================================================
    // VIRUS SCAN
    // =========================================================

    private void scanVirusIfNeeded(MultipartFile file, String contentType) {

        if (contentType == null || contentType.isBlank()) {
            return;
        }

        boolean image = contentType.startsWith("image/");

        boolean video = contentType.startsWith("video/");


        /*
         * Chỉ scan ảnh và video theo yêu cầu hiện tại.
         */
        if (image || video) {
            virusScanner.scan(file);
        }
    }


    // =========================================================
    // VALIDATE FILE
    // =========================================================

    private void validateFile(MultipartFile file) {
        if (file == null) {
            throw new FileValidationException("File không được null");
        }

        if (file.isEmpty()) {
            throw new FileValidationException("File không được rỗng");
        }


        String contentType = file.getContentType();


        if (contentType == null || contentType.isBlank()) {
            throw new FileValidationException("Không xác định được Content-Type"
            );
        }


        validateContentType(contentType);
    }


    // =========================================================
    // VALIDATE CONTENT TYPE
    // =========================================================

    private void validateContentType(String contentType) {

        if (!uploadProperties.isContentTypeAllowed(contentType)) {
            throw new FileValidationException("Loại file không được hỗ trợ: " + contentType);
        }
    }


    // =========================================================
    // VALIDATE KEY
    // =========================================================

    private void validateKey(String key) {

        if (key == null || key.isBlank()) {
            throw new FileValidationException("File key không được rỗng");
        }
    }


    // =========================================================
    // GENERATE KEY
    // =========================================================

    private String generateKey(String folder, String originalFilename) {
        String safeFolder = normalizeFolder(folder);
        LocalDate now = LocalDate.now();
        String extension = getExtension(originalFilename);
        String fileName = UUID.randomUUID().toString();
        if (!extension.isBlank()) {
            fileName = fileName + "." + extension;
        }


        return "%s/%d/%02d/%s"
                .formatted(
                        safeFolder,
                        now.getYear(),
                        now.getMonthValue(),
                        fileName
                );
    }


    // =========================================================
    // NORMALIZE FOLDER
    // =========================================================

    private String normalizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return "uploads";
        }
        String result = folder.trim();
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        if (result.isBlank()) {
            return "uploads";
        }
        return result;
    }


    // =========================================================
    // GET EXTENSION
    // =========================================================

    private String getExtension(String filename) {

        if (filename == null || filename.isBlank()) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        String extension = filename.substring(dot + 1);
        return extension.replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase();
    }


    // =========================================================
    // GENERATE PRESIGNED GET URL
    // =========================================================

    private String generateGetUrl(String key, int expiry) {
        try {
            return presignMinioClient
                    .getPresignedObjectUrl(
                            GetPresignedObjectUrlArgs
                                    .builder()
                                    .method(Method.GET)
                                    .bucket(minioProperties.bucket())
                                    .object(key)
                                    .expiry(expiry)
                                    .build()
                    );


        } catch (Exception e) {
            throw new StorageException("Không thể tạo URL cho file", e);
        }
    }


    // =========================================================
    // NORMALIZE EXPIRY
    // =========================================================

    private int normalizeExpiry(Integer expirySeconds) {
        long defaultExpiry = uploadProperties.defaultPresignExpirySeconds();
        long maxExpiry = uploadProperties.maxPresignExpirySeconds();
        long expiry = expirySeconds == null ? defaultExpiry : expirySeconds;
        if (expiry <= 0) {
            expiry = defaultExpiry;
        }
        if (expiry > maxExpiry) {
            expiry = maxExpiry;
        }
        return (int) expiry;
    }


    // =========================================================
    // CHECK FILE NOT FOUND
    // =========================================================

    private boolean isFileNotFound(ErrorResponseException e) {
        if (e.errorResponse() == null) {
            return false;
        }


        String code = e.errorResponse().code();


        return "NoSuchKey".equals(code)
                || "NoSuchObject".equals(code)
                || "NoSuchBucket".equals(code);
    }
}