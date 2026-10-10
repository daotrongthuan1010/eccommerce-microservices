package com.dtthuan3.ecommerce.catalogservice.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class FileServiceConfig {

    @Bean
    public RestClient fileServiceRestClient(
            @Value("${file-service.url}") String baseUrl
    ) {

        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
