# Tra cứu backend local

Bản nguồn: `9c08093`. Cleanup được triển khai và kiểm chứng trên bản sao ngoài Git trước khi đưa vào nhánh review. Repository không chứa cấu hình database production.

- [API_INDEX.md](API_INDEX.md): từ HTTP method/URL tìm controller và service đầu tiên.
- [ROLE_MATRIX.md](ROLE_MATRIX.md): quyền ở filter chain; đọc thêm guard tại service.
- [SYMBOL_INDEX.md](SYMBOL_INDEX.md): tìm `Class#method`, signature và dòng hiện tại.
- [DOMAIN_MAP.md](DOMAIN_MAP.md): class được nhóm theo nghiệp vụ.
- [MOVEMENT_MAP.md](MOVEMENT_MAP.md): vị trí cũ → vị trí thực tế sau cleanup, gồm production và test.
- [PERSISTENCE_MAP.md](PERSISTENCE_MAP.md): nơi giữ JDBC/native query và các thành phần infrastructure.
- [API_DTO_TESTS.md](API_DTO_TESTS.md): DTO và test tham chiếu theo từng API.
- [TEST_MAP.md](TEST_MAP.md): bằng chứng test theo nhóm nghiệp vụ quan trọng.
- [REHEARSAL.md](REHEARSAL.md): mười câu diễn tập tra code.
- [RUN_LOCAL.md](RUN_LOCAL.md): chạy bằng Maven wrapper, database local và khôi phục checkpoint.
- [VALIDATION.md](VALIDATION.md): kết quả gate cuối và giới hạn kiểm chứng.
- [Tài liệu lễ tân](../recep-presentation/README.md): luồng nghiệp vụ, demo, class và API đã đồng bộ.
- [CHANGES.md](CHANGES.md): phạm vi thay đổi thực tế, trách nhiệm mới và các invariant giữ nguyên.
- [PROGRESS.md](PROGRESS.md): trạng thái thực hiện và bằng chứng, không nhầm kế hoạch với kết quả.
- `api-index.json`, `symbol-index.json`, `applied-movement-manifest.json`: dữ liệu hiện tại; `movement-manifest.json` giữ kế hoạch đầu vào và caller/test trước di chuyển.

Khi thuyết trình: chức năng → URL trong index → Controller#method → Service#method → repository/entity → test. Không suy toàn bộ quyền từ URL hoặc annotation Swagger.

## Quy ước cleanup

- Nhóm theo nghiệp vụ, không nhân bản service theo role.
- Giữ tên DTO/schema, URL, JSON, role, lỗi và hành vi.
- Tab cho indent; một statement mỗi dòng; annotation riêng dòng; import tường minh.
- Một đợt chỉ format, di chuyển hoặc tách trách nhiệm. Không ghép sửa lỗi nghiệp vụ vào refactor.
- Entity và loại dùng chung có thể ở gốc domain. Không tạo thư mục shared chung cho mọi thứ.
- Khi chuyển package, cập nhật caller/test, fixture và đường dẫn source trong test; chỉ mở visibility cần thiết.
- Transaction và thứ tự lock nằm ở facade đang sở hữu. GET có lifecycle không phải query thuần đọc.

Chạy bằng Maven wrapper, Java 21. Testcontainers dùng Docker và PostgreSQL riêng; không lấy `.env` của bản gốc để chạy thử.
