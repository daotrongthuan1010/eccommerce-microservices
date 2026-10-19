
package com.dtthuan3.ecommerce.productservice.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubmitPublicationRequest(
        @NotNull(message = "Product ID không được để trống")
        UUID productId,

        @NotNull(message = "Revision ID không được để trống")
        UUID revisionId,

        @NotNull(message = "Actor ID không được để trống")
        UUID actorId
) {
}
