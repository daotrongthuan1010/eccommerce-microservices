package com.dtthuan3.ecommerce.notificationservice.service;

import com.dtthuan3.ecommerce.notificationservice.config.NotificationProperties;
import com.dtthuan3.ecommerce.notificationservice.entity.NotificationRequest;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationRequestRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Recovery SENDING mai (pod crash giu transaction): worker phai chuyen lai
 * QUEUED de xu ly lai — chong kec vinh vien (NOTI-UC05 tinh huong phuc hoi).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationWorkerRecoveryTest {

    @Mock private NotificationRequestRepository requests;

    @Test
    @DisplayName("Recovery: request SENDING mai > 5 phut duoc chuyen lai QUEUED")
    void staleSendingRecovered() {
        NotificationRequest stuck = new NotificationRequest();
        stuck.setId("req-stuck");
        stuck.setStatus("SENDING");
        stuck.setAttempts(1);
        stuck.setUpdatedAt(Instant.now().minus(Duration.ofMinutes(10)));

        when(requests.findStaleSending(any(), any())).thenReturn(List.of(stuck));
        when(requests.findDueBatch(any(), any())).thenReturn(List.of());

        NotificationWorker worker = new NotificationWorker(requests,
                new NotificationRequestProcessor(null, null, null, null, null, null,
                        null, null, null, null),
                new NotificationProperties());

        worker.pollOnce();

        assertThat(stuck.getStatus()).isEqualTo("QUEUED");
        verify(requests).save(stuck);
    }

    @Test
    @DisplayName("Khong co request mai: khong save gi, poll binh thuong")
    void noStale_noop() {
        when(requests.findStaleSending(any(), any())).thenReturn(List.of());
        when(requests.findDueBatch(any(), any())).thenReturn(List.of());

        NotificationWorker worker = new NotificationWorker(requests,
                new NotificationRequestProcessor(null, null, null, null, null, null,
                        null, null, null, null),
                new NotificationProperties());

        int processed = worker.pollOnce();

        assertThat(processed).isZero();
        verify(requests, org.mockito.Mockito.never()).save(any());
    }
}
