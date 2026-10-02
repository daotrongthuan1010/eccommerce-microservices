#!/usr/bin/env bash
set -euo pipefail
create_database() {
  local service="$1" password="$2"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres --set=role="${service}_user" --set=db="${service}_db" --set=password="$password" <<'SQL'
CREATE ROLE :"role" LOGIN PASSWORD :'password';
CREATE DATABASE :"db" OWNER :"role";
REVOKE CONNECT ON DATABASE :"db" FROM PUBLIC;
GRANT CONNECT ON DATABASE :"db" TO :"role";
SQL
}
create_database keycloak "$KEYCLOAK_DB_PASSWORD"
create_database catalog "$CATALOG_DB_PASSWORD"
create_database inventory "$INVENTORY_DB_PASSWORD"
create_database order "$ORDER_DB_PASSWORD"
create_database notification "$NOTIFICATION_DB_PASSWORD"
create_database product "$PRODUCT_DB_PASSWORD"
create_database review "$REVIEW_DB_PASSWORD"
create_database analytics "$ANALYTICS_DB_PASSWORD"
