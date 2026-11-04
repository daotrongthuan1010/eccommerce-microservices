package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Chỉ rõ đúng một record DLT cần phát lại; payload không nhận từ client để tránh bị sửa khi replay.
 * Record này là hợp đồng dữ liệu đầu vào giữa client và Inventory. Các trường thể hiện thông tin cần cho
 * use case; validation ngăn dữ liệu thiếu hoặc sai trước khi service xử lý và record không đại diện cho
 * bảng database.
 */
public record ReplayProductEventDltRequest(
        /** Partition của product-events.DLT; giữ nguyên partition nguồn tương ứng. */
        @NotNull(message = "partition là bắt buộc")
        @PositiveOrZero(message = "partition không được âm")
        Integer partition,

        /** Offset chính xác của record trong product-events.DLT. */
        @NotNull(message = "offset là bắt buộc")
        @PositiveOrZero(message = "offset không được âm")
        Long offset,

        /** Người vận hành phải cung cấp lý do để ghi audit log. */
        @NotBlank(message = "reason là bắt buộc")
        @Size(max = 500, message = "reason tối đa 500 ký tự")
        String reason,

        /** Cờ xác nhận rõ ràng vì thao tác sẽ phát lại một Kafka record. */
        @NotNull(message = "confirmReplay là bắt buộc")
        @AssertTrue(message = "confirmReplay phải là true")
        Boolean confirmReplay
) {
}
