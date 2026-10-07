package com.dtthuan3.ecommerce.inventoryservice.exception;

/** Báo client đang ghi dựa trên version cũ của inventory. */
public class ConcurrencyConflictException extends RuntimeException {
    public ConcurrencyConflictException(String message) { super(message); }
}
