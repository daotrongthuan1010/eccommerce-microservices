# Build riêng ảnh công cụ backup; luôn truyền image nền đã được tổ chức pin bằng digest khi build.
ARG POSTGRES_IMAGE=postgres:16-bookworm
ARG MINIO_MC_IMAGE=bitnamilegacy/minio:latest

FROM ${MINIO_MC_IMAGE} AS minio-client
FROM ${POSTGRES_IMAGE}

COPY --from=minio-client /opt/bitnami/minio-client/bin/mc /usr/local/bin/mc
COPY ops/backup/backup-inventory.sh /usr/local/bin/backup-inventory
COPY ops/backup/restore-inventory.sh /usr/local/bin/restore-inventory

RUN chmod 0555 /usr/local/bin/mc /usr/local/bin/backup-inventory /usr/local/bin/restore-inventory

# PostgreSQL base image chạy bằng user postgres; job không cần quyền root.
USER postgres
ENTRYPOINT ["/bin/bash", "-e"]
