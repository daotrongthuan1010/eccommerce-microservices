-- Notification service v1: notification requests (business-key dedup), inbox dedup,
-- delivery attempts, dead-letter cases. Tien: VND so nguyen. Thoi gian: UTC.
-- NOTI-03: businessKey = (businessEventId, recipientId, channel, templateKey) chong gui lap.
-- PostgreSQL syntax (TIMESTAMP(6), UNIQUE/INDEX) — khong dung cu phap MySQL.

CREATE TABLE notification_request (
    id                VARCHAR(36)  NOT NULL,
    business_event_id VARCHAR(64)  NOT NULL,
    recipient_id      VARCHAR(64)  NOT NULL,
    channel           VARCHAR(16)  NOT NULL,          -- EMAIL | SMS
    template_key      VARCHAR(64)  NOT NULL,          -- vd order-confirmed.vi
    purpose           VARCHAR(16)  NOT NULL,          -- TRANSACTIONAL | MARKETING
    business_key      VARCHAR(255) NOT NULL,          -- businessEventId|recipientId|channel|templateKey
    subject           VARCHAR(255) NULL,
    body              TEXT         NOT NULL,
    status            VARCHAR(24)  NOT NULL,          -- QUEUED|SENDING|ACCEPTED|DELIVERED|FAILED_RETRYABLE|FAILED_FINAL|CANCELLED
    attempts          INT          NOT NULL DEFAULT 0,
    next_attempt_at   TIMESTAMP(6) NULL,
    provider_message_id VARCHAR(128) NULL,
    last_error        VARCHAR(512) NULL,
    order_id          VARCHAR(36)  NULL,               -- tham chieu truy vet
    correlation_id    VARCHAR(64)  NULL,
    created_at        TIMESTAMP(6) NOT NULL,
    updated_at        TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_business_key UNIQUE (business_key)
);
CREATE INDEX idx_notification_due ON notification_request (status, next_attempt_at);
CREATE INDEX idx_notification_recipient ON notification_request (recipient_id, created_at);
CREATE INDEX idx_notification_order ON notification_request (order_id);

-- Inbox dedup: Kafka event den lai khong tao request lan 2.
CREATE TABLE notification_inbox (
    event_id     VARCHAR(36)  NOT NULL,
    source       VARCHAR(64)  NOT NULL,
    event_type   VARCHAR(64)  NOT NULL,
    processed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (event_id, source)
);

-- Moi lan goi provider: NOTI-05/06, UNKNOWN khong tu dong thanh DELIVERED.
CREATE TABLE notification_delivery_attempt (
    id                  VARCHAR(36) NOT NULL,
    request_id          VARCHAR(36) NOT NULL,
    attempt_no          INT        NOT NULL,
    outcome             VARCHAR(24) NOT NULL,        -- ACCEPTED|DELIVERED|FAILED_RETRYABLE|FAILED_FINAL|UNKNOWN
    provider_message_id VARCHAR(128) NULL,
    error_code          VARCHAR(64)  NULL,
    error_message       VARCHAR(512) NULL,
    attempted_at        TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX idx_attempt_request ON notification_delivery_attempt (request_id, attempt_no);

-- DLQ co nguoi xu ly: NOTI-10.
CREATE TABLE notification_dead_letter (
    id          VARCHAR(36) NOT NULL,
    request_id  VARCHAR(36) NOT NULL,
    reason      VARCHAR(64)  NOT NULL,
    detail      VARCHAR(512) NULL,
    status      VARCHAR(24)  NOT NULL DEFAULT 'OPEN',  -- OPEN|RESOLVED
    created_at  TIMESTAMP(6) NOT NULL,
    resolved_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id)
);
CREATE INDEX idx_dl_request ON notification_dead_letter (request_id);
