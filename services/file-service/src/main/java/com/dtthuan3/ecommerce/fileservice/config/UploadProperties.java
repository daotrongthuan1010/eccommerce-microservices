package com.dtthuan3.ecommerce.fileservice.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "storage.upload")
public record UploadProperties(
        @DefaultValue("900") int defaultPresignExpirySeconds,
        @DefaultValue("86400") int maxPresignExpirySeconds,
        @DefaultValue List<String> allowedContentTypes) {

    public boolean isContentTypeAllowed(String contentType) {
        if (allowedContentTypes == null || allowedContentTypes.isEmpty()) {
            return true;
        }
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        String ct = contentType.toLowerCase();
        int semi = ct.indexOf(';');
        if (semi >= 0) {
            ct = ct.substring(0, semi).trim();
        }
        for (String allowed : allowedContentTypes) {
            String a = allowed.toLowerCase().trim();
            if (a.endsWith("/*") ? ct.startsWith(a.substring(0, a.length() - 1)) : ct.equals(a)) {
                return true;
            }
        }
        return false;
    }
}
