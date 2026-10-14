package com.dtthuan3.ecommerce.inventoryservice.repository;

import com.dtthuan3.ecommerce.inventoryservice.entity.SkuCatalogProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SkuCatalogProjectionRepository extends JpaRepository<SkuCatalogProjection, String> {
    List<SkuCatalogProjection> findAllBySkuCodeIn(Collection<String> skuCodes);
}
