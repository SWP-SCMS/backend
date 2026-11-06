# Hủy đợt phải hủy lịch buổi học

Ngày 05/11/2026. Không push/merge.

## Nguyên nhân và thay đổi

CourseRunClosureService.cancelEffects trước đây chỉ xử lý đơn, giữ chỗ, thanh toán, hoàn tiền và thông báo; không đổi course_sessions. Các query kiểm tra trùng phòng/HLV dựa vào trạng thái buổi SCHEDULED/MAKEUP_SCHEDULED, nên buổi của đợt đã hủy vẫn giữ tài nguyên.

- Bổ sung CourseSessionStatus.CANCELLED và cập nhật hợp đồng OpenAPI.
- Trong transaction hủy đợt, chuyển SCHEDULED, MAKEUP_SCHEDULED, CANCELLED_PENDING_MAKEUP sang CANCELLED, tăng version, cập nhật updated_at. Áp dụng chung cho manager hủy và scheduler hủy do thiếu tối thiểu.
- Giữ lịch sử COMPLETED, UNRESOLVED, RESOLVED_BY_REFUND, REMOVED_BY_RUN_CHANGE; không xóa dòng buổi, điểm danh hoặc đổi logic hoàn tiền.
- Giữ run lock hiện tại; thao tác thêm/sửa buổi cùng đợt cũng khóa run, không thể thêm buổi vào đợt đã hủy. Query tài nguyên hiện tại tự loại CANCELLED.
- Migration V74 mở rộng check constraint và sửa các buổi còn sót của đợt CANCELLED từ trước. Cần triển khai BE và migration cùng nhau vì enum mới chưa tồn tại trong bản BE cũ.
- FE khóa nút thêm/sửa buổi khi đợt CANCELLED/COMPLETED/SETTLEMENT_PENDING. Nhãn CANCELLED → Đã hủy đã có sẵn.

## Test

- BE: 40 test chọn lọc PASS (CourseRunTests, CourseSessionTests, CourseManagementServiceTests, CourseRunClosureSchedulerTests, CourseWeeklyRunServiceTests).
- Thêm integration test hủy thủ công, hủy do thiếu tối thiểu, giữ buổi hoàn thành/đã hoàn tiền, giải phóng overlap phòng/HLV và rollback trạng thái buổi nếu audit thất bại.
- FE: lint/build PASS; build có cảnh báo bundle authService có sẵn.
- Integration suite đã compile nhưng không chạy được vì không có Docker (Could not find a valid Docker environment). Không đánh dấu bộ này PASS.
- BE local khởi động lại, Flyway V74 đã áp dụng thành công trên DB cấu hình local sau một lần timeout kết nối PostgreSQL.
- Chrome MCP + BE thật: đợt cũ eb95a88d-5dfd-4170-9788-5384c5e5f667 trả 4 buổi CANCELLED, version tăng lên 1 (xác minh backfill).
- Tạo QA-CANCEL-FIX-1793847440086 (86cd9def-cf0a-412a-b974-fd7c566ef7c5) dùng lại đúng phòng F1-020, HLV Anh Bô Xin, 03/05/07/10 tháng 02/2031 18:00–19:00: thành công. Khi đợt mới còn hoạt động, tạo trùng tiếp nhận 409 COURSE_SESSION_RESOURCE_CONFLICT.
- Bấm Hủy đợt mở trên UI, xác nhận 0 học viên/0 đơn chờ, nhập lý do và xác nhận: GET lại 4 buổi đều CANCELLED; nút thêm/sửa bị khóa.
- Gửi lại payload từng bị 409 sau khi hủy: tạo thành công đủ 4 buổi, đợt ee0d3b07-3edb-4550-8632-ed0e96c44c16. Đây là bằng chứng phòng/HLV thực sự được giải phóng, không chỉ đổi nhãn UI.
- Dữ liệu kiểm thử mới được tạo qua API thật trong Chrome MCP; thao tác hủy chính thực hiện bằng UI. Đợt chứng minh dùng lại lịch cũng đã được hủy để dọn QA, không có đăng ký/thanh toán.

## Cách test lại

1. Tạo đợt với 4 buổi, chọn HLV/phòng và khung giờ trống, mở đăng ký nếu cần.
2. Hủy đợt, nhập lý do và xác nhận. Tải lại: đợt Đã hủy, cả 4 buổi Đã hủy; nút thêm/sửa bị khóa.
3. Tạo đợt khác với mã mới nhưng cùng phòng, HLV và chính những giờ vừa hủy. Phải tạo thành công.
4. Đợt cũ CANCELLED có buổi SCHEDULED trước triển khai: sau migration V74, GET sessions/UI phải trả CANCELLED.
5. Với integration test, xác nhận buổi COMPLETED/RESOLVED_BY_REFUND giữ nguyên; hủy do không đủ tối thiểu cũng hủy lịch; lỗi audit rollback cả đợt lẫn lịch.
