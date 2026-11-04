package com.dtthuan3.ecommerce.shippingservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI shippingOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Shipping Service API")
                .version("v1")
                .description("Fee quote (han 15 phut) + van don qua fake carrier kieu GHN + webhook trang thai ky HMAC."));
    }
}
