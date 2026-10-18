package com.dtthuan3.ecommerce.common.exception;

/** Thrown when a create or update violates a business uniqueness rule. */
public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}
