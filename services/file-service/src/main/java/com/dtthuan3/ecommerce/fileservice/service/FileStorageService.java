package com.dtthuan3.ecommerce.fileservice.service;

import com.dtthuan3.ecommerce.fileservice.dto.request.PresignUploadRequest;
import com.dtthuan3.ecommerce.fileservice.dto.response.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface FileStorageService {

    FileResponse upload(
            MultipartFile file,
            String folder
    );

    // dùng cho gRPC
    FileResponse upload(
            byte[] data,
            String filename,
            String contentType,
            String folder
    );

    BatchUploadResponse uploadBatch(
            List<MultipartFile> files,
            String folder
    );

    FileInfoResponse getInfo(String key);

    UrlResponse getUrl(
            String key,
            Integer expirySeconds
    );

    PresignUploadResponse presignUpload(
            PresignUploadRequest request
    );

    InputStream download(String key);

    void delete(String key);
}