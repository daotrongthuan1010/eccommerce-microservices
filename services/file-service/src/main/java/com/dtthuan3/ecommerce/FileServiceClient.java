package com.dtthuan3.ecommerce;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

/**
 * Client mau de goi file-service tu service khac (copy vao service can dung, KHONG them module chung).
 * Yeu cau: service co spring-cloud-starter-netflix-eureka-client (da co) + spring-web.
 * "file-service" duoc Eureka + LoadBalancer phan giai thanh dia chi that.
 *
 * Token: chuyen tiep JWT cua nguoi dung dang goi (neu co) sang file-service.
 * Neu goi tu job nen/Kafka consumer (khong co user), can lay token client_credentials tu Keycloak.
 */
@Configuration
class FileServiceClientConfig {
    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}

@Component
public class FileServiceClient {

    public record FileResponse(String key, String bucket, String originalName, String contentType,
                               long size, String url, long urlExpiresInSeconds) {}
    public record UrlResponse(String key, String url, long expiresInSeconds) {}

    private final RestClient rest;

    public FileServiceClient(RestClient.Builder lbBuilder) {
        this.rest = lbBuilder.baseUrl("http://file-service").build();
    }

    public FileResponse upload(MultipartFile file, String folder) throws Exception {
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("file", new ByteArrayResource(file.getBytes()) {
            @Override public String getFilename() { return file.getOriginalFilename(); }
        }).contentType(MediaType.parseMediaType(
                file.getContentType() == null ? "application/octet-stream" : file.getContentType()));
        if (folder != null) body.part("folder", folder);

        return rest.post().uri("/files")
                .headers(h -> bearer(h))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body.build())
                .retrieve()
                .body(FileResponse.class);
    }

    public UrlResponse downloadUrl(String key, Integer expirySeconds) {
        return rest.get()
                .uri(u -> u.path("/files/url").queryParam("key", key)
                        .queryParamIfPresent("expirySeconds", java.util.Optional.ofNullable(expirySeconds)).build())
                .headers(h -> bearer(h))
                .retrieve()
                .body(UrlResponse.class);
    }

    public void delete(String key) {   // can ROLE_ADMIN
        rest.delete().uri(u -> u.path("/files").queryParam("key", key).build())
                .headers(h -> bearer(h))
                .retrieve().toBodilessEntity();
    }

    private void bearer(org.springframework.http.HttpHeaders h) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a instanceof JwtAuthenticationToken jwt) {
            h.setBearerAuth(jwt.getToken().getTokenValue());
        }
    }
}
