package com.dtthuan3.ecommerce.fileservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileInfoResponse {

    private String key;

    private String bucket;

    private String contentType;

    private long size;

    private String etag;

    private Instant lastModified;
}