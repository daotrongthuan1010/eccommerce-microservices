#!/usr/bin/env bash
set -Eeuo pipefail

# Sao lưu riêng inventory_db ra object storage S3-compatible ngoài cụm ứng dụng.
# Credential chỉ được truyền qua secret/env; script không in giá trị nhạy cảm ra log.
required=(PGHOST PGPORT PGDATABASE PGUSER PGPASSWORD BACKUP_S3_ENDPOINT BACKUP_S3_ACCESS_KEY BACKUP_S3_SECRET_KEY BACKUP_S3_BUCKET)
for name in "${required[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    echo "Thiếu biến môi trường bắt buộc: ${name}" >&2
    exit 2
  fi
done

command -v pg_dump >/dev/null || { echo "Không tìm thấy pg_dump" >&2; exit 127; }
command -v mc >/dev/null || { echo "Không tìm thấy MinIO Client (mc)" >&2; exit 127; }

backup_id="$(date -u +%Y%m%dT%H%M%S%NZ)"
work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT
dump_name="inventory-${PGDATABASE}-${backup_id}.dump"
dump_path="${work_dir}/${dump_name}"
checksum_name="${dump_name}.sha256"

# Dùng custom format để hỗ trợ restore có chọn lọc và xác minh checksum trước khi nạp.
pg_dump --format=custom --no-owner --no-privileges --file "$dump_path"
(cd "$work_dir" && sha256sum "$dump_name" > "$checksum_name")

# Tên alias và prefix có thể cấu hình để lưu vào tài khoản/region backup độc lập.
alias_name="${BACKUP_S3_ALIAS:-inventory-offsite}"
prefix="${BACKUP_S3_PREFIX:-inventory/postgres}"
mkdir -p "${MC_CONFIG_DIR:-${HOME}/.mc}"
mc alias set "$alias_name" "$BACKUP_S3_ENDPOINT" \
  "$BACKUP_S3_ACCESS_KEY" "$BACKUP_S3_SECRET_KEY" --api S3v4 >/dev/null

remote_path="${alias_name}/${BACKUP_S3_BUCKET}/${prefix}/${backup_id}"
mc cp "$dump_path" "$remote_path/${dump_name}"
mc cp "${work_dir}/${checksum_name}" "$remote_path/${checksum_name}"
mc stat "$remote_path/${dump_name}" >/dev/null
mc stat "$remote_path/${checksum_name}" >/dev/null

echo "Đã upload backup Inventory ${backup_id}; dung lượng $(du -h "$dump_path" | awk '{print $1}')"
