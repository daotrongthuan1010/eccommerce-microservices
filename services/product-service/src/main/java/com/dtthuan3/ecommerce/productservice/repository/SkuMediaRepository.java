package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.SkuMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkuMediaRepository
        extends JpaRepository<SkuMedia, Long> {

    List<SkuMedia> findBySkuId(Long skuId);

    List<SkuMedia> findBySkuIdOrderByDisplayOrderAsc(
            Long skuId
    );

    List<SkuMedia> findBySkuIdAndPrimaryTrue(
            Long skuId
    );

    void deleteBySkuId(Long skuId);
}