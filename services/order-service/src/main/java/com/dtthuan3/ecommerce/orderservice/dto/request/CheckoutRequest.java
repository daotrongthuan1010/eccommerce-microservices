package com.dtthuan3.ecommerce.orderservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Dat hang tu gio hien tai (ORD-UC01). Idempotency-Key header bat buoc:
 * khach bam dat nhieu lan (ORD-UC03) chi tao mot don.
 * Payment method phac hoa theo payment-service: VNPAY/ZALOPAY/COD.
 */
public record CheckoutRequest(
        @NotBlank @Size(max = 512) String shippingAddress,
        @NotBlank @Size(max = 128) String recipientName,
        @NotBlank @Size(max = 32) String recipientPhone,
        @NotNull @Pattern(regexp = "VNPAY|ZALOPAY|COD", message = "Phương thức phải là VNPAY, ZALOPAY hoặc COD")
        String paymentMethod) {

    /** V1: checkout tu gio; danh sach item rieng le se la mo rong sau. */
    public record Item(
            @NotBlank String skuId,
            @NotNull int quantity) {

        public static List<Item> of() { return List.of(); }
    }
}
