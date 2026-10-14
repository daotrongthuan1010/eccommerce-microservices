# INV-01 — Kho, vị trí, vùng phục vụ, SKU và quyền thao tác

**Cách xử lý nghiệp vụ**
- Inventory sở hữu danh mục Warehouse, WarehouseLocation và ServiceArea. Warehouse có trạng thái ACTIVE/INACTIVE; location thuộc đúng warehouse và enterprise.
- Mỗi dòng số dư được định danh duy nhất bằng `(enterpriseId, warehouseId, locationId, sku)`. Chỉ nhận hàng khi warehouse/location hoạt động và SKU đã tồn tại trong danh mục Catalog.
- Khi nhận request, kiểm tra người thao tác thuộc enterprise và được cấp quyền cho warehouse đó. Không coi `ADMIN` toàn hệ thống là quyền đủ cho mọi kho.
- Inventory không gọi Catalog đồng bộ trong lúc đang giữ khóa tồn. Duy trì bản chiếu SKU hợp lệ từ sự kiện Catalog; nếu projection cũ hoặc thiếu thì từ chối/đưa vào quy trình đồng bộ, không tạo tồn cho SKU chưa được xác thực.

**Java/Spring**
- Spring Boot REST controller nhận lệnh; Bean Validation (`@Valid`, `@NotBlank`) kiểm tra cấu trúc, còn service kiểm tra trạng thái kho/SKU/quyền.
- Spring Security Resource Server kiểm tra JWT; method security (`@PreAuthorize`) kiểm tra role, service kiểm tra thêm enterpriseId/warehouseId trong claims hoặc ACL.
- Spring Data JDBC/JPA lưu Warehouse, Location và SKU projection. Khóa ngoại trong cùng database; SKU liên service được xác thực qua projection/event, không dùng foreign key xuyên service.

**Công nghệ**
- PostgreSQL: foreign key, unique constraint và index theo enterprise/warehouse/location/SKU.
- Keycloak/OIDC cho danh tính; Kafka để nhận `SkuCreated/SkuDisabled` và `WarehouseStatusChanged`.
- Redis chỉ cache dữ liệu tra cứu kho/vùng; không làm nguồn xác thực quyền hay trạng thái SKU.

# INV-02 — Nhập mua, nhập điều chuyển và nhận hàng trả

**Cách xử lý nghiệp vụ**
- Mỗi lần nhập là một chứng từ có `receiptId`, loại nguồn (PURCHASE/TRANSFER/RETURN), mã chứng từ ngoài, người nhận, thời gian và các dòng SKU/số lượng.
- `businessOperationId` duy nhất ngăn gửi lại cùng chứng từ làm tăng tồn hai lần. Kiểm tra SKU, kho, vị trí và số lượng trước khi ghi.
- Nhập mua/nhập điều chuyển được ghi vào lớp tồn phù hợp. Hàng trả không được nhập thẳng vào hàng bán: tạo ReturnReceipt ở QUARANTINED, sau đó kiểm tra từng dòng để chuyển số đạt sang SELLABLE và số hỏng sang DAMAGED.
- Cập nhật số dư, đổi trạng thái chứng từ và ghi StockLedger phải commit hoặc rollback cùng nhau.

**Java/Spring**
- `@Transactional` trên application service bao trọn việc ghi chứng từ, số dư và ledger.
- Tách command/service theo nghiệp vụ (`ReceivePurchase`, `ReceiveTransfer`, `ReceiveReturn`, `InspectReturn`) để kiểm tra điều kiện khác nhau.
- Dùng Bean Validation cho quantity dương; dùng service validation để kiểm tra giới hạn theo chứng từ đã duyệt.
- Người thao tác lấy từ `SecurityContext`/JWT, không nhận `performedBy` tự do từ request body.

**Công nghệ**
- PostgreSQL: `Receipt`, `ReceiptLine`, `ReturnInspection`, `StockLedger`; unique constraint trên `businessOperationId` và mã chứng từ phù hợp.
- Transactional Outbox lưu event trong cùng transaction với dữ liệu tồn; Debezium/Kafka Connect phát event, hoặc dùng outbox polling nếu chưa triển khai CDC.
- Kafka phát `StockReceived` và `ReturnedStockInspected`; consumer phía Order/Catalog phải xử lý event lặp an toàn.

# INV-03 — Mô hình số dư và công thức Available

**Cách xử lý nghiệp vụ**
- Tách các đại lượng: `onHandSellable`, `reserved`, `committed`, `safetyStock`, `quarantined`, `damaged`, `inTransit`. Chỉ `onHandSellable` là hàng vật lý đạt chuẩn tại vị trí.
- Tính thống nhất: `Available = onHandSellable - reserved - committed - safetyStock`. Hàng cách ly/hỏng/đang vận chuyển không được cộng vào `onHandSellable`.
- HELD làm tăng reserved; COMMIT giảm reserved và tăng committed cùng lượng; DISPATCH giảm onHandSellable và committed cùng lượng; RELEASE giảm đúng reserved hoặc committed của dòng tương ứng.
- Mọi đổi bucket ghi ledger trước/sau. Không duy trì `available` như một số dư ghi độc lập dễ lệch với các thành phần.

**Java/Spring**
- Dùng `BigDecimal` cho lượng tồn; thống nhất scale, rounding và đơn vị đo ở biên API.
- Tạo domain service duy nhất tính Available và kiểm tra invariant; DTO không tự tính bằng công thức khác.
- Dùng `@Transactional` cho chuyển bucket. Không để `@Async` chia nhỏ một chuyển đổi cần nguyên tử.

**Công nghệ**
- PostgreSQL `NUMERIC`, `CHECK` không âm, transaction và StockLedger bất biến.
- Viết SQL/view chuẩn để truy vấn Available; kiểm soát schema bằng Flyway thay vì để Hibernate tự thay đổi schema production.
- Redis/Spring Cache (`@Cacheable`, TTL) chỉ cho màn hình tra cứu. Sau commit phát invalidation/cập nhật cache; checkout luôn xác minh lại bằng PostgreSQL.

# INV-04 — Reservation nhiều SKU, TTL 15 phút

**Cách xử lý nghiệp vụ**
- Tạo Reservation header gồm `reservationId`, `orderId`, trạng thái, `expiresAt`; mỗi ReservationLine gắn SKU, inventory position, requested/held/committed/dispatched/released quantity.
- Nếu đơn cần nhiều SKU, xác định dòng phân bổ trước; khóa các dòng tồn theo thứ tự khóa ổn định để giảm deadlock.
- Trong một transaction, kiểm tra Availability và giữ tất cả dòng. Nếu bất kỳ dòng nào không đủ, rollback toàn bộ và trả mã lý do hết hàng.
- Lưu hạn hết trong PostgreSQL bằng UTC. Mặc định 15 phút; chính sách đổi TTL được cấu hình và ghi nhận.
- Cùng `reservationId` gọi lại trả kết quả đã lưu; không tạo thêm dòng giữ.

**Java/Spring**
- `@Transactional(rollbackFor = Exception.class)` ở use case reserve; không đặt transaction chỉ quanh từng SKU.
- Repository dùng SQL `UPDATE ... WHERE available >= :qty RETURNING ...` hoặc Spring Data `@Lock(PESSIMISTIC_WRITE)` để khóa các dòng; chọn một chiến lược nhất quán, và dùng SQL có điều kiện cho điểm tải cao.
- Dùng DTO command riêng và trả Reservation result có trạng thái/mã lỗi ổn định.

**Công nghệ**
- PostgreSQL: Reservation/ReservationLine, conditional update, row lock, unique `(reservationId, operationType)`.
- Redis không làm nơi giữ reservation chuẩn và không dùng distributed lock thay transaction SQL.
- Kafka chỉ phát event sau khi giữ thành công qua Outbox; việc ra quyết định giữ là đồng bộ tại Inventory.

# INV-05 — Chống oversell dưới tải cạnh tranh

**Cách xử lý nghiệp vụ**
- Điểm ghi tồn dùng PostgreSQL làm trọng tài. Đọc số khả dụng rồi cập nhật theo điều kiện trong cùng lệnh/transaction; không dùng kết quả cache để chấp nhận đơn.
- Với cùng một SKU cuối, các transaction tranh cùng dòng; một request cập nhật thành công, các request còn lại không cập nhật và nhận `INSUFFICIENT_STOCK`.
- Retry do timeout phải gửi cùng idempotency key để không tạo lần giữ mới.
- Có timeout/giới hạn retry; lỗi serialization/deadlock được retry có giới hạn và jitter, không retry lỗi nghiệp vụ hết hàng.

**Java/Spring**
- Spring `@Transactional` bao cập nhật balance và ledger.
- Với hot row, ưu tiên custom repository dùng conditional SQL/JdbcTemplate để giới hạn round-trip. `@Version` hữu ích cho optimistic update thông thường nhưng không thay điều kiện Available trong câu UPDATE.
- Ánh xạ lỗi constraint/deadlock thành lỗi nghiệp vụ riêng; retry bằng Spring Retry hoặc Resilience4j chỉ cho lỗi tạm thời đã chọn.

**Công nghệ**
- PostgreSQL row lock/conditional `UPDATE`, unique constraint và connection pool được đo tải.
- Redis cache có thể giảm tải GET nhưng Redis down/hết hạn không được làm tăng lượng giữ.
- Testcontainers PostgreSQL và bài test đồng thời 1.000 request; kiểm tra duy nhất một HELD, Available không âm, ledger khớp balance.

# INV-06 — Commit reservation, xuất kho và xuất từng kiện

**Cách xử lý nghiệp vụ**
- Commit chỉ hợp lệ nếu reservation còn HELD và chưa hết hạn. Chuyển reserved → committed; tồn vật lý không giảm ở bước thanh toán/đặt cam kết.
- Khi Shipping yêu cầu xuất, Inventory xác thực order/reservation/shipment và số lượng còn committed; dispatch mới giảm onHandSellable và committed.
- Hỗ trợ xuất một phần: lưu quantity theo ShipmentLine, phần đã xuất CONSUMED_PARTIAL, phần còn lại vẫn COMMITTED cho đến khi xuất tiếp hoặc release theo chính sách.
- Shipping không sửa StockBalance; Shipping gửi command, Inventory trả kết quả và phát StockDispatched.

**Java/Spring**
- State machine ở domain service kiểm tra chuyển trạng thái hợp lệ; cập nhật trạng thái và các bucket trong một `@Transactional` method.
- Dùng `@Version`/điều kiện trạng thái để từ chối request stale; với số lượng tranh chấp cao, kiểm tra trạng thái trong SQL update.
- `@TransactionalEventListener(AFTER_COMMIT)` chỉ dùng cho hành động nội bộ best-effort; sự kiện liên service phải qua Outbox bền vững.

**Công nghệ**
- PostgreSQL ReservationLine, Shipment/ShipmentLine, StockLedger.
- Kafka Outbox cho `StockCommitted`/`StockDispatched`; Inbox ở Shipping/Order để dedup.
- Không giảm tồn vật lý khi chỉ thanh toán thành công; việc này xảy ra tại xác nhận dispatch theo nghiệp vụ đã chọn.

# INV-07 — Release, callback lặp và idempotency

**Cách xử lý nghiệp vụ**
- Mỗi command/callback có `businessOperationId` bất biến; cùng ID và payload trả kết quả cũ, cùng ID nhưng payload khác bị từ chối để tránh dùng lại key sai.
- Release chỉ giảm phần còn HELD/COMMITTED chưa dispatch. Không release phần CONSUMED; partial dispatch thì release tối đa phần còn lại.
- Mỗi thay đổi trạng thái/bucket và ledger có operation ID. Consumer event trùng phải dừng trước khi áp dụng lần hai.
- Lưu kết quả command/idempotency đủ lâu theo thời gian retry của Order/Payment; không chỉ dựa vào Kafka offset.

**Java/Spring**
- Tạo `IdempotencyService`/repository; kiểm tra hoặc claim key trong transaction bằng insert unique, sau đó lưu response/result code.
- Kafka listener dùng Inbox table có unique `eventId`; acknowledge offset sau khi DB commit.
- Dùng `@Transactional` cho claim key, transition, bucket, ledger và outbox cùng một lần.

**Công nghệ**
- PostgreSQL unique constraint trên `business_operation_id`; Inbox/Outbox table.
- Kafka producer idempotence/transactions có ích trong Kafka boundary nhưng không thay idempotency nghiệp vụ giữa Kafka và PostgreSQL.
- Redis có thể cache kết quả idempotency để tăng tốc, nhưng PostgreSQL unique key mới là kiểm soát cuối cùng.

# INV-08 — Chọn kho và tối đa ba kiện

**Cách xử lý nghiệp vụ**
- Nhận địa chỉ giao và danh sách dòng hàng; loại kho không hoạt động hoặc ngoài vùng phục vụ.
- Tìm kho có Available cho từng SKU, tính chi phí vận chuyển, thời gian giao, mức ưu tiên và số kiện. Ràng buộc tối đa ba shipment cơ sở.
- Nếu không chấp nhận tách, chỉ chọn một kho có đủ tất cả SKU; nếu không có, báo không thể phân bổ thay vì tạo reservation thiếu.
- Bộ chọn tạo đề xuất; reservation sau đó khóa tồn và xác nhận. Nếu tranh chấp làm hụt tồn, chạy phân bổ lại.
- Lưu giải thích quyết định: kho được chọn, điểm số, chi phí, lý do loại các kho khác.

**Java/Spring**
- Tách `WarehouseAllocationService` khỏi controller; thuật toán thuần Java có input/output xác định để dễ kiểm thử.
- Bắt đầu bằng heuristic xếp hạng có quy tắc rõ; dùng Google OR-Tools Java nếu bài toán tối ưu nhiều ràng buộc thực sự cần solver.
- Dùng Spring Cache cho vùng phục vụ/chi phí tham khảo với version; không cache quyền giữ tồn.

**Công nghệ**
- PostgreSQL/PostGIS cho polygon/điểm địa lý, index GiST và lọc vùng phục vụ.
- Tích hợp hãng vận chuyển/chi phí qua adapter riêng; timeout/fallback không được bỏ qua điều kiện tồn.
- Reservation nguyên tử ở PostgreSQL là bước xác nhận cuối cùng.

# INV-09 — Kiểm kê, phê duyệt và thiếu hàng đã cam kết

**Cách xử lý nghiệp vụ**
- Mở CountSession theo vị trí/SKU, chụp snapshot version, ghi số đếm thực tế, sai lệch, lý do, người kiểm và người duyệt.
- Không cho sửa balance trực tiếp. Điều chỉnh tạo StockAdjustment và StockLedger; sai lệch vượt ngưỡng cần người thứ hai phê duyệt.
- Nếu hàng thực tế thấp hơn tổng nghĩa vụ, ghi chính xác shortage theo reservation/order line; chuyển phần không còn hàng thành AT_RISK/SHORTAGE, không giả vờ hàng vẫn có thể xuất.
- Khóa phân bổ mới cho phần thiếu; phát sự cố để chọn kho khác, mua bù, thông báo khách hoặc hủy/hoàn tiền. Chỉ đóng sự cố khi nghĩa vụ đã được giải quyết.
- Available hiển thị không âm cho lượng có thể phân bổ; shortage/commitment-at-risk hiển thị riêng, không bị cache che khuất.

**Java/Spring**
- Spring service triển khai workflow `DRAFT → COUNTED → APPROVAL_REQUIRED → POSTED`; kiểm tra expectedVersion để phát hiện snapshot cũ.
- `@Transactional` khi post adjustment, ghi ledger và đánh dấu shortage; kiểm tra quyền approver khác người kiểm với policy phù hợp.
- Dùng Spring State Machine chỉ khi workflow mở rộng nhiều nhánh; nếu không, enum + domain transition rõ ràng dễ vận hành hơn.

**Công nghệ**
- PostgreSQL CountSession, StockAdjustment, ShortageIncident, Approval và StockLedger.
- Kafka Outbox phát shortage tới Order/Payment; Saga theo dõi phân bổ lại/hủy/refund.
- Prometheus/Grafana cảnh báo Available âm, shortage chưa giải quyết, sai lệch ledger–balance và thời gian xử lý.

# INV-10 — Điều chuyển với trạng thái IN_TRANSIT

**Cách xử lý nghiệp vụ**
- Tạo Transfer header/lines có kho nguồn, kho đích, SKU, lượng dự kiến, người tạo và chứng từ.
- Dispatch: xác thực đủ hàng khả dụng tại nguồn, giảm onHandSellable nguồn và tăng lượng inTransit của transfer; ghi ledger xuất và trạng thái IN_TRANSIT.
- Hàng IN_TRANSIT không được tính khả dụng ở nguồn hoặc đích. Transfer không cộng tồn đích trước khi nhận.
- Receive: ghi số lượng thực nhận theo dòng; phần tốt giảm inTransit và tăng sellable đích; phần hỏng chuyển damaged; phần thiếu tạo discrepancy cần biên bản.
- Mỗi bước có businessOperationId riêng, transferId chung; callback nhận lặp không cộng lại.

**Java/Spring**
- `@Transactional` riêng cho dispatch và receive; không giữ một transaction DB mở suốt thời gian vận chuyển.
- State machine cho `CREATED → DISPATCHED/IN_TRANSIT → RECEIVED/PARTIALLY_RECEIVED/CLOSED`.
- Khóa hai balance theo thứ tự ID khi xử lý cùng transaction; validate cùng enterprise và SKU.

**Công nghệ**
- PostgreSQL Transfer/TransferLine/StockLedger với operation ID duy nhất.
- Kafka Outbox `TransferDispatched`, `TransferReceived`, `TransferDiscrepancy`; Inbox consumer dedup.
- PostGIS/adapter logistics tùy nhu cầu hành trình; hệ thống vận tải không được tự ghi StockBalance.

# INV-11 — Reservation hết hạn và tranh chấp commit/expire

**Cách xử lý nghiệp vụ**
- Luồng commit tự kiểm tra `status=HELD` và `expiresAt > database time`; nếu hết hạn thì không commit dù Quartz chưa chạy.
- Luồng expire chỉ chuyển `HELD` khi `expiresAt <= database time`. Commit và expire dùng conditional update cùng trạng thái gốc; bên cập nhật thành công trước là kết quả duy nhất.
- Worker quét theo lô các bản ghi đến hạn, giải phóng reserved, cập nhật reservation/ledger/outbox trong transaction.
- Payment đến sau EXPIRED không hồi sinh reservation cũ. Tạo reservation mới theo chính sách; nếu không giữ được hàng thì bắt đầu refund và liên kết payment transaction ID.

**Java/Spring**
- Dùng Quartz Spring Boot integration với JDBC JobStore clustered để chạy job bền vững trên nhiều instance; job xử lý batch nhỏ, retry có backoff.
- Dùng `@DisallowConcurrentExecution` để tránh cùng JobDetail chạy song song trên một identity; vẫn phải dùng khóa/conditional SQL vì các request commit chạy ngoài Quartz.
- Repository chạy SQL `UPDATE ... WHERE status='HELD' AND expires_at ... RETURNING`; dùng thời gian DB (`CURRENT_TIMESTAMP`/`clock_timestamp` theo semantics được chọn) làm nguồn thời gian duy nhất.
- Không dùng `@Scheduled` in-memory làm bộ hết hạn duy nhất trong môi trường nhiều instance/restart.

**Công nghệ**
- PostgreSQL index `(status, expires_at)`; worker claim theo lô với `FOR UPDATE SKIP LOCKED` hoặc conditional update.
- Quartz JDBC JobStore với clustering và cùng database được cấu hình đúng; trạng thái reservation vẫn lưu trong bảng Inventory riêng.
- Kafka Outbox/Saga cho event hết hạn, thử giữ lại và refund; không coi Kafka message timer là nguồn thời gian chuẩn.

# INV-12 — Cảnh báo sắp hết và tồn tổng hợp cho Catalog

**Cách xử lý nghiệp vụ**
- Cấu hình ngưỡng theo SKU/kho hoặc chính sách nhóm hàng. Cảnh báo khi chuyển từ trên ngưỡng xuống dưới; không phát lại ở mỗi lần giảm nhỏ.
- Dùng trạng thái cảnh báo (`NORMAL/LOW/OUT_OF_STOCK`) và chỉ phát khi trạng thái thay đổi; có cooldown nếu cần nhắc lại.
- Mỗi availability projection có `sku`, lượng tổng hợp theo phạm vi, `version` tăng đơn điệu, `updatedAt` và eventId.
- Catalog nhận event idempotent; bỏ qua version cũ. Khi nhận gap/version bất thường, yêu cầu đồng bộ snapshot.

**Java/Spring**
- Spring application service cập nhật trạng thái threshold trong cùng transaction với balance/outbox.
- Kafka listener phía Catalog dùng Inbox/version check; không dùng listener để sửa dữ liệu Inventory trực tiếp.
- `@CacheEvict`/event sau commit để xóa cache Availability. Cache miss hoặc Redis outage thì đọc PostgreSQL.

**Công nghệ**
- PostgreSQL threshold, projection version và Transactional Outbox.
- Debezium/Kafka Connect CDC là lựa chọn phát outbox; outbox polling worker là lựa chọn đơn giản hơn nếu chưa vận hành Kafka Connect.
- Kafka cho `InventoryAvailabilityChanged` và `LowStockDetected`; Prometheus/Grafana theo dõi outbox lag, consumer lag, version và số cảnh báo.
- Redis/Spring Data Redis TTL cache cho truy vấn đọc thường xuyên; thiết lập TTL, key gồm enterprise/SKU/phạm vi và invalidation theo event.

# INV-UC01 — Tranh mua SKU cuối cùng

**Cách xử lý nghiệp vụ**
- 1.000 request khác reservationId cùng giữ một đơn vị. Mọi lệnh cạnh tranh cùng cập nhật dòng balance chuẩn trong PostgreSQL.
- Chỉ lệnh có conditional update thành công được ghi HELD; các lệnh khác đọc kết quả thất bại và trả hết hàng.
- Retry cùng reservationId/operationId trả lại HELD hoặc REJECTED trước đó; không cộng reserved lần hai.
- Với đơn nhiều SKU, giữ toàn bộ dòng trong một transaction; thiếu dòng cuối thì rollback toàn bộ và không để sót reservation line.

**Java/Spring**
- `@Transactional` ở reserve command; repository custom SQL cho conditional update, không tách reserve mỗi SKU sang `@Async`.
- Unique idempotency key được claim trước khi xử lý; lưu response để retry trả cùng kết quả.
- Test bằng Testcontainers PostgreSQL và executor đồng thời; assert chỉ một HELD, reserved đúng 1, Available không âm, ledger khớp.

**Công nghệ**
- PostgreSQL row lock/conditional UPDATE là cơ chế chống oversell.
- Redis không giữ quyền sở hữu đơn vị hàng; Redis bị tắt trong bài kiểm thử vẫn phải cho kết quả nghiệp vụ đúng.
- Kafka chỉ phát `StockReserved` hoặc `ReservationRejected` qua Outbox sau khi kết quả đã commit.

# INV-UC02 — Payment đến đúng ranh giới hết hạn

**Cách xử lý nghiệp vụ**
- Commit và expire cùng kiểm tra state HELD, deadline và cập nhật có điều kiện. Không có trạng thái vừa COMMITTED vừa EXPIRED cho cùng lượng.
- Order chờ phản hồi chính thức hoặc event có reservationId/version; không tự suy luận từ timeout phía client.
- Nếu EXPIRED thắng, payment thành công đến muộn kích hoạt reservation mới. Nếu hết hàng, tạo refund; lưu quan hệ paymentId → orderId → reservationId cũ/mới.
- Callback payment lặp không tạo nhiều reservation/refund.

**Java/Spring**
- Commit API gọi Inventory đồng bộ với idempotency key; trạng thái được đổi trong `@Transactional` bằng SQL điều kiện thời gian DB.
- Spring Kafka listener cho callback/event dùng Inbox table; refund/new reservation điều phối bằng Saga state lưu bền vững.
- Quartz chỉ chạy cleanup; tính đúng hạn nằm ở service commit/expire, không phụ thuộc lịch chạy job.

**Công nghệ**
- PostgreSQL conditional transition, unique operation/payment IDs và audit timestamps.
- Kafka Outbox/Inbox cho ReservationExpired, late payment và RefundRequested; retry có backoff và dead-letter queue có giám sát.
- Quartz JDBC clustered job để dọn HELD quá hạn; kiểm thử race commit-expire với nhiều instance.

# INV-UC03 — Nhận hàng trả và kiểm tra

**Cách xử lý nghiệp vụ**
- Chỉ tạo ReturnReceipt khi ReturnRequest được duyệt; tổng nhận theo SKU không vượt lượng đã duyệt trừ lượng nhận trước đó.
- Nhận kiện tạo số dư QUARANTINED và ledger; chưa cộng Available.
- Kiểm tra từng dòng/đơn vị: đạt chuẩn chuyển QUARANTINED → SELLABLE; hỏng chuyển QUARANTINED → DAMAGED; giữ ảnh/chứng từ nếu quy trình yêu cầu.
- Inspection có operationId duy nhất. Gửi lặp cùng event trả kết quả cũ; số lượng inspected không thể vượt số lượng quarantined của receipt.
- Lưu người nhận, người kiểm, người duyệt, thời gian, lý do và liên kết order/return request.

**Java/Spring**
- Tách `ReceiveReturnService` và `InspectReturnService`; `@Transactional` cho kiểm tra giới hạn, chuyển bucket, trạng thái và ledger.
- Bean Validation kiểm tra quantity dương; SQL conditional update/unique key bảo vệ khi hai nhân viên nhận cùng receipt đồng thời.
- Spring Security kiểm tra quyền theo kho; audit subject từ JWT.

**Công nghệ**
- PostgreSQL ReturnReceipt/ReturnLine/ReturnInspection/StockLedger, unique inspection operation ID.
- Kafka Outbox phát `ReturnedStockInspected`; consumer dedup bằng eventId và liên kết returnId.
- Testcontainers kiểm tra nhận vượt lượng, hai lần nhận cùng receipt, inspection lặp và rollback khi ghi ledger lỗi.
