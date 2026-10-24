-- V2: Chi SYSTEM_ADMIN la menu mac dinh khong-xoa-duoc (goc cay admin).
-- V1 seed is_default=1 cho USER/PRODUCT/ORDER lam FE disable nut Xoa 3 menu nghiep vu,
-- trong khi cac menu admin tao sau qua API (MenuAdminService.create luon isDefault=false)
-- nen SYSTEM_ADMIN van xoa duoc. Migration nay dao lai:
--   1. Ha is_default cua USER/PRODUCT/ORDER ve 0 (xoa mem duoc, dung yeu cau).
--   2. Nang is_default cua SYSTEM_ADMIN len 1 (khoa xoa mem).
-- Dung UPDATE nen idempotent: chay lai hoac chay tren DB chua co SYSTEM_ADMIN van khong loi.
UPDATE menus SET is_default = 0 WHERE code IN ('USER', 'PRODUCT', 'ORDER');
UPDATE menus SET is_default = 1 WHERE code = 'SYSTEM_ADMIN';
