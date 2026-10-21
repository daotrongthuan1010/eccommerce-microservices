package com.dtthuan3.ecommerce.productservice.service;

public interface OutboxPublisherSchedulerService {
    void publishPendingEvents();
}
