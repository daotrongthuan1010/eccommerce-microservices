package com.dtthuan3.ecommerce.authservice.web;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RestClient restClient = RestClient.create();

    @Value("${auth.keycloak.token-uri:http://localhost:7080/realms/ecommerce/protocol/openid-connect/token}")
    private String tokenUri;

    @Value("${auth.keycloak.client-id:ecommerce-app}")
    private String clientId;

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("username", request.username());
        form.add("password", request.password());
        Map<?, ?> token = restClient
                .post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
        if (token == null) {
            return Map.of();
        }
        return Map.<String, Object>copyOf((Map<String, Object>) (Map<?, ?>) token);
    }
}
