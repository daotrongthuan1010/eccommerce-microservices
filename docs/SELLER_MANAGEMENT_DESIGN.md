# Thiết kế Phase 2 — Seller Management (multi-tenant marketplace)

> Nguồn: deep-research 2026-11-07 (104 agent, verify đối kháng 3-vote; nguồn chính: Stripe Connect, Spree Marketplace, Vendure, Medusa, Saleor docs). Áp vào hệ thống 16 microservices Java Spring Boot + Keycloak + Kafka + k8s hiện có, CHƯA có role SELLER. Mục đích tiếp tục: chuẩn doanh nghiệp TMĐT + CV.

## 0. Quyết định kiến trúc tổng

- **Tạo `seller-service` mới** (port 8096) thay vì sửa core product/order — đúng khuyến nghị Medusa (module riêng Vendor + VendorAdmin link sang Product/Order). Tránh phá vỡ 16 service đang chạy.
- **Shared schema + cột `seller_id`** (không schema-per-tenant): 1 node k8s học/UAT, RLS PostgreSQL là overkill; scoping làm ở tầng ứng dụng (repository query + guard service).
- **Không hard-delete seller**: lifecycle trạng thái CLOSED là soft-delete; dữ liệu order/revenue phải giữ cho báo cáo tài chính.

## 1. Domain model (seller-service)

```
Seller (sellers)
├── id (UUID), ownerId (Keycloak user id — 1 seller : 1 owner account)
├── shopName (unique), slug, description, logoFileId
├── status: PENDING_REVIEW → ACTIVE → SUSPENDED / CLOSED (+ REJECTED)
├── KYC: taxId (MST), businessLicenseNo (GPKD), idDocumentFileId, bankAccount{no, bank, holder}
├── kycStatus: NOT_SUBMITTED | PENDING | VERIFIED | REJECTED (tách khỏi status kinh doanh)
├── commissionRate (basis points, default 10%)
├── createdAt/closedAt/closedReason, version (optimistic lock)
└── Soft-delete qua status=CLOSED

SellerMember (seller_members) — optional mở rộng: nhiều nhân viên/shop
├── sellerId, userId (Keycloak), role: OWNER | STAFF
└── unique (sellerId, userId)

SellerStatus enum + state machine (tái dùng pattern OrderStatus §ADR-0001):
PENDING_REVIEW → ACTIVE | REJECTED
ACTIVE → SUSPENDED | CLOSED
SUSPENDED → ACTIVE | CLOSED
REJECTED/CLOSED = terminal
```

**Workflow transition có tên** (không set status trực tiếp — pattern Spree): mỗi transition là 1 API action riêng: `approve`, `reject`, `suspend`, `reactivate`, `close` — kèm reason, audit log, và side-effect (phát Kafka event `SellerApproved`, `SellerSuspended`... để các service khác phản ứng: ẩn sản phẩm seller bị suspend, khóa payout...).

## 2. Onboarding/KYC flow (bản địa hóa VN)

1. User (đã có account Keycloak) nộp đơn mở shop: POST `/sellers/registration` → Seller PENDING_REVIEW + KYC PENDING (upload CCCD/GPKD qua file-service sẵn có, ClamAV scan).
2. Admin duyệt: POST `/admin/sellers/{id}/approve` (hoặc reject + reason). Approve → gán role SELLER cho owner trong Keycloak (kcadm qua auth-service admin API), gán attribute `seller_id`.
3. **Tách thu thập khỏi xác minh theo ngưỡng** (mẫu Stripe Connect): seller được bán ngay khi ACTIVE, nhưng **payout khóa đến khi KYC VERIFIED**; khi doanh số vượt ngưỡng (ví dụ >50 triệu VND) mà chưa KYC → tự động flag yêu cầu bổ sung.
4. Bank account verify: Phase 2 chỉ lưu + đối chiếu thủ công khi settlement; Phase 3 mới tích API bank (BIDV/VCB open API).

## 3. Keycloak: role SELLER + per-seller authorization

- Realm role `SELLER` (cấp khi approve). JWT sẽ mang `seller_id` attribute (custom claim qua user attribute + protocol mapper).
- **Phân quyền 2 tầng** (mẫu Saleor: group + scope):
  - RBAC: role SELLER cho phép gọi `/seller/**` endpoints.
  - ABAC: mọi query scoping theo `seller_id` claim trong JWT — seller chỉ thấy sản phẩm/đơn/revenue của mình. Guard: `SecurityUtils.requireSellerId()` (mỗi request, so khớp path sellerId vs claim, 403 nếu lệch).
- ADMIN giữ nguyên toàn quyền + thấy mọi seller.

## 4. Product management cho seller

- `product-service` thêm cột `seller_id` (nullable — null = platform/hàng tự vận hành).
- Seller CRUD: tái dùng ProductController hiện có nhưng thêm branch: role SELLER → tự động ép `seller_id` = claim, chỉ được sửa sản phẩm của mình (query where seller_id = ?), không thấy sản phẩm seller khác.
- **Publishing workflow duyệt sản phẩm giữ nguyên** (draft → submit → approve/reject đã có) — thêm điều kiện: sản phẩm chỉ publish được khi seller ACTIVE; seller SUSPENDED → tự động hide toàn bộ sản phẩm (Kafka event `SellerSuspended` consumer bên product-service).

## 5. Doanh thu per-seller + settlement ledger

- **Tách seller-order**: order-service giữ aggregate order (khách mua 1 lần nhiều shop) nhưng mỗi OrderLine thêm `seller_id` (lấy từ product lúc checkout). Event `OrderPaid` payload thêm `sellerId` trên từng line (đã có lines[] trong payload — additive).
- **analytics-service mở rộng** (tận dụng Phase 1):
  - `daily_revenue` thêm cột `seller_id` → PK (stat_date, seller_id, currency).
  - `product_sales` thêm `seller_id`.
  - API mới: `GET /analytics/sellers/{sellerId}/revenue`, `GET /analytics/sellers?from&to` (xếp hạng seller theo GMV — "thống kê theo từng nhà bán hàng" bạn yêu cầu).
  - Refund event (PaymentRefunded đã có trên payment-events) → consumer mới trừ vào `refunded_revenue`.
- **Settlement ledger** (bảng mới trong seller-service hoặc service tài chính riêng — để Phase 3):
  ```
  settlement_entries: sellerId, periodStart/End, grossRevenue, commission, netPayable,
                      status: OPEN | SETTLING | PAID, payoutRef
  ```
  Commission = revenue × commissionRate per-seller. Payout: Phase 2 chỉ tính toán + export CSV; Phase 3 nối cổng thanh toán.

## 6. Admin console (FE)

- `AdminSellersPage`: danh sách seller + filter status, drawer chi tiết (KYC, doanh số, sản phẩm), actions approve/reject/suspend/reactivate/close (confirm modal + reason), nút "xóa" hiển thị nhưng thực chất = close (giữ dữ liệu).
- `SellerDashboardPage` (role SELLER): doanh số shop, sản phẩm, đơn của mình.
- Sửa `ProductsPage`: seller thấy lọc theo shop của mình.

## 7. Thứ tự triển khai (ước 3-4 tuần part-time)

| Bước | Việc | Phụ thuộc |
|---|---|---|
| 1 | seller-service: domain + state machine + admin API + Keycloak gán role/attribute | auth-service admin API có sẵn |
| 2 | Keycloak: role SELLER + protocol mapper seller_id | bước 1 |
| 3 | product-service: cột seller_id + scoping + consumer SellerSuspended | bước 1-2 |
| 4 | order-service: seller_id trên OrderLine + event OrderPaid mở rộng | bước 3 |
| 5 | analytics: schema per-seller + API ranking + refund consumer | bước 4 |
| 6 | FE: AdminSellersPage + SellerDashboard + ProductsPage scoping | bước 1-5 |
| 7 | Settlement ledger tính toán + export | bước 5 |

## 8. Trích dẫn nguồn chính

- Stripe Connect onboarding/identity: docs.stripe.com/connect/onboarding, /connect/identity-verification (KYC theo ngưỡng, khóa payout)
- Spree Marketplace: spreecommerce.org/docs/developer/how-to/build-a-marketplace (workflow transitions, first-class seller, per-seller order split)
- Vendure multi-vendor: docs.vendure.io (sellerChannelId trên OrderLine, commission per seller)
- Medusa marketplace recipe: docs.medusajs.com (Vendor + VendorAdmin module riêng, ownership scoping)
- Saleor permissions: docs.saleor.io (permission theo group + channel scope — mẫu cho Keycloak ABAC)

## 9. Cảnh báo từ verify

- Các nguồn là vendor docs mô tả nền tảng riêng, không phải benchmark độc lập — cần điều chỉnh cho Java/Keycloak/Kafka.
- Chi tiết Stripe KYC theo compliance Mỹ/EU, khác VN (CCCD/GPKD/MST) — bản địa hóa ở mục 2.
- Claim "channel-per-seller isolation của Vendure" bị refuted 0-3 vote — không lấy pattern đó.
