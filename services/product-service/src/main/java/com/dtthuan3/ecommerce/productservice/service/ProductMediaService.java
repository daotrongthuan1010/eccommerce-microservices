package com.dtthuan3.ecommerce.productservice.service;

import com.dtthuan3.ecommerce.productservice.dto.request.ProductMediaRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.SkuMediaRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductMediaResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.SkuMediaResponse;

public interface ProductMediaService {

    ProductMediaResponse createProductMedia(
            ProductMediaRequest request
    );

    ProductMediaResponse getProductMediaById(Long id);

    ProductMediaResponse updateProductMedia(
            Long id,
            ProductMediaRequest request
    );

    void deleteProductMedia(Long id);

    SkuMediaResponse createSkuMedia(
            SkuMediaRequest request
    );

    SkuMediaResponse getSkuMediaById(Long id);

    SkuMediaResponse updateSkuMedia(
            Long id,
            SkuMediaRequest request
    );

    void deleteSkuMedia(Long id);
}