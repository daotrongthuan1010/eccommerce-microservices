package com.dtthuan3.ecommerce.notificationservice.controller;

import com.dtthuan3.ecommerce.notificationservice.entity.NotificationRequest;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationDeadLetterRepository;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationRequestRepository;
import com.dtthuan3.ecommerce.notificationservice.security.SecurityUtils;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST console (docs notification.md): khach xem thong bao cua minh, admin xem jobs/DLQ.
 * Moi request kiem role voi chinh resource: /me dung principal, /admin can ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRequestRepository requests;
    private final NotificationDeadLetterRepository deadLetters;

    /** GET /api/v1/me/notifications — thong bao cua principal hien tai. */
    @GetMapping("/me/notifications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> myNotifications() {
        String userId = SecurityUtils.requireUser().subject();
        List<NotificationRequest> list = requests.findByRecipientIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(list.stream().map(NotificationController::toSummary).toList());
    }

    /** GET /api/v1/admin/notifications/jobs — admin xem tat ca request theo trang thai. */
    @GetMapping("/admin/notifications/jobs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> jobs() {
        long queued = requests.countByStatus("QUEUED");
        long sending = requests.countByStatus("SENDING");
        long accepted = requests.countByStatus("ACCEPTED");
        long delivered = requests.countByStatus("DELIVERED");
        long retryable = requests.countByStatus("FAILED_RETRYABLE");
        long finalFailed = requests.countByStatus("FAILED_FINAL");
        long cancelled = requests.countByStatus("CANCELLED");
        long dlqOpen = deadLetters.countByStatus("OPEN");
        return ResponseEntity.ok(Map.of(
                "queued", queued, "sending", sending, "accepted", accepted,
                "delivered", delivered, "failedRetryable", retryable,
                "failedFinal", finalFailed, "cancelled", cancelled,
                "dlqOpen", dlqOpen));
    }

    /** GET /api/v1/admin/notifications/dlq — admin xem DLQ dang mo. */
    @GetMapping("/admin/notifications/dlq")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> dlq() {
        return ResponseEntity.ok(deadLetters.findByStatusOrderByCreatedAtDesc("OPEN").stream()
                .map(d -> Map.<String, Object>of(
                        "id", d.getId(),
                        "requestId", d.getRequestId(),
                        "reason", d.getReason(),
                        "detail", d.getDetail() == null ? "" : d.getDetail(),
                        "createdAt", d.getCreatedAt().toString()))
                .toList());
    }

    /** GET /api/v1/admin/notifications/orders/{orderId} — truy vet thong bao theo don. */
    @GetMapping("/admin/notifications/orders/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> byOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(requests.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(NotificationController::toSummary)
                .toList());
    }

    /** Che PII (docs 15.2): khong tra email/subject/body day du qua API list. */
    private static Map<String, Object> toSummary(NotificationRequest r) {
        return Map.of(
                "id", r.getId(),
                "channel", r.getChannel(),
                "templateKey", r.getTemplateKey(),
                "templateVersion", r.getTemplateVersion() == null ? 0 : r.getTemplateVersion(),
                "purpose", r.getPurpose(),
                "status", r.getStatus(),
                "attempts", r.getAttempts(),
                "orderId", r.getOrderId() == null ? "" : r.getOrderId(),
                "createdAt", r.getCreatedAt().toString(),
                "lastError", r.getLastError() == null ? "" : r.getLastError());
    }
}
