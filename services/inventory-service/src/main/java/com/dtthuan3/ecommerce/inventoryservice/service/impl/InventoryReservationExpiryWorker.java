package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReservationRepository;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tự giải phóng lượng tồn bị giữ bởi reservation đã hết hạn.
 * Mỗi lượt chỉ claim một batch nhỏ; lease và SKIP LOCKED phối hợp giữa nhiều replica, còn transaction nghiệp vụ
 * riêng của từng reservation bảo đảm lỗi một bản ghi không rollback các bản ghi đã xử lý trước đó.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryReservationExpiryWorker {
    private final InventoryReservationRepository reservationRepository;
    private final InventoryReservationService reservationService;

    @Value("${inventory.reservation-expiry.batch-size}")
    private int batchSize;

    @Value("${inventory.reservation-expiry.lease-seconds}")
    private int leaseSeconds;

    /** Quét theo chu kỳ cấu hình; database là nguồn thời gian chuẩn để quyết định reservation đã hết hạn. */
    @Scheduled(fixedDelayString = "${inventory.reservation-expiry.poll-delay-ms}")
    public void expireDueReservations() {
        List<String> reservationIds = reservationRepository.claimDueForExpiry(batchSize, leaseSeconds);
        for (String reservationId : reservationIds) {
            try {
                // Service mở transaction riêng cho từng reservation, vì vậy một lỗi không chặn phần còn lại của lô.
                reservationService.expire(reservationId, "inventory-expiry-worker");
            } catch (RuntimeException exception) {
                // Không xóa lease ở đây; khi lease hết hạn, worker khác sẽ thử lại sau khi lỗi được khắc phục.
                log.warn("Không thể expire reservation {}; sẽ thử lại sau khi lease hết hạn",
                        reservationId, exception);
            }
        }
    }
}
