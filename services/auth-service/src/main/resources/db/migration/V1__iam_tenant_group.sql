-- V1: IAM tenant / group / role (hoc theo smart-contract authen-service).
CREATE TABLE IF NOT EXISTS tenants (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(50) NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  created_by VARCHAR(100) NULL,
  updated_by VARCHAR(100) NULL
);

CREATE TABLE IF NOT EXISTS group_types (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(50) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(255) NULL,
  is_active BIT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  created_by VARCHAR(100) NULL,
  updated_by VARCHAR(100) NULL
);

CREATE TABLE IF NOT EXISTS user_groups (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(500) NULL,
  group_type_id BIGINT NULL,
  parent_id BIGINT NULL,
  is_active BIT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  created_by VARCHAR(100) NULL,
  updated_by VARCHAR(100) NULL,
  CONSTRAINT fk_group_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
  CONSTRAINT fk_group_type FOREIGN KEY (group_type_id) REFERENCES group_types (id),
  CONSTRAINT fk_group_parent FOREIGN KEY (parent_id) REFERENCES user_groups (id)
);
CREATE INDEX idx_group_tenant ON user_groups (tenant_id);
CREATE INDEX idx_group_parent ON user_groups (parent_id);
CREATE INDEX idx_group_type ON user_groups (group_type_id);

CREATE TABLE IF NOT EXISTS positions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  group_id BIGINT NULL,
  name VARCHAR(100) NOT NULL,
  level INT NOT NULL,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  created_by VARCHAR(100) NULL,
  updated_by VARCHAR(100) NULL,
  CONSTRAINT fk_position_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
  CONSTRAINT fk_position_group FOREIGN KEY (group_id) REFERENCES user_groups (id)
);
CREATE INDEX idx_position_tenant ON positions (tenant_id);
CREATE INDEX idx_position_group ON positions (group_id);
CREATE INDEX idx_position_level ON positions (level);

CREATE TABLE IF NOT EXISTS auth_users (
  id VARCHAR(36) PRIMARY KEY,
  tenant_id BIGINT NULL,
  username VARCHAR(100) NOT NULL UNIQUE,
  full_name VARCHAR(200) NULL,
  email VARCHAR(150) NULL UNIQUE,
  group_id BIGINT NULL,
  position_id BIGINT NULL,
  is_system_admin BIT(1) NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  created_by VARCHAR(100) NULL,
  updated_by VARCHAR(100) NULL,
  CONSTRAINT fk_authuser_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
  CONSTRAINT fk_authuser_group FOREIGN KEY (group_id) REFERENCES user_groups (id),
  CONSTRAINT fk_authuser_position FOREIGN KEY (position_id) REFERENCES positions (id)
);
CREATE INDEX idx_auth_user_tenant ON auth_users (tenant_id);
CREATE INDEX idx_auth_user_group ON auth_users (group_id);
CREATE INDEX idx_auth_user_position ON auth_users (position_id);

CREATE TABLE IF NOT EXISTS roles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NULL,
  code VARCHAR(50) NOT NULL,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(255) NULL,
  is_system_role BIT(1) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  created_by VARCHAR(100) NULL,
  updated_by VARCHAR(100) NULL,
  CONSTRAINT fk_role_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);
CREATE INDEX idx_role_tenant ON roles (tenant_id);
CREATE INDEX idx_role_code ON roles (code);

CREATE TABLE IF NOT EXISTS menus (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(50) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  path VARCHAR(255) NULL,
  icon VARCHAR(100) NULL,
  parent_id BIGINT NULL,
  order_no INT NULL,
  is_active BIT(1) NOT NULL DEFAULT 1,
  is_deleted BIT(1) NOT NULL DEFAULT 0,
  is_default BIT(1) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  created_by VARCHAR(100) NULL,
  updated_by VARCHAR(100) NULL,
  CONSTRAINT fk_menu_parent FOREIGN KEY (parent_id) REFERENCES menus (id)
);
CREATE INDEX idx_menu_parent ON menus (parent_id);

CREATE TABLE IF NOT EXISTS actions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(50) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(255) NULL,
  is_active BIT(1) NOT NULL DEFAULT 1,
  is_deleted BIT(1) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  created_by VARCHAR(100) NULL,
  updated_by VARCHAR(100) NULL
);

CREATE TABLE IF NOT EXISTS permissions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  menu_id BIGINT NOT NULL,
  action_id BIGINT NOT NULL,
  code VARCHAR(100) NOT NULL UNIQUE,
  description VARCHAR(255) NULL,
  is_deleted BIT(1) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  created_by VARCHAR(100) NULL,
  updated_by VARCHAR(100) NULL,
  CONSTRAINT fk_perm_menu FOREIGN KEY (menu_id) REFERENCES menus (id),
  CONSTRAINT fk_perm_action FOREIGN KEY (action_id) REFERENCES actions (id),
  CONSTRAINT uk_permission_menu_action UNIQUE (menu_id, action_id)
);
CREATE INDEX idx_permission_menu ON permissions (menu_id);
CREATE INDEX idx_permission_action ON permissions (action_id);

CREATE TABLE IF NOT EXISTS user_roles (
  user_id VARCHAR(36) NOT NULL,
  role_id BIGINT NOT NULL,
  assigned_by VARCHAR(100) NULL,
  assigned_at TIMESTAMP NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_userrole_user FOREIGN KEY (user_id) REFERENCES auth_users (id),
  CONSTRAINT fk_userrole_role FOREIGN KEY (role_id) REFERENCES roles (id)
);
CREATE INDEX idx_user_role_role ON user_roles (role_id);

CREATE TABLE IF NOT EXISTS user_group_roles (
  group_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (group_id, role_id),
  CONSTRAINT fk_grouprole_group FOREIGN KEY (group_id) REFERENCES user_groups (id),
  CONSTRAINT fk_grouprole_role FOREIGN KEY (role_id) REFERENCES roles (id)
);
CREATE INDEX idx_group_role_role ON user_group_roles (role_id);

CREATE TABLE IF NOT EXISTS role_permissions (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_roleperm_role FOREIGN KEY (role_id) REFERENCES roles (id),
  CONSTRAINT fk_roleperm_perm FOREIGN KEY (permission_id) REFERENCES permissions (id)
);

-- Seed: group types, actions, menus, TENANT_ADMIN system role, admin user.
INSERT IGNORE INTO group_types (code, name, description) VALUES
  ('PLATFORM', 'Van hanh nen tang', 'Nhom thuoc doi van hanh he thong'),
  ('TENANT', 'Don vi thue', 'Cong ty/to chuc thue'),
  ('DEPARTMENT', 'Phong ban', 'Phong ban trong tenant'),
  ('TEAM', 'Nhom', 'Nhom du an tam thoi');

INSERT IGNORE INTO actions (code, name) VALUES
  ('VIEW', 'Xem'), ('CREATE', 'Them moi'), ('EDIT', 'Sua'), ('DELETE', 'Xoa'), ('APPROVE', 'Duyet');

INSERT IGNORE INTO menus (code, name, path, is_default) VALUES
  ('USER', 'Nguoi dung', '/users', 1),
  ('PRODUCT', 'San pham', '/products', 1),
  ('ORDER', 'Don hang', '/orders', 1);

INSERT IGNORE INTO roles (tenant_id, code, name, description, is_system_role) VALUES
  (NULL, 'TENANT_ADMIN', 'Tenant Admin', 'Quan tri don vi thue', 1);
