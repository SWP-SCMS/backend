# T9 — Ghi kết quả buổi cũ bị chặn sau khi hội viên đổi gói

## Trạng thái

Đã tái hiện bằng Chrome DevTools MCP với FE và BE local thật ngày 04/11/2026. Mức ưu tiên đề xuất P2.

## Môi trường

- FE `http://localhost:5175`.
- BE `http://localhost:8080/api/v1`.
- HLV Võ Lực Sĩ, tài khoản `0801010101`.
- Hội viên Huynh Van Bon, mã `MB-100023`, tài khoản `0904040404`.
- Member account ID `547ce51c-695b-4db8-8077-c26d6fe22c95`.

## Dữ liệu tái hiện

- Buổi Boxing Fight ngày 19/10/2026, 14 giờ đến 15 giờ 30 theo giờ Việt Nam, đã hoàn thành.
- Session ID `ba764a13-2e4f-42ee-bfb6-2530a09f5000`.
- Hội viên có booking trạng thái `BOOKED` cho buổi này.
- Gói PLUS có thời gian từ `2026-10-16T09:28:39.284161Z` đến `2026-10-28T06:39:55.645363Z`, hiện mang trạng thái `TERMINATED`.
- API lịch sử gói trả quyền `TRAINING_RESULT` và `TRAINING_FEEDBACK` cho gói PLUS này.
- Gói Premium hiện tại bắt đầu từ `2026-10-28T06:40:51.031781Z`, trạng thái `ACTIVE`, cũng có hai quyền trên.
- Ngày học 19/10 nằm trong khoảng hiệu lực của gói PLUS.

## Các bước tái hiện

1. Đăng nhập HLV `0801010101`.
2. Mở `/coach/schedule/ba764a13-2e4f-42ee-bfb6-2530a09f5000/students`.
3. Chọn “Xem kết quả” của hội viên `MB-100023`.
4. Nhập nội dung vào “Kết quả tập luyện”.
5. Bấm “Lưu kết quả tập luyện”.

## Kết quả thực tế

Request sau trả HTTP 409.

```http
POST /api/v1/coach/class-sessions/ba764a13-2e4f-42ee-bfb6-2530a09f5000/members/547ce51c-695b-4db8-8077-c26d6fe22c95/training-results
Content-Type: application/json
```

```json
{
  "activityAt": "2026-10-19T07:00:00Z",
  "content": {
    "notes": "QA T9 04/11/2026 — Hoàn thành 3 hiệp boxing, mỗi hiệp 2 phút. Giữ nhịp thở và kỹ thuật đấm cơ bản."
  }
}
```

FE hiển thị “Gói tập của hội viên không có quyền lợi ghi kết quả hoặc nhận xét tại thời điểm buổi tập.” Bản nháp vẫn được giữ lại.

## Kết quả mong muốn

HLV có quyền phụ trách buổi được ghi kết quả khi hội viên có booking hợp lệ và đủ quyền lợi tại thời điểm buổi tập. Quyền lợi lịch sử của buổi 19/10 cần được bảo toàn khi hội viên chuyển từ PLUS sang Premium.

## Vị trí cần kiểm tra

- `src/main/java/com/scms/backend/training/SessionTrainingResultService.java`, hàm `requireTrainingResultBenefit` truyền `session.getStartTime()` để kiểm tra quyền lợi.
- `src/main/java/com/scms/backend/training/CoachAssignmentRepository.java`, hàm `hasActiveMembershipBenefit`, khoảng dòng 90–109.
- Query vừa lọc `membership.starts_at <= :at` và `membership.ends_at > :at`, vừa bắt `membership.status = 'ACTIVE'` tại dòng 97.
- Điều kiện trạng thái hiện tại loại gói PLUS đã `TERMINATED` dù khoảng thời gian của gói bao phủ ngày học. Gói Premium hiện tại bắt đầu sau ngày học nên cũng bị loại bởi điều kiện thời gian. Request vì vậy bị từ chối.
- Query còn join quyền lợi từ `membership_plan_benefits`. BE cần xác nhận cách bảo toàn quyền lợi lịch sử nếu cấu hình quyền lợi của plan thay đổi sau ngày học.

## Phạm vi đã kiểm chứng

- Đã tái hiện lỗi tạo kết quả cho buổi đã hoàn thành ngày 19/10.
- Chưa gửi request tạo nhận xét trên buổi này vì FE yêu cầu có kết quả trước. BE nên rà thêm các điểm dùng chung hàm kiểm tra quyền lợi.
- Ca đối chứng trên buổi Boxing Fight ngày 06/11, thuộc thời hạn Premium hiện tại, tạo kết quả và nhận xét trả 201, sửa cả hai trả 200.
- Sau reload, HLV và hội viên đều thấy đúng nội dung cập nhật của ca đối chứng.
- Ca đối chứng dùng một booking QA mới và nội dung có nhãn “QA T9 04/11/2026”. Đây là buổi tương lai tại thời điểm kiểm thử.

## Nghiệm thu đề xuất cho BE

- Tạo và sửa kết quả cho buổi cũ có quyền lợi hợp lệ tại ngày học sau khi gói chuyển trạng thái kết thúc hoặc chấm dứt.
- Tạo và sửa nhận xét tương ứng khi hội viên có quyền `TRAINING_FEEDBACK` tại ngày học.
- Từ chối buổi nằm ngoài thời hạn gói hoặc trong thời gian đóng băng có hiệu lực tại ngày học.
- Kiểm tra riêng trường hợp trước đây chỉ có Basic, sau đó mới mua Premium, để xác nhận quy tắc quyền lợi theo ngày học.
- Giữ các kiểm tra HLV phụ trách, booking và tài khoản theo quy tắc nghiệp vụ hiện hành.
