package com.dtthuan3.ecommerce.fileservice.controller;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;

/** Cac DTO request/response cua file-service. */
public final class FileDtos {

    private FileDtos() {
    }

    /** Ket qua upload mot file. `key` la dinh danh dung cho moi API khac va nen duoc luu o service goi. */
    public record FileResponse(
            String key,
            String bucket,
            String originalName,
            String contentType,
            long size,
            String url,
            long urlExpiresInSeconds) {
    }

    public record BatchUploadResponse(List<FileResponse> files) {
    }

    public record FileInfoResponse(String key, String bucket, String contentType, long size, String etag,
                                   Instant lastModified) {
    }

    public record UrlResponse(String key, String url, long expiresInSeconds) {
    }

    public record PresignUploadRequest(
            @NotBlank String filename,
            @NotBlank String contentType,
            String folder,
            Integer expirySeconds) {
    }

    /** Client PUT file thang len `uploadUrl` (header Content-Type phai khop `contentType`). */
    public record PresignUploadResponse(String key, String uploadUrl, String contentType, long expiresInSeconds) {
    }
}
