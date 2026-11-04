-- V2: Notification templates (NOTI-02) + snapshot columns cho request.
-- Template co phien ban, trang thai DRAFT/ACTIVE/RETIRED; request chot templateVersion
-- tai thoi diem tao (NOTI-UC04 buoc 5) nen template sua sau khong doi noi dung request cu.
-- Render thuc hien luc gui (co du lieu nguoi nhan: displayName), khong render trong consumer.

CREATE TABLE notification_template (
    id                VARCHAR(36)  NOT NULL,
    template_key      VARCHAR(64)  NOT NULL,   -- order-confirmed | order-canceled
    version           INT          NOT NULL,
    channel           VARCHAR(16)  NOT NULL,    -- EMAIL
    locale            VARCHAR(8)   NOT NULL,    -- vi
    purpose           VARCHAR(16)  NOT NULL,    -- TRANSACTIONAL | MARKETING
    subject_template  VARCHAR(255) NOT NULL,
    body_template     TEXT         NOT NULL,
    required_variables VARCHAR(255) NOT NULL DEFAULT '',  -- phan cach bang dau phay
    status            VARCHAR(16)  NOT NULL,    -- DRAFT | ACTIVE | RETIRED
    created_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at        TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_template UNIQUE (template_key, channel, version)
);
CREATE INDEX idx_notification_template_lookup
    ON notification_template (template_key, channel, locale, status);

-- Request snapshot nguoi nhan + template version + bien render (NOTI-UC01 buoc 2).
ALTER TABLE notification_request
    ADD COLUMN recipient_email  VARCHAR(255) NULL,
    ADD COLUMN recipient_name   VARCHAR(255) NULL,
    ADD COLUMN template_version INT          NULL,
    ADD COLUMN locale           VARCHAR(8)   NOT NULL DEFAULT 'vi',
    ADD COLUMN variables        TEXT         NULL;
-- Body/subject gio duoc render luc gui; luc tao chi chot version + bien.
ALTER TABLE notification_request ALTER COLUMN body DROP NOT NULL;

-- Seed template da duyet (ACTIVE). Sua mau = tao version moi + RETIRED version cu,
-- khong update noi dung version da dung (NOTI-UC04 buoc 4-6).
INSERT INTO notification_template
    (id, template_key, version, channel, locale, purpose,
     subject_template, body_template, required_variables, status, created_by, created_at)
VALUES
    ('11111111-1111-4111-8111-111111111111', 'order-confirmed', 1, 'EMAIL', 'vi', 'TRANSACTIONAL',
     'Đơn hàng {{orderCode}} đã được xác nhận',
     E'Xin chào {{displayName}},\n\nĐơn hàng {{orderCode}} của bạn đã được xác nhận.\nTổng tiền: {{totalAmount}}.\n\nNếu có câu hỏi, vui lòng liên hệ bộ phận hỗ trợ.\n\nCảm ơn bạn đã mua sắm tại shop.',
     'orderCode,displayName,totalAmount', 'ACTIVE', 'system-seed', now()),
    ('22222222-2222-4222-8222-222222222222', 'order-canceled', 1, 'EMAIL', 'vi', 'TRANSACTIONAL',
     'Đơn hàng {{orderCode}} đã bị hủy',
     E'Xin chào {{displayName}},\n\nĐơn hàng {{orderCode}} của bạn đã bị hủy.\nSố tiền đã thanh toán (nếu có) sẽ được hoàn lại theo chính sách hoàn tiền.\n\nNếu có câu hỏi, vui lòng liên hệ bộ phận hỗ trợ.\n\nCảm ơn bạn.',
     'orderCode,displayName', 'ACTIVE', 'system-seed', now());
