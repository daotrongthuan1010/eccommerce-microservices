package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Sự kiện thiếu hàng được phát hiện khi soạn một allocation đã cam kết. */
public record ReportInventoryShortageRequest(
        /** Khóa idempotency do Shipping/Order tạo cho một lần phát hiện thiếu hàng. */
        @NotBlank(message = "operationId là bắt buộc")
        @Size(max = 100, message = "operationId tối đa 100 ký tự")
        String operationId,

        /** Balance bị thiếu hàng tại kho nguồn. */
        @NotBlank(message = "inventoryId là bắt buộc")
        @Size(max = 36, message = "inventoryId tối đa 36 ký tự")
        String inventoryId,

        /** ID đơn hàng nghiệp vụ bên Order Service, lưu dạng tham chiếu thuần. */
        @NotBlank(message = "orderId là bắt buộc")
        @Size(max = 100, message = "orderId tối đa 100 ký tự")
        String orderId,

        /** Dòng đơn hàng bị ảnh hưởng. */
        @NotBlank(message = "orderLineId là bắt buộc")
        @Size(max = 100, message = "orderLineId tối đa 100 ký tự")
        String orderLineId,

        /** Allocation cũ đã được commit tại Inventory. */
        @NotBlank(message = "allocationId là bắt buộc")
        @Size(max = 100, message = "allocationId tối đa 100 ký tự")
        String allocationId,

        /** Tổng lượng Order đã cam kết từ kho này. */
        @NotNull(message = "committedQuantity là bắt buộc")
        @Positive(message = "committedQuantity phải lớn hơn 0")
        BigDecimal committedQuantity,

        /** Lượng allocation đã dispatch; phần này không được tái phân bổ lại. */
        @NotNull(message = "dispatchedQuantity là bắt buộc")
        @PositiveOrZero(message = "dispatchedQuantity không được âm")
        BigDecimal dispatchedQuantity,

        /** Lượng thiếu thực tế được nhân viên kho xác nhận khi soạn. */
        @NotNull(message = "shortageQuantity là bắt buộc")
        @Positive(message = "shortageQuantity phải lớn hơn 0")
        BigDecimal shortageQuantity,

        /** Mô tả nguyên nhân/biên bản soạn hàng để người xử lý có ngữ cảnh. */
        @NotBlank(message = "reason là bắt buộc")
        @Size(max = 500, message = "reason tối đa 500 ký tự")
        String reason
) {}
