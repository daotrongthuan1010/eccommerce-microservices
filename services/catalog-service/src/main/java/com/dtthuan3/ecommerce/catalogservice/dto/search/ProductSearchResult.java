package com.dtthuan3.ecommerce.catalogservice.dto.search;

import com.dtthuan3.ecommerce.catalogservice.document.ProductSearchDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ProductSearchResult {

    private ProductSearchDocument product;

    private float score;
}