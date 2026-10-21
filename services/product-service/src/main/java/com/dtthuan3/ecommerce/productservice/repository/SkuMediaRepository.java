
package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.SkuMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SkuMediaRepository
        extends JpaRepository<SkuMedia, UUID> {

    List<SkuMedia> findBySkuId(UUID skuId);

    List<SkuMedia> findBySkuIdOrderByDisplayOrderAsc(UUID skuId);

    List<SkuMedia> findBySkuIdAndPrimaryTrue(UUID skuId);

    void deleteBySkuId(UUID skuId);
}
