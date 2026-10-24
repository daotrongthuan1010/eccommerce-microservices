package com.dtthuan3.ecommerce.catalogservice.client;


import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.catalogservice.dto.response.ProductExistsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/products/{productId}")
    ApiResponse<ProductExistsResponse> getById(
            @PathVariable("productId") UUID productId
    );
}