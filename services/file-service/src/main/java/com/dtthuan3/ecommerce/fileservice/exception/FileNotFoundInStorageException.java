package com.dtthuan3.ecommerce.fileservice.exception;

public class FileNotFoundInStorageException extends RuntimeException {
    public FileNotFoundInStorageException(String key) {
        super("File not found: " + key);
    }
}
