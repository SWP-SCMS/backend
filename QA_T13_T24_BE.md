# Bàn giao BE T13–T24 — 05/11/2026

Thay đổi bổ sung, giữ các API/chữ ký cũ; không push/merge. FE dùng các contract dưới đây.

| Mục | Thay đổi |
|---|---|
| T15 | GET `/manager/members` nhận tùy chọn `planCode`, lọc DB trước phân trang. Thêm `activePlanCode` vào response. Chỉ gói ACTIVE có hiệu lực tại thời điểm đọc. Lấy mã gói cho cả trang bằng query gộp. |
| T20 | GET danh sách buổi quản lý thêm `bookedCount`, lấy bằng query gộp. Giữ constructor response cũ. |
| T19 | GET `/manager/course-runs/{runId}/cancellation-impact` trả số đăng ký ACTIVE/WITHDRAWAL_PENDING và đơn PENDING_PAYMENT. Request hủy nhận thêm lý do tùy chọn 5–1000 ký tự, lưu audit; giữ version lock và xử lý hủy cũ. |
| T14 | Khi đối soát FAILED, thông báo `PAYMENT_FAILED` cho đúng hội viên, khóa sự kiện ổn định, không gửi chứng từ staff trong payload. |
| T14 ảnh | GET/POST `/payments/{paymentId}/evidence-images`; GET `/payments/{paymentId}/evidence-images/{evidenceId}`. Multipart part `file`. Response upload/list gồm id, createdAt. Ảnh chỉ staff ACTIVE đọc được; hội viên 403. |

## Migration upload

`V73__payment_evidence_images.sql` thêm bảng riêng, không sửa dữ liệu/cột cũ. Lưu bytea ảnh cùng người tải, payment, MIME đã xác minh và SHA-256. Mỗi ảnh tối đa 4 MiB và 20 triệu pixel, chỉ PNG/JPEG thực sự decode được; tối đa 10 ảnh/payment. Không dùng filename client để truy cập filesystem. Khóa hàng payment khi tải; chỉ PENDING được tải, unique payment/hash cho retry. GET kiểm paymentId + evidenceId và trả no-store/nosniff.

Ảnh giữ theo payment kể cả người dùng đóng form chưa quyết định; đây là chứng từ lưu bền, không phải file tạm. Cần backup bảng này cùng dữ liệu nghiệp vụ. Upload ảnh không thay đổi trạng thái thanh toán và không làm mất yêu cầu lý do/chứng từ của endpoint đối soát cũ.

## Kiểm thử

JDK 21:

```powershell
.\mvnw.cmd '-Dtest=PaymentEvidenceServiceTests,FailedReconciliationNotificationTests,ManagerMemberServiceTests,ManagerMemberControllerTests,ClassSessionServiceTests,CourseManagementServiceTests' test
```

**44 tests, 0 failures, 0 errors, 0 skipped.** Báo cáo local: `target/t13-t24-final-tests.log` và Surefire. Không khẳng định toàn bộ BE/integration suite đã chạy.

Chrome với BE local thật: T15 lọc Premium/reload đúng; T20 số đã đặt đúng; T19 đọc tác động modal đúng; upload/xem ảnh đúng, duplicate cùng ảnh chỉ một bản, file giả 400, hội viên đọc ảnh 403. Migration V73 đã chạy trên DB cấu hình local khi restart. T21 lưu UI trong giờ và đọc lại PRESENT; thông báo nghỉ HLV thật dẫn đúng trang.

E2E PAYMENT_FAILED chưa hoàn tất: automatic approval review chặn xác nhận đối soát thật; payment vẫn PENDING. Unit test nhánh notification và retry đã đạt. Không thay API/công cụ để vượt chặn.

Không commit thư mục `data/`, `.env`, log hay báo cáo sinh trong `target/`.
