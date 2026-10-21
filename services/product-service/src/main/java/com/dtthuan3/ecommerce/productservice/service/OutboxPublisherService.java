
package com.dtthuan3.ecommerce.productservice.service;

import java.util.UUID;

public interface OutboxPublisherService {

    void publish(UUID eventRowId);
}
