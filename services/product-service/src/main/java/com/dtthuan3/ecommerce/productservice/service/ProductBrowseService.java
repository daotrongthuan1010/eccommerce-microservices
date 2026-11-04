package com.dtthuan3.ecommerce.productservice.service;

import com.dtthuan3.ecommerce.productservice.dto.response.ProductCardResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

/**
 * Marketplace browse (2026-11-05 fe-marketplace D4): danh sach san pham da
 * xuat ban cho trang chu / tim kiem / danh muc, paging + filter.
 */
public interface ProductBrowseService {

    /**
     * @param page      0-based
     * @param size      page size (max 100)
     * @param categoryId loc theo category (null = all)
     * @param brandId    loc theo brand (null = all)
     * @param minPrice   loc gia tu (VND long, null = khong loc)
     * @param maxPrice   loc gia den (VND long, null = khong loc)
     * @param keyword    tim kiem ten (LIKE, null = all)
     * @param sort       newest|price_asc|price_desc (default newest)
     */
    Page<ProductCardResponse> browse(
            int page,
            int size,
            UUID categoryId,
            UUID brandId,
            Long minPrice,
            Long maxPrice,
            String keyword,
            String sort
    );
}
