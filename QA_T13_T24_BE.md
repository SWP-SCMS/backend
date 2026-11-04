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

E2E chốt ngày 05/11/2026 sau xác nhận của người dùng: **PASS**. Chrome xác nhận hủy đợt QA `de06eb85-adf5-4379-9678-0e6738a369c5` (0 đăng ký/0 đơn): GET lại trả CANCELLED/version 1. Chrome xác nhận không hợp lệ payment `092bc9a1-751b-4f43-b4e2-8cdaf35b3051`: GET result FAILED, không membership/receipt; queue bỏ hàng, chứng từ ảnh vẫn xem được. Hội viên MB-100023 nhận đúng một PAYMENT_FAILED (ID `f949fdd9-6690-41dd-82a2-0ed0116d3521`), bấm thông báo tới `/member/plan` và đánh dấu đã đọc. Không chuyển tiền thật. Hai ca từng bị automatic approval review chặn đã được hoàn tất; không sửa code trong lượt chốt này.

Không commit thư mục `data/`, `.env`, log hay báo cáo sinh trong `target/`.
