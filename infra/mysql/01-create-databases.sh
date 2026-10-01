#!/usr/bin/env bash
set -euo pipefail
# Hex-encode passwords before SQL interpolation; no shell or SQL execution of password text.
create_database() {
  local service="$1" password="$2" encoded
  encoded=$(printf '%s' "$password" | od -An -v -tx1 | tr -d ' \n')
  MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --protocol=socket -uroot <<SQL
CREATE DATABASE ${service}_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @password = CONVERT(0x${encoded} USING utf8mb4);
SET @statement = CONCAT('CREATE USER ''${service}_user''@''%'' IDENTIFIED BY ', QUOTE(@password));
PREPARE stmt FROM @statement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
GRANT ALL PRIVILEGES ON ${service}_db.* TO '${service}_user'@'%';
SQL
}
create_database auth "$AUTH_DB_PASSWORD"
create_database user "$USER_DB_PASSWORD"
create_database payment "$PAYMENT_DB_PASSWORD"
