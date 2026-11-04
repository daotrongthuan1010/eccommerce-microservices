#!/usr/bin/env bash
set -Eeuo pipefail

# Chỉ restore vào database trống trong môi trường cô lập; không dùng script này để ghi đè production trực tiếp.
required=(BACKUP_S3_ENDPOINT BACKUP_S3_ACCESS_KEY BACKUP_S3_SECRET_KEY BACKUP_S3_BUCKET BACKUP_OBJECT_PATH RESTORE_DATABASE_URL)
for name in "${required[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    echo "Thiếu biến môi trường bắt buộc: ${name}" >&2
    exit 2
  fi
done

command -v mc >/dev/null || { echo "Không tìm thấy MinIO Client (mc)" >&2; exit 127; }
command -v pg_restore >/dev/null || { echo "Không tìm thấy pg_restore" >&2; exit 127; }
command -v psql >/dev/null || { echo "Không tìm thấy psql" >&2; exit 127; }

alias_name="${BACKUP_S3_ALIAS:-inventory-offsite}"
mkdir -p "${MC_CONFIG_DIR:-${HOME}/.mc}"
work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT
dump_name="$(basename "$BACKUP_OBJECT_PATH")"
checksum_name="${dump_name}.sha256"
remote_dir="${alias_name}/${BACKUP_S3_BUCKET}/$(dirname "$BACKUP_OBJECT_PATH")"

mc alias set "$alias_name" "$BACKUP_S3_ENDPOINT" \
  "$BACKUP_S3_ACCESS_KEY" "$BACKUP_S3_SECRET_KEY" --api S3v4 >/dev/null
mc cp "${alias_name}/${BACKUP_S3_BUCKET}/${BACKUP_OBJECT_PATH}" "${work_dir}/${dump_name}"
mc cp "${remote_dir}/${checksum_name}" "${work_dir}/${checksum_name}"
(cd "$work_dir" && sha256sum --check "$checksum_name")

# Từ chối restore nếu database có bảng nghiệp vụ; thao tác này không xóa bảng có sẵn.
has_user_tables="$(psql "$RESTORE_DATABASE_URL" -XAtqc \
  "SELECT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE c.relkind IN ('r','p') AND n.nspname NOT IN ('pg_catalog','information_schema') AND n.nspname NOT LIKE 'pg_toast%')")"
if [[ "$has_user_tables" == "t" ]]; then
  echo "Database đích không trống; dừng để tránh ghi đè dữ liệu" >&2
  exit 3
fi

pg_restore --exit-on-error --no-owner --no-privileges --dbname "$RESTORE_DATABASE_URL" "${work_dir}/${dump_name}"
echo "Đã restore Inventory vào database đích trống; tiếp tục kiểm tra dữ liệu và smoke test thủ công."
