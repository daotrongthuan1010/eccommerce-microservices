package com.dtthuan3.ecommerce.analyticsservice.dto.response;

/** Top SKU ban chay trong khoang ngay, sort theo doanh thu giam dan. */
public record TopProductResponse(
        String skuId,
        String productName,
        long totalQuantity,
        long totalRevenue) {}
