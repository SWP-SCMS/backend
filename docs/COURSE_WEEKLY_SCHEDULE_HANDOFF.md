# BE: đợt khóa học theo thứ trong tuần

Ngày: 05/11/2026. Chưa push/merge. BE đã hoàn thành; FE đã nối API mới và test Chrome MCP, xem frontend/COURSE_WEEKLY_SCHEDULE_QA.md để biết kết quả UI và cách test.

## Flow
Chọn khóa PUBLISHED → nhập thông tin đợt và lịch tuần → xem trước các buổi/ngày kết thúc do BE tính → lưu đợt DRAFT cùng toàn bộ lịch → mở đăng ký qua API cũ → giữ nguyên luồng đặt chỗ/thanh toán/chốt tối thiểu.
Không cần migration. Lịch từng buổi đã lưu là dữ liệu chính; không thêm bảng lịch lặp. API tạo đợt thủ công cũ vẫn tồn tại để tương thích.

## API xem trước
POST /api/v1/manager/course-runs/preview-schedule
Quản lý ACTIVE; không ghi DB hoặc giữ chỗ tài nguyên.

```json
{
  "openingDate": "2030-02-04",
  "weekdays": [1, 3, 5],
  "startTime": "18:00",
  "endTime": "19:00",
  "totalUnits": 4
}
```

ISO weekday: 1=thứ Hai, 2=thứ Ba, ..., 7=Chủ nhật. UI “2–4–6” gửi [1,3,5]. Múi giờ Asia/Ho_Chi_Minh, không dùng timezone trình duyệt/server. Ngày khai giảng phải là thứ đã chọn; không tự dời sang ngày khác. Giờ kết thúc sau bắt đầu trong cùng ngày, tổng buổi 1..366. Không bỏ ngày lễ tự động.

Response gồm timezone, plannedStartAt, plannedEndAt, sessions[{unitNumber,startAt,endAt}]. Ví dụ trên tạo 04/02, 06/02, 08/02, 11/02; plannedStartAt=2030-02-04T11:00:00Z, plannedEndAt=2030-02-11T12:00:00Z. Manager không truyền ngày kết thúc.
Preview chỉ tính lịch; khi lưu BE kiểm tra khả dụng lại. Xung đột không tự động đẩy buổi sang ngày khác.

## API tạo đợt và lịch nguyên khối
POST /api/v1/manager/course-runs/with-schedule

```json
{
  "runCode": "SWIM-203002",
  "courseId": "UUID_KHOA_DA_CONG_KHAI",
  "primaryCoachId": "UUID_HLV",
  "roomId": "UUID_PHONG",
  "tuitionVnd": 2400000,
  "capacity": 12,
  "minimumEnrollment": 6,
  "registrationOpensAt": "2030-01-01T00:00:00Z",
  "registrationClosesAt": "2030-02-01T00:00:00Z",
  "namedCoachCommitment": true,
  "schedule": {
    "openingDate": "2030-02-04",
    "weekdays": [1, 3, 5],
    "startTime": "18:00",
    "endTime": "19:00",
    "totalUnits": 4
  }
}
```

Response 201: {run, sessions}. run giữ nguyên shape CourseRunResponse; sessions giữ nguyên shape CourseSessionResponse. Không có plannedEndAt đầu vào; totalUnits nằm trong schedule.
HLV chính và phòng dùng chung cho các buổi sinh ra; có thể dùng API sửa từng buổi hiện có sau đó. Buổi được đánh số 1..N, phân bổ học phí chia đều, phần dư VND vào buổi cuối; học phí phải ít nhất N VND.
Tạo run + mọi session + audit nằm trong một transaction. Dùng lại resource locks và kiểm tra xung đột phòng/HLV/lớp thường/nghỉ HLV/đình chỉ phòng của CourseSessionService. Lỗi ở bất kỳ buổi nào rollback toàn bộ. Không tự bỏ buổi bị trùng.
runCode duy nhất: gửi lại cùng mã sau lần lưu thành công trả lỗi trùng, không tạo thêm đợt; chưa có replay response qua Idempotency-Key ở API mới.

## Mở đăng ký
POST /api/v1/manager/course-runs/{id}/open-registration giữ nguyên payload {expectedVersion,policyVersion}.
Bổ sung kiểm tra khóa còn PUBLISHED; đủ N buổi SCHEDULED, số thứ tự duy nhất trong 1..N, không phải buổi bù/thay thế, tất cả chưa bắt đầu, nằm trong khung đợt và không chồng giờ nhau. Lỗi COURSE_VALIDATION_ERROR, field sessions/courseId, nội dung tiếng Việt.
Các điều kiện phiên bản và trạng thái DRAFT hiện có vẫn giữ. Đợt cũ thiếu lịch phải bổ sung trước khi mở; đợt đã mở không bị thay trạng thái tự động.

## Thay đổi code
- CourseWeeklySchedule: tính lịch thuần, cố định múi giờ, chặn ngày/giờ/thứ/số buổi sai.
- CourseWeeklyRunController/Service: preview, tạo nguyên khối; dùng lại service khóa học hiện có.
- CourseManagementService: kiểm tra lịch trước mở đăng ký.
- OpenApiConfiguration: mô tả response 201 cho endpoint mới.
- CourseImageResources: tách cấu hình static resource khỏi controller ảnh để tránh controller phụ thuộc AccountRepository bị kéo vào mọi WebMvcTest. Không đổi hợp đồng upload ảnh.
- Điều chỉnh fixture integration cũ mở đăng ký trước khi tạo lịch: chuyển sang xếp lịch rồi mở ở test lịch/HLV/chuyển đợt. Fixture lịch sử rút học/chốt đợt seed trạng thái trực tiếp, vì các test đó kiểm tra nghiệp vụ sau mở đợt; không nới điều kiện production.

## Kiểm tra
- PASS: 63 test chọn lọc gồm calendar, MVC quyền/validation, quản lý khóa, run/session và upload ảnh.
- Integration PostgreSQL: đã thêm case tạo 4 buổi/mở đợt, lỗi ở buổi thứ hai rollback run/session/audit, thiếu lịch không mở được. Chưa chạy được vì không có Docker (Testcontainers).
- BE local đã khởi động lại thành công sau lỗi DNS/timeout tạm thời. PASS Chrome MCP gọi API thật qua phiên quản lý: preview; tạo 4 buổi, GET lại đủ 4; tự tính ngày kết thúc; phân bổ 1001 VND thành [250,250,250,251]; mở đăng ký; chặn thiếu lịch (400); chặn ngày khai giảng không khớp thứ (400).
- PASS rollback qua API thật: tạo blocker tại buổi thứ hai → tạo đợt mới 4 buổi nhận 409 COURSE_SESSION_RESOURCE_CONFLICT → danh sách không có runCode thất bại → tạo được đợt khác vào chính khung buổi đầu chứng minh không lưu sót buổi đó. Đây là kiểm tra runtime thật; bộ Testcontainers vẫn chưa chạy do thiếu Docker.
- Bộ dữ liệu QA-WEEKLY-1793845918728: đợt thành công 5837131b-52c1-450c-820f-7db9e1dd0ca8; kết thúc tự tính 2030-02-11T14:00:00Z (21:00 VN), 4 buổi 20:00–21:00 thứ 2–4–6. Blocker 51e1fdbc-c0cd-48ee-abc7-0cb59a6a707f; đợt thiếu lịch a6cecf9a-c557-46d0-89a3-28bb6748146b; chứng minh rollback 7e2c4f89-e2a2-4a04-8925-aefff9b69c77. Đã hủy 4 đợt QA sau kiểm tra; không có hội viên đăng ký/thanh toán.
- Chưa test giao diện chọn thứ vì FE chưa nối API mới. Chrome MCP được dùng để gọi API BE thật, không phải test một form mới.

Lệnh test:
```powershell
mvn -o -Dtest=CourseWeeklyScheduleTests,CourseWeeklyRunServiceTests,CourseWeeklyRunControllerTests,CourseManagementServiceTests,CourseControllerTests,CourseSessionServiceTests,CourseRunTests,CourseImageControllerTests test
mvn -Dtest=CourseManagementIntegrationTests test
```

## Cách test API thủ công
1. Manager gọi preview với payload trên: đủ 4 ngày, kết thúc đúng 11/02 19:00 giờ Việt Nam; ngày 04/02 thứ Hai phải là buổi 1.
2. Chọn ngày khai giảng thứ Ba nhưng weekdays=[1,3,5], chọn thứ trùng, giờ ngược hoặc tổng buổi 0: 400.
3. Gọi with-schedule với UUID thật: 201, DRAFT, GET sessions có đúng 4 buổi. GET đợt trả ngày kết thúc tự tính.
4. Tạo một buổi khác trùng phòng hoặc HLV ở ngày thứ hai của lịch mới; gọi with-schedule với runCode khác: lỗi conflict; kiểm tra không có runCode thất bại và không có buổi đầu lưu sót.
5. Mở đăng ký cho đợt đủ lịch: thành công. Tạo đợt theo API cũ nhưng chưa xếp đủ lịch rồi mở: bị chặn.
6. Gửi với vai trò hội viên/lễ tân: 403; không có phiên đăng nhập: 401.
7. Chạy tiếp đăng ký/thanh toán theo API cũ để xác nhận dữ liệu lịch sinh ra được dùng như lịch nhập tay.

## FE đã nối
Form tạo đợt đã bỏ ngày kết thúc nhập tay; thêm các thứ, giờ bắt đầu/kết thúc, phòng và ngày khai giảng. Gọi preview để hiện bảng trước khi lưu. Gửi with-schedule để lưu; hiển thị run.plannedEndAt do BE trả. Chrome MCP đã tạo thành công từ form, mở đăng ký và kiểm tra lịch công khai. Các thao tác sau mở đăng ký giữ nguyên. Chi tiết kết quả và dữ liệu QA còn lại ở frontend/COURSE_WEEKLY_SCHEDULE_QA.md.
