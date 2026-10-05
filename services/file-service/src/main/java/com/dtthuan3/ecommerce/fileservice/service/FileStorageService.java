package com.dtthuan3.ecommerce.fileservice.service;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import com.dtthuan3.ecommerce.fileservice.config.MinioProperties;
import com.dtthuan3.ecommerce.fileservice.config.UploadProperties;
import com.dtthuan3.ecommerce.fileservice.exception.FileNotFoundInStorageException;
import com.dtthuan3.ecommerce.fileservice.exception.FileValidationException;
import com.dtthuan3.ecommerce.fileservice.exception.StorageException;
import com.dtthuan3.ecommerce.fileservice.controller.FileDtos.FileInfoResponse;
import com.dtthuan3.ecommerce.fileservice.controller.FileDtos.FileResponse;
import com.dtthuan3.ecommerce.fileservice.controller.FileDtos.PresignUploadResponse;
import com.dtthuan3.ecommerce.fileservice.controller.FileDtos.UrlResponse;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private static final Pattern FOLDER = Pattern.compile("^[a-z0-9][a-z0-9_-]*(/[a-z0-9][a-z0-9_-]*)*$");
    private static final Pattern KEY = Pattern.compile("^[A-Za-z0-9._-]+(/[A-Za-z0-9._-]+)*$");
    private static final Pattern EXT = Pattern.compile("^[a-z0-9]{1,10}$");
    private static final String DEFAULT_FOLDER = "general";

    private final MinioClient minio;
    private final MinioClient presignMinio;
    private final MinioProperties props;
    private final UploadProperties uploadProps;

    public FileStorageService(@Qualifier("minioClient") MinioClient minio,
                              @Qualifier("presignMinioClient") MinioClient presignMinio,
                              MinioProperties props,
                              UploadProperties uploadProps) {
        this.minio = minio;
        this.presignMinio = presignMinio;
        this.props = props;
        this.uploadProps = uploadProps;
    }

    // ---------------------------------------------------------------- upload

    public FileResponse upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("File is empty");
        }
        String contentType = file.getContentType();
        if (!uploadProps.isContentTypeAllowed(contentType)) {
            throw new FileValidationException("Content type not allowed: " + contentType);
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String key = generateKey(folder, original);

        try (InputStream in = file.getInputStream()) {
            minio.putObject(PutObjectArgs.builder()
                    .bucket(props.bucket())
                    .object(key)
                    .stream(in, file.getSize(), -1)
                    .contentType(contentType == null ? "application/octet-stream" : contentType)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Upload failed for key " + key, e);
        }

        int expiry = uploadProps.defaultPresignExpirySeconds();
        return new FileResponse(key, props.bucket(), original, contentType, file.getSize(),
                presignedGet(key, expiry), expiry);
    }

    // -------------------------------------------------------------- download

    public StatObjectResponse stat(String key) {
        validateKey(key);
        try {
            return minio.statObject(StatObjectArgs.builder().bucket(props.bucket()).object(key).build());
        } catch (ErrorResponseException e) {
            if (isNotFound(e)) {
                throw new FileNotFoundInStorageException(key);
            }
            throw new StorageException("Stat failed for key " + key, e);
        } catch (Exception e) {
            throw new StorageException("Stat failed for key " + key, e);
        }
    }

    public FileInfoResponse info(String key) {
        StatObjectResponse s = stat(key);
        return new FileInfoResponse(key, props.bucket(), s.contentType(), s.size(), s.etag(),
                s.lastModified() == null ? null : s.lastModified().toInstant());
    }

    /** Mo stream doc object. Caller phai dong stream. */
    public InputStream open(String key) {
        validateKey(key);
        try {
            return minio.getObject(GetObjectArgs.builder().bucket(props.bucket()).object(key).build());
        } catch (ErrorResponseException e) {
            if (isNotFound(e)) {
                throw new FileNotFoundInStorageException(key);
            }
            throw new StorageException("Download failed for key " + key, e);
        } catch (Exception e) {
            throw new StorageException("Download failed for key " + key, e);
        }
    }

    // ---------------------------------------------------------------- delete

    public void delete(String key) {
        validateKey(key);
        try {
            // S3 delete la idempotent: xoa key khong ton tai van thanh cong.
            minio.removeObject(RemoveObjectArgs.builder().bucket(props.bucket()).object(key).build());
        } catch (Exception e) {
            throw new StorageException("Delete failed for key " + key, e);
        }
    }

    // ------------------------------------------------------------- presigned

    public UrlResponse downloadUrl(String key, Integer expirySeconds) {
        validateKey(key);
        stat(key); // 404 neu khong ton tai thay vi tra URL chet
        int expiry = clampExpiry(expirySeconds);
        return new UrlResponse(key, presignedGet(key, expiry), expiry);
    }

    public PresignUploadResponse presignUpload(String filename, String contentType, String folder,
                                               Integer expirySeconds) {
        if (!uploadProps.isContentTypeAllowed(contentType)) {
            throw new FileValidationException("Content type not allowed: " + contentType);
        }
        String key = generateKey(folder, filename);
        int expiry = clampExpiry(expirySeconds);
        try {
            String url = presignMinio.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(props.bucket())
                    .object(key)
                    .expiry(expiry, TimeUnit.SECONDS)
                    .build());
            return new PresignUploadResponse(key, url, contentType, expiry);
        } catch (Exception e) {
            throw new StorageException("Cannot presign upload for key " + key, e);
        }
    }

    // --------------------------------------------------------------- helpers

    private String presignedGet(String key, int expirySeconds) {
        try {
            return presignMinio.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(props.bucket())
                    .object(key)
                    .expiry(expirySeconds, TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Cannot presign download for key " + key, e);
        }
    }

    private int clampExpiry(Integer requested) {
        int v = requested == null ? uploadProps.defaultPresignExpirySeconds() : requested;
        if (v < 1) {
            throw new FileValidationException("expirySeconds must be >= 1");
        }
        return Math.min(v, uploadProps.maxPresignExpirySeconds());
    }

    /** Key dang: {folder}/{yyyy}/{MM}/{uuid}.{ext}. Ten file goc khong bao gio duoc dung lam key. */
    private String generateKey(String folder, String originalFilename) {
        String f = (folder == null || folder.isBlank()) ? DEFAULT_FOLDER : folder.trim().toLowerCase(Locale.ROOT);
        if (f.length() > 100 || !FOLDER.matcher(f).matches()) {
            throw new FileValidationException(
                    "Invalid folder. Use lowercase letters, digits, '-', '_' separated by '/'");
        }
        LocalDate d = LocalDate.now();
        String ext = extensionOf(originalFilename);
        return "%s/%d/%02d/%s%s".formatted(f, d.getYear(), d.getMonthValue(), UUID.randomUUID(),
                ext.isEmpty() ? "" : "." + ext);
    }

    private String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return EXT.matcher(ext).matches() ? ext : "";
    }

    /** Chan path traversal va ky tu la. */
    private void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > 512 || key.contains("..")
                || !KEY.matcher(key).matches()) {
            throw new FileValidationException("Invalid key");
        }
    }

    private boolean isNotFound(ErrorResponseException e) {
        String code = e.errorResponse() == null ? null : e.errorResponse().code();
        return "NoSuchKey".equals(code) || "NoSuchObject".equals(code) || "NoSuchBucket".equals(code);
    }
}
