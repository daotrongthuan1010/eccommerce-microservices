package com.dtthuan3.ecommerce.productservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Card san pham cho marketplace browse (trang chu / tim kiem / danh muc).
 * Gia la long VND (integer, khong thap phan) theo AGENTS.md luat 6 —
 * priceFrom = gia thap nhat trong cac SKU dang ban (price-schedule ACTIVE).
 * Anh: fileKey goc (MinIO); FE resolve URL qua file-service.
 */
@Getter
@Builder
public class ProductCardResponse {

    private UUID id;

    private String productCode;

    private String name;

    private String shortDescription;

    private UUID categoryId;

    private UUID brandId;

    /** Gia tu (min cua cac SKU dang ban), VND long. */
    private long priceFrom;

    private String currency;

    /** fileKey anh chinh (isPrimary); null neu chua co anh. */
    private String primaryImageKey;

    private int sellableSkuCount;
}
