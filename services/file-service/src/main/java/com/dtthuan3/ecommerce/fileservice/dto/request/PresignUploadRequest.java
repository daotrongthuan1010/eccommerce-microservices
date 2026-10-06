package com.dtthuan3.ecommerce.fileservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PresignUploadRequest {

    @NotBlank
    private String filename;

    @NotBlank
    private String contentType;

    private String folder;

    private Integer expirySeconds;
}