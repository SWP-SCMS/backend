# Kế hoạch chuyển gửi OTP từ SMTP sang Resend Email API

**Ngày lập:** 2026-10-26
**Trạng thái:** Kế hoạch để BE, FE và DevOps thống nhất trước khi triển khai
**Phạm vi:** Chỉ thay kênh gửi email OTP từ SMTP sang Resend qua HTTPS API

## 1. Bối cảnh và mục tiêu

Railway chặn outbound SMTP trên Free, Trial và Hobby; SMTP chỉ khả dụng từ Pro plan. Vì vậy backend hiện tại dùng `JavaMailSender` không phù hợp với môi trường Railway đang sử dụng.

Mục tiêu của thay đổi:

- Gửi email OTP qua Resend REST API tại `https://api.resend.com/emails` thay vì SMTP.
- Giữ nguyên toàn bộ logic sinh OTP, thời hạn, cooldown và giới hạn số lần thử; mở rộng outbox/retry vừa đủ để đáp ứng idempotency của Resend.
- Giữ nguyên API contract giữa FE và BE để FE không phải đổi luồng hoặc tích hợp trực tiếp với Resend.
- Áp dụng cho cả ba loại email đang dùng chung `EmailProvider`:
  - OTP xác thực tài khoản sau đăng ký;
  - OTP quên/đặt lại mật khẩu;
  - OTP xác minh email mới khi đổi email.

## 2. Quyết định kỹ thuật

Backend sẽ gọi Resend bằng HTTPS từ server. FE không gọi Resend và không được nhận hoặc lưu `RESEND_API_KEY`.

Luồng sau thay đổi:

```text
FE -> SCMS API -> email_outbox -> EmailOutboxDispatcher
   -> ResendEmailProvider -> HTTPS POST api.resend.com/emails
   -> Resend -> hộp thư người dùng
```

Giữ interface `EmailProvider` làm ranh giới tích hợp và thêm `ResendEmailProvider` song song với `SmtpEmailProvider` trong giai đoạn rollout. Chọn provider bằng `SCMS_EMAIL_PROVIDER=smtp|resend`. Nên dùng `RestClient` có sẵn trong Spring thay vì thêm Resend SDK để giảm dependency và chủ động kiểm soát timeout, status code, retry và idempotency.

Mỗi request gửi mail phải có header `Idempotency-Key` lấy từ outbox ID, ví dụ `scms-email-outbox/{outboxId}`. Resend chỉ chấp nhận reuse key khi request payload giống hệt lần đầu. Vì vậy subject, HTML, text và các field gửi provider phải được render đúng một lần lúc enqueue, mã hóa và snapshot trong outbox; mọi lần retry giải mã và gửi lại đúng payload đó. Không tính lại `expiresInMinutes`, không đọc lại tên người nhận và không render lại template khi retry.

Resend hiện lưu idempotency key trong 24 giờ. Outbox phải giữ invariant `deliver_before - created_at < 24h`, đồng thời không schedule retry nào tại hoặc sau `deliver_before`. OTP hiện có delivery window khoảng 10 phút nên đáp ứng invariant này. Nếu sau này tăng OTP/delivery TTL, cấu hình phải bị từ chối khi chạm hoặc vượt retention window của provider, trừ khi thiết kế idempotency được thay đổi có chủ đích.

## 3. Phạm vi thay đổi backend

### 3.1. Cấu hình

Thêm các biến môi trường:

```dotenv
SCMS_EMAIL_PROVIDER=smtp
RESEND_API_KEY=re_xxxxxxxxx
RESEND_FROM=SCMS <no-reply@your-verified-domain.com>
RESEND_BASE_URL=https://api.resend.com
RESEND_CONNECT_TIMEOUT_MS=5000
RESEND_READ_TIMEOUT_MS=5000
```

Quy tắc cấu hình:

- Khi `SCMS_EMAIL_PROVIDER=resend`, `RESEND_API_KEY` và `RESEND_FROM` phải tồn tại; thiếu cấu hình thì ứng dụng fail-fast lúc khởi động. Điều kiện này áp dụng cho delivery provider dùng chung, không chỉ riêng feature flag activation OTP, vì forgot-password và change-email cũng gửi qua provider đó.
- Không log API key, header `Authorization`, OTP plaintext hoặc toàn bộ request body.
- `RESEND_FROM` phải thuộc domain đã được xác minh trên Resend trước khi production gửi tới người dùng thật.
- Giữ các biến `EMAIL_OTP_*`, khóa HMAC, khóa mã hóa và cookie hiện tại vì chúng thuộc bảo mật OTP, không thuộc SMTP.
- Trong đợt rollout đầu, mặc định `SCMS_EMAIL_PROVIDER=smtp` và giữ các biến `SMTP_*` để rollback nhanh.
- Chỉ xóa dependency Java Mail và các biến `SMTP_*` trong cleanup release sau khi Resend đã ổn định và có quyết định bỏ đường rollback SMTP.

### 3.2. Resend provider

Tạo `ResendEmailProvider` triển khai `EmailProvider` và gửi:

```http
POST https://api.resend.com/emails
Authorization: Bearer ${RESEND_API_KEY}
Content-Type: application/json
Idempotency-Key: scms-email-outbox/{outboxId}
```

Payload tối thiểu:

```json
{
  "from": "SCMS <no-reply@your-verified-domain.com>",
  "to": ["member@example.com"],
  "subject": "Mã xác thực tài khoản SCMS",
  "html": "<html>...</html>",
  "text": "Mã xác thực tài khoản SCMS của bạn là ..."
}
```

Tiếp tục render ba template HTML hiện có trong `src/main/resources/templates/email/`. Không đưa OTP vào URL, log, audit hoặc response API.

### 3.3. Snapshot payload bất biến

Không dùng `expiresInMinutes = Duration.between(now, deliverBefore).toMinutes()` khi gửi nữa, vì giá trị này thay đổi giữa các lần retry và làm payload không còn idempotent.

Khi tạo outbox job cho bất kỳ luồng OTP nào, backend phải:

1. Chốt `from`, `to`, `subject`, HTML và text tại thời điểm enqueue.
2. Dùng thời lượng OTP ban đầu cố định trong nội dung, ví dụ `10 phút`; không tính lại thời lượng còn lại lúc dispatcher gửi.
3. Serialize provider-neutral email payload thành một format có version, ví dụ `EMAIL_REQUEST_V1`.
4. Mã hóa toàn bộ serialized payload bằng cơ chế/key rotation hiện có trước khi lưu, vì HTML/text chứa OTP plaintext.
5. Snapshot `delivery_provider` từ `SCMS_EMAIL_PROVIDER` vào chính outbox job.
6. Mỗi lần retry chỉ giải mã và gửi lại snapshot qua đúng `delivery_provider` của job; không render template hoặc chọn lại provider từ config.
7. Xóa ciphertext/nonce khi job `SENT`, `CANCELLED` hoặc `TERMINAL_FAILED`, giống chính sách dọn OTP hiện tại.

Provider phải sticky theo outbox job. Nếu Resend đã accept nhưng app crash trước khi cập nhật `SENT`, sau đó operator đổi config về SMTP, job cũ vẫn phải retry qua Resend với cùng idempotency key; gửi lại job đó qua SMTP có thể tạo email trùng. Thay đổi `SCMS_EMAIL_PROVIDER` chỉ áp dụng cho job được enqueue sau thời điểm đổi config.

Để nâng cấp an toàn, thêm `payload_format` cho outbox. Dữ liệu cũ được xem là `OTP_V1`; dữ liệu mới là `EMAIL_REQUEST_V1`. Trong rollout:

- deploy schema/code snapshot trong khi vẫn chọn SMTP;
- đợi các job `OTP_V1` đang pending được gửi hoặc hết hạn;
- chỉ chuyển `SCMS_EMAIL_PROVIDER=resend` khi không còn job legacy có thể retry với payload được render động.

### 3.4. Outbox, idempotency và xử lý lỗi

Điều chỉnh contract nội bộ để `EmailProvider` nhận được `outboxId`. `EmailOutboxDeliveryService` truyền ID này cho provider khi gửi.

Phân loại kết quả:

- HTTP `2xx` có response body hợp lệ và `id` không rỗng: lưu `provider_message_id`, rồi đánh dấu outbox `SENT`. Trong hệ thống này, `SENT` chỉ có nghĩa Resend đã accept request, không khẳng định email đã tới inbox.
- HTTP `2xx` nhưng thiếu `id`, `id` rỗng hoặc response không parse được: xem là lỗi tạm thời và retry bằng cùng idempotency key/payload; không đánh `SENT`.
- Với response lỗi từ Resend, parse structured JSON error body vào DTO nội bộ theo field thực tế của provider, gồm tối thiểu `name` và message/status nếu có. Không phân loại chỉ bằng HTTP status và không giả định field lỗi tên `code`.
- HTTP `409` có `name=concurrent_idempotent_requests`: lỗi tạm thời, retry cùng key/payload sau delay.
- HTTP `409` có `name=invalid_idempotent_request`: lỗi terminal vì cùng key đã đi với payload khác; ghi tên lỗi đã làm sạch để điều tra invariant bị vi phạm.
- HTTP `409` có body malformed, thiếu `name` hoặc `name` không được nhận diện: mặc định terminal và phát cảnh báo vận hành; không retry mù.
- HTTP `429`, `5xx`, timeout hoặc lỗi kết nối: lỗi tạm thời; tôn trọng `Retry-After` nếu có và còn trước `deliver_before`.
- HTTP `400`, `401`, `403`, `404`, `422` và các `4xx` không nằm trong allow-list lỗi tạm thời: lỗi cấu hình/payload không thể tự hồi phục; đánh dấu `TERMINAL_FAILED`, không retry mù.
- Khi hết thời gian OTP: giữ hành vi `OTP_EXPIRED` hiện tại.

Retry policy được chốt là **initial attempt + tối đa 3 retries = tối đa 4 attempts**:

| Attempt vừa thất bại | Hành động |
|---|---|
| 1 (initial) | retry sau 30 giây |
| 2 | retry sau 120 giây |
| 3 | retry sau 300 giây |
| 4 | `TERMINAL_FAILED` |

`retry_count` hiện tăng khi claim job, nên điều kiện terminal phải là `retry_count >= 4`, không phải `>= 3`. Nếu delay kế tiếp vượt hoặc quá sát `deliver_before`, chuyển terminal ngay thay vì gửi OTP đã hết hạn.

Trên nhánh `main` tại ngày lập/cập nhật kế hoạch, migration cao nhất là `V47__freeze_effective_from_scheduled_start.sql`; vì vậy migration kế tiếp là `V48__resend_delivery_metadata.sql`. Migration thêm tối thiểu:

- `payload_format` để phân biệt payload OTP legacy và snapshot email request;
- `delivery_provider` để retry/khôi phục luôn dùng đúng provider đã chọn cho job;
- ciphertext/nonce/version cho snapshot đã mã hóa nếu không tái sử dụng an toàn được các cột payload hiện tại;
- `provider_message_id` nullable để đối soát response của Resend.

Migration chỉ bổ sung cột nullable/default tương thích ngược, không sửa hoặc xóa dữ liệu outbox cũ.

Đổi mã lỗi nội bộ `SMTP_DELIVERY_FAILED` thành tên trung lập như `EMAIL_PROVIDER_DELIVERY_FAILED` hoặc mã cụ thể đã được allow-list. Không lưu raw response có thể chứa thông tin nhạy cảm.

### 3.5. Các file dự kiến tác động

- `pom.xml`: giữ `spring-boot-starter-mail` trong rollout đầu; xóa ở cleanup release sau.
- `src/main/resources/application.properties`: thêm provider selector và cấu hình Resend, chưa xóa SMTP ở rollout đầu.
- `.env.example`: mô tả `SCMS_EMAIL_PROVIDER` và biến Resend; giữ SMTP cho rollback ở rollout đầu.
- `EmailProvider.java`: nhận delivery/outbox ID và immutable email request nội bộ.
- `SmtpEmailProvider.java`: giữ lại có điều kiện khi `SCMS_EMAIL_PROVIDER=smtp`.
- `ResendEmailProvider.java`: implementation mới khi `SCMS_EMAIL_PROVIDER=resend`.
- Provider router/factory: chọn implementation theo `delivery_provider` đã lưu trên job, không đọc lại config trong mỗi lần retry.
- Ba điểm enqueue trong `EmailVerificationService`, `AccountEmailChangeService` và `PasswordResetService`: render và mã hóa snapshot payload một lần.
- `EmailOutboxDeliveryService.java`: đọc snapshot, truyền idempotency key, phân loại lỗi, lưu provider message ID.
- `EmailOutboxDispatcher.java`: gọi nhánh retry/terminal đúng loại lỗi.
- `V48__resend_delivery_metadata.sql`: migration bổ sung format/snapshot/provider metadata.
- Test provider/outbox và tài liệu vận hành liên quan.

Không cần sửa controller, DTO hoặc OpenAPI vì contract bên ngoài được giữ nguyên. Schema `email_outbox` có thay đổi additive theo migration V48 như mô tả ở trên; schema challenge/digest OTP không thay đổi.

## 4. Contract dành cho frontend

### 4.1. Kết luận quan trọng

**FE không cần đổi endpoint, request body hoặc response body chỉ vì backend chuyển từ SMTP sang Resend.** “Resend” là nhà cung cấp email của backend; endpoint `/resend` hiện tại vẫn có nghĩa là yêu cầu gửi lại OTP.

FE tuyệt đối không:

- gọi `api.resend.com` trực tiếp;
- thêm Resend SDK;
- lưu `RESEND_API_KEY` trong source, `.env` frontend, local storage hoặc browser;
- coi response `202 Accepted` là bằng chứng email đã tới inbox. Response này chỉ xác nhận BE đã chấp nhận/queue yêu cầu.

### 4.2. Các luồng FE tiếp tục sử dụng

#### Đăng ký và xác thực email

1. Đăng ký qua endpoint hiện tại.
2. Nếu response có `verificationRequired=true`, điều hướng tới màn hình nhập OTP.
3. Xác thực bằng:

```http
POST /api/v1/auth/email-verification/verify
Content-Type: application/json

{
  "email": "member@example.com",
  "otp": "123456"
}
```

4. Gửi lại OTP bằng:

```http
POST /api/v1/auth/email-verification/resend
Content-Type: application/json

{
  "email": "member@example.com"
}
```

FE tiếp tục dùng `serverTime` và `resendAvailableAt` từ response để tính countdown, không tự giả định chính xác 60 giây theo đồng hồ client.

#### Quên mật khẩu

Giữ nguyên:

- `POST /api/v1/auth/password-reset/request`
- `POST /api/v1/auth/password-reset/verify`
- `POST /api/v1/auth/password-reset/confirm`

FE phải gửi cookie theo chính sách hiện tại ở bước confirm nếu frontend và backend khác origin.

#### Đổi email tài khoản

Giữ nguyên các endpoint hiện tại:

- `/api/v1/accounts/me/email-change` và `/verify`, `/resend`;
- `/api/v1/staff/me/email-change` và `/verify`, `/resend` trong luồng staff đang dùng.

Các request cần xác thực vẫn gửi bearer token/cookie như trước. Việc gửi email qua Resend không thay đổi authentication của SCMS API.

### 4.3. UX và xử lý lỗi FE

- Sau khi BE trả `201` hoặc `202`, hiển thị thông báo trung lập: “Nếu địa chỉ email hợp lệ, mã OTP sẽ được gửi trong ít phút.”
- Cho phép người dùng kiểm tra Spam/Junk và xác nhận lại địa chỉ email đã nhập.
- Chỉ bật nút “Gửi lại mã” khi tới `resendAvailableAt`.
- Với `429`, dùng thông tin lỗi/rate-limit của SCMS API và yêu cầu người dùng chờ; FE không tự gọi Resend.
- Với OTP sai/hết hạn, giữ mapping lỗi hiện tại; không đổi nội dung chỉ vì thay nhà cung cấp email.
- Không hiển thị raw error của Resend cho người dùng. Lỗi nhà cung cấp phải được BE chuyển thành thông báo SCMS ổn định.

## 5. Thiết lập Resend và Railway

1. Tạo Resend account/team dùng chung cho dự án.
2. Thêm sending domain của SCMS trong Resend.
3. Thêm các DNS record do Resend cung cấp và chờ trạng thái sending domain được xác minh. Resend yêu cầu SPF/DKIM cho sending domain; DMARC nên được cấu hình.
4. Tạo API key chỉ có quyền gửi email nếu dashboard hỗ trợ scope tương ứng.
5. Thêm `RESEND_API_KEY` và `RESEND_FROM` vào Railway Variables; không commit secret vào Git.
6. Deploy staging với `EMAIL_VERIFICATION_ENABLED=true` và gửi thử đến ít nhất Gmail và Outlook.
7. Xác nhận email tới inbox, nội dung tiếng Việt/HTML đúng, OTP verify được và resend không tạo email trùng ngoài ý muốn.
8. Deploy production, theo dõi outbox, Resend Logs/Metrics và tỷ lệ `TERMINAL_FAILED`.

Theo bảng giá Resend tại thời điểm lập kế hoạch, Free tier có giới hạn 3.000 email/tháng và 100 email/ngày. Nhóm phải kiểm tra lại quota trước khi go-live và đặt cảnh báo để tránh luồng OTP dừng đột ngột khi vượt hạn mức.

## 6. Kế hoạch kiểm thử

### Unit test

- Provider tạo đúng `from`, `to`, subject, HTML và text cho cả ba template.
- Header bearer và idempotency có mặt; test không in secret/OTP ra log.
- Snapshot cùng một outbox job luôn tạo byte-for-byte cùng Resend request body, kể cả clock đã chuyển từ phút thứ 10 sang phút thứ 9, tên account thay đổi hoặc template/config được reload.
- Escape dữ liệu người nhận trước khi chèn vào HTML.
- `2xx` chỉ thành công khi response có `id` hợp lệ; thiếu/rỗng/malformed `id` phải retry với cùng key/payload.
- Parse structured Resend error body theo field `name`; không dùng field giả định `code` hoặc chỉ dựa vào HTTP status.
- Mapping riêng hai lỗi `409`: `name=concurrent_idempotent_requests` là retryable, `name=invalid_idempotent_request` là terminal; body thiếu/không rõ `name` là terminal.
- Mapping `429`, các `4xx` khác, `5xx`, timeout và lỗi kết nối.
- Chứng minh retry schedule là initial + 3 retries tại 30/120/300 giây, tối đa 4 attempts.
- Fail-fast khi `SCMS_EMAIL_PROVIDER=resend` nhưng thiếu `RESEND_API_KEY` hoặc `RESEND_FROM`; test không phụ thuộc riêng vào `EMAIL_VERIFICATION_ENABLED`.
- Validate `deliver_before - created_at < 24h` và không tạo retry tại/sau `deliver_before`; cấu hình TTL vi phạm retention window phải bị từ chối.

### Integration test

- Dùng mock HTTP server, không gửi email thật trong test suite.
- Một outbox job retry nhiều lần vẫn dùng cùng idempotency key.
- Một outbox job retry nhiều lần gửi cùng request payload, không chỉ cùng key.
- Đổi `SCMS_EMAIL_PROVIDER` sau lần attempt đầu không đổi provider của job cũ; job mới dùng provider mới.
- `2xx` có `id` chuyển job sang `SENT`, xóa ciphertext/nonce và lưu provider message ID.
- Lỗi tạm thời chuyển sang `FAILED` với `next_retry_at` hợp lệ.
- Lỗi cấu hình/payload chuyển thẳng `TERMINAL_FAILED`.
- Test riêng đủ ba producer: activation OTP, change-email OTP và forgot-password OTP đều tạo `EMAIL_REQUEST_V1` snapshot đúng template.
- Mô phỏng app crash sau khi mock Resend đã accept/trả `id` nhưng trước transaction cập nhật `SENT`: job `PROCESSING` được reclaim sau lock timeout, gửi lại cùng key/payload, nhận cùng provider ID và chỉ có một email logic được tạo.
- Chạy lại toàn bộ test đăng ký, verify/resend OTP, password reset và email change để chứng minh API contract không đổi.

### Smoke test staging

- Đăng ký tài khoản mới -> nhận OTP -> verify -> đăng nhập.
- Resend sau cooldown -> OTP cũ bị vô hiệu -> OTP mới dùng được.
- Quên mật khẩu -> nhận OTP -> đổi mật khẩu.
- Đổi email -> nhận OTP tại email mới -> session cũ bị xử lý đúng contract.
- Tạm dùng API key sai để xác nhận alert/terminal behavior, sau đó khôi phục secret đúng.

## 7. Rollout và rollback

### Rollout

- Release 1: thêm migration, snapshot payload, `ResendEmailProvider` và `SCMS_EMAIL_PROVIDER`; production vẫn để `smtp`.
- Đợi/kiểm tra không còn outbox `OTP_V1` ở trạng thái `PENDING`, `FAILED` hoặc `PROCESSING`.
- Xác minh domain và thêm Resend secrets trên Railway.
- Chuyển staging sang `SCMS_EMAIL_PROVIDER=resend`, chạy smoke test đủ ba luồng.
- Chuyển production sang `resend`; nếu có sự cố có thể đổi config về `smtp` và redeploy mà chưa cần revert code/schema. Config mới chỉ định provider cho job mới; job Resend đã tồn tại vẫn retry qua Resend để giữ idempotency và tránh gửi trùng chéo provider.
- Trong 24 giờ đầu theo dõi số job `PENDING`, `FAILED`, `TERMINAL_FAILED`, HTTP `429`, độ trễ từ lúc queue tới `SENT` và quota Resend.
- Release cleanup chỉ được thực hiện sau thời gian ổn định đã thống nhất và truy vấn xác nhận không còn job `PENDING`, `FAILED` hoặc `PROCESSING` có `delivery_provider` trỏ tới provider sắp xóa. Chỉ khi điều kiện này đúng mới xóa implementation, dependency và variables của provider đó.

### Rollback

- Trong rollout đầu, đổi `SCMS_EMAIL_PROVIDER=smtp` để các job mới dùng SMTP ở môi trường có SMTP khả dụng, hoặc rollback Railway về image/commit trước đó. Không ép job đã snapshot `delivery_provider=resend` chạy qua SMTP.
- Vì SMTP không hoạt động trên Railway Free/Hobby, provider fallback chỉ thực sự gửi được mail ở môi trường/network cho phép SMTP. Trên Railway Free/Hobby, phương án phục hồi ưu tiên vẫn là sửa cấu hình/API key/domain Resend và redeploy.
- Migration thêm cột nullable là tương thích ngược, không cần drop cột khi rollback.
- Không xóa outbox đang pending; sau khi provider hoạt động lại, dispatcher tiếp tục xử lý các job còn trước `deliver_before`.

## 8. Tiêu chí hoàn thành

- Cả ba loại OTP gửi thành công qua Resend HTTPS API.
- Resend API key chỉ tồn tại ở secret store của backend/Railway.
- Cùng một outbox job retry dùng cùng idempotency key và payload byte-for-byte bất biến.
- Initial attempt + tối đa ba retries chạy đúng lịch 30/120/300 giây.
- Structured error được phân loại bằng field `name`: `409` + `concurrent_idempotent_requests` được retry; `409` + `invalid_idempotent_request` kết thúc terminal.
- Chỉ đánh `SENT` khi response `2xx` có provider message ID hợp lệ; `SENT` được hiểu là provider accepted, không phải inbox delivered.
- Crash window sau provider accept và trước DB `SENT` không tạo email trùng.
- Đổi provider trong lúc có job retry không làm job cũ chuyển provider hoặc tạo email trùng chéo provider.
- Không cleanup provider khi còn bất kỳ job `PENDING`, `FAILED` hoặc `PROCESSING` tham chiếu provider đó.
- Contract OpenAPI và hành vi FE hiện tại không thay đổi.
- Tất cả unit/integration test liên quan vượt qua.
- Smoke test staging trên domain đã xác minh thành công.
- Có log/metric đủ để đối soát outbox ID với Resend message ID mà không lộ OTP hoặc secret.
- Sau giai đoạn ổn định, có quyết định riêng để xóa SMTP fallback/dependency/variables.

## 9. Phân công bàn giao

### Backend

- Implement snapshot payload mã hóa cho đủ ba producer, Resend provider, provider selector, error mapping, retry bốn attempts, idempotency và migration metadata.
- Giữ nguyên controller/DTO/OpenAPI.
- Cập nhật test và tài liệu biến môi trường.

### DevOps/người quản trị Railway và DNS

- Tạo Resend account/API key.
- Xác minh sending domain.
- Cấu hình secret trên Railway, deploy và giám sát quota/log.

### Frontend

- Xác nhận không có code gọi SMTP/Resend trực tiếp.
- Regression test bốn màn hình: đăng ký/xác thực email, gửi lại OTP, quên mật khẩu và đổi email.
- Giữ countdown theo `serverTime`/`resendAvailableAt`, xử lý `429` và gửi credential như contract hiện tại.

## 10. Tài liệu tham khảo

- [Railway Outbound Networking](https://docs.railway.com/networking/outbound-networking)
- [Resend Email API](https://resend.com/features/email-api)
- [Resend idempotency keys and 409 error semantics](https://resend.com/changelog/idempotency-keys)
- [Resend engineering notes: same key requires the same payload](https://resend.com/blog/engineering-idempotency-keys)
- [Resend pricing and current Free-tier quota](https://resend.com/pricing)
- [Resend domain verification overview](https://resend.com/docs/dashboard/domains/introduction)
