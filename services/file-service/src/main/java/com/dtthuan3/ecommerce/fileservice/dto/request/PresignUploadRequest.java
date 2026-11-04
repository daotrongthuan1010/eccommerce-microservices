package com.dtthuan3.ecommerce.fileservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yeu cau presign upload URL (FILE-07 + ch.14 §385: FE gui ten, loai VA size du kien;
 * service kiem quota/limit roi cap URL co han).
 * sizeBytes bat buoc: PostPolicy MinIO se gioi content-length-range theo gia tri nay —
 * client khong the PUT file lon hon size da khai bao.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PresignUploadRequest {

    @NotBlank
    @Size(max = 255)
    private String filename;

    @NotBlank
    private String contentType;

    @NotNull
    @Min(1)
    @Max(50L * 1024 * 1024) // cung limit voi REST upload 50MB
    private Long sizeBytes;

    @Size(max = 128)
    private String folder;

    private Integer expirySeconds;
}
