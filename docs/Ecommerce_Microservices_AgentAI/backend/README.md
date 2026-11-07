# Backend — Java 21 + Spring Boot + Microservices

Đọc [`AGENTS.md`](../AGENTS.md), [`shared/contracts/`](../shared/README.md) và source chapter theo từng service.

## 14 thành phần từ tài liệu gốc

- Authen, User, Product, Catalog, Inventory, Order, Payment, Shipping, Review, File, Notification, Analytics, API-Gateway, Discovery.
- Keycloak là hệ thống định danh độc lập; Kafka, Redis, DB, MinIO, Elasticsearch là hạ tầng.
- **Không cần bổ sung search-service:** tìm kiếm thuộc Catalog. Giỏ, quote, voucher, Saga thuộc Order.

## Hướng dẫn kiến trúc

- [`architecture/01-ranh-gioi.md`](architecture/01-ranh-gioi.md)
- [`architecture/02-dong-bo-bat-dong-bo.md`](architecture/02-dong-bo-bat-dong-bo.md)
- [`architecture/03-outbox-saga-idempotency.md`](architecture/03-outbox-saga-idempotency.md)
- [`architecture/04-data-va-hieu-nang.md`](architecture/04-data-va-hieu-nang.md)
- [`architecture/05-trien-khai-dev-production.md`](architecture/05-trien-khai-dev-production.md)
- [`architecture/06-observability-va-debug.md`](architecture/06-observability-va-debug.md)

## Mẫu cấu trúc service [ĐỀ XUẤT]

```text
service-name/
  src/main/java/.../
    api/              REST controllers, DTO, API exception
    application/      use cases, command handlers, orchestration
    domain/           entities, value objects, policies, events
    infrastructure/   JPA, Kafka, REST/gRPC clients, Redis
    config/           security, serialization, tracing
  src/test/...         unit, integration, contract, concurrency
  src/main/resources/db/migration/    Flyway/Liquibase migrations
```

Không tạo dự án trống cho cả 14 service trước khi có vertical slice có thể chạy và kiểm thử.
