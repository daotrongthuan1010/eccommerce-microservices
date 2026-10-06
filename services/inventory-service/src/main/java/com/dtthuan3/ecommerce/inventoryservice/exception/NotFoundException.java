package com.dtthuan3.ecommerce.inventoryservice.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}