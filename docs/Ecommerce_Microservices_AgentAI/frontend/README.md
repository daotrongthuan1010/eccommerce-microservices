# Frontend — React + TypeScript (.tsx) + Redux + Ant Design

## Apps/giao diện đề xuất

- `customer-web`: trải nghiệm mua hàng, mobile-first, SEO-friendly nếu có SSR; có thể bắt đầu Vite React SPA ở DEV.
- `seller-portal`: Seller Center, chỉ triển khai trong giai đoạn marketplace P2+.
- `admin-portal`: quản lý nội bộ retail và quản trị platform (mở dần P2/P3).
- `ops-console`: màn hình theo dõi thao tác kẹt, log/audit, finance, kho; có thể là route trong `admin-portal` khi MVP.

## Tổ chức mã đề xuất

```text
apps/
  customer-web/src/features/{home,catalog,product,cart,checkout,orders,account}/
  seller-portal/src/features/{onboarding,listings,fulfillment,marketing,finance}/
  admin-portal/src/features/{products,categories,inventory,payments,shipping,reviews,ops}/
packages/
  api-client/            Kiểu TypeScript sinh từ OpenAPI, wrappers/query hooks
  ui/                    Component dùng chung, design tokens, form fields
  auth/                  OIDC PKCE, permission helpers, session guards
  shared-types/          DTO types từ contract, money/date/format utilities
```

## Nguyên tắc FE

- Mọi component React viết bằng `.tsx`; state qua Redux Toolkit/RTK Query hoặc adapter query được chốt, tránh hai nguồn state mạng.
- Đăng nhập qua Keycloak OIDC PKCE; FE public client không có client secret. Không lưu password/tokens vào localStorage nếu kiến trúc xác thực có giải pháp an toàn hơn; nếu SPA dùng token phải quản lý theo threat model.
- Chỉ BE quyết định giá, tồn, quyền, trạng thái. Không dùng giá trên card sản phẩm để gửi giá thanh toán.
- Các hành động quan trọng phải có trạng thái `loading/processing/unknown/error/success`, nút retry cùng operationId và thông báo tiếng Việt dễ hiểu.
- Form dùng Ant Design khi hợp lý, nhưng không mặc định layout admin cho trang khách. Responsive, accessible và không bê nguyên giao diện Shopee/Tiki/Lazada.

## Tài liệu FE

- [design/01-design-system.md](design/01-design-system.md)
- [design/02-routing-va-navigation.md](design/02-routing-va-navigation.md)
- [design/03-state-query-auth.md](design/03-state-query-auth.md)
- [design/04-ui-states-va-cach-viet-loi.md](design/04-ui-states-va-cach-viet-loi.md)
- [design/05-seo-hieu-nang-a11y.md](design/05-seo-hieu-nang-a11y.md)
- [customer/](customer/), [seller/](seller/), [admin/](admin/), [ops/](ops/).

**Nguồn nền:** [chương frontend gốc](../reference/chapters/27-frontend-reactjs-redux-va-ant-design.md) và các use case theo service trong `reference/chapters`.
