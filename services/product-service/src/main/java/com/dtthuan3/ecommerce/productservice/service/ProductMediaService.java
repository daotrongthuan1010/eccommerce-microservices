package com.dtthuan3.ecommerce.productservice.service;

import com.dtthuan3.ecommerce.productservice.dto.response.ProductMediaResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.SkuMediaResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ProductMediaService {

    ProductMediaResponse uploadProductMedia(
            UUID productId,
            UUID revisionId,
            MultipartFile file,
            Integer displayOrder,
            Boolean primary
    );

    ProductMediaResponse getProductMediaById(UUID id);

    ProductMediaResponse updateProductMedia(
            UUID id,
            Integer displayOrder,
            Boolean primary
    );

    void deleteProductMedia(UUID id);

    SkuMediaResponse uploadSkuMedia(
            UUID skuId,
            UUID revisionId,
            MultipartFile file,
            Integer displayOrder,
            Boolean primary
    );

    SkuMediaResponse getSkuMediaById(UUID id);

    SkuMediaResponse updateSkuMedia(
            UUID id,
            Integer displayOrder,
            Boolean primary
    );

    void deleteSkuMedia(UUID id);
}