package com.dtthuan3.ecommerce.fileservice.exception;

public class VirusScanException extends RuntimeException {

    public VirusScanException(String message) {
        super(message);
    }

    public VirusScanException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}