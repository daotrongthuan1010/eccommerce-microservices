package com.dtthuan3.ecommerce.catalogservice.config;



import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI catalogOpenAPI() {

        return new OpenAPI()
                .info(
                        new Info()
                                .title("Catalog Service API")
                                .description(
                                        "API quản lý danh mục sản phẩm"
                                )
                                .version("1.0.0")
                );
    }
}