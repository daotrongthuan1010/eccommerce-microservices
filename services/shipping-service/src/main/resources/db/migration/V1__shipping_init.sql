-- Shipping service v1: quotes, shipments, status history, transactional outbox. Tien: VND so nguyen.

CREATE TABLE shipping_quotes (
    id                       VARCHAR(36) NOT NULL,
    from_province_code       VARCHAR(8)  NOT NULL,
    to_province_code         VARCHAR(8)  NOT NULL,
    weight_grams             INT         NOT NULL,
    billable_weight_grams    INT         NOT NULL,
    length_cm                INT         NULL,
    width_cm                 INT         NULL,
    height_cm                INT         NULL,
    service_level            VARCHAR(16) NOT NULL,
    zone                     VARCHAR(16) NOT NULL,
    fee                      BIGINT      NOT NULL,
    currency                 VARCHAR(3)  NOT NULL DEFAULT 'VND',
    expires_at               TIMESTAMPTZ NOT NULL,
    consumed_by_shipment_id  VARCHAR(36) NULL,
    version                  BIGINT      NOT NULL DEFAULT 0,
    created_at               TIMESTAMPTZ NOT NULL,
    updated_at               TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE shipments (
    id                       VARCHAR(36)  NOT NULL,
    order_id                 VARCHAR(64)  NOT NULL,
    idempotency_key          VARCHAR(128) NOT NULL,
    request_hash             VARCHAR(64)  NOT NULL,
    quote_id                 VARCHAR(36)  NOT NULL,
    carrier                  VARCHAR(16)  NOT NULL DEFAULT 'FAKE_GHN',
    carrier_code             VARCHAR(64)  NULL,
    status                   VARCHAR(24)  NOT NULL,
    service_level            VARCHAR(16)  NOT NULL,
    shipping_fee             BIGINT       NOT NULL,
    cod_amount               BIGINT       NOT NULL DEFAULT 0,
    currency                 VARCHAR(3)   NOT NULL DEFAULT 'VND',
    weight_grams             INT          NOT NULL,
    sender_name              VARCHAR(128) NULL,
    sender_phone             VARCHAR(32)  NULL,
    sender_address           VARCHAR(512) NULL,
    sender_province_code     VARCHAR(8)   NULL,
    recipient_name           VARCHAR(128) NOT NULL,
    recipient_phone          VARCHAR(32)  NOT NULL,
    recipient_address        VARCHAR(512) NOT NULL,
    recipient_province_code  VARCHAR(8)   NOT NULL,
    delivery_attempts        INT          NOT NULL DEFAULT 0,
    failure_reason           VARCHAR(512) NULL,
    note                     VARCHAR(512) NULL,
    version                  BIGINT       NOT NULL DEFAULT 0,
    created_at               TIMESTAMPTZ  NOT NULL,
    updated_at               TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (idempotency_key),
    UNIQUE (carrier_code)
);
CREATE INDEX idx_shipments_order ON shipments (order_id);
CREATE INDEX idx_shipments_carrier_code ON shipments (carrier_code);

CREATE TABLE shipment_status_history (
    id          VARCHAR(36)  NOT NULL,
    shipment_id VARCHAR(36)  NOT NULL REFERENCES shipments (id),
    status      VARCHAR(24)  NOT NULL,
    note        VARCHAR(512) NULL,
    occurred_at TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX idx_history_shipment ON shipment_status_history (shipment_id, occurred_at);

CREATE TABLE shipping_outbox (
    event_id     VARCHAR(36)  NOT NULL,
    aggregate_id VARCHAR(36)  NOT NULL,
    event_type   VARCHAR(64)  NOT NULL,
    topic        VARCHAR(128) NOT NULL,
    payload      TEXT         NOT NULL,
    attempts     INT          NOT NULL DEFAULT 0,
    published_at TIMESTAMPTZ  NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (event_id)
);
CREATE INDEX idx_ship_outbox_pub ON shipping_outbox (published_at, created_at);
