# Ngừng/lưu trữ khóa học: hủy các đợt chưa bắt đầu

Ngày 05/11/2026. Không push/merge.

## Quy tắc

- Hệ thống không có trạng thái Course.CANCELLED. Hai thao tác hiện có là Ngừng hoạt động (PUBLISHED → INACTIVE) và Lưu trữ (DRAFT/INACTIVE → ARCHIVED); cả hai nay hủy liên đới các đợt chưa khai giảng.
- Đợt bị hủy khi trạng thái DRAFT, OPEN_FOR_REGISTRATION hoặc CONFIRMED **và** plannedStartAt > thời điểm kiểm tra sau khi khóa dòng đợt. Đúng giờ bắt đầu hoặc đã qua giờ bắt đầu thì giữ nguyên, kể cả scheduler chưa đổi trạng thái.
- IN_PROGRESS, SETTLEMENT_PENDING, COMPLETED, CANCELLED giữ nguyên. Không xóa lịch sử khóa/đợt/buổi.
- Dùng lại cancelEffects: hủy lịch chưa hoàn thành, giải phóng phòng/HLV, hết hạn đơn/giữ chỗ, xử lý hàng chờ/chuyển đợt/rút học và tạo nghĩa vụ hoàn tiền cho enrollment đã thanh toán. Không tự chi tiền.
- Công khai lại khóa học không khôi phục đợt/buổi đã hủy.

## Thay đổi

- CourseManagementService.transitionCourse: khóa khóa học, đổi trạng thái, khóa các đợt theo thứ tự ID, kiểm tra lại điều kiện, hủy và ghi audit COURSE_RUN_CANCELLED với reasonCode COURSE_DEACTIVATED/COURSE_ARCHIVED. Audit khóa có cancelledRunIds. Tất cả trong cùng transaction: lỗi bất kỳ bước nào rollback toàn bộ.
- createRun dùng khóa dòng course để tránh tạo đợt mới chen vào lúc ngừng/lưu trữ. API/path/payload/response giữ nguyên.
- CourseRunRepository bổ sung query ID đợt theo khóa.
- FE: modal xác nhận Ngừng hoạt động/Lưu trữ mô tả rõ hủy đợt chưa khai giảng, hủy lịch, hết hạn đơn, yêu cầu hoàn tiền và giữ đợt đã bắt đầu.
- Không thêm migration và không tự hủy dữ liệu khóa INACTIVE/ARCHIVED cũ chỉ vì triển khai bản sửa. Thao tác ngừng/lưu trữ mới áp dụng quy tắc này. Cần bản sửa V74 của hủy buổi đã thực hiện trước đó.

## Kiểm thử

- 54 test BE chọn lọc PASS: CourseManagementServiceTests, CourseRunTests, CourseSessionTests, CourseWeeklyRunServiceTests, CourseControllerTests.
- Test mới kiểm tra DRAFT/OPEN/CONFIRMED tương lai bị hủy; IN_PROGRESS/COMPLETED/CANCELLED không đổi; đúng giờ/qua giờ khai giảng không đổi; công khai lại không hồi phục; lưu trữ hủy đợt tương lai còn lại.
- Thêm test PostgreSQL vào CourseRunClosureIntegrationTests: cascade có người thanh toán tạo hoàn tiền, hết hạn đơn chờ, hủy buổi; lưu trữ sau ngừng không nhân đôi hoàn tiền; lỗi audit rollback cả course/run/session/refund. Môi trường hiện thiếu Docker nên chưa chạy bộ integration này; không ghi PASS.
- FE lint/build PASS; còn cảnh báo bundle authService có sẵn.

## Cách test thủ công

1. Tạo khóa riêng, công khai, tạo ít nhất 2 đợt tương lai với lịch khác nhau; mở đăng ký một đợt, để đợt còn lại ở nháp.
2. Chi tiết khóa → Ngừng hoạt động → đọc cảnh báo và Xác nhận.
3. Khóa INACTIVE; hai đợt CANCELLED; các buổi dự kiến CANCELLED, không còn giữ phòng/HLV. Có đơn chờ thì hết hạn; có enrollment đã trả tiền thì có nghĩa vụ hoàn tiền cần xử lý.
4. Đợt có plannedStartAt <= hiện tại hoặc IN_PROGRESS không đổi.
5. Công khai lại khóa: các đợt đã hủy vẫn CANCELLED. Tạo đợt mới để mở lớp lại.
6. Ngừng rồi Lưu trữ: ARCHIVED, không phát sinh thêm refund/audit hủy đợt đã CANCELLED.

## Kết quả Chrome MCP với BE thật

- Khóa QA riêng: ab384ebc-406d-481a-a5bf-43f32d1b01c3 / QA-CASCADE-1793848062304. Không dùng khóa thật để thử hủy.
- Tạo đợt DRAFT 01ee113a-b667-4c66-8333-f89bb24fa449 và OPEN_FOR_REGISTRATION e0ac3ea4-a72d-4887-8295-2537e6e24a20, mỗi đợt 2 buổi ngày 02/04 tháng 02/2032.
- Bấm Ngừng hoạt động → đọc cảnh báo mới → Xác nhận trên UI: khóa INACTIVE, cả hai đợt CANCELLED, đủ 4 buổi CANCELLED. PASS.
- Fixture DRAFT có plannedStartAt trong quá khứ 8b845e0c-5d99-4898-8143-7ec1b2277e85 giữ DRAFT. PASS kiểm tra điều kiện thời gian, không giả định scheduler đã đổi trạng thái. Trạng thái IN_PROGRESS và mốc đúng giờ được kiểm tra ở unit test, không seed trực tiếp DB thật.
- Công khai lại qua UI: khóa PUBLISHED, đợt đã hủy vẫn CANCELLED. PASS.
- Tạo đợt 8a4fe460-1c41-4286-99af-57eacb8942f9 cùng phòng F1-020, HLV Anh Bô Xin và đúng giờ 15:00–16:00 ngày 02/04 tháng 02/2032 của đợt nháp cũ: thành công. PASS giải phóng tài nguyên.
- Ngừng khóa lần nữa: đợt mới và 2 buổi mới đều CANCELLED; fixture quá giờ vẫn DRAFT. Sau khi xác minh, hủy riêng fixture quá giờ để dọn QA (0 học viên/0 đơn chờ).
- Dữ liệu QA được tạo bằng API thật trong Chrome MCP; thao tác ngừng chính, công khai lại và lưu trữ được bấm trên UI. Không chạy thanh toán hoặc hoàn tiền thật trong lượt này; các case tài chính/rollback đã thêm vào integration suite nhưng chưa chạy do thiếu Docker.
- Dọn QA: cả 4 đợt đã CANCELLED; khóa đã ARCHIVED sau thao tác Lưu trữ trên UI. Không còn đợt QA mở đăng ký.
