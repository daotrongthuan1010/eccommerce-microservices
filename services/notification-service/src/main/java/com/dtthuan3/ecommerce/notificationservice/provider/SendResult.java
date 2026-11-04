package com.dtthuan3.ecommerce.notificationservice.provider;

/**
 * Ket qua mot lan goi provider gia lap (NOTI-05).
 * UNKNOWN rieng biet: provider co the da nhan nhung mat ACK — khong tu gui lai ngay (NOTI-07).
 */
public record SendResult(
        String outcome,          // ACCEPTED | DELIVERED | FAILED_RETRYABLE | FAILED_FINAL | UNKNOWN
        String providerMessageId,
        String errorCode,
        String errorMessage) {

    public static SendResult accepted(String providerMessageId) {
        return new SendResult("ACCEPTED", providerMessageId, null, null);
    }

    public static SendResult delivered(String providerMessageId) {
        return new SendResult("DELIVERED", providerMessageId, null, null);
    }

    public static SendResult retryable(String code, String message) {
        return new SendResult("FAILED_RETRYABLE", null, code, message);
    }

    public static SendResult permanent(String code, String message) {
        return new SendResult("FAILED_FINAL", null, code, message);
    }

    public static SendResult unknown(String code, String message) {
        return new SendResult("UNKNOWN", null, code, message);
    }
}
