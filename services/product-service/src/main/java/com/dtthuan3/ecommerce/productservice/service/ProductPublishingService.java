
package com.dtthuan3.ecommerce.productservice.service;

import com.dtthuan3.ecommerce.productservice.dto.response.ProductPublishingHistoryResponse;

import java.util.List;
import java.util.UUID;

public interface ProductPublishingService {

    UUID submitForApproval(
            UUID productId,
            UUID revisionId,
            UUID actorId
    );

    void approve(
            UUID approvalId,
            UUID actorId
    );

    void reject(
            UUID approvalId,
            UUID actorId,
            String reason
    );

    List<ProductPublishingHistoryResponse> getPublishingHistory(
            UUID productId
    );
}
