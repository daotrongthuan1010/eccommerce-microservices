package com.dtthuan3.ecommerce.fileservice.controller;

import io.minio.StatObjectResponse;
import jakarta.validation.Valid;
import com.dtthuan3.ecommerce.fileservice.service.FileStorageService;
import com.dtthuan3.ecommerce.fileservice.controller.FileDtos.BatchUploadResponse;
import com.dtthuan3.ecommerce.fileservice.controller.FileDtos.FileInfoResponse;
import com.dtthuan3.ecommerce.fileservice.controller.FileDtos.FileResponse;
import com.dtthuan3.ecommerce.fileservice.controller.FileDtos.PresignUploadRequest;
import com.dtthuan3.ecommerce.fileservice.controller.FileDtos.PresignUploadResponse;
import com.dtthuan3.ecommerce.fileservice.controller.FileDtos.UrlResponse;
import java.io.InputStream;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * API file dung chung. Qua gateway: /api/file/files/... (gateway StripPrefix=2).
 * Goi truc tiep giua cac service: http://file-service/files/... (Eureka + LoadBalancer).
 * Dung query param `key` (khong dung path variable) vi key chua dau '/'.
 */
@RestController
@RequestMapping("/files")
public class FileController {

    private final FileStorageService storage;

    public FileController(FileStorageService storage) {
        this.storage = storage;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileResponse> upload(@RequestParam("file") MultipartFile file,
                                               @RequestParam(value = "folder", required = false) String folder) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storage.upload(file, folder));
    }

    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BatchUploadResponse> uploadBatch(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "folder", required = false) String folder) {
        List<FileResponse> result = files.stream().map(f -> storage.upload(f, folder)).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(new BatchUploadResponse(result));
    }

    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> download(
            @RequestParam("key") String key,
            @RequestParam(value = "attachment", defaultValue = "false") boolean attachment) {
        StatObjectResponse stat = storage.stat(key);
        String name = key.substring(key.lastIndexOf('/') + 1);
        ContentDisposition disposition = (attachment ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(name).build();
        MediaType mediaType = parseMediaType(stat.contentType());

        StreamingResponseBody body = out -> {
            try (InputStream in = storage.open(key)) {
                in.transferTo(out);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(mediaType)
                .contentLength(stat.size())
                .body(body);
    }

    @GetMapping("/url")
    public UrlResponse url(@RequestParam("key") String key,
                           @RequestParam(value = "expirySeconds", required = false) Integer expirySeconds) {
        return storage.downloadUrl(key, expirySeconds);
    }

    @GetMapping("/info")
    public FileInfoResponse info(@RequestParam("key") String key) {
        return storage.info(key);
    }

    @PostMapping("/presign-upload")
    public PresignUploadResponse presignUpload(@Valid @RequestBody PresignUploadRequest req) {
        return storage.presignUpload(req.filename(), req.contentType(), req.folder(), req.expirySeconds());
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam("key") String key) {
        storage.delete(key);
        return ResponseEntity.noContent().build();
    }

    private static MediaType parseMediaType(String contentType) {
        try {
            return contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
