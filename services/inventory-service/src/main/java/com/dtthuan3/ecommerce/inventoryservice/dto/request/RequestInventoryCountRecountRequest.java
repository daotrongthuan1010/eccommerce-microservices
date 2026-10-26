package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Thông tin người duyệt yêu cầu nhân viên xác minh và đếm lại phiên kiểm kê. */
public record RequestInventoryCountRecountRequest(
        /** Khóa chống gửi lại cùng thao tác; mỗi yêu cầu đếm lại phải có operationId riêng. */
        @NotBlank(message = "operationId là bắt buộc")
        @Size(max = 100, message = "operationId tối đa 100 ký tự")
        String operationId,

        /** Version phiên người duyệt đã xem; server từ chối nếu phiên đổi trong lúc duyệt. */
        @NotNull(message = "expectedVersion là bắt buộc")
        @PositiveOrZero(message = "expectedVersion không được âm")
        Long expectedVersion,

        /** Lý do cụ thể để người kiểm kê biết dòng/bucket nào cần xác minh. */
        @NotBlank(message = "reason là bắt buộc")
        @Size(max = 500, message = "reason tối đa 500 ký tự")
        String reason
) {}
