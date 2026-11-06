# Hoàn tiền theo phân bổ từng buổi ban đầu — 2026-11-05

## Quy tắc

- Giữ `course_enrollment_sessions.allocation_vnd` đã chốt lúc thanh toán; không lấy tiền gói chia lại cho số buổi sau khi giảm.
- Gói 1.000.000đ / 10 buổi: giảm còn 8 tạo khoản phải hoàn 200.000đ. Cung cấp xong 1 buổi, hủy đợt: tạo thêm 700.000đ. Tổng 900.000đ.
- Buổi đã diễn ra bình thường vẫn tính đã cung cấp dù học viên vắng mặt. Buổi hủy chưa được học bù vẫn thuộc phần chưa cung cấp. Nếu đã học bù, dùng trạng thái buổi bù để xác định.
- Giảm nhiều lần tạo nghĩa vụ hoàn riêng theo từng sự kiện. Mỗi đơn vị buổi học chỉ được gắn với một nghĩa vụ hoàn do trung tâm; thao tác lại không cộng trùng.
- Khoản giảm buổi được ghi nhận ngay, nhưng chỉ chi khi đợt COMPLETED hoặc CANCELLED, vẫn qua các bước duyệt/chi/xác minh hiện có.
- Tiền lẻ dùng nguyên phân bổ đã lưu cho từng buổi, gồm phần dư làm tròn từ lúc mua.

## Thay đổi

- `CourseUnitRefunds`: chọn buổi chưa cung cấp, cộng phân bổ gốc; ghi nghĩa vụ và claim trong cùng giao dịch.
- `CourseRunRescheduleService`, `CourseManagementService`: tạo khoản hoàn khi giảm số buổi bằng cả hai API hiện có.
- `CourseRunClosureService`: hủy đợt chỉ hoàn các buổi chưa cung cấp, thay cho hoàn toàn bộ tiền gói.
- `CourseSessionDisruptionService`: quyết toán buổi chưa xử lý dùng cùng cơ chế chống trùng.
- `CourseWithdrawalService`, `CourseScheduleChangeService`, `CourseTransferService`: loại buổi đã ghi nhận hoàn khỏi phần tiền còn lại khi lập yêu cầu mới. Yêu cầu rút đang chờ trước khi giảm buổi được hủy để người dùng gửi lại theo số tiền mới.
- `CourseRefundObligationService`: cho phép chi khoản giảm buổi khi đợt bị hủy.
- `CourseRefundObligationWriter` và các chỗ insert: khóa nguồn hoàn bao gồm `source_event_id`; truy vấn khoản không có sự kiện phải lọc `source_event_id is null`.
- Migration V75 thêm nguồn REDUCED_SESSION_COUNT, định danh sự kiện giảm buổi và bảng claim có khóa duy nhất theo buổi đăng ký. Không sửa tiền của các nghĩa vụ lịch sử đã duyệt/chi.
- FE: nhãn nguồn hoàn tiếng Việt và lý do yêu cầu rút bị hủy do giảm buổi.

## Kiểm thử và giới hạn xác nhận

### Test lại Chrome MCP với BE thật — PASS ca chính

- FE `http://localhost:5175`, API `http://localhost:8080/api/v1`. Gọi API ứng dụng qua phiên Quản lý trả 200. Lần kiểm tra trước gọi `/actuator/health` ngoài context `/api/v1` rồi kết luận BE không phản hồi là chưa đủ căn cứ; không dùng lỗi đó làm bằng chứng BE tắt.
- Tiến trình BE cũ chưa nạp bản sửa. Đã khởi động lại đúng profile local, Flyway áp dụng V75 thành công lên PostgreSQL 17.6. Log `target/refund-chrome-local.log`.
- Đợt QA: `QA-REFUND-CHROME-1793851556432`, ID `354ea080-48f4-4371-8dd9-fc1f3060bc32`; hội viên riêng `MB-100068`, enrollment `ef1d9b4f-add0-45b0-88ff-9a59c9f81063`.
- Chrome gọi API thật để tạo đủ 10 buổi, mở đăng ký, tạo đơn và thanh toán tiền mặt mô phỏng 1.000.000đ. Không phát sinh giao dịch tiền thật.
- Giảm 10→8: tạo đúng một khoản REDUCED_SESSION_COUNT 200.000đ. Replay cùng idempotency key vẫn chỉ một khoản. Giao diện hiển thị buổi 9–10 “Đã bỏ do giảm số buổi”, phân bổ vẫn 100.000đ/buổi.
- Duyệt khoản giảm buổi rồi thử chi trước khi kết thúc: HTTP 409 COURSE_REFUND_DISBURSEMENT_NOT_ELIGIBLE, đúng quy tắc.
- Để không chờ đến ngày diễn ra, chỉ dữ liệu QA được đặt buổi 1 thành COMPLETED và đợt thành IN_PROGRESS qua script fixture. Đây là mô phỏng trạng thái đã học, không phải test scheduler/điểm danh theo thời gian thực.
- Hủy đợt bằng nút giao diện và nhập lý do: thành công, tạo thêm COURSE_RUN_CANCELLED 700.000đ. Màn Hoàn tiền khóa học hiển thị đúng hai dòng 200.000đ + 700.000đ.
- Duyệt, ghi nhận chi tiền mặt giả lập và xác minh qua API thật trong Chrome: cả hai khoản COMPLETED, verifiedAmountVnd lần lượt 200.000 và 700.000.
- Đối chiếu DB read-only: toàn bộ 10 phân bổ gốc vẫn 100.000đ; bảng claim có đúng 9 buổi, tổng 900.000đ. Buổi 1 giữ trạng thái đã hoàn thành; buổi 2–8 bị hủy; 9–10 giữ lịch sử giảm buổi.
- Đã đọc trực tiếp giao diện Chrome thấy đúng hai dòng tiền hoàn trước bước xác minh. Không lưu được snapshot bằng Chrome tool vì đường dẫn repo nằm ngoài workspace được tool cho phép. Cuối lượt phiên Quản lý về trang đăng nhập; sau khi đăng nhập lại, mở `/manager/course-refunds`, tìm mã đợt QA trên để xem lại. Trạng thái COMPLETED được xác nhận bằng phản hồi API, không phải snapshot UI sau cùng.
- Trong lượt retest này không phát hiện lỗi tính tiền ở ca chính, không phải sửa thêm logic. Một request thử ban đầu dùng nhầm `id` thay vì `orderId` trả 400; đã sửa script test và thanh toán thành công, không phải lỗi ứng dụng.
- Giới hạn: ca giảm nhiều lần và các integration suite Docker bên dưới chưa được chạy lại trong lượt Chrome này; không suy rộng PASS ca chính thành PASS mọi nhánh.

### Kết quả tự động và trạng thái trước lượt retest

- PASS: biên dịch Java 21, gồm mã production và test.
- PASS: 11 test trong CourseUnitRefundsTests, CourseWithdrawalPolicyTests, CourseTransferPolicyTests, CourseRefundStateMachineTests. Log: `target/refund-policy-verified.log`.
- PASS: 9 test FE `node --test src/utils/courseWorkflow.test.js`.
- PASS: `git diff --check`.
- Đã thêm 2 integration test vào CourseScheduleChangeIntegrationTests: 10→8, học 1 rồi hủy = 900.000đ; giảm 10→9→8 rồi quyết toán không tạo trùng. Đã sửa fixture CourseRunClosureIntegrationTests để có bản ghi phân bổ buổi giống thanh toán thật.
- CHƯA PASS integration Testcontainers: không tìm thấy Docker; lỗi trước khi thực thi test. Migration V75 đã xác nhận trên DB thật trong lượt Chrome ở trên. Log lỗi Testcontainers: `target/refund-direct.log`.
- CHƯA PASS nhóm Mockito service test: tiến trình dừng lâu ở CourseManagementServiceTests; không tính nhóm này là pass.
- Kết quả Chrome ban đầu chưa đủ xác nhận; đã được thay thế bằng kết quả PASS ca chính ở trên.
- Không push/merge. Không tự điều chỉnh khoản hoàn lịch sử đã duyệt/chi theo công thức cũ; dữ liệu đó cần đối soát riêng trước khi dùng bản mới.

## Cách test lại

1. Chạy PostgreSQL/Docker test; chạy `mvn -Dtest=CourseScheduleChangeIntegrationTests,CourseRunClosureIntegrationTests,CourseRefundObligationIntegrationTests,CourseWithdrawalIntegrationTests,CourseTransferIntegrationTests test`.
2. Chạy BE để Flyway áp dụng V75. Tạo đợt có đủ 10 buổi, học phí 1.000.000đ; đăng ký và thanh toán một học viên.
3. Quản lý giảm số buổi còn 8. Vào Hoàn tiền khóa học: có nguồn “Hoàn tiền buổi bị giảm”, số tiền 200.000đ; chưa được chi khi đợt đang học. Không được đổi phân bổ thành 125.000đ/buổi.
4. Hoàn thành 1 trong 8 buổi còn lại. Hủy đợt. Kiểm tra khoản “Hủy đợt học” là 700.000đ, khoản giảm buổi vẫn 200.000đ; tổng 900.000đ. Hai khoản đi qua luồng duyệt/chi hiện tại.
5. Thử lại thao tác hoặc tải lại trang: không thêm khoản trùng. Thử giảm 10→9→8: hai khoản giảm tổng 200.000đ.
6. Thử học hết 8 buổi rồi kết thúc đợt: chỉ có 200.000đ hoàn giảm buổi. Thử một buổi hủy có/không học bù để kiểm tra không hoàn buổi đã được cung cấp bù.
7. Sau giảm buổi, tạo yêu cầu rút/chuyển: phần tiền buổi đã tạo nghĩa vụ hoàn phải bị loại khỏi tiền rút/tín dụng chuyển.
