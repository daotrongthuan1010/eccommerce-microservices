package com.dtthuan3.ecommerce.fileservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PresignUploadResponse {

    private String key;

    private String uploadUrl;

    private String contentType;

    /** PostPolicy upload: form fields FE phai gui kem (policy + signature cua MinIO). */
    private java.util.Map<String, String> formData;

    /** Size da duyet cho upload nay (bytes) — PostPolicy gioi content-length-range. */
    private long sizeBytes;

    private long expiresInSeconds;
}