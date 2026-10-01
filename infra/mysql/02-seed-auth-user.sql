-- Seed demo cho hoc sinh query truc tiep.
-- Day la bang demo, chua phai schema nghiep vu chot cua du an.
CREATE TABLE IF NOT EXISTS auth_db.demo_accounts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  display_name VARCHAR(200) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT IGNORE INTO auth_db.demo_accounts (username, display_name) VALUES
  ('admin', 'Admin demo'),
  ('student', 'Hoc vien demo');

CREATE TABLE IF NOT EXISTS user_db.demo_users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  email VARCHAR(200) NOT NULL,
  full_name VARCHAR(200) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT IGNORE INTO user_db.demo_users (username, email, full_name) VALUES
  ('admin', 'admin@ecommerce.local', 'Admin Local'),
  ('student', 'student@ecommerce.local', 'Student Local');
