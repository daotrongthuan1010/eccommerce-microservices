package com.dtthuan3.ecommerce.catalogservice.mapper;


import com.dtthuan3.ecommerce.catalogservice.document.BrandSearchDocument;
import com.dtthuan3.ecommerce.catalogservice.document.CategorySearchDocument;
import com.dtthuan3.ecommerce.catalogservice.document.ManufacturerSearchDocument;
import com.dtthuan3.ecommerce.catalogservice.document.ProductSearchDocument;
import com.dtthuan3.ecommerce.catalogservice.dto.response.ProductSearchSourceResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ProductSearchDocumentMapper {

    public ProductSearchDocument toDocument(
            ProductSearchSourceResponse product
    ) {
        if (product == null || product.id() == null) {
            throw new IllegalArgumentException("Product or product ID must not be null");
        }

        String name = product.currentRevision() != null
                ? product.currentRevision().name()
                : null;

        String shortDescription = product.currentRevision() != null
                ? product.currentRevision().shortDescription()
                : null;

        String description = product.currentRevision() != null
                ? product.currentRevision().description()
                : null;

        return ProductSearchDocument.builder()
                .productId(product.id().toString())

                // Tạm mapping productCode sang sku.
                // Cần xác nhận productCode chính là SKU theo nghiệp vụ.
                .sku(product.productCode())

                .name(name)
                .brand(toBrandDocument(product.brandId()))
                .manufacturer(toManufacturerDocument(product.manufacturerId()))
                .categories(toCategoryDocuments(product.categoryId()))
                .status(product.status())
                .searchText(buildSearchText(
                        product.productCode(),
                        name,
                        shortDescription,
                        description
                ))
                .stock(null)
                .rating(null)
                .updatedAt(null)
                .build();
    }

    private BrandSearchDocument toBrandDocument(UUID brandId) {
        if (brandId == null) {
            return null;
        }

        return BrandSearchDocument.builder()
                .id(brandId.toString())
                .name(null)
                .build();
    }

    private ManufacturerSearchDocument toManufacturerDocument(
            UUID manufacturerId
    ) {
        if (manufacturerId == null) {
            return null;
        }

        return ManufacturerSearchDocument.builder()
                .id(manufacturerId.toString())
                .name(null)
                .build();
    }

    private List<CategorySearchDocument> toCategoryDocuments(
            UUID categoryId
    ) {
        if (categoryId == null) {
            return List.of();
        }

        return List.of(
                CategorySearchDocument.builder()
                        .id(categoryId.toString())
                        .name(null)
                        .path(null)
                        .build()
        );
    }

    private String buildSearchText(String... values) {
        return Stream.of(values)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" "));
    }
}
