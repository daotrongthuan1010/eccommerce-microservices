package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.Sku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SkuRepository
        extends JpaRepository<Sku, Long> {

    Optional<Sku> findBySkuCode(String skuCode);

    @Query("select s.skuCode from Sku s where s.skuCode in :skuCodes and s.active = true")
    List<String> findActiveSkuCodesBySkuCodeIn(@Param("skuCodes") List<String> skuCodes);

    Optional<Sku> findByBarcode(String barcode);

    List<Sku> findByVariantId(Long variantId);

    List<Sku> findByVariantIdAndActiveTrue(Long variantId);

    boolean existsBySkuCode(String skuCode);

    boolean existsBySkuCodeAndIdNot(
            String skuCode,
            Long id
    );

    boolean existsByBarcode(String barcode);

    boolean existsByBarcodeAndIdNot(
            String barcode,
            Long id
    );
}
