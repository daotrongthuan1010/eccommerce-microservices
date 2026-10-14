package com.dtthuan3.ecommerce.catalogservice.exception;

/**
 * Ngoai le khi tao moi tai nguyen nhung du lieu da ton tai (trung ten, trung khoa chinh).
 * <p>Duoc {@code GlobalExceptionHandler} bat va tra ve 409 Conflict.</p>
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}