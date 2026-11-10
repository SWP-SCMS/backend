# Những gì đã thay đổi trong bản local

Nguồn đối chiếu: `backend-latest` tại `9c08093505df43da557b7fa6ff91478f4bef770d`. Kết quả gate và trạng thái nhận bản nằm tại [VALIDATION.md](VALIDATION.md) và [PROGRESS.md](PROGRESS.md).

## Tìm code theo nghiệp vụ

Production và test của scheduling, training, membership, course và emailverification được nhóm theo nghiệp vụ. Controller, service, DTO và repository gần nhau; entity/enum/exception dùng chung có thể ở gốc domain. Các domain nhỏ và identity giữ cấu trúc quen thuộc. [DOMAIN_MAP.md](DOMAIN_MAP.md) là cấu trúc thực tế, [MOVEMENT_MAP.md](MOVEMENT_MAP.md) ghi vị trí cũ/mới và caller/test tham chiếu.

Giữ tên type/DTO và schema. Những top-level DTO cần truy cập xuyên package được tách thành file riêng; nested DTO tiếp tục nằm trong type sở hữu. Chỉ mở visibility khi caller xuyên package cần dùng. Không đổi tên bean, bảng/entity, URL, JSON hoặc constructor dependency bắt buộc/tùy chọn.

## Service giữ điều phối, component giữ trách nhiệm hẹp

| Facade | Phần được tách |
|---|---|
| ClassSessionService | ClassSessionCatalog, ClassSessionResources |
| CourseManagementService | CourseManagementSnapshots, CourseAdministration, CourseRunAdministration |
| RoomSuspensionService | RoomAvailabilityAnalysis, RoomImpactPolicy |
| PaymentFulfillmentService | PaymentProviderEventRepository, PaymentFulfillmentRepository, MembershipPaymentFulfillment, CoursePaymentFulfillment |
| Withdrawal | Repository và snapshot |
| Transfer | Repository và completion |
| Refund obligation | Repository và validation |
| Schedule change | Repository và notification |

Entrypoint và transaction owner vẫn ở facade. Giữ thứ tự lock/cập nhật trạng thái, `noRollbackFor` của transfer và callback sau commit của withdrawal. Catalog có lifecycle ghi dữ liệu không được đổi thành `readOnly`. Dependency tùy chọn của payment được truyền giá trị hiện tại vào helper ở từng lần gọi. Dùng lại cancellation/policy/writer có sẵn.

## Persistence rõ vị trí

SQL và row mapping chuyển vào repository nghiệp vụ, sử dụng lại repository có sẵn khi phù hợp. Không đổi SQL, binding, `FOR UPDATE`, advisory lock, exception translation hoặc chuyển JPA sang JDBC. Không thêm index/migration/dependency.

Đối chiếu riêng 361 lời gọi SQL được di chuyển; inventory cuối phân giải 454 lời gọi JDBC. Các thành phần infrastructure có sẵn giữ truy xuất dữ liệu của chúng, không thêm wrapper. Xem [PERSISTENCE_MAP.md](PERSISTENCE_MAP.md) để biết từng chủ sở hữu và các ngoại lệ có lý do.

## Security và lỗi

Một filter chain, 135 rule matcher được chia thành bảy helper theo đúng thứ tự liên tiếp cũ. Fallback, JWT, CORS và phản hồi 401/403 giữ nguyên. Không gom role làm thay đổi ưu tiên matcher.

GlobalExceptionHandler vẫn là advice duy nhất. Annotation xử lý lỗi ở advice; 63 phần dựng phản hồi được chuyển sang bảy helper theo domain, dùng lại ProblemDetails. Các handler framework, response advice và lỗi bất ngờ vẫn do advice hiện có xử lý.

## Kiểm chứng và tài liệu

Thêm hai snapshot test cho OpenAPI và quyết định authorization; thêm một characterization test cho rollback toàn bộ coach leave batch khi mục thứ hai lỗi. Test rollback được chạy cả trên baseline riêng và bản refactor. Không sửa expectation cũ để chấp nhận regression.

Chỉ mục cuối bao phủ 331 mapping, 330 HTTP operation, 847 type kể cả nested; 984 ánh xạ type đầu vào, gồm production/test. Bản đồ tham chiếu dùng để tra cứu, không được hiểu là call graph đầy đủ do compiler dựng.

Bộ tài liệu lễ tân hiện có được đồng bộ trên bản sao local: API, class/method/dòng, demo và câu hỏi. Đã bổ sung coach leave batch, `expectedVersion`, training summary, chặn hủy buổi có hội viên và V77. V77 là migration có sẵn từ nguồn, không phải migration tạo mới trong cleanup.

## Dùng bản nào

Chỉ dùng bản có gate đạt trong [PROGRESS.md](PROGRESS.md). Checkpoint nằm ngoài thư mục làm việc; cách khôi phục vào thư mục mới nằm trong [RUN_LOCAL.md](RUN_LOCAL.md). Bản gốc và Git giữ nguyên, không deploy bản refactor lên web.

Frontend, ngân hàng/email/storage thật và buổi diễn tập của người thuyết trình vẫn cần kiểm tra riêng. Kiểm chứng tự động không khẳng định hệ thống không còn lỗi nghiệp vụ ngoài phạm vi test.
