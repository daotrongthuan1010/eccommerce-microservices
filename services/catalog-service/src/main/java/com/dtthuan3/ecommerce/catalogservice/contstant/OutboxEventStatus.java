package com.dtthuan3.ecommerce.catalogservice.contstant;

public enum OutboxEventStatus {

    PENDING, // đang chờ
    PUBLISHED, // đã xuất bản
    FAILED // that bại
}