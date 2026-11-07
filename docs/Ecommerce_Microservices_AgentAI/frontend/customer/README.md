# Customer — danh sách màn hình

| File | Route | Mục tiêu |
|---|---|---|
| [Trang chủ mua sắm](01-trang-chu.md) | `/` | CAT-UC05, CAT-UC01 |
| [Tìm kiếm, lọc và sắp xếp](02-tim-kiem-loc.md) | `/search?q=` | CAT-UC01, CAT-UC06 |
| [Trang danh mục](03-danh-muc.md) | `/categories/:slug` | CAT-UC04, CAT-UC01 |
| [Chi tiết sản phẩm (PDP)](04-chi-tiet-san-pham.md) | `/products/:slug` | CAT-UC07, PROD-UC02, REV-UC01 |
| [Flash Sale theo khung giờ](05-flash-sale.md) | `/flash-sale` | ORD-UC06, INV-UC01 |
| [Giỏ hàng](06-gio-hang.md) | `/cart` | ORD-UC04 |
| [Rà soát và tạo đơn](07-thanh-toan.md) | `/checkout` | ORD-UC01, ORD-UC05, ORD-UC07, AUTH-UC03 |
| [Trạng thái xử lý đặt đơn](08-tien-trinh-dat-hang.md) | `/checkout/operations/:id` | ORD-UC03, PAY-UC02 |
| [Danh sách đơn hàng của tôi](09-don-hang-cua-toi.md) | `/orders` | ORD-UC01, SHIP-UC06 |
| [Chi tiết đơn và tracking](10-chi-tiet-don.md) | `/orders/:id` | ORD-UC02, SHIP-UC01, SHIP-UC06 |
| [Trả hàng và yêu cầu hoàn](11-yeu-cau-tra-hang.md) | `/orders/:id/returns/new` | ORD-UC08, PAY-UC08, INV-UC03 |
| [Tài khoản, địa chỉ, đồng ý marketing](12-tai-khoan-dia-chi.md) | `/account` | USER-UC04, USER-UC05, USER-UC07 |
| [Sản phẩm yêu thích](13-yeu-thich.md) | `/wishlist` | USER-UC06 |
| [Gian hàng công khai marketplace](14-gian-hang-cong-khai.md) | `/stores/:slug` | P2: Seller Center |
