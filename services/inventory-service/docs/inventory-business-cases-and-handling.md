# Tổng hợp tình huống nghiệp vụ và xử lý trong Inventory Service

Tài liệu này gom các tình huống dễ phát sinh khi nhiều người dùng hoặc nhiều service cùng thao tác tồn kho. Mỗi mục phân biệt cơ chế đang có trong code với lưu ý sử dụng API, để người gọi biết khi nào được retry và khi nào phải tải dữ liệu mới.

## 1. Mô hình số lượng tồn

Một dòng `inventory` đại diện cho một tổ hợp enterprise, warehouse, location và SKU. Database có unique constraint trên tổ hợp này để ngăn tạo hai dòng tồn cho cùng vị trí/SKU.

Các bucket được theo dõi riêng:

- `physicalQuantity`: lượng vật lý tại vị trí.
- `reservedQuantity`: lượng đã giữ chỗ.
- `defectiveQuantity`: lượng hỏng, không được xem là hàng tốt.
- `pendingQuantity`: lượng chờ kiểm tra/chốt chất lượng.
- `inTransitQuantity`: lượng đang đi giữa hai vị trí.
- Tồn khả dụng được tính bằng physical trừ reserved, defective và pending. Không lấy hàng đang chuyển làm tồn có thể bán tại nguồn.

Mọi thay đổi tồn quan trọng phải đi kèm stock movement/audit trong cùng transaction. Nếu một phần ghi thất bại, balance và movement phải rollback cùng nhau.

## 2. Các case và cách xử lý

| Case | Nguy cơ | Cơ chế trong Inventory | Cách xử lý khi gọi API |
|---|---|---|---|
| Hai request cùng tạo Inventory cho một SKU/vị trí | Tạo hai dòng tồn trùng | Unique constraint theo enterprise + warehouse + location + SKU; đường tạo-or-find dựa vào DB để một dòng thắng | Nếu gặp conflict, đọc lại dòng tồn đã có; không tự tạo ID khác để lặp lại cùng nghiệp vụ |
| Hai người cùng xuất hoặc giữ hàng | Cả hai cùng đọc một số dư và xuất vượt tồn | Khóa dòng tồn `FOR UPDATE`; repository cập nhật có điều kiện theo tồn khả dụng; request không đủ số dư bị từ chối | `INSUFFICIENT_STOCK`: tải lại tồn khả dụng và điều chỉnh số lượng; không retry mù cùng payload |
| Hai người cùng cập nhật từ màn hình cũ | Ghi đè kết quả người kia | `version` trên Inventory (`@Version`); các request có `expectedVersion` được so với version hiện tại | `VERSION_CONFLICT`: GET lại Inventory, cho người dùng xem số mới và gửi thao tác mới với version mới |
| Client timeout sau khi lệnh có thể đã commit | Retry có thể cộng/trừ tồn lần hai | Một số workflow có `operationId` idempotent và snapshot kết quả; không phải mọi API balance thông thường đều có khóa idempotency | Chỉ retry nguyên payload và giữ nguyên operationId. Với endpoint không có idempotency key, trước tiên GET balance/movement hoặc tra chứng từ để xác định lần đầu đã commit chưa |
| Cùng operationId nhưng body khác | Một khóa retry bị dùng cho hai nghiệp vụ | Workflow có idempotency hash/canonical payload; payload khác bị từ chối | Không thay đổi payload khi retry. Payload cần đổi thì dùng operationId mới sau khi xác định trạng thái nghiệp vụ cũ |
| Nhập từ Product không xác thực được SKU | Tồn có thể được ghi cho SKU không tồn tại/ngừng hoạt động | Inventory dùng projection SKU còn mới; SKU thiếu/cũ thì gọi Product Service trước khi ghi. Không có instance Eureka thì trả `SERVICE_UNAVAILABLE`; lỗi phụ thuộc trả lỗi dependency | Kiểm tra Product Service/Eureka hoặc dữ liệu projection. Lỗi xảy ra ở bước xác thực trước khi gọi ghi balance; sau khi dependency hoạt động mới gửi lại |
| Location/Warehouse không tồn tại, sai kho hoặc ngừng hoạt động | Ghi tồn sai enterprise/vị trí hoặc ghi vào kho đã khóa | `InventoryLocationAccessService` đọc PostgreSQL qua JDBC, kiểm tra ownership/trạng thái và khóa SHARE theo lô trong transaction | Sửa warehouse/location/enterprise trong request hoặc kích hoạt đúng tài nguyên; không dựa vào cache Location để vượt kiểm tra |
| Giữ nhiều hơn tồn khả dụng | Bán vượt hàng sẵn sàng | `reserve` chỉ cập nhật nếu đủ available; nếu không trả `INSUFFICIENT_STOCK` | Giảm lượng giữ hoặc chọn vị trí khác |
| Bỏ giữ nhiều hơn lượng đã giữ | Làm số reserved âm hoặc sai lệch | `release` cập nhật có điều kiện; lỗi trả `INVALID_QUANTITY` | Đọc lại reserved hiện tại và chỉ bỏ phần còn giữ |
| Điều chỉnh kiểm kê thấp hơn lượng đã cam kết | Tồn physical không đủ chứa reserved/defective/pending | `adjust` chặn điều chỉnh vi phạm các bucket đã cam kết | Đối chiếu kiểm kê và xử lý reservation/quality trước khi điều chỉnh |
| Dispatch phiếu điều chuyển nhiều dòng, một SKU thiếu | Một phần phiếu bị trừ, phần còn lại không | Kiểm tra toàn bộ dòng trước; khóa các inventory theo ID ổn định; cập nhật batch; header, movement, dòng và balance commit/rollback cùng nhau | Sửa số lượng/dòng hàng hoặc bổ sung tồn rồi dispatch lại theo trạng thái/version mới |
| Nhận điều chuyển nhiều đợt hoặc retry sau timeout | Cộng destination lặp hoặc nhận vượt planned quantity | Header bị khóa; operationId + payload hash ghi nhận từng lần nhận; tiến độ dòng và balance cập nhật cùng transaction | Retry đúng operationId/body để lấy lại snapshot; đợt nhận mới dùng operationId khác và expectedVersion hiện tại |
| Parcel hàng trả được gửi nhận nhiều lần | Cộng hàng trả lặp hoặc nhận vượt duyệt | Khóa tiến độ dòng trả; định danh parcel chống trùng; unique DB và kiểm tra tổng received không vượt approved. Hàng mới nhận đi vào `pendingQuantity` | Cùng parcel và cùng body trả kết quả cũ; cùng parcel khác nội dung bị từ chối. Không dùng parcelIdentity mới để né lỗi retry |
| Kiểm tra chất lượng parcel chạy lặp hoặc hai người cùng kiểm | Chuyển pending thành good/defective hai lần | Khóa receipt; `operationId` và kết quả kiểm tra chống trùng; good + damaged phải bằng lượng parcel đã nhận; movement và đổi bucket cùng transaction | Retry cùng operationId/body; nội dung kiểm tra khác phải xử lý như thao tác mới sau khi kiểm tra trạng thái receipt |
| PostgreSQL lỗi giữa thao tác | Balance, chứng từ và audit lệch nhau | Giao dịch DB bao trùm các cập nhật liên quan; lỗi làm rollback. Outbox event được ghi cùng transaction của dữ liệu nguồn | Kiểm tra DB/log/trace trước khi retry các lệnh không idempotent |
| Kafka ngừng hoặc publisher chết sau khi broker nhận message | Event không phát hoặc phát lại | Transactional outbox; poller claim event với lease và `SKIP LOCKED`, publish sau commit, retry backoff; consumer inbox khử trùng lặp eventId/version | Theo dõi pending/age/failure. Khi downstream nhận trùng, consumer phải xử lý theo eventId; không xóa tay event khi chưa điều tra |
| Product event trùng, đến sai thứ tự hoặc payload mâu thuẫn | Projection SKU bị lùi hoặc ghi sai | Inbox unique theo consumer + eventId và hash payload; cùng ID/body là duplicate; cùng ID/body khác đi DLT; version cũ/bằng hiện tại được đánh dấu stale | Điều tra producer/DLT; chỉ replay sau khi sửa nguyên nhân và xác nhận event/version an toàn |
| Redis tạm lỗi khi đọc Location | Request không phục vụ hoặc phải dùng dữ liệu cũ | Redis là cache đọc; GET lỗi ghi metric/log rồi fallback PostgreSQL. Cache ID có TTL | Kiểm tra `inventory.redis.errors`, Redis memory/health và tải DB; không xem cache là dữ liệu chuẩn |
| Xóa cache Location lỗi sau commit | Instance khác tiếp tục thấy cache cũ | Bản ghi `cache_invalidation_outbox` được commit cùng thay đổi; worker retry theo backoff và có metric backlog/age | Theo dõi `inventory.cache.invalidation.pending`, `oldest_age_seconds`, failures và log; khôi phục Redis để worker tự xử lý |
| Danh sách Location lớn | Payload lớn, nhiều RAM/serialization, cache snapshot khó invalidation | GET danh sách phân trang; page mặc định 0, size mặc định 50, size tối đa 100; truy vấn trực tiếp PostgreSQL | Dùng `content`, `page`, `size`, `totalElements`, `totalPages` trong response. Deep pagination lớn có thể cần cursor/keyset |
| Nhiều thao tác cùng lúc khóa nhiều Inventory | Deadlock hoặc request chờ lâu | Khóa nhiều dòng theo thứ tự ID ổn định; batch update và transaction ngắn; gọi service ngoài transaction ghi khi có thể | Nếu DB deadlock/timeout, retry chỉ khi lệnh idempotent hoặc đã kiểm tra trạng thái; theo dõi Hikari wait, DB lock và trace |

## 3. Idempotence cần hiểu đúng

Idempotence có nghĩa là gửi lại cùng một lệnh do timeout không làm thay đổi tồn lần thứ hai và có thể trả lại kết quả đã lưu. Trong code hiện tại, cách này được áp dụng rõ cho các workflow chứng từ như tạo phiếu nhập/điều chuyển, dispatch/receive điều chuyển và nhận/kiểm tra parcel hàng trả.

`expectedVersion` và `operationId` giải quyết hai vấn đề khác nhau:

- `expectedVersion` phát hiện client đang ghi trên snapshot cũ; nó không tự khử trùng lặp lệnh.
- `operationId` nhận diện cùng một thao tác retry; payload hash ngăn dùng lại key cho nội dung khác.

Các thao tác đơn giản như `/inventory/receive`, `/reserve`, `/release`, `/issue`, `/adjust` có `expectedVersion` tùy theo request, nhưng không nên mặc định coi chúng idempotent nếu request DTO không có operationId và repository không lưu kết quả thao tác. Nếu mất response, hãy xác minh balance và `stock_movement` trước khi gửi lệnh lần nữa.

## 4. Ví dụ xử lý version conflict

1. Client A và B cùng đọc Inventory version 7.
2. A gửi thao tác với `expectedVersion=7`, commit thành công và version lên 8.
3. B gửi thao tác vẫn với `expectedVersion=7`; service trả `VERSION_CONFLICT` và không áp dụng thay đổi.
4. B đọc lại Inventory version 8, người dùng xác nhận thao tác còn hợp lệ rồi gửi lệnh mới với version 8.

Không tự động thay expectedVersion rồi gửi lại một lệnh xuất/điều chỉnh; quyết định đó có thể làm thay đổi kết quả người dùng đã duyệt.

## 5. Ví dụ xử lý timeout và operationId

Client gửi nhận điều chuyển với `operationId=receive-batch-001`. Server commit tồn và movement nhưng response bị mất. Client gửi lại cùng operationId, expectedVersion và nội dung dòng nhận. Service tìm operation đã lưu và trả snapshot trước đó, không cộng tồn lần nữa. Nếu cùng operationId nhưng lượng nhận khác, request bị từ chối vì payload không khớp.

## 6. Nguyên tắc vận hành

- Dùng PostgreSQL làm nguồn dữ liệu chuẩn cho balance, chứng từ, tiến độ và quyết định cho phép ghi.
- Không retry mọi HTTP 5xx/timeout một cách mù quáng. Phân biệt request có operationId idempotent với thao tác balance không có idempotency key.
- Không sửa trực tiếp balance bằng SQL vận hành thông thường; dùng API nghiệp vụ để có movement/audit/outbox đồng bộ.
- Khi điều tra sai lệch, đối chiếu balance, stock movement, chứng từ, version, operationId, outbox/inbox và traceId.
- Theo dõi dashboard Prometheus/Grafana, backlog/age, DLT, HTTP p95/p99 và Hikari connection wait. Tránh đưa SKU/user/inventory ID thành label metric vì cardinality tăng nhanh.
- Dùng backup/restore đã diễn tập; dữ liệu tồn và audit cần backup ở storage độc lập.

## 7. Nơi xem chi tiết trong repository

- `services/inventory-service/src/main/java/.../service/impl/InventoryServiceImpl.java`: nhập, giữ, bỏ giữ, xuất, điều chỉnh tồn.
- `services/inventory-service/src/main/java/.../service/impl/InventoryTransferServiceImpl.java`: tạo, dispatch và nhận điều chuyển.
- `services/inventory-service/src/main/java/.../service/impl/InventoryReturnReceiptServiceImpl.java` và `InventoryReturnInspectionServiceImpl.java`: nhận và phân loại hàng trả.
- `services/inventory-service/src/main/java/.../service/impl/SupplierReceiptServiceImpl.java`: lifecycle phiếu nhập và operationId/version.
- `services/inventory-service/docs/inventory-reliability-operations.md`: Kafka outbox/inbox, DLT, metrics, backup/restore và load test.
