
package com.dtthuan3.ecommerce.catalogservice.kafka.handler;

import com.dtthuan3.ecommerce.catalogservice.client.ProductServiceClient;
import com.dtthuan3.ecommerce.catalogservice.dto.response.ProductSearchSourceResponse;
import com.dtthuan3.ecommerce.catalogservice.mapper.ProductSearchDocumentMapper;
import com.dtthuan3.ecommerce.catalogservice.service.ProductSearchProjectionService;
import com.dtthuan3.ecommerce.catalogservice.kafka.event.ProductPublishedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventHandler {

    private final ProductServiceClient productServiceClient;
    private final ProductSearchDocumentMapper productSearchDocumentMapper;
    private final ProductSearchProjectionService productSearchProjectionService;

    public void handle(ProductPublishedEvent event) {

        if (event == null || event.eventType() == null) {
            throw new IllegalArgumentException("Invalid product event");
        }

        switch (event.eventType()) {

            case "ProductPublished" ->
                    handleProductPublished(event);

            case "ProductSubmittedForApproval",
                 "ProductPublicationRejected" ->
                    log.info(
                            "Ignoring product event: eventType={}, productId={}",
                            event.eventType(),
                            event.productId()
                    );

            default ->
                    log.warn(
                            "Unsupported product event: eventType={}, productId={}",
                            event.eventType(),
                            event.productId()
                    );
        }
    }

    private void handleProductPublished(ProductPublishedEvent event) {

        if (event.productId() == null) {
            throw new IllegalArgumentException("Product ID must not be null");
        }

        log.info(
                "Handling ProductPublished: productId={}, revisionId={}",
                event.productId(),
                event.revisionId()
        );

        // TODO: Gọi Product Service và lấy ProductSearchSourceResponse.
        // Phần này phụ thuộc cấu trúc JSON ApiResponse thực tế.

        ProductSearchSourceResponse product =
                fetchProductFromProductService(event);

        if (product == null) {
            throw new IllegalStateException(
                    "Product Service returned empty product"
            );
        }

        if (!event.productId().equals(product.id())) {
            throw new IllegalStateException(
                    "Product ID in event does not match Product Service response"
            );
        }

        var document = productSearchDocumentMapper.toDocument(product);

        productSearchProjectionService.index(document);

        log.info(
                "Product search projection completed: productId={}",
                event.productId()
        );
    }

    private ProductSearchSourceResponse fetchProductFromProductService(
            ProductPublishedEvent event
    ) {
        // Cần hoàn thiện theo cấu trúc ApiResponse của Product Service.
        throw new UnsupportedOperationException(
                "Product API response extraction has not been implemented"
        );
    }
}
