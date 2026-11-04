package com.dtthuan3.ecommerce.inventoryservice.dto.response;

/**
 * Kết quả ACK của một lần replay đơn-record từ Product DLT về topic nguồn.
 * Record này là hợp đồng dữ liệu trả về cho client. Service/controller chọn và tổng hợp các trường cần công
 * khai, giúp API không phụ thuộc trực tiếp vào cấu trúc entity lưu trong database.
 */
public record ReplayProductEventDltResponse(
        String replayRequestId,
        String dltTopic,
        int partition,
        long dltOffset,
        String sourceTopic,
        Integer targetPartition,
        Long targetOffset,
        String recordKey
) {
}
