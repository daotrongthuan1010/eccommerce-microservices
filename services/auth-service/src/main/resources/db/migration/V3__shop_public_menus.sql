-- V3: Seed shop menus (sider buyer 100% tu DB, khong link cung FE).
-- Quy uoc public: menu KHONG co permission nao gan vao duoc IamQueryService.menusForUser
-- coi la public (tra ve actions = [] cho moi user da login). Cac menu duoi day khong
-- tao permission kem theo nen mac nhien public; muon khoa lai thi tao permission
-- MENU.* + gan role (luc do user khong grant se tu dong bi an).
-- is_default = 1: chuc nang cot loi khong cho xoa (Trang chu, San pham, Gio hang,
-- Thanh toan, Don hang, Tai khoan). Con lai is_default = 0 (xoa mem duoc).
-- Idempotent: INSERT IGNORE (bo qua khi code da ton tai) + UPDATE chuan hoa gia tri
-- canonical cho tung code (phong truong hop menu da duoc tao tay voi path/name lech).
-- parent_id NULL: hien thi root, khong gom nhom bua.
-- order_no 10..140 theo thu tu sider mong muon.

INSERT IGNORE INTO menus (code, name, path, icon, parent_id, order_no, is_active, is_default) VALUES
  ('SHOP_HOME', 'Trang chu', '/', 'home', NULL, 10, 1, 1),
  ('SHOP_PRODUCTS', 'San pham', '/products', 'shopping', NULL, 20, 1, 1),
  ('SHOP_CATALOG', 'Danh muc', '/catalog', 'appstore', NULL, 30, 1, 0),
  ('SHOP_INVENTORY', 'Ton kho', '/inventory', 'list', NULL, 40, 1, 0),
  ('SHOP_RECEIPTS', 'Phieu nhap', '/receipts', 'file', NULL, 50, 1, 0),
  ('SHOP_FILES', 'Tep tin', '/files', 'file', NULL, 60, 1, 0),
  ('SHOP_PUBLISHING', 'Xuat ban', '/publishing', 'solution', NULL, 70, 1, 0),
  ('SHOP_REVIEWS', 'Danh gia', '/reviews', 'audit', NULL, 80, 1, 0),
  ('SHOP_CART', 'Gio hang', '/cart', 'cart', NULL, 90, 1, 1),
  ('SHOP_CHECKOUT', 'Thanh toan', '/checkout', 'shopping', NULL, 100, 1, 1),
  ('SHOP_ORDERS', 'Don hang', '/orders', 'orders', NULL, 110, 1, 1),
  ('SHOP_DASHBOARD', 'Tong quan', '/dashboard', 'dashboard', NULL, 120, 1, 0),
  ('SHOP_ACCOUNT', 'Tai khoan', '/account', 'user', NULL, 130, 1, 1),
  ('SHOP_CHANGE_PASSWORD', 'Doi mat khau', '/change-password', 'lock', NULL, 140, 1, 0);

-- Chuan hoa gia tri canonical (chi cham vao 14 code shop, khong dung den menu admin
-- hay 3 menu V1 USER/PRODUCT/ORDER).
UPDATE menus SET name = 'Trang chu', path = '/', icon = 'home', parent_id = NULL, order_no = 10, is_active = 1, is_default = 1 WHERE code = 'SHOP_HOME';
UPDATE menus SET name = 'San pham', path = '/products', icon = 'shopping', parent_id = NULL, order_no = 20, is_active = 1, is_default = 1 WHERE code = 'SHOP_PRODUCTS';
UPDATE menus SET name = 'Danh muc', path = '/catalog', icon = 'appstore', parent_id = NULL, order_no = 30, is_active = 1, is_default = 0 WHERE code = 'SHOP_CATALOG';
UPDATE menus SET name = 'Ton kho', path = '/inventory', icon = 'list', parent_id = NULL, order_no = 40, is_active = 1, is_default = 0 WHERE code = 'SHOP_INVENTORY';
UPDATE menus SET name = 'Phieu nhap', path = '/receipts', icon = 'file', parent_id = NULL, order_no = 50, is_active = 1, is_default = 0 WHERE code = 'SHOP_RECEIPTS';
UPDATE menus SET name = 'Tep tin', path = '/files', icon = 'file', parent_id = NULL, order_no = 60, is_active = 1, is_default = 0 WHERE code = 'SHOP_FILES';
UPDATE menus SET name = 'Xuat ban', path = '/publishing', icon = 'solution', parent_id = NULL, order_no = 70, is_active = 1, is_default = 0 WHERE code = 'SHOP_PUBLISHING';
UPDATE menus SET name = 'Danh gia', path = '/reviews', icon = 'audit', parent_id = NULL, order_no = 80, is_active = 1, is_default = 0 WHERE code = 'SHOP_REVIEWS';
UPDATE menus SET name = 'Gio hang', path = '/cart', icon = 'cart', parent_id = NULL, order_no = 90, is_active = 1, is_default = 1 WHERE code = 'SHOP_CART';
UPDATE menus SET name = 'Thanh toan', path = '/checkout', icon = 'shopping', parent_id = NULL, order_no = 100, is_active = 1, is_default = 1 WHERE code = 'SHOP_CHECKOUT';
UPDATE menus SET name = 'Don hang', path = '/orders', icon = 'orders', parent_id = NULL, order_no = 110, is_active = 1, is_default = 1 WHERE code = 'SHOP_ORDERS';
UPDATE menus SET name = 'Tong quan', path = '/dashboard', icon = 'dashboard', parent_id = NULL, order_no = 120, is_active = 1, is_default = 0 WHERE code = 'SHOP_DASHBOARD';
UPDATE menus SET name = 'Tai khoan', path = '/account', icon = 'user', parent_id = NULL, order_no = 130, is_active = 1, is_default = 1 WHERE code = 'SHOP_ACCOUNT';
UPDATE menus SET name = 'Doi mat khau', path = '/change-password', icon = 'lock', parent_id = NULL, order_no = 140, is_active = 1, is_default = 0 WHERE code = 'SHOP_CHANGE_PASSWORD';
