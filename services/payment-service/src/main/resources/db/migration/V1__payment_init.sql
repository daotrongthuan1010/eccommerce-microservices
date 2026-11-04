-- Payment service v1: payments, refunds, callback audit/dedup, transactional outbox.
-- Tien: VND so nguyen (BIGINT), khong dung so thap phan.

CREATE TABLE payments (
    id               VARCHAR(36)  NOT NULL,
    order_id         VARCHAR(64)  NOT NULL,
    user_id          VARCHAR(64)  NULL,
    idempotency_key  VARCHAR(128) NOT NULL,
    request_hash     VARCHAR(64)  NOT NULL,
    method           VARCHAR(16)  NOT NULL,
    amount           BIGINT       NOT NULL,
    currency         VARCHAR(3)   NOT NULL DEFAULT 'VND',
    status           VARCHAR(24)  NOT NULL,
    provider_txn_ref VARCHAR(128) NULL,
    checkout_url     VARCHAR(1024) NULL,
    qr_content       VARCHAR(1024) NULL,
    return_url       VARCHAR(1024) NULL,
    description      VARCHAR(512) NULL,
    expires_at       DATETIME(6)  NULL,
    paid_at          DATETIME(6)  NULL,
    refunded_amount  BIGINT       NOT NULL DEFAULT 0,
    failure_code     VARCHAR(64)  NULL,
    failure_message  VARCHAR(512) NULL,
    late_payment     BIT          NOT NULL DEFAULT 0,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payments_idempotency (idempotency_key),
    UNIQUE KEY uk_payments_provider_txn (provider_txn_ref),
    KEY idx_payments_order (order_id),
    KEY idx_payments_status_expires (status, expires_at)
);

CREATE TABLE payment_refunds (
    id                 VARCHAR(36)  NOT NULL,
    payment_id         VARCHAR(36)  NOT NULL,
    idempotency_key    VARCHAR(128) NOT NULL,
    request_hash       VARCHAR(64)  NOT NULL,
    amount             BIGINT       NOT NULL,
    reason             VARCHAR(512) NULL,
    status             VARCHAR(16)  NOT NULL,
    provider_refund_ref VARCHAR(128) NULL,
    version            BIGINT       NOT NULL DEFAULT 0,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refunds_idempotency (idempotency_key),
    KEY idx_refunds_payment (payment_id),
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
);

CREATE TABLE payment_callback_logs (
    id               VARCHAR(36)  NOT NULL,
    provider         VARCHAR(16)  NOT NULL,
    provider_txn_ref VARCHAR(128) NULL,
    event_key        VARCHAR(64)  NOT NULL,
    signature_valid  BIT          NOT NULL,
    processed        BIT          NOT NULL,
    raw_payload      TEXT         NULL,
    received_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_callback_event_key (event_key)
);

CREATE TABLE payment_outbox (
    event_id     VARCHAR(36)  NOT NULL,
    aggregate_id VARCHAR(36)  NOT NULL,
    event_type   VARCHAR(64)  NOT NULL,
    topic        VARCHAR(128) NOT NULL,
    payload      TEXT         NOT NULL,
    attempts     INT          NOT NULL DEFAULT 0,
    published_at DATETIME(6)  NULL,
    created_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (event_id),
    KEY idx_pay_outbox_pub (published_at, created_at)
);
