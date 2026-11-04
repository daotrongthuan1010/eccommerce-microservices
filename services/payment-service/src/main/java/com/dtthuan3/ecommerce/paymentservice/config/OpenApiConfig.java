package com.dtthuan3.ecommerce.paymentservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI paymentOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Payment Service API")
                .version("v1")
                .description("Provider abstraction: fake VNPay (redirect+IPN), fake ZaloPay (callback), COD. "
                        + "Webhook la nguon su that duy nhat; return-url khong bao gio duoc tin."));
    }
}
