-- Chạy một lần trên database hiện hữu nếu stock_movement.id vẫn là cột IDENTITY kiểu số.
-- Dừng Inventory Service trước khi chạy để Hibernate không cập nhật schema đồng thời.
-- Dữ liệu ID số cũ được chuyển thành chuỗi (ví dụ 15 -> '15'); ID mới sẽ là UUID.
BEGIN;

ALTER TABLE public.stock_movement
    ALTER COLUMN id DROP IDENTITY IF EXISTS;

ALTER TABLE public.stock_movement
    ALTER COLUMN id DROP DEFAULT;

ALTER TABLE public.stock_movement
    ALTER COLUMN id TYPE varchar(36) USING id::text;

COMMIT;
