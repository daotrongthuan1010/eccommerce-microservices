package com.dtthuan3.ecommerce.fileservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.fileservice.dto.request.PresignUploadRequest;
import com.dtthuan3.ecommerce.fileservice.dto.response.*;
import com.dtthuan3.ecommerce.fileservice.service.FileStorageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.List;

/**
 * Mẫu enterprise cho service — trả ApiResponse chuẩn.
 * Service khác học theo: mọi endpoint trả ApiResponse<T>, trừ download stream.
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Validated
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<ApiResponse<FileResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false) @Size(max = 128) String folder
    ) {
        FileResponse data = fileStorageService.upload(file, folder);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(data));
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<BatchUploadResponse>> uploadBatch(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "folder", required = false) @Size(max = 128) String folder
    ) {
        BatchUploadResponse resp = fileStorageService.uploadBatch(files, folder);
        ApiResponse<BatchUploadResponse> body = ApiResponse.ok(resp);
        if (resp.getFailures() != null && !resp.getFailures().isEmpty()) {
            return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(body);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<FileInfoResponse>> getInfo(@RequestParam("key") @Size(max = 1024) String key) {
        return ResponseEntity.ok(ApiResponse.ok(fileStorageService.getInfo(key)));
    }

    @GetMapping("/url")
    public ResponseEntity<ApiResponse<UrlResponse>> getUrl(@RequestParam("key") @Size(max = 1024) String key,
                                              @RequestParam(value = "expirySeconds", required = false) @Min(60) @Max(86400) Integer expirySeconds
    ) {
        return ResponseEntity.ok(ApiResponse.ok(fileStorageService.getUrl(key, expirySeconds)));
    }

    @PostMapping("/presign-upload")
    public ResponseEntity<ApiResponse<PresignUploadResponse>> presignUpload(@Valid @RequestBody PresignUploadRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(fileStorageService.presignUpload(request)));
    }

    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> download(@RequestParam("key") @Size(max = 1024) String key) {
        FileInfoResponse info = fileStorageService.getInfo(key);
        InputStream in = fileStorageService.download(key);
        StreamingResponseBody body = out -> {
            try (in) {
                in.transferTo(out);
            }
        };
        String filename = key.contains("/") ? key.substring(key.lastIndexOf('/') + 1) : key;
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(info.getContentType() != null ? info.getContentType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentLength(info.getSize())
                .body(body);
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> delete(@RequestParam("key") @Size(max = 1024) String key) {
        fileStorageService.delete(key);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
