package com.dtthuan3.ecommerce.fileservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UrlResponse {

    private String key;

    private String url;

    private long expiresInSeconds;
}