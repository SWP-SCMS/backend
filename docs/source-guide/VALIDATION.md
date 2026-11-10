# Kiểm chứng bản cleanup local

Nguồn `9c08093505df43da557b7fa6ff91478f4bef770d`. Tổng gate cuối: **1,733 test, 0 failure/error/skip**. Không cộng các lần chạy lặp ở từng đợt vào tổng này.

| Bộ kiểm tra cuối | Test | Kết quả | Bằng chứng |
|---|---:|---|---|
| final-unit | 1067 | Đạt | [Bằng chứng local](verification/README.md#local-gates) |
| final-integration-0 | 176 | Đạt | [Bằng chứng local](verification/README.md#local-gates) |
| final-integration-1 | 119 | Đạt | [Bằng chứng local](verification/README.md#local-gates) |
| final-integration-2 | 143 | Đạt | [Bằng chứng local](verification/README.md#local-gates) |
| final-integration-3 | 228 | Đạt | [Bằng chứng local](verification/README.md#local-gates) |

Xem [bổ sung regex email trước merge](EMAIL_VALIDATION_FIX.md). Các số liệu trong bảng trên thuộc checkpoint cleanup; CI commit cuối gồm thêm ba test characterization.

## Contract và cấu trúc

- OpenAPI snapshot đầy đủ giữ 330 HTTP operation, schema, `$ref`, status và mô tả security. OpenAPI snapshot tập trung vào contract; quyền truy cập được kiểm tra riêng.
- AuthorizationSnapshotTests đối chiếu 1.992 quyết định của AuthorizationFilter thật (332 request × 6 loại danh tính). Các test security/authentication riêng kiểm tra 401/403, JWT và trạng thái tài khoản; OpenAPI test tắt filter không thay thế chúng.
- 798 method giữ annotation transaction, HTTP mapping, exception handler, scheduling, event và bean. Chỉ một GlobalExceptionHandler advice và một filter chain.
- 133 file main resources và pom.xml giữ nguyên byte; không thêm migration/dependency.
- ConstructorProof đối chiếu 382 constructor, 376 bộ annotation class và 413 field có annotation/dependency với nguồn gốc. SqlMoveProof đối chiếu 361 lời gọi SQL/mapper và thứ tự binding; SqlLiteralProof đối chiếu 570 literal SQL runtime riêng biệt.
- Danh sách gate bao phủ đủ 221 lớp unit/web và 105 lớp integration; bốn file Java còn lại trong test là tiện ích QA, không phải test case.
- HTTP thật với full Spring context và PostgreSQL Testcontainers riêng: `/api/v1/v3/api-docs` 200, `/api/v1/docs` 200, `/api/v1/reception/members` chưa đăng nhập 401. OpenAPI HTTP khớp baseline sau khi bỏ URL server theo cổng ngẫu nhiên.

## Bổ sung characterization

`CoachLeaveBatchRollbackIntegrationTests#secondSessionConflictRollsBackFirstAssignmentAndAuditInPostgres` đạt trên bản baseline tách riêng và bản refactor. Buổi thứ hai xung đột coach; coach/version/audit của buổi đầu được rollback. Thử lại riêng buổi đầu thành công. Không sửa expectation của test cũ.

## Bản gốc và khôi phục

Đã đối chiếu 1114 SHA-256 đầu vào; HEAD và working tree của backend-latest nguyên trạng. Bản làm việc và thư mục cha không có `.git`; không sao chép `.env` chứa secret.

Checkpoint đạt và tiến trình: [PROGRESS.md](PROGRESS.md). Cách chạy/khôi phục: [RUN_LOCAL.md](RUN_LOCAL.md). Bằng chứng máy đọc: [test-results.json](test-results.json).

## Giới hạn kiểm chứng

- Chưa demo frontend đang deploy và chưa kiểm tra bằng dữ liệu production. Mọi integration/HTTP smoke của cleanup dùng database riêng.
- Diễn tập tra cứu 10 câu là phép kiểm tra tự động; người thuyết trình cần tự luyện và bấm giờ theo REHEARSAL.md.
- HTTP probe ban đầu thất bại cả ở baseline vì PostgreSQL container không nhận timezone Asia/Saigon từ JVM Windows. Chạy baseline/refactor với -Duser.timezone=UTC như test hiện có; log lỗi môi trường được giữ ở evidence/http-baseline-host-timezone-failure.log. Hướng dẫn local đã bổ sung tùy chọn UTC.
- Baseline đã có cảnh báo Hikari kết nối container đóng và một số lần Surefire chờ JVM thoát. Giữ log để đối chiếu; chỉ nhận gate khi số test đạt và Maven BUILD SUCCESS/exit 0.
- Refactor giữ hành vi hiện có; đây không phải cam kết không còn bug nghiệp vụ ngoài các tình huống được kiểm chứng.
