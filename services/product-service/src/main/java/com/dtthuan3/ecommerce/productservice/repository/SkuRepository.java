package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.Sku;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkuRepository extends JpaRepository<Sku, UUID> {

    boolean existsBySkuCodeIgnoreCase(String skuCode);

    boolean existsByBarcode(String barcode);

    Optional<Sku> findByIdAndVariantId(UUID id, UUID variantId);

    List<Sku> findAllByVariantId(UUID variantId);

    /** Marketplace browse: SKU dang ban (SELLABLE) cua nhieu variant mot luc. */
    List<Sku> findAllByVariantIdInAndSellingStatus(
            java.util.Collection<UUID> variantIds,
            com.dtthuan3.ecommerce.productservice.constant.SkuSellingStatus sellingStatus
    );
}