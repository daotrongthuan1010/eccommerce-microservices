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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
public class MinioFileStorageService implements FileStorageService {

    private static final long MAX_SINGLE_FILE_SIZE = 50L * 1024 * 1024; // 50MB
    private static final long MAX_BATCH_TOTAL_SIZE = 200L * 1024 * 1024; // 200MB
    private static final int MAX_BATCH_COUNT = 20;

    private final MinioClient minioClient;
    private final MinioClient presignMinioClient;
    private final MinioProperties minioProperties;
    private final UploadProperties uploadProperties;
    private final VirusScanner virusScanner;
    private final Executor fileUploadExecutor;

    public MinioFileStorageService(
            @Qualifier("minioClient") MinioClient minioClient,
            @Qualifier("presignMinioClient") MinioClient presignMinioClient,
            MinioProperties minioProperties,
            UploadProperties uploadProperties,
            VirusScanner virusScanner,
            @Qualifier("fileUploadExecutor") Executor fileUploadExecutor) {
        this.minioClient = minioClient;
        this.presignMinioClient = presignMinioClient;
        this.minioProperties = minioProperties;
        this.uploadProperties = uploadProperties;
        this.virusScanner = virusScanner;
        this.fileUploadExecutor = fileUploadExecutor;
    }

    @Override
    public FileResponse upload(MultipartFile file, String folder) {
        validateFile(file);
        String contentType = file.getContentType();
        if (file.getSize() > MAX_SINGLE_FILE_SIZE) {
            throw new FileValidationException("File vượt quá giới hạn 50MB: " + file.getOriginalFilename());
        }
        // Cache bytes once to avoid double-read after virus scan
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new StorageException("Không thể đọc file", e);
        }
        // wrap with cached multipart for scan + upload
        com.dtthuan3.ecommerce.fileservice.util.ByteArrayMultipartFile cached =
                new com.dtthuan3.ecommerce.fileservice.util.ByteArrayMultipartFile(bytes, file.getOriginalFilename(), contentType);
        scanVirusIfNeeded(cached, contentType);

        String key = generateKey(folder, file.getOriginalFilename());
        String safeKey = sanitizeKey(key);

        try (InputStream inputStream = cached.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(safeKey)
                            .stream(inputStream, bytes.length, -1)
                            .contentType(contentType)
                            .build()
            );
            long expiry = uploadProperties.defaultPresignExpirySeconds();
            String url = generateGetUrl(safeKey, (int) expiry);
            return new FileResponse(safeKey, minioProperties.bucket(), file.getOriginalFilename(), contentType, bytes.length, url, expiry);
        } catch (Exception e) {
            if (e instanceof FileValidationException || e instanceof StorageException) throw (RuntimeException) e;
            throw new StorageException("Không thể upload file lên MinIO", e);
        }
    }

    @Override
    public FileResponse upload(byte[] data, String filename, String contentType, String folder) {
        if (data == null || data.length == 0) {
            throw new FileValidationException("Dữ liệu file không được rỗng");
        }
        if (data.length > 10 * 1024 * 1024) {
            throw new FileValidationException("File qua gRPC vượt quá 10MB, hãy dùng REST/presign cho file lớn");
        }
        com.dtthuan3.ecommerce.fileservice.util.ByteArrayMultipartFile multipartFile =
                new com.dtthuan3.ecommerce.fileservice.util.ByteArrayMultipartFile(data, filename, contentType);
        return upload(multipartFile, folder);
    }

    @Override
    public BatchUploadResponse uploadBatch(List<MultipartFile> files, String folder) {
        if (files == null || files.isEmpty()) {
            throw new FileValidationException("Danh sách file không được rỗng");
        }
        if (files.size() > MAX_BATCH_COUNT) {
            throw new FileValidationException("Batch vượt quá " + MAX_BATCH_COUNT + " file");
        }
        long total = files.stream().mapToLong(f -> f.getSize()).sum();
        if (total > MAX_BATCH_TOTAL_SIZE) {
            throw new FileValidationException("Tổng batch vượt quá 200MB");
        }

        record Result(String filename, FileResponse response, String error) {}
        List<CompletableFuture<Result>> futures = files.stream()
                .map(file -> CompletableFuture.supplyAsync(() -> {
                    try {
                        FileResponse r = upload(file, folder);
                        return new Result(file.getOriginalFilename(), r, null);
                    } catch (Exception e) {
                        return new Result(file.getOriginalFilename(), null, e.getMessage());
                    }
                }, fileUploadExecutor)
                .orTimeout(60, TimeUnit.SECONDS)
                .exceptionally(ex -> new Result(file.getOriginalFilename(), null, ex.getMessage()))
                ).toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<FileResponse> successes = new ArrayList<>();
        List<BatchUploadResponse.UploadFailure> failures = new ArrayList<>();
        for (CompletableFuture<Result> f : futures) {
            Result r = f.join();
            if (r.response() != null) successes.add(r.response());
            else failures.add(new BatchUploadResponse.UploadFailure(r.filename(), r.error()));
        }
        return new BatchUploadResponse(successes, failures);
    }

    @Override
    public FileInfoResponse getInfo(String key) {
        validateKey(key);
        String safeKey = sanitizeKey(key);
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder().bucket(minioProperties.bucket()).object(safeKey).build());
            return new FileInfoResponse(safeKey, minioProperties.bucket(), stat.contentType(), stat.size(), stat.etag(),
                    stat.lastModified() != null ? stat.lastModified().toInstant() : null);
        } catch (ErrorResponseException e) {
            if (isFileNotFound(e)) throw new FileNotFoundInStorageException("Không tìm thấy file: " + key);
            throw new StorageException("Không thể lấy thông tin file", e);
        } catch (Exception e) {
            if (e instanceof FileNotFoundInStorageException) throw (FileNotFoundInStorageException) e;
            throw new StorageException("Không thể lấy thông tin file", e);
        }
    }

    @Override
    public UrlResponse getUrl(String key, Integer expirySeconds) {
        validateKey(key);
        String safeKey = sanitizeKey(key);
        getInfo(safeKey);
        int expiry = normalizeExpiry(expirySeconds);
        String url = generateGetUrl(safeKey, expiry);
        return new UrlResponse(safeKey, url, expiry);
    }

    @Override
    public PresignUploadResponse presignUpload(PresignUploadRequest request) {
        if (request == null) throw new FileValidationException("Request không được null");
        if (request.getFilename() == null || request.getFilename().isBlank())
            throw new FileValidationException("Filename không được rỗng");
        if (request.getContentType() == null || request.getContentType().isBlank())
            throw new FileValidationException("Content-Type không được rỗng");
        validateContentType(request.getContentType());
        String key = generateKey(request.getFolder(), request.getFilename());
        String safeKey = sanitizeKey(key);
        int expiry = normalizeExpiry(request.getExpirySeconds());
        try {
            String uploadUrl = presignMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder().method(Method.PUT).bucket(minioProperties.bucket()).object(safeKey).expiry(expiry).build());
            return new PresignUploadResponse(safeKey, uploadUrl, request.getContentType(), expiry);
        } catch (Exception e) {
            throw new StorageException("Không thể tạo presigned upload URL", e);
        }
    }

    @Override
    public InputStream download(String key) {
        validateKey(key);
        String safeKey = sanitizeKey(key);
        getInfo(safeKey);
        try {
            return minioClient.getObject(GetObjectArgs.builder().bucket(minioProperties.bucket()).object(safeKey).build());
        } catch (Exception e) {
            throw new StorageException("Không thể download file: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        validateKey(key);
        String safeKey = sanitizeKey(key);
        getInfo(safeKey);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(minioProperties.bucket()).object(safeKey).build());
        } catch (Exception e) {
            throw new StorageException("Không thể xóa file: " + key, e);
        }
    }

    private void scanVirusIfNeeded(MultipartFile file, String contentType) {
        if (contentType == null || contentType.isBlank()) return;
        String ct = contentType.toLowerCase();
        boolean image = ct.startsWith("image/");
        boolean video = ct.startsWith("video/");
        boolean pdf = ct.equals("application/pdf");
        if (image || video || pdf) {
            virusScanner.scan(file);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null) throw new FileValidationException("File không được null");
        if (file.isEmpty()) throw new FileValidationException("File không được rỗng");
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank())
            throw new FileValidationException("Không xác định được Content-Type");
        validateContentType(contentType);
    }

    private void validateContentType(String contentType) {
        if (!uploadProperties.isContentTypeAllowed(contentType)) {
            throw new FileValidationException("Loại file không được hỗ trợ: " + contentType);
        }
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank()) throw new FileValidationException("File key không được rỗng");
        if (key.contains("..")) throw new FileValidationException("Key chứa ký tự không hợp lệ");
    }

    private String sanitizeKey(String key) {
        if (key == null) return key;
        // decode check for %2e is handled by contains check; normalize separators
        String s = key.trim();
        while (s.startsWith("/")) s = s.substring(1);
        s = s.replaceAll("//+", "/");
        if (s.contains("..")) throw new FileValidationException("Key chứa path traversal");
        return s;
    }

    private String generateKey(String folder, String originalFilename) {
        String safeFolder = normalizeFolder(folder);
        LocalDate now = LocalDate.now();
        String extension = getExtension(originalFilename);
        String fileName = UUID.randomUUID().toString();
        if (!extension.isBlank()) fileName = fileName + "." + extension;
        return "%s/%d/%02d/%s".formatted(safeFolder, now.getYear(), now.getMonthValue(), fileName);
    }

    private String normalizeFolder(String folder) {
        if (folder == null || folder.isBlank()) return "uploads";
        String result = folder.trim();
        while (result.startsWith("/")) result = result.substring(1);
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        if (result.isBlank()) return "uploads";
        if (result.contains("..")) throw new FileValidationException("Folder chứa ký tự không hợp lệ");
        result = result.replaceAll("[^a-zA-Z0-9/_.-]", "_");
        return result;
    }

    private String getExtension(String filename) {
        if (filename == null || filename.isBlank()) return "";
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "";
        return filename.substring(dot + 1).replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    private String generateGetUrl(String key, int expiry) {
        try {
            return presignMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder().method(Method.GET).bucket(minioProperties.bucket()).object(key).expiry(expiry).build());
        } catch (Exception e) {
            throw new StorageException("Không thể tạo URL cho file", e);
        }
    }

    private int normalizeExpiry(Integer expirySeconds) {
        long defaultExpiry = uploadProperties.defaultPresignExpirySeconds();
        long maxExpiry = uploadProperties.maxPresignExpirySeconds();
        long expiry = expirySeconds == null ? defaultExpiry : expirySeconds;
        if (expiry <= 0) expiry = defaultExpiry;
        if (expiry > maxExpiry) expiry = maxExpiry;
        return (int) expiry;
    }

    private boolean isFileNotFound(ErrorResponseException e) {
        if (e.errorResponse() == null) return false;
        String code = e.errorResponse().code();
        return "NoSuchKey".equals(code) || "NoSuchObject".equals(code) || "NoSuchBucket".equals(code);
    }
}
