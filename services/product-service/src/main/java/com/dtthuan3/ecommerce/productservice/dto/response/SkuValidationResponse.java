package com.dtthuan3.ecommerce.productservice.dto.response;

import java.util.List;

public record SkuValidationResponse(
        List<String> activeSkuCodes,
        List<String> invalidSkuCodes
) {
}
