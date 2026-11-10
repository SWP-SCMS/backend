# Thuyết trình toàn bộ role Receptionist trong SCMS

**Nguồn chuẩn:** backend-refactor local, nguồn tại commit 9c08093505df43da557b7fa6ff91478f4bef770d, đối chiếu ngày 10/11/2026. Phạm vi là backend; các sơ đồ mô tả code đã đọc. Công thức là quy tắc phần mềm hiện tại, không phải quy định pháp luật hay chính sách suy đoán của trung tâm.

## 1. Cách giới thiệu vai trò lễ tân

“Receptionist là nhân sự vận hành tại quầy, kết nối hội viên với các dịch vụ của trung tâm. Lễ tân tiếp nhận và cập nhật hồ sơ, hỗ trợ mua gói và khóa học, thu tiền và đối soát, xác nhận khách ra vào, hỗ trợ đặt lịch, đồng thời tiếp nhận các yêu cầu bảo lưu, chấm dứt và hoàn tiền. Những quyết định cần kiểm soát như phê duyệt yêu cầu và xác nhận hoàn tiền cuối cùng thuộc Manager.”

Chia chức năng thành sáu nhóm để dễ nhớ:

1. **Tài khoản và hồ sơ:** đăng nhập, hồ sơ cá nhân, tạo/tìm/sửa hội viên, ảnh, reset mật khẩu, đề nghị kích hoạt thủ công.
2. **Gói hội viên và tài chính:** xem gói, tạo đơn, tiền mặt, QR SePay, đối soát, chứng từ, biên lai, lịch sử gói.
3. **Vận hành trung tâm:** check-in/check-out, danh sách khách đang ở trong trung tâm.
4. **Lịch tập:** danh mục buổi học nhóm/PT, đặt và hủy booking, danh sách chờ.
5. **Sau bán gói:** bảo lưu, kết thúc bảo lưu sớm, chấm dứt gói và hoàn tiền mặt.
6. **Khóa học:** đăng ký, thanh toán, enrollment, waitlist, phản hồi đổi lịch, chuyển khóa, rút khóa và chi hoàn tiền.

**Không thuộc quyền lễ tân:** tạo/quản trị tài khoản nhân viên, đình chỉ tài khoản, định giá gói, tạo/sửa lịch và phòng, phân công coach, chấm điểm/ghi nhận kết quả tập, phê duyệt bảo lưu/chấm dứt/hoàn tiền, xem báo cáo quản trị. Đặc biệt POST /reception/courses bị denyAll trong cấu hình hiện tại; không dùng tên file cũ để suy ra lễ tân được tạo khóa học.

Nguồn: [SecurityConfiguration.securityFilterChain](../../src/main/java/com/scms/backend/config/SecurityConfiguration.java#L51).

## 2. Phân biệt các khái niệm trước khi nói về code

### 2.1. Ba định danh rất dễ nhầm

- **accountId/memberAccountId:** UUID, khóa tài khoản trong accounts.
- **memberCode:** mã nghiệp vụ dạng MB-…, được lưu trong member_profiles.member_code. Nhiều API gọi biến này là memberId dù kiểu Java là String.
- **actorId/receptionistId:** UUID lấy từ JWT sub, chỉ người thực hiện thao tác. Không phải hội viên được phục vụ.

Ví dụ lễ tân R đặt lịch cho hội viên M: JWT sub = R; path memberId = UUID của M; Booking lưu M là người tập và R là người tạo. Audit ghi R là actor. Không truyền JWT của hội viên để “đóng vai” hội viên.

API hồ sơ, mua gói hội viên, receipts, freeze, termination, refund dùng mã MB-…; booking, khóa học, checkout dùng UUID. Phụ lục API ghi nguyên kiểu tham số controller để kiểm tra từng endpoint.

### 2.2. Gói hội viên và khóa học là hai miền khác nhau

**Membership:** Plan định nghĩa nhóm quyền lợi; Offer là sản phẩm bán với tên, giá và số ngày; Order ghi nhận ý định mua; Payment ghi nhận thanh toán; Membership là quyền sử dụng sau thanh toán; Receipt là biên lai.

**Course:** Course là khóa học; CourseRun là đợt mở cụ thể; CourseSession là từng buổi; CourseOrder là đơn mua; CourseReservation giữ chỗ tạm; CoursePayment thu tiền; CourseEnrollment ghi danh; course_enrollment_sessions phân bổ quyền học và giá trị tiền theo từng buổi.

**ClassSession/Booking:** lịch tập được đặt theo quyền lợi Membership. Không đồng nhất ClassSession với CourseSession hay Booking với Enrollment.

**CenterVisit:** lượt vào/ra trung tâm. **Attendance:** điểm danh tham dự buổi học, do Coach xử lý. Check-in không tự động đồng nghĩa đã được điểm danh.

## 3. Kiến trúc và đường đi chung của một request

Backend dùng Java 21, Spring Boot 4.1.1, Spring MVC, Spring Security OAuth2 Resource Server, JPA, JdbcTemplate, PostgreSQL, Flyway và Bean Validation theo pom.xml.

~~~mermaid
flowchart LR
    A[Frontend hoặc API client] --> B[SecurityFilterChain và JWT validator]
    B --> C[Controller: nhận DTO và JWT sub]
    C --> D[Service: quyền và quy tắc nghiệp vụ]
    D --> E[Repository theo nghiệp vụ (JPA hoặc JDBC)]
    E --> F[(PostgreSQL)]
    D --> G[AuditEvent]
    D --> H[Notification outbox]
    H --> I[Dispatcher tạo thông báo]
    D --> J[Response DTO]
    J --> A
~~~

Giải thích từng lớp:

- **SecurityConfiguration:** xác định endpoint công khai, role được phép và chính sách token.
- **Controller:** ánh xạ URL/HTTP method, đọc path/query/body/header, lấy UUID actor từ JWT, gọi service và trả HTTP status.
- **Request DTO:** mô tả dữ liệu đầu vào và validation. Ví dụ ReceptionMemberCreateRequest kiểm tra tên, email, điện thoại.
- **Service:** nơi quyết định thao tác có hợp lệ với trạng thái hiện tại hay không. Nhiều phương thức dùng @Transactional.
- **Entity + Repository:** ánh xạ bảng và truy vấn JPA. Các nghiệp vụ commerce/course dùng repository JDBC để giữ SQL và row mapping; service/component quyết định nghiệp vụ. Một số thành phần infrastructure như outbox và rate limiter vẫn sở hữu SQL của chúng.
- **Response DTO:** chỉ trả dữ liệu dành cho người dùng của API, không serialize cả entity một cách tùy ý.
- **GlobalExceptionHandler/SecurityProblemDetailHandler:** chuyển lỗi nghiệp vụ, validation, authentication và authorization thành phản hồi lỗi có cấu trúc.

Một transaction giúp tránh trạng thái “đã thu tiền nhưng chưa có gói”, hoặc “đã đổi trạng thái yêu cầu nhưng chưa ghi audit”. Việc gửi notification được tách qua outbox để ghi nhận sự kiện bền vững cùng giao dịch.

Nguồn: [GlobalExceptionHandler](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L93), [SecurityProblemDetailHandler](../../src/main/java/com/scms/backend/common/error/SecurityProblemDetailHandler.java#L17), [NotificationWriter.write](../../src/main/java/com/scms/backend/notification/NotificationWriter.java#L21), [NotificationOutboxDispatcher.dispatchAvailable](../../src/main/java/com/scms/backend/notification/NotificationOutboxDispatcher.java#L29).

## 4. Đăng nhập, phiên đăng nhập và quyền truy cập

**API:** POST /auth/login; POST /auth/refresh; POST /auth/logout; POST /auth/change-password.

**Luồng đăng nhập:** AuthenticationController.login → AuthenticationService.login → tìm account ACTIVE bằng email hoặc điện thoại → PasswordEncoder.matches kiểm tra BCrypt → khóa/đọc lại account để kiểm tra trạng thái và password → RefreshTokenService.issue → JwtTokenService.issue → trả AuthResponse và refresh cookie.

JWT chứa định danh và role. Access token theo cấu hình mặc định có TTL 15 phút; refresh token 7 ngày. Refresh token nằm trong cookie HttpOnly; refresh thực hiện rotation. Request nghiệp vụ dùng Authorization: Bearer ….

**Mỗi request:** JWT được kiểm tra chữ ký/thời hạn; ActiveAccountJwtValidator kiểm tra account còn ACTIVE và authVersion khớp DB. Sau đó cấu hình role và các guard trong service kiểm tra quyền nghiệp vụ. Biết URL hoặc sửa role trên frontend không cấp quyền backend.

**Đổi mật khẩu cá nhân:** kiểm tra mật khẩu hiện tại; mật khẩu mới hợp lệ và khác mật khẩu cũ; lưu BCrypt; thu hồi refresh token của phiên hiện tại; controller xóa cookie. Code changePassword không tăng authVersion nên không được khẳng định mọi access token cũ hết hiệu lực ngay.

**Reset mật khẩu cho hội viên** là luồng riêng ở phần 7, không phải đổi mật khẩu cá nhân.

Nguồn: [AuthenticationController](../../src/main/java/com/scms/backend/auth/AuthenticationController.java#L18), [AuthenticationService.login](../../src/main/java/com/scms/backend/auth/AuthenticationService.java#L34), [AuthenticationService.changePassword](../../src/main/java/com/scms/backend/auth/AuthenticationService.java#L60), [ActiveAccountJwtValidator.validate](../../src/main/java/com/scms/backend/auth/ActiveAccountJwtValidator.java#L24), [JwtTokenService](../../src/main/java/com/scms/backend/auth/JwtTokenService.java#L14), [RefreshTokenService](../../src/main/java/com/scms/backend/auth/RefreshTokenService.java#L17), [RefreshTokenCookieService](../../src/main/java/com/scms/backend/auth/RefreshTokenCookieService.java#L7), [AuthProperties](../../src/main/java/com/scms/backend/auth/AuthProperties.java#L8).

## 5. Tạo hội viên tại quầy

**Mục đích:** tạo tài khoản và hồ sơ cho khách đến trực tiếp. **API:** POST /reception/members, trả 201.

**Đầu vào:** fullName, phone, email, birthDate, profileImageUrl tùy chọn. DTO không cho client tự gửi role/status/password để quyết định các trường đó.

**Nghiệp vụ:** tên đúng quy tắc tên người; phone gồm 10 chữ số bắt đầu bằng 0; email hợp lệ; ngày sinh không ở tương lai và tuổi tối thiểu 16; email/điện thoại không trùng. Email được trim và lowercase, điện thoại được trim.

**Luồng code:**

1. ReceptionMemberController.createMember nhận @Valid ReceptionMemberCreateRequest và JWT.
2. ReceptionMemberService.createMember kiểm tra tên/ngày sinh và AccountIdentifierAvailability.ensureAvailableForRegistration.
3. Tạo Account với role MEMBER, status ACTIVE. Mật khẩu khởi tạo là số điện thoại, được BCrypt trước khi lưu.
4. accountRepository.saveAndFlush; nếu DB báo trùng, chuyển DataIntegrityViolationException thành lỗi định danh tương ứng.
5. Tạo MemberProfile, saveAndFlush để nhận memberCode.
6. Ghi AuditEvent ACCOUNT_CREATED với actor là lễ tân.
7. NotificationWriter.write đưa ACCOUNT_CREATED vào outbox cho hội viên.
8. Trả ReceptionMemberCreateResponse gồm UUID, mã hội viên, thông tin cơ bản, role và status.

**Bảng liên quan:** accounts, member_profiles, audit_events và notification_outbox. Việc tạo hội viên không tạo Membership hay thanh toán. Hội viên ACTIVE vẫn có thể chưa có quyền vào trung tâm.

**Ví dụ:** khách Nguyễn Minh An, sinh năm 2000, thông tin liên hệ chưa tồn tại → tạo account/hồ sơ → nhận mã MB-… → mua gói ở bước tiếp theo.

**Điểm trình bày:** tạo ở quầy đang kích hoạt trực tiếp ACTIVE; khác đăng ký online chờ xác minh email. Mật khẩu mặc định theo số điện thoại là hành vi code hiện tại; nên hướng dẫn hội viên đổi ngay sau lần đăng nhập đầu, không tuyên bố hệ thống bắt buộc đổi nếu không có cơ chế đó.

Nguồn: [ReceptionMemberController.createMember](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberController.java#L45), [ReceptionMemberService.createMember](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberService.java#L55), [ReceptionMemberCreateRequest](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberCreateRequest.java#L13), [AccountIdentifierAvailability](../../src/main/java/com/scms/backend/auth/AccountIdentifierAvailability.java#L10), [MemberProfile](../../src/main/java/com/scms/backend/account/MemberProfile.java#L18).

## 6. Tìm kiếm, xem/sửa hồ sơ và tải ảnh hội viên

**API:** GET /reception/members/search?memberId=… hoặc ?phone=…; GET/PATCH /reception/members/{memberId}/profile; POST /reception/members/{memberId}/profile/image.

**Tìm kiếm:** ReceptionMemberProfileService.search yêu cầu chính xác một trong memberId hoặc phone. Đây là tra cứu định danh, không phải tìm mờ theo tên hoặc danh sách toàn bộ hội viên. Chỉ hồ sơ MEMBER có account ACTIVE hoặc SUSPENDED nằm trong phạm vi hiển thị này.

Kết quả trả UUID, memberCode, tên, liên hệ, ảnh, trạng thái account và activeMembership. Tóm tắt gói chọn Membership ACTIVE đang nằm trong khoảng starts_at ≤ now < ends_at; bổ sung plan/offer, quyền lợi và cờ frozen theo ngày Việt Nam. Cờ frozen có thể được tính từ kỳ SCHEDULED đã đến ngày bắt đầu, không chỉ đợi scheduler chuyển ACTIVE.

**Cập nhật:** controller → updateProfile → kiểm tra actor → tìm profile → merge từng field → validator → kiểm tra trùng email/phone → cập nhật Account và MemberProfile → flush → trả hồ sơ mới.

MemberProfilePatchRequest có các hàm hasFullName/hasPhone/… để phân biệt trường không gửi với trường có gửi. Những trường hỗ trợ gồm họ tên, điện thoại, email, ngày sinh, ảnh, mục tiêu tập, người liên hệ khẩn cấp, giới tính, địa chỉ, ghi chú thể trạng, quan hệ và thời gian liên hệ khẩn cấp. Kiểm tra liên hệ khẩn cấp được thực hiện sau khi merge.

**Ảnh:** multipart field file → ProfileImageUploadService.uploadForReceptionist → ProfileImageStorage.store → replaceProfileImage → transaction hoàn tất mới dọn ảnh cũ; nếu giao dịch thất bại thì dọn ảnh mới. Điều này tránh xóa ảnh đang dùng trước khi DB ghi thành công.

**Lưu ý quyền:** nhìn thấy hội viên SUSPENDED không đồng nghĩa được check-in, bán gói hay đặt lịch cho họ. Từng service kiểm tra riêng. Lễ tân không đổi status account bằng PATCH hồ sơ.

Nguồn: [ReceptionMemberProfileService.search](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberProfileService.java#L55), [ReceptionMemberProfileService.updateProfile](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberProfileService.java#L78), [ReceptionMemberProfileService.activeMembership](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberProfileService.java#L141), [MemberProfilePatchRequest](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L8), [MemberProfileDetailsValidator](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileDetailsValidator.java#L10), [ProfileImageUploadController](../../src/main/java/com/scms/backend/member/ProfileImageUploadController.java#L17), [ProfileImageUploadService.uploadForReceptionist](../../src/main/java/com/scms/backend/member/ProfileImageUploadService.java#L51), [ProfileImageStorage](../../src/main/java/com/scms/backend/member/ProfileImageStorage.java#L5).

## 7. Reset mật khẩu hội viên và hỗ trợ kích hoạt thủ công

### 7.1. Reset mật khẩu

**API:** POST /reception/members/{memberId}/reset-password; memberId là mã MB-…; thành công 204.

ReceptionMemberController → ResetPasswordService.receptionistResetMember → kiểm tra lễ tân ACTIVE → tìm hội viên ACTIVE → khóa account → reset.

reset đặt password thành BCrypt của số điện thoại hiện tại, flush, RefreshTokenService.revokeAll cho hội viên và ghi audit PASSWORD_RESET. Không trả password hash. Không dùng endpoint này để reset Manager/Coach/Receptionist hay hội viên bị đình chỉ.

**Giới hạn phải nói đúng:** revokeAll là thu hồi refresh token; Account.changePassword chỉ đổi hash, nên không suy ra access token JWT cũ bị thu hồi tức thời.

Nguồn: [ResetPasswordService.receptionistResetMember](../../src/main/java/com/scms/backend/manager/ResetPasswordService.java#L49), [ResetPasswordService.reset](../../src/main/java/com/scms/backend/manager/ResetPasswordService.java#L57), [Account.changePassword](../../src/main/java/com/scms/backend/account/Account.java#L136).

### 7.2. Đề nghị kích hoạt tài khoản online chưa xác minh email

**API:** GET /reception/email-verification/pending-accounts?query=…; POST/GET /reception/email-verification/manual-activation-requests; GET /…/{requestId}.

**Điều kiện:** MEMBER đang INACTIVE, email_verification_required=true và registration_expires_at còn hạn. Tra cứu bằng member code, phone hoặc email; email trong response được che một phần.

**Luồng:** ReceptionEmailVerificationController → ManualActivationService.search/create → xác nhận đối tượng còn đủ điều kiện → rate limit tạo yêu cầu (10 lần/giờ theo actor) → khóa account → tạo email_verification_manual_activation_requests PENDING với lý do, phương thức xác minh, ghi chú bằng chứng và snapshot người tạo → audit → thông báo Manager.

Lễ tân chỉ xem danh sách/chi tiết yêu cầu **do chính mình tạo**. Manager quyết định approve/reject. Approve kiểm tra lại trạng thái/hạn, kích hoạt qua approveManualEmailVerificationBypass, kết thúc các challenge/session/outbox email cũ liên quan, ghi audit và thông báo hội viên. Tạo yêu cầu không tự kích hoạt tài khoản.

Nguồn: [ReceptionEmailVerificationController](../../src/main/java/com/scms/backend/emailverification/verification/ReceptionEmailVerificationController.java#L25), [ManualActivationService.create](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L66), [ManualActivationService.list](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L90), [ManualActivationService.approve](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L103), [ManualActivationCreateRequest](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationCreateRequest.java#L10).

## 8. Tư vấn gói, tạo đơn và gia hạn Membership

**API công khai:** GET /membership-offers và /membership-offers/{id}. **API lễ tân:** POST /reception/members/{memberId}/membership-orders; GET /…/membership-orders/pending.

**Đầu vào tạo đơn:** offerId. Không nhận giá từ client để quyết định số tiền phải trả.

**Luồng:** MembershipOrderController.createForReceptionist → MembershipOrderService.createForReceptionist → kiểm tra actor → tìm member theo code → prepareMember gọi MembershipLifecycleService.reconcile, khóa hội viên ACTIVE → tìm Offer ACTIVE thuộc Plan ACTIVE → create.

create kiểm tra hội viên đã có Membership RESERVED hoặc Order PENDING_PAYMENT chưa. Nếu có gói ACTIVE thì đơn mới mang fulfillmentMode RESERVED; nếu chưa có thì IMMEDIATE. Lưu orderNumber, member, actor, offer và snapshot tên gói/plan/giá/tiền tệ/số ngày.

**Kết quả:** Order PENDING_PAYMENT. Tạo order chưa tạo Payment, chưa tạo QR, chưa kích hoạt Membership và chưa bắt đầu cửa sổ 24 giờ. GET pending trả 200 nếu có, 204 nếu không có.

**Gia hạn:** trả tiền khi còn gói ACTIVE tạo Membership RESERVED; đây là gói chờ kích hoạt tiếp theo, không lập tức cộng số ngày vào gói đang dùng. MembershipLifecycleService quản lý hết hạn/kích hoạt kế tiếp. Khi RESERVED được tạo, starts_at/ends_at có thể chưa có; không gọi đó là dữ liệu lỗi.

**Chống trùng:** khóa account, kiểm tra trạng thái hiện tại và ràng buộc DB cho pending order. Quy tắc hiện tại không cho chất nhiều gói RESERVED hoặc nhiều đơn pending cùng hội viên.

Nguồn: [MembershipOfferController](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferController.java#L13), [MembershipOrderService.createForReceptionist](../../src/main/java/com/scms/backend/membership/order/MembershipOrderService.java#L67), [MembershipOrderService.create](../../src/main/java/com/scms/backend/membership/order/MembershipOrderService.java#L88), [MembershipOrder](../../src/main/java/com/scms/backend/membership/MembershipOrder.java#L19), [MembershipLifecycleService.reconcile](../../src/main/java/com/scms/backend/membership/MembershipLifecycleService.java#L30), [MembershipStatus](../../src/main/java/com/scms/backend/membership/MembershipStatus.java#L3).

## 9. Thu tiền mặt gói hội viên

**API chuẩn:** POST /reception/members/{memberId}/cash-payments; body CashPaymentRequest chứa offerId; header Idempotency-Key. Endpoint cũ /reception/membership-orders/{orderId}/cash đã deprecated và trả conflict.

**Nghiệp vụ:** lễ tân chỉ xác nhận sau khi đã nhận tiền thực tế. Backend không kết nối két tiền; nó ghi nhận việc nhân viên xác nhận tiền mặt.

**Luồng:**

1. PaymentController.cash → PaymentService.cash.
2. Kiểm tra lễ tân ACTIVE, request và key; tìm hội viên ACTIVE và khóa account.
3. Nếu key đã được actor dùng, kiểm tra hội viên khớp và trả kết quả cũ.
4. Reconcile vòng đời gói; khóa Offer/Plan đang bán; chặn pending order hoặc RESERVED trùng.
5. Nếu gia hạn khi đang có gói ACTIVE, bắt buộc Idempotency-Key.
6. Tạo trực tiếp một MembershipOrder CASH/PENDING_PAYMENT từ offer, rồi gọi PaymentFulfillmentService.fulfillCash.
7. Hoàn tất trong transaction: Payment PAID, Order PAID, Membership ACTIVE hoặc RESERVED, Receipt, audit PAYMENT_PAID và notification outbox.

**Điểm dễ nhầm:** luồng tiền mặt gói tạo order riêng từ offer. Không bắt đầu bằng tạo bank-transfer order rồi mặc định gọi tiền mặt trên order đó. Nếu đã có order pending thì phải xử lý/hủy order theo quy tắc trước.

**Idempotency:** gửi lại một lần thu cùng key để tránh thu hai lần ở backend. Không dùng lại key cho thao tác mua mới. Code cash hiện kiểm tra replay theo actor/key và member; không nên khẳng định nó so toàn bộ payload giống cơ chế course reconciliation.

Nguồn: [PaymentController.cash](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L39), [PaymentService.cash](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L66), [PaymentFulfillmentService.fulfillCash](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L252), [PaymentFulfillmentService.createArtifacts](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L367), [CashPaymentRequest](../../src/main/java/com/scms/backend/membership/payment/CashPaymentRequest.java#L6).

## 10. Chuyển khoản SePay và đối soát gói

### 10.1. Tạo QR và nhận webhook

**API:** POST /reception/members/{memberId}/membership-orders/{orderId}/payments/sepay.

SepayController.createForReceptionist → SepayService.createForReceptionist → xác nhận actor/member → createPayment khóa order đúng hội viên và PENDING_PAYMENT → kiểm tra cấu hình ngân hàng → tạo Payment BANK_TRANSFER/PENDING với snapshot tài khoản nhận và nội dung chuyển khoản → đặt expires_at = thời điểm tạo payment + 24 giờ → trả amount, nội dung, destination, QR và hạn.

Gọi lại có pending payment thì dùng payment/snapshot đã lưu. Đổi cấu hình ngân hàng sau này không được âm thầm đổi đích của giao dịch cũ.

Ngân hàng/SePay gọi POST /payments/sepay/webhook. Đây là callback công khai ở tầng JWT nhưng vẫn phải xác thực API key riêng. Không dùng JWT lễ tân để giả lập một callback thật trong lời giải thích.

PaymentFulfillmentService.processSepayWebhook kiểm tra ledger sự kiện, reference/content, tiền VND đúng số tiền, tài khoản nhận khớp snapshot, chiều giao dịch vào, trạng thái order/payment và cửa sổ thanh toán. Nếu hợp lệ → fulfillLocked → tạo Membership và Receipt, audit và notification. Sự kiện lặp không tạo thêm quyền lợi.

**HTTP 200 của webhook không đồng nghĩa PAID:** success=true có thể chỉ nghĩa sự kiện được ghi nhận bền vững; processed=false cần xem outcome và đối soát.

### 10.2. Đối soát thủ công và hủy đơn

**API:** GET /payments/reconciliation-queue; POST /payments/{paymentId}/reconcile; GET /payments/{paymentId}/result; PATCH /membership-orders/{orderId}/cancel.

Lễ tân/Manager xem queue, đối chiếu chứng từ và gửi PaymentActionRequest. PAID cần status, reason, evidence, receivedAmount, transferContent, providerTransactionId; amount/content phải khớp snapshot và transaction ngân hàng không được dùng cho giao dịch khác. Service gọi chung fulfillment để không nhân đôi logic cấp gói.

Chọn FAILED chỉ được khi Payment còn PENDING và cửa sổ thanh toán đã hết. Hủy order chỉ dành cho PENDING_PAYMENT, bắt buộc reason; payments PENDING thành FAILED và order thành EXPIRED, có audit. Trạng thái hủy order hiện tại là EXPIRED, không tự đặt tên CANCELLED cho MembershipOrder.

**Khác biệt đáng nhớ:** webhook Membership kiểm tra cửa sổ 24 giờ; đường reconcile PAID gọi fulfill và không có cùng điều kiện chặn hết giờ như Course reconciliation. Không trình bày hai miền có một chính sách hết hạn giống hệt nhau.

### 10.3. Ảnh bằng chứng thanh toán

GET/POST /payments/{paymentId}/evidence-images; GET /…/{evidenceId}. PaymentEvidenceService yêu cầu actor Receptionist/Manager ACTIVE; upload chỉ khi Payment PENDING. PNG/JPEG tối đa 4 MiB, tối đa 20 triệu pixel và 10 ảnh/giao dịch; kiểm tra nội dung ảnh thực; SHA-256 tránh lưu trùng cùng ảnh. Dữ liệu ảnh lưu payment_evidence_images; response đọc ảnh có no-store và nosniff.

Nguồn: [SepayService.createForReceptionist](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L80), [SepayService.createPayment](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L179), [SepayService.webhook](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L207), [PaymentFulfillmentService.processSepayWebhook](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L94), [PaymentService.reconcile](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L141), [PaymentService.cancelOrder](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L172), [PaymentEvidenceService](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceService.java#L22).

## 11. Biên lai và lịch sử Membership

**API:** GET /reception/members/{memberId}/receipts; GET /receipts/{receiptId}; GET /reception/members/{memberCode}/memberships.

PaymentService.receiptsForReceptionist tìm hội viên bằng code và thêm điều kiện payment.processed_by_account_id = actor khi actor là lễ tân. receipt cũng kiểm tra điều kiện đó khi xem một biên lai. Manager có phạm vi khác.

Do đó lễ tân **không mặc nhiên xem mọi biên lai của mọi lễ tân**. Payment do webhook tự động xử lý có thể không có processed_by là lễ tân đang đăng nhập; cần phân biệt khả năng đọc kết quả thanh toán với quyền đọc biên lai.

Lịch sử Membership cho lễ tân hỗ trợ hội viên ACTIVE hoặc SUSPENDED, có trạng thái, mốc thời gian, snapshot gói và quyền lợi. Snapshot giá/tên/số ngày lưu lại dữ liệu giao dịch; danh mục quyền lợi hiển thị được service tra theo plan code, không phải mọi dữ liệu trong response đều là snapshot bất biến.

Nguồn: [PaymentService.receiptsForReceptionist](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L214), [PaymentService.receipt](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L231), [PaymentService.result](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L248), [PaymentService.receptionMembershipHistory](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L273), [ReceiptResponse](../../src/main/java/com/scms/backend/membership/payment/ReceiptResponse.java#L8), [StaffMembershipResponse](../../src/main/java/com/scms/backend/membership/StaffMembershipResponse.java#L9).

## 12. Check-in, check-out và khách đang trong trung tâm

**API:** POST /reception/center-visits; GET /reception/center-visits/open; PATCH /reception/members/{memberAccountId}/center-visits/current/checkout.

**Đầu vào check-in:** đúng một memberId dạng MB-… hoặc phone, identityVerified=true; courseSessionId nếu vào học khóa; Idempotency-Key tùy chọn. Xác minh danh tính là hành động thật của lễ tân, không phải nhận diện khuôn mặt tự động.

**Luồng:** CenterVisitController.checkIn → CenterVisitService.checkIn → kiểm tra identityVerified → tra cứu qua ReceptionMemberProfileService → kiểm tra replay → account ACTIVE và có ảnh → khóa member, kiểm tra lại → chọn một trong hai căn cứ vào cửa.

- **Vào theo Membership:** không đang freeze; Membership ACTIVE đúng khoảng thời gian và có benefit CENTER_ACCESS.
- **Vào theo CourseSession:** Enrollment ACTIVE, enrollment session ENROLLED, run không CANCELLED, session hợp lệ; thời gian vào nằm trong [start − 30 phút, end + 30 phút). Nhánh này dùng quyền học khóa, không bắt buộc Membership CENTER_ACCESS.

Nếu còn lượt chưa checkout, hệ thống đóng lượt cũ với AUTO_REENTRY rồi tạo lượt mới. Ghi CenterVisit với actor, thời gian, source, membership hoặc enrollment/session và audit CENTER_VISIT_CREATED.

**Kết quả:** 201 nếu tạo mới, 200 nếu replay. Cùng key mà member hoặc courseSession khác thì conflict.

**Checkout:** nhận UUID member account → kiểm tra lễ tân → khóa member → đóng visit hiện tại, ghi người đóng/giờ đóng/source RECEPTIONIST, audit. Nếu không còn lượt mở nhưng có lượt đã đóng, trả lượt đóng gần nhất, tránh tạo thêm một lần checkout. GET open trả danh sách phân trang các lượt chưa có checkedOutAt.

**Câu nói cần nhớ:** “Account ACTIVE là điều kiện tài khoản; Membership/course entitlement là điều kiện sử dụng dịch vụ; identityVerified và ảnh là điều kiện xác minh tại quầy. Ba lớp này khác nhau.”

Nguồn: [CenterVisitService.checkIn](../../src/main/java/com/scms/backend/centervisit/CenterVisitService.java#L48), [CenterVisitService.checkoutReceptionist](../../src/main/java/com/scms/backend/centervisit/CenterVisitService.java#L135), [CenterVisitService.openVisits](../../src/main/java/com/scms/backend/centervisit/CenterVisitService.java#L142), [CenterVisitRepository.findCurrentMembership](../../src/main/java/com/scms/backend/centervisit/CenterVisitRepository.java#L15), [CenterVisitRepository.findEligibleCourseEnrollmentId](../../src/main/java/com/scms/backend/centervisit/CenterVisitRepository.java#L27), [CenterVisitRequest](../../src/main/java/com/scms/backend/centervisit/CenterVisitRequest.java#L5), [CenterVisit](../../src/main/java/com/scms/backend/centervisit/CenterVisit.java#L12), [CheckoutSource](../../src/main/java/com/scms/backend/centervisit/CheckoutSource.java#L3).

## 13. Xem lịch nhóm và PT; đặt/hủy booking thay hội viên

**Danh mục:** GET /reception/class-sessions và GET /reception/pt-sessions. ClassSessionService.receptionSchedule/staffPtSchedule trả các session SCHEDULED trong tương lai, lọc thời gian, phân trang; danh mục thường loại PT_1_1, danh mục PT chỉ lấy PT_1_1. Response có số đã đặt, sức chứa và yêu cầu benefit.

**Đặt:** POST /reception/members/{memberId}/bookings với UUID hội viên và body sessionId. **Lịch hội viên:** GET /reception/members/{memberId}/bookings. **Hủy:** PATCH /…/bookings/{bookingId}/cancel.

**Book flow:** BookingController.bookForMember → BookingService.bookForMember → đảm bảo actor ACTIVE RECEPTIONIST → book(actor, member, session).

Service lần lượt kiểm tra:

1. Member ACTIVE, không đang freeze.
2. SessionLifecycleService.catchUp cập nhật trạng thái theo thời gian, khóa session.
3. Session SCHEDULED và chưa bắt đầu.
4. Nếu PT_1_1, coach của session phải đúng CoachAssignment ACTIVE của hội viên.
5. Khóa discipline; tìm Membership có hiệu lực tại giờ bắt đầu và cung cấp đầy đủ benefit yêu cầu.
6. Chưa BOOKED session này, không overlap booking khác, số BOOKED nhỏ hơn capacity.
7. Tạo Booking BOOKED, lưu membership dùng để xác định quyền lợi, member và actor; audit BOOKING_CREATED, notification.

**Một Membership phải đáp ứng đủ bộ benefit**, không cộng tùy ý quyền lợi của nhiều gói để vượt điều kiện.

**Hủy:** kiểm tra member, booking thuộc member và BOOKED; khóa session/booking; nếu now > start − 2 giờ thì từ chối. Đúng mốc trước 2 giờ vẫn được phép. Đổi CANCELLED, source RECEPTIONIST, ghi audit và notification, gọi WaitlistPromotionService.promoteAvailable để lấp chỗ.

**Ví dụ:** buổi 18:00 thì 16:00 vẫn hủy được; 16:00:01 đã quá cửa sổ theo kiểm tra thời gian code. Đừng mô tả lễ tân có thể bỏ qua điều kiện vì “đặt hộ”.

Nguồn: [ClassSessionService.receptionSchedule](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L162), [ClassSessionService.futureCatalogue](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L193), [BookingService.bookForMember](../../src/main/java/com/scms/backend/scheduling/booking/BookingService.java#L84), [BookingService.book](../../src/main/java/com/scms/backend/scheduling/booking/BookingService.java#L79), [BookingService.cancel](../../src/main/java/com/scms/backend/scheduling/booking/BookingService.java#L132), [BookingRepository](../../src/main/java/com/scms/backend/scheduling/booking/BookingRepository.java#L16), [Booking](../../src/main/java/com/scms/backend/scheduling/Booking.java#L13), [CoachAssignmentRepository](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentRepository.java#L18).

## 14. Danh sách chờ của ClassSession

**API:** POST/DELETE /reception/members/{memberId}/class-sessions/{sessionId}/waitlist; GET /reception/class-sessions/{sessionId}/waitlist.

WaitlistService.joinForMember → kiểm tra actor → khóa session và member → session còn mở trong tương lai và **đã đủ chỗ** → validateEligibility tương tự booking: member ACTIVE, không freeze, không trùng/overlap, đúng coach PT và đủ benefit → tạo SessionWaitlist WAITING và audit.

Rời danh sách qua leaveForMember đổi CANCELLED với reason LEFT_WAITLIST. Khi session đã bắt đầu, các entry còn chờ được expire.

**Promotion:** có chỗ trống → chọn entry theo thứ tự trong hàng đợi → kiểm tra lại điều kiện tại thời điểm promote → đủ điều kiện thì tạo Booking và đổi PROMOTED; không đủ thì SKIPPED với reason, thông báo và thử người tiếp theo. Khóa member không lấy được thì dừng lượt để xử lý lại, không tùy tiện vượt qua người đang đứng trước.

**Khác waitlist khóa học:** SessionWaitlist tự tạo Booking khi promote; CourseWaitlist gửi INVITED rồi cần accept để tạo order và trả tiền.

Nguồn: [WaitlistService.joinForMember](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistService.java#L73), [WaitlistService.validateEligibility](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistService.java#L147), [WaitlistPromotionService.promoteAvailable](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionService.java#L56), [WaitlistPromotionCoordinator](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionCoordinator.java#L18), [SessionWaitlist](../../src/main/java/com/scms/backend/scheduling/SessionWaitlist.java#L12), [SessionWaitlistRepository](../../src/main/java/com/scms/backend/scheduling/waitlist/SessionWaitlistRepository.java#L14), [WaitlistStatus](../../src/main/java/com/scms/backend/scheduling/WaitlistStatus.java#L3).

## 15. Bảo lưu Membership và thu phí bảo lưu

**API lễ tân:** GET/POST /reception/members/{memberCode}/membership-freeze-requests; POST /reception/membership-freeze-requests/{requestId}/cash-payment; POST /…/{requestId}/end-early.

**Yêu cầu:** startDate, endDate và reason; ngày kết thúc sau ngày bắt đầu, ngày bắt đầu không trước ngày hiện tại theo Asia/Ho_Chi_Minh; có Membership ACTIVE; không có yêu cầu bảo lưu đang mở trùng.

**Trạng thái chính:** PENDING → Manager duyệt → APPROVED_PENDING_PAYMENT → lễ tân thu tiền → SCHEDULED → hệ thống đến ngày → ACTIVE → COMPLETED. Nhánh khác gồm REJECTED, WITHDRAWN, OBSOLETE. WITHDRAWN có endpoint tự rút của Member, không suy ra có endpoint rút thay của lễ tân.

**Luồng code:** MembershipFreezeController → MembershipFreezeService.createForReceptionist/create → khóa member, membership → tạo request và notify Manager. Manager approve tính phí từ snapshot gói. Lễ tân collectCash kiểm tra expectedVersion, trạng thái, membership còn ACTIVE và ngày bắt đầu hợp lệ; ghi membership_freeze_fee_payments + membership_freeze_periods SCHEDULED; chuyển request SCHEDULED.

**Công thức:** phí = floor(giá gói × số ngày bảo lưu × 20 / (số ngày gói × 100)). Số ngày = endDate − startDate, khoảng [startDate, endDate). Ví dụ 3.000.000 VND/30 ngày, bảo lưu 10 ngày → 200.000 VND.

**Hiệu lực:** MembershipFreezeExecutionService.reconcileDue/activate kích hoạt kỳ bảo lưu, hủy booking thuộc khoảng bảo lưu, checkout lượt vào theo Membership và kích hoạt promotion sau commit. Membership không có enum FROZEN; nó vẫn có thể ACTIVE, còn freeze là request/period riêng.

**Kết thúc:** đủ kỳ thì cộng creditedDays vào endsAt. Kết thúc sớm chỉ khi request ACTIVE, kiểm tra version và lấy số ngày thực từ actualStart đến ngày hiện tại, cộng đúng số ngày đã bảo lưu; request COMPLETED, period ENDED_EARLY. Không khôi phục tự động các booking đã hủy, cũng không có bước hoàn phí tự động trong endEarly.

Nguồn: [MembershipFreezeService.createForReceptionist](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L106), [MembershipFreezeService.collectCash](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L155), [MembershipFreezeService.fee](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L192), [MembershipFreezeService.endEarly](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L217), [MembershipFreezeExecutionService](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeExecutionService.java#L33), [MembershipFreezeRequest](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L18), [MembershipFreezeRequestStatus](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequestStatus.java#L3).

## 16. Chấm dứt Membership

**API lễ tân:** GET/POST /reception/members/{memberCode}/membership-termination-requests. Body có mode và reason.

- IMMEDIATE: kết thúc khi Manager phê duyệt.
- END_OF_CYCLE: ghi nhận kết thúc ở thời điểm endsAt theo vòng đời gói.

**Luồng:** MembershipTerminationService.createForReceptionist → kiểm tra actor, Member ACTIVE và Membership ACTIVE → chặn yêu cầu đang mở trùng → tạo PENDING → audit và thông báo Manager. Chưa thay đổi Membership ngay.

Manager approve kiểm tra version, request PENDING và membership ACTIVE; gán effectiveAt. IMMEDIATE gọi MembershipTerminationExecutionService.executeImmediate. Service xử lý freeze đang hoạt động, terminate gói hiện tại và xét kích hoạt RESERVED qua MembershipLifecycleService; checkout visit theo Membership, hủy future booking, kết thúc coach assignment ACTIVE, đánh dấu EXECUTED và thông báo. Promotion waitlist chạy sau commit.

**Không nhầm:** chấm dứt Membership không xóa account, không mặc nhiên trả tiền, và không nhất thiết làm hội viên mất toàn bộ quyền nếu có RESERVED được kích hoạt. Hoàn tiền là request riêng có điều kiện riêng. END_OF_CYCLE không đồng nghĩa “hủy mọi gia hạn dự trữ”.

Nguồn: [MembershipTerminationService.createForReceptionist](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L84), [MembershipTerminationService.approve](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L108), [MembershipTerminationExecutionService.executeImmediate](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationExecutionService.java#L67), [MembershipLifecycleService.terminateActiveAndActivateReserved](../../src/main/java/com/scms/backend/membership/MembershipLifecycleService.java#L74), [MembershipTerminationMode](../../src/main/java/com/scms/backend/membership/MembershipTerminationMode.java#L3).

## 17. Hoàn tiền Membership bằng tiền mặt

**API:** GET /reception/members/{memberCode}/refund-eligible-sources; POST /…/refund-requests; GET /reception/refund-requests; POST /reception/refund-requests/{requestId}/cash-disbursement.

**Nguồn hợp lệ trong luồng này:** payment CASH và PAID, có liên kết receipt/order/membership; Membership TERMINATED hoặc RESERVED; không có refund đang có hiệu lực trùng nguồn. Không mô tả luồng này hoàn cả mọi chuyển khoản vì create chặn method khác CASH.

**Tạo request:** paymentId, membershipId, requestedAmount > 0, reason và idempotencyKey trong body. MembershipRefundService khóa actor và member, kiểm tra replay/payload, khóa nguồn tiền và Membership, tính mức được hoàn, chặn số yêu cầu vượt trần rồi lưu REQUESTED.

**Công thức:**

- Hủy RESERVED: suggested = floor(giá gói × 80%).
- TERMINATED: remainingDays = max(0, ngày bắt đầu + duration gốc + creditedDays − ngày tính toán); phải còn ít nhất 30% duration gốc. suggested = floor(giá gói × remainingDays / duration gốc × 50%).
- Manager firstApproval tính lại theo ngày duyệt và approvedAmount không vượt requestedAmount hoặc mức suggested mới.

Ví dụ gói 3.000.000 VND/30 ngày, còn 12 ngày → 40% ≥ 30%, suggested 600.000 VND. Còn 8 ngày → không đạt ngưỡng. RESERVED giá 3.000.000 → trần 2.400.000 VND. Đây là ví dụ với creditedDays=0.

~~~mermaid
flowchart LR
    A[Lễ tân tạo REQUESTED] --> B[Manager FIRST_APPROVED]
    B --> C[Lễ tân trả tiền và ghi CASH_DISBURSED]
    C --> D[Manager xác nhận COMPLETED]
    D --> E[Credit note]
~~~

**Chi tiền:** MembershipRefundService.cashDisbursement chỉ chấp nhận FIRST_APPROVED, expectedVersion đúng và evidence có nội dung. Nó ghi nhận việc chi tiền, không phải tự chuyển tiền ngân hàng.

**Hoàn tất:** Manager finalApproval cần CASH_DISBURSED; nếu là hủy gói RESERVED thì cancelReserved; tạo credit_notes. Biên lai/payment gốc được giữ để truy vết, không xóa giao dịch cũ. Các bước có audit và notification đến bên cần xử lý.

Nguồn: [MembershipRefundService.refundEligibleSources](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L91), [MembershipRefundService.create](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L99), [MembershipRefundService.eligibleQuoteValues](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L265), [MembershipRefundService.cashDisbursement](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L168), [MembershipRefundService.finalApproval](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L183), [RefundRequest](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L18), [RefundRequestStatus](../../src/main/java/com/scms/backend/membership/RefundRequestStatus.java#L3).

## 18. Khóa học: danh mục, đơn mua, giữ chỗ và thanh toán

**Danh mục công khai:** GET /courses, /courses/{id}, /course-runs, /course-runs/{id}; ảnh khóa qua /course-images/{id}. Lễ tân tư vấn dựa trên đợt mở, học phí, lịch và chỗ còn.

**Tạo đơn:** POST /reception/members/{memberId}/course-orders, UUID hội viên, body courseRunId và Idempotency-Key bắt buộc ở service. GET /…/course-orders/{orderId} tiếp tục xem đơn.

**CourseOrderService.createForReceptionist → create:** khóa actor/member → replay theo actor/key/member/run → khóa CourseRun → expire hold đến hạn → run OPEN_FOR_REGISTRATION, Course PUBLISHED, trong khoảng mở đăng ký → chưa có enrollment/hold trùng → đếm chỗ dùng → kiểm tra xung đột lịch → tạo CourseOrder và CourseReservation HELD.

**Chỗ đã dùng** gồm enrollment ACTIVE/WITHDRAWAL_PENDING, reservation HELD còn hạn, lời mời waitlist INVITED còn hạn, transfer hold còn hạn. Khi accept từ waitlist, phép tính bù lại chỗ mời của chính người đó.

**Hạn giữ chỗ:** min(now + 15 phút, registrationClosesAt). Kiểm tra xung đột với Booking đã đặt và lịch của enrollment/held reservation thuộc đợt khác. Không cần mua một Membership mới chỉ để tạo CourseOrder; service dùng quyền khóa học riêng.

**Thanh toán:**

- POST /reception/members/{memberId}/course-orders/{orderId}/payments/cash.
- POST /reception/members/{memberId}/course-orders/{orderId}/payments/sepay.

SepayService kiểm tra actor/member, Idempotency-Key và order đúng chủ. Cash gọi PaymentFulfillmentService.fulfillCourseCash; bank tạo CoursePayment theo đúng hạn giữ chỗ, không mở thêm 24 giờ như Membership.

**Thành công:** CoursePayment PAID, CourseOrder PAID, CourseReservation ENROLLED, CourseEnrollment và các course_enrollment_sessions. Học phí được chia theo đơn vị buổi học bằng phép chia và phần dư số nguyên VND để tổng phân bổ bằng đúng học phí. Dữ liệu này làm căn cứ tính credit và refund sau này. Không suy ra có Receipt trong bảng receipts giống Membership; luồng Course trả enrollment và payment result riêng.

**Đối soát Course:** GET /course-payments/reconciliation-queue, GET /course-payments/{id}, GET /course-payments/{id}/result, POST /course-payments/{id}/reconcile. CourseReconciliationService kiểm tra key/payload và lưu response replay. Xác nhận PAID phải còn hold/cửa sổ hợp lệ, đúng amount/content/transaction; không hồi sinh reservation đã hết hạn. FAILED chỉ khi pending đã hết hạn.

Nguồn: [CourseCatalogController](../../src/main/java/com/scms/backend/course/catalog/CourseCatalogController.java#L17), [CourseOrderController](../../src/main/java/com/scms/backend/course/order/CourseOrderController.java#L20), [CourseOrderService.create](../../src/main/java/com/scms/backend/course/order/CourseOrderService.java#L110), [CourseOrderService.holdExpiresAt](../../src/main/java/com/scms/backend/course/order/CourseOrderService.java#L105), [SepayService.fulfillCourseCash](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L115), [PaymentFulfillmentService.fulfillCourse](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L218), [CourseReservationExpiryService](../../src/main/java/com/scms/backend/course/order/CourseReservationExpiryService.java#L14), [CourseReconciliationService.reconcile](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationService.java#L87).

## 19. Tra cứu đăng ký khóa và waitlist khóa học

**Enrollment:** GET /reception/members/{memberId}/course-enrollments và /…/{enrollmentId}; filter status, withdrawalEligible, phân trang.

ReceptionCourseEnrollmentService trả khóa/đợt học, trạng thái enrollment, tổng số buổi, số buổi còn lại, buổi tiếp theo, khả năng rút và mức refund gợi ý. Các lý do như ENROLLMENT_NOT_ACTIVE, COURSE_RUN_NOT_WITHDRAWABLE, FINAL_UNIT_ALREADY_STARTED, ELIGIBLE_WITHOUT_REFUND, ELIGIBLE_WITH_REFUND giúp lễ tân giải thích thay vì chỉ thấy nút bị khóa.

**Waitlist API dưới /reception/members/{memberId}:** GET /course-waitlists; POST/DELETE /course-runs/{runId}/waitlist; POST /course-waitlists/{waitlistId}/accept.

CourseWaitlistService.join kiểm tra lễ tân/member ACTIVE, run đang mở và chưa đóng đăng ký, đã đầy chỗ, không trùng enrollment/hold/waitlist → WAITING. Khi có chỗ, promote chọn người theo created_at/id, xét lại điều kiện và tạo INVITED, notify hội viên.

**Hạn lời mời:** min(thời điểm mời + 24 giờ, đóng đăng ký, buổi học sắp tới nếu có). Lễ tân accept thay hội viên trước hạn → CourseOrderService.createFromWaitlist → tạo order và hold thanh toán → waitlist ACCEPTED. Như vậy “có lời mời” hoặc “đã accept” chưa đồng nghĩa “đã đóng học phí”.

Rời hàng chờ đổi LEFT; lời mời quá hạn thành EXPIRED; run không còn phù hợp có thể CANCELLED. Luồng trả replay khi accept lặp sau ACCEPTED.

Nguồn: [ReceptionCourseEnrollmentService.list](../../src/main/java/com/scms/backend/course/enrollment/ReceptionCourseEnrollmentService.java#L35), [CourseEnrollmentController.receptionEnrollments](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentController.java#L36), [CourseReceptionActionsController](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L35), [CourseWaitlistService.join](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L71), [CourseWaitlistService.promote](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L161), [CourseWaitlistService.accept](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L131).

## 20. Hỗ trợ phản hồi thay đổi lịch và chuyển khóa

### 20.1. Phản hồi đổi lịch

**API dưới /reception/members/{memberId}:** GET /course-schedule-changes; POST /course-schedule-changes/{responseId}/response.

CourseScheduleChangeService.respond hỗ trợ đúng ba lựa chọn CONTINUE, REQUEST_WITHDRAWAL, REQUEST_TRANSFER. Kiểm tra actor/member, response thuộc enrollment của member, còn PENDING và chưa tới responseDeadline. Gửi lại cùng lựa chọn có thể replay; đổi sang lựa chọn khác sau khi đã trả lời gây conflict.

- CONTINUE: ghi đồng ý tiếp tục.
- REQUEST_WITHDRAWAL: kiểm tra withdrawalEligible, gọi CourseWithdrawalService.createForScheduleChange.
- REQUEST_TRANSFER: gọi CourseTransferService.requestFromScheduleChange.

Lễ tân ghi nhận lựa chọn của hội viên, không trực tiếp tự thay lịch buổi học.

### 20.2. Chấp nhận đề nghị chuyển khóa đã được Manager xử lý

**API:** GET /…/course-transfer-offers; POST /…/course-transfer-offers/{offerId}/accept với Idempotency-Key.

Manager lựa chọn/duyệt đợt đích và tạo TransferOffer/hold. Lễ tân chỉ hỗ trợ xem và accept. CourseTransferService.accept khóa actor, member, offer và runs; kiểm tra trạng thái OFFERED, hạn, credit chưa thay đổi và chỗ đợt đích.

- Học phí đích cao hơn credit: tạo difference order, offer PENDING_PAYMENT; sau trả phần chênh lệch mới completeTransfer.
- Không phải trả thêm: hoàn tất chuyển trực tiếp.
- Credit thừa: tạo refund obligation TRANSFER_CREDIT_EXCESS, đi vào luồng hoàn tiền khóa học.

Nguồn enrollment thành TRANSFERRED, target enrollment và phân bổ buổi học mới được tạo. Credit là phần giá trị buổi chưa dùng theo chính sách code, không lấy tùy ý từ học phí ban đầu. Hạn offer tối đa 24 giờ, còn bị giới hạn bởi đóng đăng ký và buổi tiếp theo.

Nguồn: [CourseScheduleChangeService.respond](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeService.java#L84), [CourseTransferService.requestFromScheduleChange](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L57), [CourseTransferService.accept](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L169), [CourseTransferService.completeTransfer](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L234), [CourseTransferPolicy](../../src/main/java/com/scms/backend/course/transfer/CourseTransferPolicy.java#L7).

## 21. Rút khỏi khóa học

**API:** POST /reception/members/{memberId}/course-enrollments/{enrollmentId}/withdrawal-requests; GET /…/course-withdrawal-requests và /…/{requestId}; POST /…/course-withdrawal-requests/{requestId}/cancel.

CourseWithdrawalService.createForReceptionist kiểm tra quyền, enrollment thuộc hội viên, trạng thái có thể rút, còn buổi chưa bắt đầu và không xung đột yêu cầu chuyển khóa đang mở. Tạo request PENDING và snapshot số buổi/giá trị chưa dùng/chính sách tại thời điểm yêu cầu; enrollment chuyển WITHDRAWAL_PENDING.

**Chính sách tự nguyện:** còn ít nhất một buổi chưa bắt đầu thì có thể rút. Nếu còn ≥ 70% tổng buổi, mức hoàn = floor(80% × giá trị phân bổ phần chưa dùng); dưới 70% vẫn có thể rút nhưng refund bằng 0. Không dùng “đã điểm danh hay chưa” thay cho mốc thời gian buổi học.

**Do thay đổi đáng kể từ trung tâm:** CourseWithdrawalPolicy bỏ điều kiện ngưỡng 70% của VOLUNTARY, nhưng vẫn áp dụng điều kiện còn buổi và công thức 80% phần giá trị chưa dùng. Nguồn cause và snapshot được lưu để Manager không tính lại tùy ý khi duyệt trễ.

Manager approve dùng snapshot đã chốt, chuyển enrollment WITHDRAWN, reservation WITHDRAWN và request APPROVED; tạo course_refund_obligations và đề nghị promotion waitlist sau commit. Việc duyệt rút chưa tự chi tiền. Manager reject hoặc lễ tân/hội viên cancel yêu cầu pending thì đưa enrollment lại ACTIVE theo kiểm tra trạng thái.

Ví dụ khóa 10 buổi, phân bổ đều 3.000.000 VND: còn 7 buổi → chưa dùng 2.100.000, tự nguyện hoàn 1.680.000; còn 6 buổi → tự nguyện vẫn rút được nhưng refund 0. Ví dụ chỉ minh họa phân bổ đều; dữ liệu thật dùng allocation đã lưu từng buổi.

Nguồn: [CourseWithdrawalService.createForReceptionist](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L67), [CourseWithdrawalService.approve](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L150), [CourseWithdrawalService.cancelForReceptionist](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L198), [CourseWithdrawalPolicy](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalPolicy.java#L6), [CourseWithdrawalApiModels](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalApiModels.java#L9).

## 22. Chi hoàn tiền khóa học

**API quầy:** GET /reception/course-refunds và /…/{refundId}; POST /reception/course-refund-obligations/{obligationId}/disburse. **Lịch sử theo hội viên:** GET /reception/members/{memberId}/course-refund-obligations và /…/{obligationId}.

Queue quầy chỉ lấy obligation APPROVED; có filter eligible và response rút gọn, không trả thông tin ngân hàng/liên hệ/bằng chứng như phần quản lý. Không đồng nhất refundId trong tên route với bảng refund_requests của Membership; đây là course refund obligation.

**Luồng:** PENDING_REVIEW → Manager APPROVED → lễ tân DISBURSEMENT_RECORDED → Manager verify chuyển qua PENDING_VERIFICATION rồi COMPLETED. Nhánh khác có REJECTED/FAILED/RETURNED theo state machine.

recordDisbursement nhận Idempotency-Key và body expectedVersion, amountVnd, method, reference, evidenceReference. method chỉ CASH hoặc BANK_TRANSFER. Service khóa actor, obligation và các dữ liệu liên quan; kiểm tra replay cùng actor/key/payload; trạng thái APPROVED; đúng version và số tiền bằng approvedAmountVnd.

**Điều kiện thời điểm chi:** thông thường CourseRun phải COMPLETED. Nguồn COURSE_RUN_CANCELLED, PAYMENT_EXCEPTION, TRANSFER_CREDIT_EXCESS được phép chi ngay theo quy tắc riêng; REDUCED_SESSION_COUNT có ngoại lệ khi run CANCELLED. Chỉ thấy APPROVED chưa đủ để kết luận được chi ngay.

Ghi nhận số tiền, người chi, phương thức, mã tham chiếu và bằng chứng; audit COURSE_REFUND_DISBURSED và notification Manager. Manager verify phải khớp số đã duyệt/đã chi/mã tham chiếu/bằng chứng. Endpoint này ghi nhận giao dịch hoàn thực tế; không có lệnh tự chuyển tiền ngân hàng trong recordDisbursement.

**So sánh để trả lời:** Membership refund của lễ tân đang chỉ hỗ trợ nguồn CASH và bốn bước REQUESTED/FIRST_APPROVED/CASH_DISBURSED/COMPLETED; Course refund có obligation từ nhiều nguyên nhân, hỗ trợ ghi nhận CASH/BANK_TRANSFER, điều kiện thời điểm chi và đối chiếu bằng chứng riêng.

Nguồn: [CourseRefundObligationService.listForReceptionist](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L106), [CourseRefundObligationService.recordDisbursement](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L162), [CourseRefundObligationService.verify](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L200), [CourseRefundStateMachine](../../src/main/java/com/scms/backend/course/refund/CourseRefundStateMachine.java#L7), [CourseRefundApiModels](../../src/main/java/com/scms/backend/course/refund/CourseRefundApiModels.java#L9), [CourseRefundSource](../../src/main/java/com/scms/backend/course/CourseRefundSource.java#L3).

## 23. Hồ sơ cá nhân lễ tân, đổi email và thông báo

**Hồ sơ:** GET/PATCH /staff/me/profile; POST/DELETE /staff/me/profile/image. StaffProfileService đọc Account của JWT; cập nhật tên, phone, ngày sinh và liên hệ khẩn cấp theo validator, kiểm tra trùng và ghi audit STAFF_SELF_PROFILE_UPDATED. Chỉ thay đổi thật mới ghi audit. Patch email sang giá trị mới bị chặn với EMAIL_CHANGE_VERIFICATION_REQUIRED.

**Đổi email:** dùng /staff/me/email-change (begin, verify, resend, cancel) hoặc luồng chung /accounts/me/email-change (có thêm GET status, PUT replace). AccountEmailChangeService kiểm tra mật khẩu hiện tại, email mới và OTP; sau verify đổi email, thu hồi refresh token, cập nhật cơ chế version xác thực và controller xóa cookie. Không nhầm với PATCH email hồ sơ hội viên ở luồng tiếp nhận.

**Quên mật khẩu bản thân:** luồng công khai /auth/password-reset/request, /verify, /confirm do PasswordResetController/PasswordResetService xử lý qua email; khác ResetPasswordService ở package manager dành cho nhân viên reset hội viên.

1. request nhận email, kiểm tra rate limit; phản hồi accepted chung, không tiết lộ email có account hay không. Với account ACTIVE hợp lệ và tính năng được bật, tạo/cập nhật password_reset_requests và enqueue OTP email.
2. verify kiểm tra OTP, hạn và số lần thử; tạo password_reset_sessions với token hash, trả cookie phiên reset. Phiên reset có TTL 10 phút.
3. confirm nhận newPassword cùng cookie; khóa request/session/account, chặn session đã dùng/hết hạn/thu hồi, kiểm tra mật khẩu mới khác cũ; BCrypt mật khẩu mới, revokeAll refresh token, đánh dấu session đã dùng và request COMPLETED, audit/notification. Controller xóa cookie reset. Cơ chế này cũng không tự tăng authVersion thông qua Account.changePassword.

**Thông báo:** GET /notifications?unreadOnly=…&page=…&size=…; PATCH /notifications/{id}/read; PATCH /notifications/read-all. NotificationService chỉ truy vấn/sửa notification thuộc account đang đăng nhập. Dùng cho các việc cần lễ tân tiếp tục như thu phí freeze sau duyệt hoặc chi refund sau first approval.

**Outbox:** service nghiệp vụ gọi NotificationWriter.write để enqueue cùng transaction. NotificationOutboxDispatcher xử lý sau; eventKey chống lặp. Không nói tất cả thông báo là email hay WebSocket: hệ thống này có notification center và cơ chế outbox; email xác minh có luồng riêng.

Nguồn: [StaffProfileController](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileController.java#L13), [StaffProfileService.updateProfile](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileService.java#L57), [StaffEmailChangeController](../../src/main/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeController.java#L19), [AccountEmailChangeController](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeController.java#L23), [AccountEmailChangeService.verify](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L120), [PasswordResetController](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetController.java#L18), [NotificationController](../../src/main/java/com/scms/backend/notification/NotificationController.java#L18), [NotificationService](../../src/main/java/com/scms/backend/notification/NotificationService.java#L14), [NotificationWriter](../../src/main/java/com/scms/backend/notification/NotificationWriter.java#L10).

## 24. Các dữ liệu và class phải nắm khi được hỏi sâu

### Nhóm identity

- Account, AccountRepository: tài khoản, role, status, mật khẩu, authVersion và khóa tài khoản khi thay đổi quan trọng.
- MemberProfile, MemberProfileRepository: mã hội viên, ảnh và hồ sơ bổ sung.
- ReceptionMemberCreateRequest/Response, ReceptionMemberSearchResponse, ReceptionActiveMembershipResponse: contract của tiếp nhận/tra cứu.
- MemberProfilePatchRequest/Response, MemberProfileDetailsValidator, AccountIdentifierAvailability: cập nhật có validation và chống trùng.
- ManualActivationCreateRequest/Response, ManualActivationService: quy trình đề nghị kích hoạt.

### Nhóm membership và tiền

- MembershipPlan, MembershipOffer, MembershipOrder, Membership, MembershipRepository: sản phẩm, đơn và quyền sử dụng theo thời gian.
- MembershipOrderService, MembershipLifecycleService: tạo đơn, gia hạn và vòng đời gói.
- PaymentService, SepayService, PaymentFulfillmentService: giao dịch, QR/callback, cấp quyền/biên lai.
- CashPaymentRequest, PaymentActionRequest, PaymentResultResponse, ReceiptResponse: dữ liệu thu tiền, đối soát và trả kết quả.
- MembershipFreezeRequest/Repository/Service/ExecutionService; MembershipTerminationRequest/Repository/Service/ExecutionService: request được duyệt và thực thi theo thời gian.
- RefundRequest/Repository, MembershipRefundService, RefundRequestCreateRequest, RefundCashDisbursementRequest: hoàn tiền Membership.
- PaymentEvidenceService: lưu và kiểm tra ảnh chứng từ qua SQL.

### Nhóm lịch và ra/vào

- ClassSession, ClassSessionRepository, ClassSessionService, SessionLifecycleService: session, danh mục và trạng thái thời gian.
- Booking, BookingRepository, BookingService, ReceptionBookingRequest: đặt lịch có kiểm tra quyền lợi và capacity.
- SessionWaitlist, SessionWaitlistRepository, WaitlistService, WaitlistPromotionService/Coordinator/Scheduler: xếp hàng và lấp chỗ.
- CenterVisit, CenterVisitRepository, CenterVisitService, CenterVisitRequest/Response: lượt vào ra theo Membership hoặc Course.
- CoachAssignmentRepository: dùng kiểm tra coach khi đặt PT; lễ tân không tự tạo assignment.

### Nhóm course

- Course, CourseRun, CourseSession và repositories: cấu trúc khóa, đợt và lịch.
- CourseOrderService, CourseReservationExpiryService: order và giữ chỗ có hạn.
- SepayService, PaymentFulfillmentService, CourseReconciliationService: thu tiền/đối soát/ghi danh.
- ReceptionCourseEnrollmentService, CourseEnrollmentController: tra cứu enrollment và khả năng rút.
- CourseReceptionActionsController: tập hợp các hành động lễ tân làm thay hội viên, rồi ủy quyền cho service nghiệp vụ tương ứng.
- CourseWaitlistService, CourseScheduleChangeService, CourseTransferService, CourseTransferPolicy: xếp hàng, phản hồi đổi lịch và chuyển khóa.
- CourseWithdrawalService/Policy/ApiModels, CourseRefundObligationService/StateMachine/ApiModels: rút khóa, tính nghĩa vụ hoàn và chi/xác minh.

Một số dữ liệu như payments, receipts, credit_notes, course_orders, course_enrollments được thao tác trực tiếp qua JdbcTemplate. Không cần và không nên bịa một entity/repository Java tương ứng nếu không tồn tại.

## 25. Transaction, khóa, idempotency và lỗi — giải thích bằng ví dụ

**Transaction:** một lần thu tiền cần nhiều bản ghi. Nếu ghi Membership lỗi thì transaction phải rollback để không để lại Payment PAID không có quyền lợi.

**Khóa hàng:** hai lễ tân cùng bán chỗ cuối khóa → cùng tranh khóa CourseRun; giao dịch sau tính lại số chỗ sau giao dịch trước. Booking dùng khóa ClassSession và kiểm tra capacity. Không chỉ disable nút trên frontend.

**Ràng buộc DB:** dù có kiểm tra trước, hai request vẫn có thể tranh chấp. Constraint và xử lý DataIntegrityViolationException bảo vệ trùng định danh/pending/booking ở lớp dữ liệu. Flyway định nghĩa các ràng buộc này qua migrations.

**expectedVersion:** màn hình đã đọc version 2 nhưng Manager/lễ tân khác sửa thành 3; gửi version 2 bị từ chối thay vì ghi đè kết quả mới. Người dùng cần tải lại dữ liệu rồi quyết định.

**Idempotency-Key:** request thành công nhưng mất mạng trước khi client nhận response; gửi lại cùng key giúp lấy kết quả cũ. Phạm vi key và kiểm tra payload khác nhau giữa check-in, cash Membership, CourseOrder, Course reconciliation và refunds; không mô tả chúng là cùng một cơ chế chung.

**Snapshot:** lưu giá, số ngày, đơn vị buổi học hoặc căn cứ hoàn tại thời điểm giao dịch/quyết định, tránh việc đổi danh mục hoặc duyệt trễ làm sai lịch sử.

**Audit:** ai làm, làm gì, tác động đối tượng nào, thời điểm và dữ liệu trước/sau hoặc lý do. Audit phục vụ truy vết; notification giúp người liên quan biết việc cần làm. Không phải mọi thao tác đọc/cập nhật đều tạo cả hai; xem service cụ thể.

**Lỗi khi demo:** 400 đầu vào sai; 401 xác thực không hợp lệ; 403 thiếu quyền; 404 không tìm thấy hoặc đối tượng không trong phạm vi; 409 xung đột trạng thái/version/trùng/chỗ đầy tùy exception; 503 có thể gặp khi SePay cấu hình không hợp lệ. Tên code lỗi và HTTP chính xác cần xem exception/handler, không chỉ dựa vào câu tiếng Việt hiển thị.

## 26. Schema và migration để mở khi giáo viên hỏi DB

Tất cả nằm tại src/main/resources/db/migration trong backend-refactor. Dùng các migration theo thứ tự, không đọc một migration cũ rồi kết luận đó là schema cuối.

- V1: identity và member profile; V71/V72: hồ sơ bổ sung và giới tính.
- V2: membership commerce; V7/V8/V9: payment window, SePay và integrity; V19/V20/V24: benefit và điều kiện booking; V27/V28/V39: vòng đời, RESERVED và hủy gói dự trữ.
- V3: scheduling/booking/attendance; V10/V11: center visit và liên hệ attendance; V56: check-in theo course.
- V4/V12/V14/V22: audit, notification, khóa sự kiện bền vững, outbox và snapshot actor.
- V31/V35: termination; V36/V37/V40/V47: freeze; V38: membership refunds.
- V51–V55: course catalog, orders/reservations, payments, enrollment và attendance.
- V57–V64: waitlist, đổi lịch, gián đoạn buổi, hủy khóa, chuyển khóa, rút khóa và refund.
- V67/V68: chính sách/snapshot rút khóa; V70: course reconciliation; V73: ảnh chứng từ; V75: phân bổ gốc cho course refund.

**Quan hệ cần vẽ khi được hỏi:** Account → MemberProfile; Account(Member) → MembershipOrder → Payment → Receipt; MembershipOrder → Membership; Member + Membership + ClassSession → Booking; Member + Membership hoặc CourseEnrollment → CenterVisit; CourseRun → CourseSession; CourseOrder → CourseReservation → CourseEnrollment → enrollment sessions; request freeze/termination/refund liên kết tới membership hoặc payment gốc.

## 27. Những câu tuyệt đối tránh nói sai

1. “Lễ tân được tạo khóa học”: POST /reception/courses bị chặn trong cấu hình hiện tại.
2. “memberId luôn là UUID”: sai với nhiều endpoint hồ sơ và Membership dùng code MB-….
3. “Tạo tài khoản là vào tập được”: còn quyền lợi, gói/khóa, trạng thái, ảnh và xác minh danh tính.
4. “Check-in là điểm danh”: CenterVisit và Attendance khác nhau.
5. “Tạo order là đã mua xong”: còn payment và fulfillment.
6. “Cash gói phải tạo bank order trước”: endpoint cash chuẩn tự tạo order từ offer.
7. “Mọi QR đều có 24 giờ”: Course bị hạn giữ chỗ tối đa 15 phút; Membership 24 giờ từ payment đầu tiên.
8. “Waitlist tự đăng ký giống nhau”: lớp tập promote thành Booking; khóa học cần accept và thanh toán.
9. “Freeze đổi Membership thành FROZEN”: enum Membership không có FROZEN.
10. “Chấm dứt là khóa tài khoản và hoàn tiền”: ba nghiệp vụ khác nhau.
11. “Hoàn tiền gói và khóa dùng cùng bảng/quy tắc”: chúng dùng request/obligation, công thức và trạng thái khác.
12. “Lễ tân xem được mọi receipt”: bị giới hạn bởi processed_by_account_id.
13. “Reset password lập tức hủy mọi JWT”: code reset thu hồi refresh token; changePassword không tăng authVersion.
14. “Webhook 200 nghĩa là đã thu tiền”: phải đọc processed/outcome/payment status.
15. “Đã đọc test nghĩa là đã chạy toàn bộ”: chỉ công bố phạm vi test thực chạy trong KIEM_CHUNG.md.

## 28. Cập nhật đã có trong nguồn 9c08093

Các chức năng trong mục này đã tồn tại trước cleanup. Cleanup chỉ đổi cách tổ chức Java nội bộ, không thêm quyền cho lễ tân hay thay công thức nghiệp vụ.

### Hủy booking khác với hủy cả buổi

Lễ tân hỗ trợ hủy booking của một hội viên theo điều kiện của `BookingService`. Manager hủy cả buổi qua `ClassSessionCancellationService#cancel`. Khi buổi còn booking `BOOKED`, service hủy buổi báo `SESSION_HAS_MEMBERS`; không được trình bày rằng hệ thống luôn tự hủy mọi booking để cho phép hủy buổi. Với course, `CourseSessionDisruptionService` cũng chặn thao tác hủy/xử lý buổi thay thế khi còn hội viên đang được ghi danh vào buổi. Hủy cả đợt course là một luồng khác, có closure/transfer/refund riêng.

Khi demo: dùng hai buổi riêng, một buổi trống để chứng minh hủy thành công và một buổi có hội viên để chứng minh bị chặn. Không dùng ca hủy booking của hội viên để kết luận API hủy cả buổi hoạt động giống nhau.

### Version và xử lý hàng loạt khi coach nghỉ

`ClassSessionAssignmentRequest` có thêm `expectedVersion`. API phân công đơn lẻ vẫn cho phép giá trị null để tương thích constructor/caller cũ; nếu truyền version thì phải đúng version hiện tại, kể cả khi lựa chọn phân công không làm thay đổi dữ liệu.

Luồng Manager: `CoachLeaveImpactController` → `CoachLeaveImpactService` đọc các buổi chịu ảnh hưởng → `CoachLeaveBatchController` → `CoachLeaveBatchService#resolve` xử lý lựa chọn. Đây là phần phối hợp Manager, không phải quyền tác nghiệp riêng của lễ tân.

- Chỉ xử lý yêu cầu nghỉ đang `PENDING`; chọn từ 1 đến 100 buổi.
- Mỗi lựa chọn cần `type`, `id`, `expectedVersion` không âm; không lặp buổi, không trộn loại buổi trong một lần gửi.
- Buổi được chọn phải còn thuộc danh sách ảnh hưởng và version phải khớp. Các buổi đổi sang cùng coach không được chồng giờ; coach thay thế phải phù hợp với toàn bộ nhóm.
- `available-coaches` lấy giao các coach phù hợp từng buổi. `resolve` khóa yêu cầu nghỉ và tài nguyên rồi mới xử lý theo thứ tự xác định.
- Hành động là `reassign` hoặc `cancel`; hủy cần lý do, đồng thời buổi không được còn hội viên. Các class thường dùng assignment/cancellation service hiện có; course dùng `CourseLeaveBatchActions`.
- Toàn batch nằm trong transaction. Một mục lỗi phải rollback cả batch; không diễn giải `processedCount` như kết quả thành công một phần.

Ca phản biện nên chuẩn bị: version cũ; gửi buổi không còn trong danh sách; trùng buổi; buổi còn hội viên; hai buổi chồng giờ; một mục cuối thất bại và kiểm tra các mục trước chưa bị ghi nhận.

### Training summary cho PT và course

`BookedSessionTrainingController` cung cấp GET/PUT cho hai đường dẫn:

- `/api/v1/coach/pt-sessions/{sessionId}/members/{memberId}/training-summary`
- `/api/v1/coach/course-sessions/{sessionId}/members/{memberId}/training-summary`

`BookedSessionTrainingService#get/save` kiểm tra tài khoản coach ACTIVE, đúng coach được phân công, đúng buổi và hội viên. Lễ tân không được ghi kết quả thay coach.

Ghi summary cần buổi đã kết thúc và trạng thái buổi/đợt nằm trong tập cho phép; hội viên vẫn phải có booking/enrollment hợp lệ. PT cần `trainingPlanVersionId` thuộc hội viên và quyền lợi TRAINING_RESULT/TRAINING_FEEDBACK tại thời điểm bắt đầu buổi. Course không nhận `trainingPlanVersionId`. `notes` và `feedback` bắt buộc, giới hạn 4.000 ký tự theo request DTO.

Service khóa session để các lần lưu cạnh tranh cập nhật cùng cặp result/feedback. Với course, thứ tự khóa là Run → Session → enrollment. Sau đó ghi result, feedback và audit trong transaction. GET có thể đọc kết quả đã tồn tại khi hội viên không còn enrolled, nhưng không cho ghi mới trong trường hợp đó. Response có `writable` để thể hiện khả năng ghi ở thời điểm đọc.

`CourseRosterEntry` có thêm `memberAccountId`; DTO result/feedback hỗ trợ `courseSessionId`. V77 thêm FK `course_session_id` vào `training_results`; ràng buộc cần ít nhất một reference phù hợp, không được đồng thời gắn class session và course session. Unique index `(course_session_id, member_account_id)` áp dụng khi course session khác null và `legacy_duplicate=false`. Cleanup không sửa V77.

## 29. Cách lần theo code sau cleanup

1. Tìm HTTP method và URL trong `docs/source-guide/API_INDEX.md`, hoặc `API_VA_CLASS_RECEP.md` cho phạm vi lễ tân.
2. Mở `Controller#method`; đọc request DTO, lấy actor từ JWT và lời gọi service.
3. Mở service entrypoint để thấy phân quyền bổ sung, transaction và thứ tự nghiệp vụ. Component được tách vẫn nằm trong package nghiệp vụ đó.
4. Mở repository JPA/JDBC để giải thích dữ liệu đọc/ghi và lock. Component nội bộ không tự mở transaction mới.
5. Kiểm tra error code ở exception và `GlobalExceptionHandler`; helper dựng response không phải advice mới.
6. Dùng `API_DTO_TESTS.md` để mở các test tham chiếu; bằng chứng test cuối nằm trong `KIEM_CHUNG.md`.

Ví dụ khi được hỏi thu tiền: `PaymentController` → `PaymentService`/`SepayService` → `PaymentFulfillmentService` → component cấp quyền lợi và repository thanh toán → audit/notification. Idempotency, thứ tự lock và transaction vẫn phải đọc tại service điều phối; không suy luận chỉ từ tên repository.

Ví dụ khi được hỏi rút khóa: controller → `CourseWithdrawalService` → policy/snapshot và repository → cập nhật enrollment/refund obligation → callback promotion sau commit. Khi được hỏi chuyển khóa hết hạn, mở overload `CourseTransferService#accept` và chú ý `noRollbackFor` thay vì cho rằng mọi exception đều rollback giống nhau.
