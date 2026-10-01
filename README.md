# Ecommerce microservices — infrastructure & project scaffold

Khung dự án cho với công nghệ Java 21, Maven multi-module, Spring Boot 3.5.6, Spring Cloud 2025.0.0. Không có controller nghiệp vụ, entity, repository, migration bảng, Kafka producer/consumer hay xử lý giao dịch. Mỗi service chỉ có lớp `Application` để chạy Spring Boot và file cấu hình. Gateway có route cấu hình; các URL nghiệp vụ trả 404 cho đến khi bạn triển khai API.

## Phân chia service và dữ liệu

| Project | Cổng | Hạ tầng dữ liệu | Database/user |
|---|---:|---|---|
| api-gateway | 8080 | Gateway WebFlux; không DB | — |
| auth-service | 8081 | MySQL | auth_db / auth_user |
| user-service | 8082 | MySQL | user_db / user_user |
| catalog-service | 8083 | PostgreSQL, MinIO | catalog_db / catalog_user |
| inventory-service | 8084 | PostgreSQL | inventory_db / inventory_user |
| order-service | 8085 | PostgreSQL | order_db / order_user |
| payment-service | 8086 | MySQL | payment_db / payment_user |
| notification-service | 8087 | PostgreSQL | notification_db / notification_user |
| search-service | 8088 | Elasticsearch | Chưa tạo index/mapping |

Tất cả service backend đã có dependency và cấu hình Kafka/Redis. Không dùng DB chung giữa các service; các DB riêng dùng chung instance MySQL/PostgreSQL để tiện phát triển. Search dùng Elasticsearch làm kho tìm kiếm, không thay thế dữ liệu giao dịch của catalog/order. Chưa có đồng bộ catalog → Elasticsearch.

## Chạy với Docker Compose

Cài Docker Engine/Docker Desktop với **Compose v2** và BuildKit. Docker build cung cấp Java 21/Maven nên không cần cài Java trên máy nếu chạy toàn bộ bằng Docker. Khuyến nghị dành 8–12 GB RAM cho Docker khi chạy đủ stack.

Linux/macOS/Git Bash:

```bash
cp .env.example .env
docker compose config --quiet
# Chỉ chạy hạ tầng; minio-init tự tạo bucket product-assets
docker compose up -d --wait
# Chạy cả 9 project (lần đầu cần tải dependency và build)
docker compose --profile apps up -d --build --wait --wait-timeout 600
docker compose --profile apps ps
```

PowerShell trên Windows:

```powershell
Copy-Item .env.example .env
docker compose config --quiet
docker compose --profile apps up -d --build --wait --wait-timeout 600
docker compose --profile apps ps
```

`.env.example` có mật khẩu **mẫu dành riêng cho local**. Có thể đổi trước lần chạy đầu; `.env` không được commit. Nếu mật khẩu chứa `$`, dùng giá trị được đặt trong dấu nháy đơn theo cú pháp dotenv của Compose. Các cổng publish chỉ bind `127.0.0.1`.

Kiểm tra:

```bash
curl --fail http://localhost:8080/actuator/health
curl --fail http://localhost:8083/actuator/health
bash scripts/smoke.sh
docker compose logs --tail=100 catalog-service
```

PowerShell: `Invoke-RestMethod http://localhost:8083/actuator/health`. JDBC health của các service SQL kiểm tra kết nối DB; search health kiểm tra Elasticsearch và Redis. Không có Kafka/MinIO health indicator ở mức ứng dụng vì chưa viết client; dùng các lệnh hạ tầng trong `docs/OPERATIONS.md`. MinIO hiện chỉ có cấu hình `storage.minio.*`, chưa có SDK/bean upload/download.

## Địa chỉ kết nối

| Hạ tầng | Từ máy host/IDE | Từ container |
|---|---|---|
| PostgreSQL | localhost:5432 | postgres:5432 |
| MySQL | localhost:3306 | mysql:3306 |
| Redis | localhost:6379 | redis:6379 |
| Kafka | localhost:9092 | kafka:29092 |
| Elasticsearch | http://localhost:9200 | http://elasticsearch:9200 |
| MinIO API | http://localhost:9000 | http://minio:9000 |
| MinIO Console | http://localhost:9001 | minio:9001 |

Hai listener Kafka giúp kết nối từ host và container đều đúng. Không dùng `localhost:9092` trong container. Với cấu hình hiện tại các cổng host chỉ dùng local; `KAFKA_EXTERNAL_HOST` mặc định là `localhost`.

## Chạy project trong IntelliJ

1. Mở `pom.xml` ở root dưới dạng Maven project, chọn JDK 21.
2. Chạy hạ tầng: `docker compose up -d --wait`.
3. Chạy lớp `*Application` của service cần làm. `application.yml` mặc định trỏ tới localhost và mật khẩu mẫu tương ứng `.env.example`.
4. Nếu đổi `.env`, truyền các biến `DB_PASSWORD`, `REDIS_PASSWORD` và, với catalog, `MINIO_ACCESS_KEY`/`MINIO_SECRET_KEY` vào Run Configuration. Spring Boot **không tự đọc** file `.env` của Compose.
5. Không chạy cùng service cả trong Docker lẫn IDE trên cùng cổng. Có thể dùng `SERVER_PORT` để đổi cổng và cập nhật URL route ở gateway.

Build ngoài Docker cần Java 21 và Maven 3.9+:

```bash
mvn -B package -DskipTests
mvn -pl services/order-service -am package -DskipTests
java -jar services/order-service/target/order-service-0.0.1-SNAPSHOT.jar
```

Build chỉ module `order-service` với `-am` cũng build parent. Không chạy `spring-boot:run` với `-am` ở root vì parent là project `pom`.

## Gateway

Route ví dụ `/api/catalog/**` → catalog-service, `/api/order/**` → order-service. `StripPrefix=2` loại `/api/catalog`, `/api/order` trước khi chuyển tiếp. Chưa có API đích nên 404 là bình thường; gateway chưa có cơ chế xác thực/phân quyền. `auth-service` cũng chỉ là project rỗng, chưa có đăng nhập/JWT.

## Cấu trúc

- `docker-compose.yml`: sáu nền tảng hạ tầng + job MinIO init + chín project.
- `Dockerfile`: build một Maven module bằng `SERVICE`, chạy JRE 21 bằng user không root.
- `services/*/pom.xml`: dependency theo nhu cầu từng project.
- `services/*/src/main/resources/application.yml`: kết nối host mặc định, Docker override bằng biến môi trường.
- `infra/postgres`, `infra/mysql`: tạo database/user tự động khi volume còn trống.
- `scripts/smoke.sh`: kiểm tra hạ tầng và health các project sau khi chạy stack đầy đủ.
- `docs/OPERATIONS.md`: vận hành, xử lý lỗi và giới hạn.
- `docs/VALIDATION.md`: kết quả kiểm tra trong môi trường dựng scaffold.

Hạ tầng dùng volume riêng để lưu dữ liệu. Không tự tạo bảng nghiệp vụ. Kafka tắt tự tạo topic; chưa có topic/message để tránh đặt trước contract nghiệp vụ.

## Dừng

```bash
# Dừng và giữ dữ liệu
docker compose --profile apps down
```

Không thêm `-v` trừ khi bạn chủ động muốn xoá dữ liệu của stack. Hướng dẫn tái khởi tạo DB sau khi đổi mật khẩu nằm trong `docs/OPERATIONS.md`.
