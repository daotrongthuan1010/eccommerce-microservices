package com.dtthuan3.ecommerce.inventoryservice.dto.response;

/** Kết quả ACK của một lần replay đơn-record từ Product DLT về topic nguồn. */
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
