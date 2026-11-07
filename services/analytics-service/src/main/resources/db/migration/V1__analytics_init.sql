-- Bao cao doanh thu event-driven (Phase 1): tong hop tu event OrderPaid.
-- Idempotent: processed_events dedup theo event_id, aggregate upsert ON CONFLICT.
-- refunded_revenue/net_revenue de san cho Phase refund (hien net = gross - refunded).

CREATE TABLE IF NOT EXISTS processed_events (
    event_id     VARCHAR(36) PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS daily_revenue (
    stat_date        DATE NOT NULL,
    currency         VARCHAR(3) NOT NULL,
    gross_revenue    BIGINT NOT NULL DEFAULT 0,
    refunded_revenue BIGINT NOT NULL DEFAULT 0,
    net_revenue      BIGINT NOT NULL DEFAULT 0,
    order_count      INT NOT NULL DEFAULT 0,
    PRIMARY KEY (stat_date, currency)
);

CREATE TABLE IF NOT EXISTS product_sales (
    stat_date    DATE NOT NULL,
    sku_id       VARCHAR(64) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity     INT NOT NULL DEFAULT 0,
    revenue      BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (stat_date, sku_id)
);
