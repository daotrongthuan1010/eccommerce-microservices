package com.dtthuan3.ecommerce.fileservice.controller;

import com.dtthuan3.ecommerce.fileservice.dto.request.PresignUploadRequest;
import com.dtthuan3.ecommerce.fileservice.dto.response.*;
import com.dtthuan3.ecommerce.fileservice.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;


    @PostMapping
    public ResponseEntity<FileResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false) String folder
    ) {

        return ResponseEntity.ok(fileStorageService.upload(file, folder));
    }


    @PostMapping("/batch")
    public ResponseEntity<BatchUploadResponse> uploadBatch(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "folder", required = false) String folder
    ) {

        return ResponseEntity.ok(fileStorageService.uploadBatch(files, folder));
    }


    @GetMapping("/info")
    public ResponseEntity<FileInfoResponse> getInfo(@RequestParam("key") String key) {
        return ResponseEntity.ok(fileStorageService.getInfo(key));
    }


    @GetMapping("/url")
    public ResponseEntity<UrlResponse> getUrl(@RequestParam("key") String key,
                                              @RequestParam(value = "expirySeconds", required = false) Integer expirySeconds
    ) {

        return ResponseEntity.ok(fileStorageService.getUrl(key, expirySeconds));
    }


    @PostMapping("/presign-upload")
    public ResponseEntity<PresignUploadResponse> presignUpload(@Valid @RequestBody PresignUploadRequest request) {
        return ResponseEntity.ok(fileStorageService.presignUpload(request));
    }


    @GetMapping("/download")
    public InputStream download(@RequestParam("key") String key) {
        return fileStorageService.download(key);
    }


    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam("key") String key) {
        fileStorageService.delete(key);
        return ResponseEntity.noContent().build();
    }
}