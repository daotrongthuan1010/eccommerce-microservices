package com.dtthuan3.ecommerce.fileservice.dto.response;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileResponse {

    private String key;

    private String bucket;

    private String originalName;

    private String contentType;

    private long size;

    private String url;

    private long urlExpiresInSeconds;
}
