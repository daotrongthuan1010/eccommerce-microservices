
package com.dtthuan3.ecommerce.productservice.controller;

import com.dtthuan3.ecommerce.productservice.dto.response.ProductMediaResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.SkuMediaResponse;
import com.dtthuan3.ecommerce.productservice.service.ProductMediaService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/products-media")
@RequiredArgsConstructor
@Validated
public class ProductMediaController {

    private final ProductMediaService productMediaService;

    @PostMapping(
            value = "/products/{productId}/media",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ProductMediaResponse> uploadProductMedia(
            @PathVariable @NotNull UUID productId,
            @RequestParam UUID revisionId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Thứ tự hiển thị không hợp lệ")
            Integer displayOrder,
            @RequestParam(defaultValue = "false") Boolean primary
    ) {
        return ResponseEntity.ok(
                productMediaService.uploadProductMedia(
                        productId,revisionId, file, displayOrder, primary
                )
        );
    }

    @GetMapping("/product-media/{id}")
    public ResponseEntity<ProductMediaResponse> getProductMedia(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                productMediaService.getProductMediaById(id)
        );
    }

    @PatchMapping("/product-media/{id}")
    public ResponseEntity<ProductMediaResponse> updateProductMedia(
            @PathVariable UUID id,
            @RequestParam(required = false) Integer displayOrder,
            @RequestParam(required = false) Boolean primary
    ) {
        return ResponseEntity.ok(
                productMediaService.updateProductMedia(
                        id, displayOrder, primary
                )
        );
    }

    @DeleteMapping("/product-media/{id}")
    public ResponseEntity<Void> deleteProductMedia(
            @PathVariable UUID id
    ) {
        productMediaService.deleteProductMedia(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(
            value = "/skus/{skuId}/media",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<SkuMediaResponse> uploadSkuMedia(
            @PathVariable @NotNull UUID skuId,
            @RequestParam UUID revisionId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Thứ tự hiển thị không hợp lệ")
            Integer displayOrder,
            @RequestParam(defaultValue = "false") Boolean primary
    ) {
        return ResponseEntity.ok(
                productMediaService.uploadSkuMedia(
                        skuId,revisionId, file, displayOrder, primary
                )
        );
    }

    @GetMapping("/sku-media/{id}")
    public ResponseEntity<SkuMediaResponse> getSkuMedia(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                productMediaService.getSkuMediaById(id)
        );
    }

    @PatchMapping("/sku-media/{id}")
    public ResponseEntity<SkuMediaResponse> updateSkuMedia(
            @PathVariable UUID id,
            @RequestParam(required = false) Integer displayOrder,
            @RequestParam(required = false) Boolean primary
    ) {
        return ResponseEntity.ok(
                productMediaService.updateSkuMedia(
                        id, displayOrder, primary
                )
        );
    }

    @DeleteMapping("/sku-media/{id}")
    public ResponseEntity<Void> deleteSkuMedia(
            @PathVariable UUID id
    ) {
        productMediaService.deleteSkuMedia(id);
        return ResponseEntity.noContent().build();
    }
}
