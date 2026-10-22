
package com.dtthuan3.ecommerce.productservice.mapper;

import com.dtthuan3.ecommerce.productservice.domain.Product;
import com.dtthuan3.ecommerce.productservice.domain.ProductRevision;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductRevisionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", source = "product.id")
    @Mapping(target = "status", source = "product.status")
    @Mapping(target = "currentRevision", source = "revision")
    ProductResponse toResponse(
            Product product,
            ProductRevision revision
    );

    ProductRevisionResponse toRevisionResponse(
            ProductRevision revision
    );
}
