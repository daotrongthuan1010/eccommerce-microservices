package com.dtthuan3.ecommerce.notificationservice.entity;

import java.io.Serializable;

/** Composite key (event_id, source) cho inbox dedup. */
public record NotificationInboxId(String eventId, String source) implements Serializable {
}
