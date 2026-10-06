package com.dtthuan3.ecommerce.reviewservice.client;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileServiceClient {

    public record FileResponse(
            String key,
            String bucket,
            String originalName,
            String contentType,
            long size,
            String url,
            long urlExpiresInSeconds
    ) {}

    public record UrlResponse(
            String key,
            String url,
            long expiresInSeconds
    ) {}

    private final RestClient rest;

    public FileServiceClient(RestClient.Builder lbBuilder) {
        this.rest = lbBuilder
                .baseUrl("http://file-service")
                .build();
    }

    public FileResponse upload(
            MultipartFile file,
            String folder
    ) throws Exception {

        MultipartBodyBuilder body = new MultipartBodyBuilder();

        body.part(
                "file",
                new ByteArrayResource(file.getBytes()) {
                    @Override
                    public String getFilename() {
                        return file.getOriginalFilename();
                    }
                }
        ).contentType(
                MediaType.parseMediaType(
                        file.getContentType() == null
                                ? "application/octet-stream"
                                : file.getContentType()
                )
        );

        if (folder != null) {
            body.part("folder", folder);
        }

        return rest.post()
                .uri("/files")
                .headers(this::bearer)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body.build())
                .retrieve()
                .body(FileResponse.class);
    }

    public UrlResponse downloadUrl(
            String key,
            Integer expirySeconds
    ) {

        return rest.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/files/url")
                                .queryParam("key", key)
                                .queryParamIfPresent(
                                        "expirySeconds",
                                        java.util.Optional.ofNullable(expirySeconds)
                                )
                                .build()
                )
                .headers(this::bearer)
                .retrieve()
                .body(UrlResponse.class);
    }

    public void delete(String key) {

        rest.delete()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/files")
                                .queryParam("key", key)
                                .build()
                )
                .headers(this::bearer)
                .retrieve()
                .toBodilessEntity();
    }

    private void bearer(
            org.springframework.http.HttpHeaders headers
    ) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwt) {

            headers.setBearerAuth(
                    jwt.getToken().getTokenValue()
            );
        }
    }
}