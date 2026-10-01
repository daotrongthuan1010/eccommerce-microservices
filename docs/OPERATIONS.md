# Vận hành local

## Kiểm tra trực tiếp hạ tầng

Các biến môi trường trong các lệnh dưới được mở rộng **bên trong container**.

```bash
docker compose exec -T postgres psql -U postgres -c '\l'
docker compose exec -T mysql sh -ec 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -e "SHOW DATABASES"'
docker compose exec -T redis sh -ec 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli ping'
docker compose exec -T kafka kafka-topics --bootstrap-server kafka:29092 --list
curl --fail http://localhost:9200/_cluster/health
curl --fail http://localhost:9000/minio/health/live
docker compose logs minio-init
```

`kafka-topics --list` trả danh sách rỗng lúc đầu là bình thường. Tạo topic khi đã có thiết kế sự kiện, ví dụ:

```bash
docker compose exec -T kafka kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists --topic ecommerce.order.events --partitions 3 --replication-factor 1
```

Đây chỉ là lệnh tham khảo, scaffold không tự tạo topic này. Mở http://localhost:9001 bằng tài khoản `.env` để xem bucket `product-assets`; bucket không được bật public access.

## Mật khẩu và volume

Init scripts của MySQL/PostgreSQL chỉ chạy khi thư mục dữ liệu trống. Đổi `.env` không đổi mật khẩu của user đã lưu trong DB. Với dữ liệu cần giữ, dùng `ALTER USER`/`ALTER ROLE` qua công cụ quản trị rồi cập nhật `.env` và recreate các ứng dụng.

Nếu đây là môi trường local bỏ được dữ liệu, bạn có thể chủ động xóa volume và chạy lại. **Lệnh sau xoá toàn bộ dữ liệu MySQL, PostgreSQL, Kafka, Redis, MinIO và Elasticsearch trong stack này:**

```bash
docker compose --profile apps down -v
docker compose --profile apps up -d --build --wait --wait-timeout 600
```

Không dùng lệnh này với dữ liệu cần lưu. Init lỗi giữa chừng có thể để lại volume DB chưa hoàn chỉnh; đọc log trước khi xử lý.

## Lỗi thường gặp

| Triệu chứng | Kiểm tra/xử lý |
|---|---|
| Port already allocated | Có DB/container khác đang dùng 3306, 5432, 6379, 8080…; đổi cổng publish bên trái trong Compose và URL host trong Run Configuration |
| Access denied / password authentication failed | `.env` không khớp dữ liệu trong volume; kiểm tra user của đúng DB; cập nhật mật khẩu có kiểm soát |
| Unknown database | Init script thất bại hoặc volume đã tồn tại từ cấu hình khác; xem log MySQL/PostgreSQL |
| Kafka timeout trong container | Dùng `kafka:29092`, không dùng `localhost:9092` |
| Gateway trả 404 | Chưa có controller; đây là trạng thái dự kiến |
| Elasticsearch exit 78 / vm.max_map_count | Đọc bootstrap log. Trên Linux/Docker VM, cấu hình host theo yêu cầu Elasticsearch nếu log báo thiếu; ví dụ `sudo sysctl -w vm.max_map_count=262144` |
| Exit 137 / OOM | Tăng RAM Docker; chạy riêng infra hoặc vài project khi phát triển |
| Docker build không tải được Maven dependency | Kiểm tra DNS, proxy và quyền truy cập Maven Central từ Docker |
| MinIO init exit khác 0 | Xem `docker compose logs minio-init`; kiểm tra credentials và MinIO health, sau đó chạy lại `docker compose up minio-init` |

## Giới hạn cấu hình

Stack dành cho local: một node Kafka KRaft, một node Elasticsearch không TLS/auth, Kafka PLAINTEXT, DB không TLS. Không triển khai nguyên cấu hình này lên production. Mỗi SQL service có quyền trong DB riêng, không quyền toàn instance. Redis/Kafka/Elasticsearch đang dùng chung instance và chưa có ACL riêng theo service. Catalog đang dùng MinIO root credentials cho cấu hình local; khi triển khai cần tài khoản/bucket policy riêng.

Chưa có outbox, retry nghiệp vụ, circuit breaker, tracing, migration, search indexing, login, gọi thanh toán hay gửi thông báo. Các kết nối/dependency không tạo sẵn nghiệp vụ. MinIO endpoint/access-key/secret-key/bucket được khai báo để bổ sung SDK khi bắt đầu viết catalog.

## Git

Repository đã đặt danh tính commit: `dtthuan3 <thuanptit1010.work@gmail.com>`. Đây là metadata tác giả, không phải thông tin đăng nhập GitHub. Remote giữ nguyên URL được cung cấp. Không lưu token trong repository.

Trong môi trường dựng, push không thành công do chưa có xác thực GitHub. Nếu dùng bản ZIP, ZIP không chứa `.git`; vào thư mục giải nén và khởi tạo lại Git:

```bash
git init -b main
git config user.name dtthuan3
git config user.email thuanptit1010.work@gmail.com
git add .
git commit -m "chore: scaffold ecommerce services and local Docker infrastructure"
git remote add origin https://github.com/daotrongthuan1010/eccommerce-microservices.git
git push -u origin main
```

Các lệnh này áp dụng cho repository đang trống tại thời điểm dựng. Đăng nhập GitHub qua credential manager/GitHub CLI trên máy bạn và bảo đảm tài khoản có quyền ghi. Không force-push nếu remote đã có commit mới.
