# Kiểm tra scaffold

Ngày dựng: 01/10/2026.

Đã kiểm tra và đạt:

- XML của parent POM và chín module POM phân tích được; khai báo module/artifact khớp thư mục và build args.
- YAML của Compose và chín application config hợp lệ ở mức parser.
- Mọi biến Compose đều có giá trị trong `.env.example`; không trùng cổng publish.
- `depends_on` tham chiếu service tồn tại; volume init và named volume tồn tại trong cấu hình.
- Database/user, JDBC URL, driver và biến mật khẩu tương ứng đúng với MySQL/PostgreSQL được chọn.
- Mỗi project chỉ có một lớp Java bootstrap; không có code nghiệp vụ.
- `bash -n` đạt cho cả bốn shell scripts.
- Kiểm tra whitespace Git đạt.

Chưa chạy được tại môi trường dựng: `docker compose config`, image pull/build, Maven package với Java 21, khởi động container và smoke checks thực tế. Môi trường không có Docker daemon/CLI, Maven hoặc Java 21 (chỉ có Java 17); truy cập trực tiếp Maven Central và binary download cũng timeout. Các image đã pin theo phiên bản, nhưng chưa được pull xác nhận trong môi trường này.

Vì vậy đây là scaffold đã kiểm tra tĩnh, không phải xác nhận toàn bộ stack đã chạy thành công. Trên máy có Docker, thực hiện các lệnh README và `bash scripts/smoke.sh` để xác nhận kết nối thực tế. Script trả exit code khác 0 khi kết nối/health thất bại.
