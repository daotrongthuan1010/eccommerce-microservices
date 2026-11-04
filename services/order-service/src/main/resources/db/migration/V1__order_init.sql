-- Order service v1: carts, orders, order lines, idempotent checkout, transactional outbox, inbox dedup.
-- Tien: VND so nguyen (BIGINT). Thoi gian: UTC.
-- Order la orchestrator duy nhất (PAYMENT_SHIPPING_PLAN D1): payment/shipping khong goi nguoc order.

CREATE TABLE carts (
    id          VARCHAR(36)  NOT NULL,
    user_id     VARCHAR(64)  NOT NULL,
    status      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_carts_user_active (user_id, status)
);

CREATE TABLE cart_items (
    id          VARCHAR(36)  NOT NULL,
    cart_id     VARCHAR(36)  NOT NULL,
    sku_id      VARCHAR(64)  NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    unit_price  BIGINT       NOT NULL,
    quantity    INT          NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_items_cart_sku (cart_id, sku_id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id)
);

CREATE TABLE orders (
    id                  VARCHAR(36)  NOT NULL,
    order_code          VARCHAR(32)  NOT NULL,
    user_id             VARCHAR(64)  NOT NULL,
    status              VARCHAR(24)  NOT NULL,
    total_amount        BIGINT       NOT NULL,
    currency            VARCHAR(3)   NOT NULL DEFAULT 'VND',
    payment_method      VARCHAR(16)  NULL,
    payment_id          VARCHAR(36)  NULL,
    shipping_address    VARCHAR(512) NOT NULL,
    recipient_name      VARCHAR(128) NOT NULL,
    recipient_phone     VARCHAR(32)  NOT NULL,
    reservation_id      VARCHAR(100) NULL,
    idempotency_key     VARCHAR(128) NOT NULL,
    request_hash        VARCHAR(64)  NOT NULL,
    cancel_reason       VARCHAR(512) NULL,
    version             BIGINT       NOT NULL DEFAULT 0,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_code (order_code),
    UNIQUE KEY uk_orders_idempotency (idempotency_key),
    KEY idx_orders_user (user_id, created_at),
    KEY idx_orders_status (status)
);

CREATE TABLE order_lines (
    id          VARCHAR(36)  NOT NULL,
    order_id    VARCHAR(36)  NOT NULL,
    line_no     INT          NOT NULL,
    sku_id      VARCHAR(64)  NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    unit_price  BIGINT       NOT NULL,
    quantity    INT          NOT NULL,
    line_amount BIGINT       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_lines_order_line (order_id, line_no),
    CONSTRAINT fk_order_lines_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

-- Transactional outbox: ghi cung transaction voi order, poller day Kafka at-least-once.
CREATE TABLE order_outbox (
    event_id     VARCHAR(36)  NOT NULL,
    aggregate_id VARCHAR(36)  NOT NULL,
    event_type   VARCHAR(64)  NOT NULL,
    topic        VARCHAR(128) NOT NULL,
    payload      TEXT         NOT NULL,
    attempts     INT          NOT NULL DEFAULT 0,
    published_at DATETIME(6)  NULL,
    created_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (event_id),
    KEY idx_order_outbox_pub (published_at, created_at)
);

-- Inbox dedup: consumer ghi event da xu ly de event gui lai khong tac dong lan 2.
CREATE TABLE order_inbox (
    event_id     VARCHAR(36)  NOT NULL,
    source       VARCHAR(64)  NOT NULL,
    event_type   VARCHAR(64)  NOT NULL,
    processed_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (event_id, source)
);
