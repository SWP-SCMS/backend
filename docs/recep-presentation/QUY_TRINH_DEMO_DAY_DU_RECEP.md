# Quy trình demo toàn bộ chức năng Receptionist và giải thích nghiệp vụ

Nguồn: backend-refactor local, nguồn commit 9c08093505df43da557b7fa6ff91478f4bef770d. Ngày đối chiếu: 10/11/2026.

Đây là kịch bản để luyện và trình diễn, chưa phải biên bản chạy demo thành công. Tên thao tác diễn đạt theo chức năng backend; chưa xác nhận tên nút/màn hình frontend. Có thể thực hiện bằng frontend nếu đã tích hợp, hoặc bằng API client. Tất cả đường dẫn API bên dưới có prefix `/api/v1`.

## Cách dùng tài liệu

Mỗi mục D01–D28 có dữ liệu chuẩn bị, thao tác, kết quả cần chỉ cho người xem, logic nghiệp vụ, nhánh lỗi và lời thuyết trình. Những phần ghi **Manager** phải dùng phiên Manager riêng; **Hệ thống** là tác vụ nền/callback, không phải nút của lễ tân.

- Demo đầy đủ: dự kiến 90–120 phút nếu chạy các nhánh phụ và mở code; chuẩn bị sẵn trạng thái theo thời gian.
- Demo bảo vệ 25–30 phút: chọn đường đi ở phần cuối; dùng bảng trạng thái để giải thích các nhánh còn lại.
- Chỗ nào có “kỳ vọng” là tiêu chí cần kiểm tra khi chạy, không khẳng định đã chạy đạt.
- Tất cả tên, giá và số điện thoại minh họa phải thay bằng dữ liệu demo phù hợp. Không dùng chung một hội viên cho mọi nhánh thay đổi trạng thái.

## Chuẩn bị trước buổi demo

### Tài khoản và bộ dữ liệu

| Mã gợi nhớ | Dữ liệu cần có | Phục vụ phần nào |
|---|---|---|
| R1 | Receptionist ACTIVE, biết thông tin đăng nhập | Phiên chính |
| R2 | Receptionist ACTIVE khác R1 | Chứng minh giới hạn đọc receipt/yêu cầu |
| G1 | Manager ACTIVE, phiên đăng nhập riêng | Các bước duyệt; không dùng token này cho phần lễ tân |
| A | Khách mới, liên hệ chưa tồn tại, từ 16 tuổi; ảnh hợp lệ | Tạo hội viên → mua gói → vào cửa → đặt lớp |
| B | Hội viên ACTIVE, chưa có pending order/RESERVED | Chuyển khoản và đối soát |
| C | Hội viên có gói ACTIVE, chưa có yêu cầu bảo lưu mở | Bảo lưu |
| C-ACTIVE | Hội viên có kỳ bảo lưu ACTIVE thực sự | Kết thúc bảo lưu sớm |
| D | Hội viên có gói CASH, có booking tương lai/lượt vào; không có RESERVED nếu muốn thấy mất quyền | Chấm dứt ngay và hoàn tiền |
| D-RESERVED | Hội viên có gói CASH RESERVED chưa bị xử lý hoàn | Hoàn gói dự trữ |
| E | MEMBER INACTIVE, chờ xác minh email, chưa hết hạn đăng ký | Đề nghị kích hoạt thủ công |
| F1/F2 | Hội viên ACTIVE, đủ benefit, không trùng lịch | Tranh chỗ/waitlist lớp |
| K1/K2/K3 | Hội viên riêng cho các nhánh khóa học | Ghi danh, chuyển, rút; tránh xung đột dữ liệu |
| Offer | Plan và Offer ACTIVE, giá/số ngày rõ, có CENTER_ACCESS và benefit lớp cần demo | Bán gói/check-in/booking |
| S1 | Lớp SCHEDULED, tương lai, cách thời điểm hủy hơn 2 giờ, còn chỗ | Đặt/hủy lớp |
| S-FULL | Lớp đã đầy, có một booking hợp lệ có thể hủy | Đôn danh sách chờ |
| PT | Buổi PT tương lai; CoachAssignment ACTIVE đúng hội viên/coach | Đặt PT |
| Run-OPEN | Course PUBLISHED, run OPEN_FOR_REGISTRATION, còn hạn/chỗ, không trùng lịch hội viên | Mua khóa |
| Run-FULL | Run đầy chỗ, vẫn trong thời gian đăng ký | Waitlist khóa |
| Run-CHANGE | Có thay đổi lịch và response PENDING chưa hết hạn | Ba lựa chọn phản hồi |
| Transfer | Các offer do Manager tạo, còn hạn, đợt đích hợp lệ | Chuyển khóa có/không chênh lệch |
| Refund-READY | Course obligation APPROVED và disbursementEligible | Chi hoàn khóa ngay trong buổi demo |

**Các trạng thái không thể chờ live:** hết hạn 24 giờ, giữ chỗ 15 phút, ngày bắt đầu/kết thúc bảo lưu, run COMPLETED, gói hết hạn. Chuẩn bị fixture hoặc bản ghi thử nghiệm phù hợp từ trước, ghi rõ đó là dữ liệu đã chuẩn bị. Nếu chưa có, giải thích trạng thái và code; không trình bày như đã thực thi live.

**Email/SePay:** cần cấu hình môi trường thử nghiệm hoạt động nếu muốn trình diễn thực nhận OTP/webhook. Callback mẫu chỉ dùng tại môi trường thử nghiệm và phải gọi đúng cơ chế xác thực của nhà cung cấp. Trước buổi demo kiểm tra một lượt đăng nhập, đọc danh mục và gửi OTP thử nếu phần đó được dùng.

### Sổ ID phải giữ trong lúc demo

| Biến | Ý nghĩa/cách lấy |
|---|---|
| receptionistToken / managerToken | Hai token từ hai lần login; tách cookie jar/phiên trình duyệt |
| memberCode | Mã MB-… trong kết quả tạo/tìm hội viên; nhiều API đặt tên path là memberId nhưng nhận mã này |
| memberAccountId | UUID tài khoản; dùng booking, khóa học và checkout |
| offerId / membershipId | Sản phẩm đang bán / gói đã được cấp; không hoán đổi |
| orderId / paymentId / receiptId | Ba đối tượng khác nhau; lấy từ từng response |
| sessionId / courseSessionId / courseRunId | Buổi lớp tập / buổi khóa / đợt khóa; không hoán đổi |
| requestId / obligationId / version | Yêu cầu hoặc nghĩa vụ hoàn; lấy version mới ngay trước bước quyết định |
| Idempotency-Key | Tạo key riêng cho thao tác mới; gửi lại cùng thao tác mới dùng lại key |

Không hard-code các UUID minh họa thành dữ liệu thật. Giá phải đọc từ offer/run hoặc snapshot giao dịch; số tiền ở ví dụ dưới đây dùng để giải thích công thức.

## D01. Đăng nhập, refresh và chứng minh đúng quyền lễ tân

**Chuẩn bị:** R1 ACTIVE; tài khoản bị đình chỉ hoặc thông tin sai cho nhánh lỗi.

**Thao tác:**
1. Gọi `POST /auth/login` với thông tin R1. Chỉ role và thông tin tài khoản trong response; không chiếu token/mật khẩu lên màn hình chung.
2. Gọi một API lễ tân bằng access token, ví dụ tra cứu hội viên.
3. Gọi `POST /auth/refresh` với refresh cookie; kiểm tra nhận token mới và cookie được thay.
4. Dùng R1 gọi một endpoint chỉ dành Manager để chứng minh backend từ chối quyền. Không thực hiện bước Manager bằng R1.

**Logic:** xác thực mật khẩu đã hash, account ACTIVE → phát JWT và refresh token. Mỗi request kiểm tra JWT, trạng thái account/authVersion và role; service còn kiểm tra đối tượng/trạng thái nghiệp vụ. Refresh token được xoay khi sử dụng.

**Nhánh lỗi:** sai mật khẩu; thiếu/hết hạn token; sai role. Phân biệt lỗi xác thực với lỗi nghiệp vụ như lớp đầy.

**Lời nói:** “Sau đăng nhập, hệ thống biết ai đang thao tác. Role cho phép lễ tân vào API, còn service quyết định thao tác cụ thể có hợp lệ hay không.”

**Mở code:** [AuthenticationController#login](../../src/main/java/com/scms/backend/auth/AuthenticationController.java#L31) → [AuthenticationService#login](../../src/main/java/com/scms/backend/auth/AuthenticationService.java#L34); [ActiveAccountJwtValidator#validate](../../src/main/java/com/scms/backend/auth/ActiveAccountJwtValidator.java#L24); [SecurityConfiguration#securityFilterChain](../../src/main/java/com/scms/backend/config/SecurityConfiguration.java#L51).

## D02. Tạo hội viên mới tại quầy

**Thao tác:**
1. Nhập fullName, phone, email, birthDate; gọi `POST /reception/members`.
2. Chỉ response tạo mới, role MEMBER, status ACTIVE; lưu cả account UUID và mã MB-….
3. Tra cứu hồ sơ vừa tạo và chỉ ra chưa có gói tập.
4. Thử lại bằng email/điện thoại đã tồn tại để trình diễn chống trùng; dùng bộ dữ liệu lỗi riêng nếu cần.

**Logic:** tên/ngày sinh hợp lệ, tối thiểu 16 tuổi; điện thoại 10 số bắt đầu 0; email hợp lệ và định danh chưa trùng. Backend quyết định role/status. Tạo Account, MemberProfile, audit người tạo và notification. Mật khẩu khởi tạo hiện là số điện thoại được BCrypt; việc tạo tại quầy kích hoạt ACTIVE trực tiếp.

**Kết quả:** có tài khoản/hồ sơ, chưa có Membership, Payment hay quyền vào cửa. Không gọi ACTIVE của Account là “đã mua gói”.

**Nhánh lỗi:** dưới 16 tuổi, ngày sinh tương lai, số điện thoại/email sai hoặc trùng. Dữ liệu lỗi không được tạo dở account/profile.

**Lời nói:** “Đây là bước định danh khách hàng. Quyền sử dụng dịch vụ chỉ xuất hiện sau khi khách mua gói hoặc được ghi danh khóa học phù hợp.”

**Mở code:** [ReceptionMemberController#createMember](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberController.java#L45) → [ReceptionMemberService#createMember](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberService.java#L55) → AccountIdentifierAvailability.

## D03. Tìm kiếm, xem, sửa hồ sơ và tải ảnh hội viên

**Thao tác:**
1. `GET /reception/members/search` bằng memberId dạng MB-…; làm lại bằng phone. Mỗi lần chỉ gửi một tiêu chí.
2. `GET /reception/members/{memberCode}/profile` để xem đầy đủ.
3. `PATCH` cùng đường dẫn: sửa mục tiêu tập, địa chỉ, thông tin thể chất hoặc liên hệ khẩn cấp; đọc lại để xác nhận.
4. Tải ảnh qua `POST /reception/members/{memberCode}/profile/image`, multipart `file`; đọc lại ảnh/hồ sơ.
5. Sau khi mua gói ở D06/D07, mở lại search để chỉ activeMembership và cờ bảo lưu nếu có.

**Logic:** đây là tra cứu định danh, không phải tìm mờ theo tên. Search hiển thị MEMBER ACTIVE/SUSPENDED; tài khoản online chờ email có luồng D05. PATCH chỉ merge field được gửi; validation và kiểm tra trùng vẫn áp dụng. Liên hệ khẩn cấp cần thỏa điều kiện cặp tên/điện thoại. Upload ảnh phục vụ hồ sơ và điều kiện xác minh tại cửa.

**Nhánh lỗi:** gửi cả mã lẫn phone hoặc không gửi tiêu chí; sai mã; sửa liên hệ thành dữ liệu đã có; file ảnh không hợp lệ. Không áp giới hạn ảnh chứng từ D08 cho ảnh hồ sơ nếu chưa đối chiếu cấu hình ảnh hồ sơ.

**Lời nói:** “Tìm thấy một tài khoản không có nghĩa mọi thao tác với tài khoản đó đều được phép. Mua gói, booking và check-in tiếp tục có điều kiện riêng.”

**Mở code:** [ReceptionMemberProfileService#search](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberProfileService.java#L55), [ReceptionMemberProfileService#getProfile](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberProfileService.java#L72), [ReceptionMemberProfileService#updateProfile](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberProfileService.java#L78); [ProfileImageUploadService#uploadForReceptionist](../../src/main/java/com/scms/backend/member/ProfileImageUploadService.java#L51).

## D04. Reset mật khẩu hội viên

**Thao tác:** dùng hội viên demo ACTIVE, gọi `POST /reception/members/{memberCode}/reset-password`. Trong phiên hội viên riêng, kiểm tra đăng nhập với mật khẩu theo quy tắc reset hiện tại.

**Logic:** actor phải là lễ tân ACTIVE; đối tượng phải là hội viên hợp lệ. Service lấy điện thoại hiện tại làm mật khẩu mới, hash rồi lưu; thu hồi refresh token của hội viên. Đây là thao tác hỗ trợ tại quầy, khác đổi mật khẩu cá nhân và quên mật khẩu qua OTP.

**Kết quả/nhánh lỗi:** hội viên có thể đăng nhập theo mật khẩu reset; tài khoản sai role/trạng thái hoặc mã không tồn tại bị chặn. Không khẳng định mọi access token cũ mất hiệu lực ngay: đường đổi mật khẩu không tự tăng authVersion.

**Lời nói:** “Lễ tân hỗ trợ khôi phục quyền truy cập cho hội viên; mật khẩu không được lưu dạng rõ. Quy tắc mật khẩu reset trong bản code này là số điện thoại hiện tại.”

**Mở code:** [ReceptionMemberController#resetPassword](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberController.java#L38) → [ResetPasswordService#receptionistResetMember](../../src/main/java/com/scms/backend/manager/ResetPasswordService.java#L49), dù service nằm trong package manager.

## D05. Đề nghị kích hoạt thủ công cho hội viên chờ email

**Chuẩn bị:** E phải INACTIVE, email_verification_required=true và chưa hết hạn đăng ký. Không dùng hội viên vừa tạo ở D02 vì đã ACTIVE.

**Thao tác:**
1. Tìm pending account bằng mã/phone/email qua API ReceptionEmailVerificationController.
2. Tạo yêu cầu với memberId, reasonCode, reasonDetail, verificationMethod, evidenceNote; chọn enum theo DTO/API thực tế.
3. Xem danh sách theo status và chi tiết yêu cầu; chứng minh trạng thái PENDING.
4. **Manager:** xem và approve kèm decisionNote. Trở về lễ tân, tải lại yêu cầu và hồ sơ để thấy APPROVED/tài khoản được kích hoạt.
5. Với yêu cầu khác, demo Manager reject; tài khoản không vì thế tự được kích hoạt.

**Logic:** lễ tân ghi lại lý do và cách đã xác minh khách; Manager quyết định ngoại lệ bỏ qua bước xác minh email. Chặn yêu cầu pending trùng và yêu cầu hết hạn/đã quyết định; có rate limit. Lễ tân chỉ đọc danh sách/chi tiết yêu cầu do mình tạo. Sau approve hệ thống kết thúc các challenge/session xác minh cũ có liên quan.

**Lời nói:** “Lễ tân không tự bật tài khoản online chưa xác minh. Hệ thống tách người đề nghị và người phê duyệt để có căn cứ và người chịu trách nhiệm.”

**Mở code:** [ManualActivationService#search](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L53), [ManualActivationService#create](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L66), [ManualActivationService#list](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L90), [ManualActivationService#getForActor](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L97), [ManualActivationService#approve](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L103), [ManualActivationService#reject](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L130).

## D06. Tư vấn gói và bán Membership bằng tiền mặt

**Chuẩn bị:** A chưa có pending order/RESERVED; Offer/Plan ACTIVE.

**Thao tác:**
1. `GET /membership-offers`, xem chi tiết offer: tên, giá, thời hạn, quyền lợi.
2. Sau khi xác nhận nhận tiền, gọi `POST /reception/members/{memberCode}/cash-payments`, body `{ "offerId": "<UUID>" }`, key riêng.
3. Chỉ Payment PAID, Order PAID, membershipId và receiptId từ kết quả; xem gói ACTIVE và biên lai.
4. Gửi lại đúng request/key: kỳ vọng lấy kết quả cũ, không thêm giao dịch mới. Một lần mua khác phải dùng key mới.

**Logic:** luồng tiền mặt chuẩn nhận offerId và tự tạo order; không phải tạo order chuyển khoản trước. Giá/số ngày lấy từ backend và lưu snapshot. Sau thu tiền, fulfillment tạo Membership/Receipt và ghi audit/notification trong transaction. Nếu có gói ACTIVE, kết quả gia hạn là RESERVED; phần này demo riêng ở D09.

**Nhánh lỗi:** offer ngừng bán, member không ACTIVE, đang có order pending hoặc gói RESERVED. Endpoint cash theo order cũ đã deprecated không dùng làm đường đi thành công.

**Lời nói:** “Payment ghi tiền đã thu, Membership ghi quyền được cấp, Receipt ghi chứng từ. Ba đối tượng phải nhất quán, nên backend hoàn tất chúng trong cùng nghiệp vụ giao dịch.”

**Mở code:** [PaymentController#cash](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L39) → [PaymentService#cash](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L66) → [PaymentFulfillmentService#fulfillCash](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L252), [PaymentFulfillmentService#createArtifacts](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L367).

## D07. Mua Membership bằng chuyển khoản/QR

**Chuẩn bị:** B khác A, không có pending order/RESERVED; cấu hình SePay thử nghiệm hợp lệ.

**Thao tác:**
1. `POST /reception/members/{memberCode}/membership-orders` với offerId; chỉ PENDING_PAYMENT.
2. Đọc `/membership-orders/pending` để minh họa tiếp tục đơn chưa thanh toán.
3. `POST /reception/members/{memberCode}/membership-orders/{orderId}/payments/sepay`.
4. Chỉ số tiền, tài khoản nhận, nội dung chuyển khoản, QR và expiresAt. Gọi lại để kiểm tra dùng payment/snapshot đang pending.
5. **Hệ thống/nhà cung cấp:** gửi callback hợp lệ tại môi trường thử nghiệm. Đọc payment result để thấy PAID và Membership được cấp.

**Logic:** tạo order chưa cấp gói, chưa bắt đầu cửa sổ thanh toán. Cửa sổ 24 giờ bắt đầu khi tạo payment đầu tiên. Callback kiểm tra khóa API, chiều tiền vào, VND, đúng tiền, nội dung/reference và tài khoản nhận đã snapshot; chống sự kiện/giao dịch trùng trước khi fulfillment.

**Nhánh lỗi:** thiếu/sai tiền, nội dung hoặc đích nhận; callback lặp; hết cửa sổ. HTTP 200 của webhook không đủ chứng minh đã PAID: xem processed/outcome và payment result.

**Lời nói:** “QR chỉ là chỉ dẫn thanh toán. Quyền tập chỉ được cấp sau khi backend xác nhận giao dịch hợp lệ; thao tác quét QR không tự đổi trạng thái gói.”

**Mở code:** [MembershipOrderService#createForReceptionist](../../src/main/java/com/scms/backend/membership/order/MembershipOrderService.java#L67); [SepayService#createForReceptionist](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L80)/[SepayService#createPayment](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L179); [PaymentFulfillmentService#processSepayWebhook](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L94).

## D08. Chứng từ, hàng đợi đối soát, xác nhận và hủy đơn gói

**Chuẩn bị:** các payment BANK_TRANSFER PENDING khác nhau: một chờ đối soát đúng, một đã quá hạn, một order cần hủy.

**Thao tác:**
1. Mở `GET /payments/reconciliation-queue`, lọc/trang theo API.
2. Khi payment còn PENDING, upload ảnh qua `/payments/{paymentId}/evidence-images`; liệt kê và mở từng ảnh.
3. Đối chiếu thực tế, gọi `/payments/{paymentId}/reconcile` với PAID, reason, evidence, receivedAmount, transferContent, providerTransactionId đúng giao dịch.
4. Đọc `/payments/{paymentId}/result` để chứng minh kết quả cuối; không chỉ nhìn thông báo gửi request.
5. Với payment quá hạn còn PENDING, trình diễn chọn FAILED đúng điều kiện.
6. Với order pending khác, `PATCH /membership-orders/{orderId}/cancel` kèm reason; thấy order EXPIRED và payment pending thành FAILED.

**Logic:** đối soát là xác nhận có căn cứ, không sửa giá hay đánh dấu tùy ý. Transaction ngân hàng không được dùng cho hai payment. Ảnh chỉ tải lên lúc PENDING; PNG/JPEG tối đa 4 MiB, 20 triệu pixel, 10 ảnh/payment; kiểm tra nội dung và hash chống trùng. Luồng Membership reconcile PAID không có cùng chặn hold hết hạn như Course.

**Nhánh lỗi:** ảnh quá giới hạn; amount/content không khớp; transaction đã dùng; đánh FAILED quá sớm; hủy đơn đã trả tiền. Chứng từ được lưu không đồng nghĩa payment đã được duyệt.

**Lời nói:** “Lễ tân đối chiếu bằng chứng với snapshot đơn hàng. Đối soát hợp lệ vẫn gọi chung logic cấp gói để tránh hai cách xử lý tạo kết quả khác nhau.”

**Mở code:** PaymentEvidenceService; [PaymentService#reconcile](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L141), [PaymentService#cancelOrder](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L172), [PaymentService#reconciliationQueue](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L198), [PaymentService#result](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L248).

## D09. Biên lai, lịch sử gói và gia hạn

**Thao tác:**
1. R1 xem `/reception/members/{memberCode}/receipts`, mở `/receipts/{receiptId}` của payment R1 đã xử lý.
2. Xem `/reception/members/{memberCode}/memberships`: trạng thái, giá/tên/thời hạn snapshot, ngày hiệu lực.
3. Với hội viên có gói ACTIVE và chưa có RESERVED/pending, mua thêm một gói theo cash hoặc QR; dùng key mới ở cash.
4. Chỉ gói cũ ACTIVE và gói mới RESERVED. Thử mua thêm để chứng minh chặn xếp nhiều gói dự trữ.
5. Nếu có fixture gói cũ hết hạn, chỉ việc hệ thống reconcile kích hoạt gói chờ. Không đợi thời hạn thật trong buổi trình diễn.

**Logic:** gia hạn là quyền dùng tiếp theo, không lập tức cộng vào endsAt gói đang dùng. RESERVED có thể chưa có startsAt/endsAt. Biên lai lễ tân bị giới hạn theo payment.processed_by_account_id; R2 không mặc nhiên đọc được receipt R1. Payment tự xử lý qua webhook có thể không gắn R1 là người xử lý, nên dùng payment result để xác nhận thanh toán.

**Lời nói:** “Lịch sử giúp phân biệt gói đang dùng, gói chờ và giao dịch đã xảy ra. Quyền đọc biên lai còn gắn với nhân viên xử lý tiền, không chỉ với mã hội viên.”

**Mở code:** [PaymentService#receiptsForReceptionist](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L214), [PaymentService#receipt](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L231), [PaymentService#receptionMembershipHistory](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L273); [MembershipLifecycleService#reconcile](../../src/main/java/com/scms/backend/membership/MembershipLifecycleService.java#L30).

## D10. Check-in, danh sách đang ở trung tâm và check-out

**Nhánh vào theo Membership:**
1. Tra A, đối chiếu người thật/ảnh; gọi `POST /reception/center-visits` với đúng một memberId dạng MB-… hoặc phone, `identityVerified=true`, key riêng.
2. Kiểm tra visit có source MEMBERSHIP, thời điểm và người tạo. Tạo mới kỳ vọng 201; replay cùng key kỳ vọng 200 và cùng visit.
3. Mở `GET /reception/center-visits/open` để thấy A trong danh sách.
4. Check-in lại bằng key mới khi còn lượt mở: chỉ lượt cũ được đóng AUTO_REENTRY và có lượt mới.
5. Checkout: `PATCH /reception/members/{memberAccountId}/center-visits/current/checkout`; lúc này path nhận UUID. Tải lại danh sách open, A không còn ở trong đó.

**Nhánh vào học khóa:** dùng K có Enrollment ACTIVE và buổi học đúng thời gian, thêm courseSessionId vào check-in. Kỳ vọng source COURSE; có thể dùng hội viên không có Membership CENTER_ACCESS để chứng minh entitlement khóa độc lập.

**Logic:** account ACTIVE, có ảnh và xác nhận danh tính; sau đó kiểm tra căn cứ sử dụng dịch vụ. Membership phải ACTIVE, còn hạn, có CENTER_ACCESS và không bảo lưu. Khóa phải có enrollment/session hợp lệ; cửa sổ vào `[giờ bắt đầu − 30 phút, giờ kết thúc + 30 phút)`. Checkout lặp trả lượt đóng gần nhất theo điều kiện code, không tạo một lượt vào mới.

**Nhánh lỗi:** identityVerified=false; thiếu ảnh; gói hết hạn/thiếu quyền/bảo lưu; account SUSPENDED; check-in khóa quá sớm/quá muộn; dùng lại key cho người hoặc buổi khóa khác.

**Lời nói:** “Check-in ghi nhận người đang ở trung tâm. Nó không thay cho điểm danh tham gia một buổi học của Coach, và ACTIVE của tài khoản chưa đủ để vào cửa.”

**Mở code:** [CenterVisitService#checkIn](../../src/main/java/com/scms/backend/centervisit/CenterVisitService.java#L48) → CenterVisitRepository; [CenterVisitService#checkoutReceptionist](../../src/main/java/com/scms/backend/centervisit/CenterVisitService.java#L135); [CenterVisitService#openVisits](../../src/main/java/com/scms/backend/centervisit/CenterVisitService.java#L142).

## D11. Xem lịch nhóm/PT, đặt và hủy lịch thay hội viên

**Thao tác:**
1. Mở `/reception/class-sessions`, lọc thời gian/lớp và phân trang; chỉ thời gian, coach, capacity và quyền lợi yêu cầu.
2. Mở `/reception/pt-sessions` để phân biệt PT với lớp nhóm.
3. Chọn S1 hợp lệ; `POST /reception/members/{memberAccountId}/bookings`, body sessionId.
4. Xem `GET /reception/members/{memberAccountId}/bookings`; chỉ BOOKED và đúng người tập.
5. Đặt một PT bằng hội viên có assignment đúng coach; dùng người không đúng assignment cho nhánh lỗi.
6. Hủy S1 trước mốc 2 giờ: `PATCH /reception/members/{memberAccountId}/bookings/{bookingId}/cancel`; tải lại thấy CANCELLED.

**Logic trước khi book:** actor lễ tân ACTIVE; member ACTIVE, không bảo lưu; session SCHEDULED/chưa bắt đầu; một Membership có hiệu lực tại giờ bắt đầu đáp ứng đủ benefit; PT đúng assignment; chưa BOOKED trùng, không overlap và còn chỗ. Service khóa session và tính lại chỗ trong transaction.

**Logic hủy:** booking thuộc đúng hội viên, còn BOOKED; chặn nếu thời điểm hiện tại sau start−2 giờ. Buổi 18:00: 16:00 vẫn được, 16:00:01 bị chặn. Khi hủy, ghi nguồn RECEPTIONIST và thử đôn waitlist.

**Nhánh lỗi:** trùng booking/trùng giờ, thiếu benefit, gói hết trước giờ học, đầy chỗ, sai coach, hủy sát giờ. Lễ tân đặt hộ vẫn chịu điều kiện, không có quyền bỏ qua chúng.

**Lời nói:** “Người thao tác là lễ tân lấy từ JWT, người sử dụng buổi học là hội viên lấy từ path. Backend lưu được cả hai để truy vết.”

**Mở code:** [ClassSessionService#receptionSchedule](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L162)/[ClassSessionService#staffPtSchedule](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L185); [BookingService#bookForMember](../../src/main/java/com/scms/backend/scheduling/booking/BookingService.java#L84) → [BookingService#book](../../src/main/java/com/scms/backend/scheduling/booking/BookingService.java#L79); [BookingService#cancelForMember](../../src/main/java/com/scms/backend/scheduling/booking/BookingService.java#L137) → [BookingService#cancel](../../src/main/java/com/scms/backend/scheduling/booking/BookingService.java#L132).

## D12. Danh sách chờ lớp tập và tự động đôn chỗ

**Chuẩn bị:** S-FULL đã đầy, hội viên chờ đủ quyền lợi và không trùng giờ; còn thời gian hủy booking cũ.

**Thao tác:**
1. `POST /reception/members/{memberAccountId}/class-sessions/{sessionId}/waitlist` để thêm F2.
2. `GET /reception/class-sessions/{sessionId}/waitlist`; chỉ F2 WAITING.
3. Hủy booking của F1 theo D11. Tải lại waitlist và bookings F2: kỳ vọng F2 PROMOTED, có BOOKED nếu còn đủ điều kiện.
4. Với một entry khác, dùng `DELETE` endpoint waitlist để rời hàng chờ và thấy CANCELLED.
5. Nếu muốn demo SKIPPED, chuẩn bị entry sau đó mất điều kiện hợp lệ; khi có chỗ hệ thống bỏ qua có lý do rồi xét tiếp.

**Logic:** chỉ tham gia waitlist khi lớp đầy; khi promote vẫn kiểm tra lại trạng thái member, gói/benefit, freeze, lịch và PT assignment. Xếp hàng không bảo đảm quyền tập nếu điều kiện thay đổi. Khi không lấy được khóa member, lượt xử lý có thể chờ lại để giữ thứ tự; session đã bắt đầu thì entry còn chờ hết hiệu lực.

**Lời nói:** “Waitlist lớp tập là cơ chế tự lấp chỗ: người hợp lệ được tạo Booking. Không có bước mua một khóa học mới trong luồng này.”

**Mở code:** [WaitlistService#joinForMember](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistService.java#L73)/[WaitlistService#leaveForMember](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistService.java#L107)/[WaitlistService#sessionList](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistService.java#L131); [WaitlistPromotionService#promoteAvailable](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionService.java#L56); [WaitlistPromotionCoordinator#promote](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionCoordinator.java#L42).

## D13. Yêu cầu bảo lưu, thu phí và kết thúc sớm

**Dữ liệu:** C có Membership ACTIVE; C-ACTIVE có kỳ bảo lưu đang ACTIVE để demo kết thúc sớm. Chọn các ngày theo Asia/Ho_Chi_Minh, không dùng ngày quá khứ cho yêu cầu mới.

**Thao tác:**
1. Lễ tân xem và tạo `/reception/members/{memberCode}/membership-freeze-requests`: startDate, endDate, reason. Chỉ PENDING; chưa bảo lưu ngay.
2. **Manager:** duyệt yêu cầu, lấy version mới. Chỉ APPROVED_PENDING_PAYMENT và feeAmount.
3. Lễ tân nhận phí rồi gọi `/reception/membership-freeze-requests/{requestId}/cash-payment` với expectedVersion mới. Chỉ SCHEDULED và thông tin đã thu.
4. **Hệ thống:** đến ngày bắt đầu chuyển kỳ bảo lưu sang ACTIVE; dùng fixture phù hợp để chỉ booking bị hủy/lượt vào theo gói được checkout.
5. Với C-ACTIVE, gọi `/reception/membership-freeze-requests/{requestId}/end-early` cùng version mới; chỉ COMPLETED, period ENDED_EARLY và hạn gói được cộng ngày thực tế.
6. Dùng bản ghi khác để giải thích hết kỳ tự hoàn tất và cộng ngày bảo lưu.

**Trạng thái:** `PENDING → APPROVED_PENDING_PAYMENT → SCHEDULED → ACTIVE → COMPLETED`. Có nhánh Manager REJECTED. Rút yêu cầu bảo lưu là endpoint của Member; không trình diễn như chức năng rút hộ của lễ tân.

**Phí:** `floor(giá gói × số ngày bảo lưu × 20 / (số ngày gói × 100))`. Khoảng ngày `[startDate, endDate)`; gói 3.000.000/30 ngày, bảo lưu 10 ngày → 200.000 VND.

**Logic:** không tạo yêu cầu mở trùng; ngày kết thúc sau ngày bắt đầu; trạng thái/version hợp lệ. Membership vẫn có thể ACTIVE, freeze là request/period riêng, không có trạng thái Membership FROZEN. Kết thúc sớm chỉ cộng ngày thực đã bảo lưu; không tự hoàn phí hoặc khôi phục booking đã hủy.

**Nhánh lỗi:** thu trước duyệt, version cũ, ngày bắt đầu đã qua lúc thu, kết thúc sớm khi mới SCHEDULED. Bước thu replay khi đã SCHEDULED có thể trả kết quả thu cũ; không khẳng định mọi lần gọi đều tạo khoản thu.

**Lời nói:** “Bảo lưu là tạm dừng sử dụng có khoảng thời gian và phí. Lễ tân tiếp nhận/thu tiền, Manager duyệt, hệ thống thực thi hiệu lực theo ngày.”

**Mở code:** [MembershipFreezeService#createForReceptionist](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L106), [MembershipFreezeService#collectCash](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L155), [MembershipFreezeService#fee](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L192), [MembershipFreezeService#endEarlyForReceptionist](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L186); [MembershipFreezeExecutionService#reconcileDue](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeExecutionService.java#L77).

## D14. Yêu cầu chấm dứt gói ngay hoặc cuối chu kỳ

**Thao tác:**
1. Dùng D, xem/tạo `/reception/members/{memberCode}/membership-termination-requests`, mode IMMEDIATE, reason. Chỉ PENDING và gói vẫn ACTIVE.
2. **Manager:** duyệt với version mới. Tải lại request, gói, bookings và lượt vào; chỉ các thay đổi sau thực thi.
3. Dùng hội viên khác tạo END_OF_CYCLE; giải thích gói còn dùng đến mốc hiệu lực, tác vụ nền xử lý khi tới hạn.
4. Dùng yêu cầu khác cho nhánh reject; việc bị từ chối không chấm dứt gói.

**Logic:** không có yêu cầu mở trùng. IMMEDIATE sau duyệt xử lý freeze liên quan, kết thúc gói, xét kích hoạt RESERVED, checkout lượt vào theo Membership, hủy future booking và kết thúc coach assignment ACTIVE; request EXECUTED. Nếu có RESERVED, không kết luận người đó mất mọi quyền sau khi gói cũ chấm dứt.

**Nhánh lỗi:** không có Membership ACTIVE, đang có request mở, version cũ hoặc request đã xử lý.

**Lời nói:** “Chấm dứt gói là kết thúc quyền của một Membership. Nó không xóa/khóa tài khoản và không tự trả tiền. Muốn hoàn tiền phải vào luồng hoàn có điều kiện riêng.”

**Mở code:** [MembershipTerminationService#createForReceptionist](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L84)/[MembershipTerminationService#approve](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L108); [MembershipTerminationExecutionService#executeImmediate](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationExecutionService.java#L67); [MembershipLifecycleService#terminateActiveAndActivateReserved](../../src/main/java/com/scms/backend/membership/MembershipLifecycleService.java#L74).

## D15. Hoàn tiền Membership nguồn tiền mặt

**Chuẩn bị:** nguồn CASH/PAID liên kết gói TERMINATED đủ ngưỡng hoặc RESERVED; không có yêu cầu hoàn có hiệu lực trùng. Nếu D ở D14 mới kết thúc gói còn gần nguyên thời hạn, dùng số tiền gợi ý API trả, không lấy ví dụ 12 ngày làm giá trị cố định.

**Thao tác:**
1. `GET /reception/members/{memberCode}/refund-eligible-sources`; chỉ paymentId, membershipId và mức gợi ý.
2. `POST /reception/members/{memberCode}/refund-requests`: paymentId, membershipId, requestedAmount, reason, idempotencyKey trong body. Chỉ REQUESTED.
3. Mở `/reception/refund-requests` để theo dõi.
4. **Manager:** first approval số tiền hợp lệ; lấy FIRST_APPROVED/version mới.
5. Lễ tân thực chi, gọi `/reception/refund-requests/{requestId}/cash-disbursement` với expectedVersion và evidence; thấy CASH_DISBURSED.
6. **Manager:** final approval → COMPLETED và credit note. Nếu nguồn RESERVED, kiểm tra gói dự trữ được hủy ở bước hoàn tất.

**Công thức:** RESERVED: `floor(80% × giá gói)`. TERMINATED: tính ngày còn lại từ ngày bắt đầu + duration gốc + ngày được bù − ngày tính; phải còn ít nhất 30% duration gốc; gợi ý `floor(giá × ngày còn / duration × 50%)`. Manager tính lại trần lúc duyệt; approved không vượt requested hoặc trần mới.

**Ví dụ:** gói 3.000.000/30 ngày, còn 12 ngày, không có ngày bù → 600.000; còn 8 ngày không đạt ngưỡng. RESERVED giá 3.000.000 → 2.400.000. Đọc số tiền thật từ response để demo.

**Nhánh lỗi:** nguồn BANK_TRANSFER, gói ACTIVE bình thường, yêu cầu quá trần, thu/chi sai trạng thái, version cũ, thiếu chứng cứ. Gửi lại theo cùng key/payload để kiểm tra replay của bước tạo.

**Lời nói:** “Có ba trách nhiệm: lễ tân đề nghị và chi, Manager duyệt mức tiền và xác nhận cuối, hệ thống giữ nguyên giao dịch gốc và tạo chứng từ điều chỉnh. Chi tiền chưa đồng nghĩa quy trình đã hoàn tất.”

**Mở code:** [MembershipRefundService#refundEligibleSources](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L91)/[MembershipRefundService#create](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L99)/[MembershipRefundService#firstApproval](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L140)/[MembershipRefundService#cashDisbursement](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L168)/[MembershipRefundService#finalApproval](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L183)/[MembershipRefundService#eligibleQuoteValues](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L265).

## D16. Tư vấn khóa học và tạo đơn giữ chỗ

**Thao tác:**
1. Mở `/courses`, chi tiết `/courses/{id}`, `/course-runs`, `/course-runs/{id}`; chỉ Course khác CourseRun và các buổi học cụ thể.
2. Chọn Run-OPEN và K1; `POST /reception/members/{memberAccountId}/course-orders` với courseRunId và Idempotency-Key.
3. Chỉ order chưa trả, reservation HELD, expiresAt; `GET /reception/members/{memberAccountId}/course-orders/{orderId}` để tiếp tục đơn.
4. Replay cùng key/payload; thử key cũ cho run khác để chứng minh conflict.

**Logic:** Course PUBLISHED; run OPEN_FOR_REGISTRATION, đang trong cửa sổ đăng ký; hội viên ACTIVE, chưa enrollment/hold trùng và không xung đột lịch. Capacity gồm enrollment ACTIVE/WITHDRAWAL_PENDING, hold còn hạn, lời mời INVITED còn hạn và transfer hold còn hạn. Vì vậy số người đã đóng tiền chưa phải toàn bộ số chỗ đã chiếm.

**Hạn giữ:** `min(thời điểm tạo + 15 phút, registrationClosesAt)`. Đặt chỗ không đồng nghĩa enrollment ACTIVE. Tác vụ nền giải phóng giữ chỗ hết hạn.

**Nhánh lỗi:** run đầy/đóng đăng ký, course chưa published, lịch chồng booking hoặc enrollment/hold khác, key dùng sai, hold hết hạn.

**Lời nói:** “Khóa học bán quyền tham gia một đợt và lịch buổi cụ thể. Giữ chỗ ngắn hạn giúp khách có thời gian thanh toán nhưng không chiếm chỗ vô thời hạn.”

**Mở code:** CourseCatalogService; [CourseOrderService#createForReceptionist](../../src/main/java/com/scms/backend/course/order/CourseOrderService.java#L76)/[CourseOrderService#create](../../src/main/java/com/scms/backend/course/order/CourseOrderService.java#L110)/[CourseOrderService#holdExpiresAt](../../src/main/java/com/scms/backend/course/order/CourseOrderService.java#L105); CourseReservationExpiryService.

## D17. Thu học phí tiền mặt, QR và đối soát khóa học

**Tiền mặt:** dùng order vừa tạo ở D16 khi còn hold; `POST /reception/members/{memberAccountId}/course-orders/{orderId}/payments/cash`, key riêng. Chỉ CoursePayment PAID, CourseOrder PAID, reservation ENROLLED, CourseEnrollment và buổi được cấp.

**Chuyển khoản:** với order/hội viên khác, gọi cùng tiền tố `/payments/sepay`; chỉ QR và expiresAt. Callback hợp lệ cấp Enrollment. Hạn thanh toán gắn hold của order, không có thêm 24 giờ như Membership.

**Đối soát:**
1. Mở `/course-payments/reconciliation-queue`, chi tiết `/course-payments/{id}` và `/result`.
2. Đối chiếu amount/content/provider transaction; gọi `/course-payments/{id}/reconcile` bằng key và payload theo DTO.
3. Đọc lại result. Với payment PENDING quá hạn, demo nhánh FAILED; thử PAID cho hold đã hết để thấy không hồi sinh chỗ cũ.

**Logic:** thanh toán khóa dùng chính CourseOrder đã giữ chỗ, khác cash Membership tự tạo order từ offer. Fulfillment tạo Enrollment và phân bổ học phí vào từng buổi bằng số nguyên VND; tổng allocation bằng tiền đã phân bổ, kể cả chia có phần dư. Đây là cơ sở hoàn/chuyển sau này, không lấy tùy ý giá hiện tại.

**Nhánh lỗi:** order sai hội viên, hết hold, tiền/nội dung sai, transaction trùng, key/payload không khớp. Course reconciliation PAID phải còn điều kiện giữ chỗ hợp lệ. Không trình bày bảng receipts của Membership như chứng từ mặc định của Course.

**Lời nói:** “Hệ thống chỉ ghi danh sau khi thu học phí thành công. Phần tiền từng buổi được lưu ngay lúc ghi danh để các nghiệp vụ rút hoặc chuyển có căn cứ tính toán.”

**Mở code:** [SepayService#fulfillCourseCash](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L115)/[SepayService#createCoursePaymentForReceptionist](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L103); [PaymentFulfillmentService#fulfillCourseCash](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L202)/[PaymentFulfillmentService#fulfillCourse](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L218); [CourseReconciliationService#reconcile](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationService.java#L87).

## D18. Tra cứu ghi danh khóa học và điều kiện rút

**Thao tác:** `GET /reception/members/{memberAccountId}/course-enrollments`, lọc status/withdrawalEligible, phân trang; mở `/course-enrollments/{enrollmentId}`. Chỉ khóa/đợt, trạng thái, tổng/số buổi còn lại, buổi tiếp theo và lý do được/không được rút.

**Logic:** Enrollment là quyền học đã cấp; CourseOrder là quá trình mua. Các lý do như ELIGIBLE_WITH_REFUND, ELIGIBLE_WITHOUT_REFUND hoặc FINAL_UNIT_ALREADY_STARTED giúp nhân viên giải thích vì sao rút có tiền hoàn hoặc không còn được rút. Không suy ra “không hoàn” luôn đồng nghĩa “không được rút”.

**Nhánh lỗi:** enrollment không thuộc hội viên; trạng thái không ACTIVE; đã đến buổi cuối. Dùng fixture có thời điểm khác nhau để giải thích.

**Lời nói:** “Màn hình tra cứu cần trả lời được ba câu: hội viên đang học gì, còn quyền học những buổi nào, và nếu muốn dừng thì được xử lý theo chính sách nào.”

**Mở code:** [CourseEnrollmentController#receptionEnrollments](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentController.java#L36)/[CourseEnrollmentController#receptionEnrollment](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentController.java#L41); [ReceptionCourseEnrollmentService#list](../../src/main/java/com/scms/backend/course/enrollment/ReceptionCourseEnrollmentService.java#L35)/[ReceptionCourseEnrollmentService#detail](../../src/main/java/com/scms/backend/course/enrollment/ReceptionCourseEnrollmentService.java#L43).

## D19. Waitlist khóa học: chờ → được mời → nhận chỗ → trả tiền

**Chuẩn bị:** Run-FULL đủ chỗ thật, còn mở đăng ký; hội viên chưa có enrollment/hold/waitlist trùng.

**Thao tác:**
1. `POST /reception/members/{memberAccountId}/course-runs/{runId}/waitlist`.
2. `GET /reception/members/{memberAccountId}/course-waitlists`; chỉ WAITING.
3. Dùng dữ liệu giữ chỗ hết hạn hoặc một nghiệp vụ hợp lệ giải phóng chỗ; **Hệ thống** promote → INVITED.
4. Lễ tân có sự đồng ý của hội viên, gọi `/course-waitlists/{waitlistId}/accept` dưới tiền tố member; chỉ waitlist ACCEPTED cùng order/hold mới.
5. Thanh toán order theo D17 rồi kiểm tra Enrollment. Với entry khác dùng DELETE để rời, thấy LEFT.

**Logic:** lời mời giữ chỗ tới mốc sớm nhất trong thời điểm mời+24 giờ, đóng đăng ký và buổi sắp tới nếu có. Accept cần trước hạn và tạo order/hold thanh toán; chưa đóng học phí thì chưa enrollment hoàn tất. Replay accept khi đã ACCEPTED lấy kết quả theo xử lý service. Entry quá hạn EXPIRED; khi run không còn phù hợp có thể CANCELLED.

**Nhánh lỗi:** vào waitlist khi run còn chỗ, trùng entry/enrollment/hold, accept quá hạn, không thanh toán trong hold. Muốn demo hết hạn cần fixture, không giữ khán giả chờ 24 giờ.

**Lời nói:** “Waitlist lớp tập tự tạo Booking, còn waitlist khóa chỉ mời mua chỗ. Hai hàng chờ có cùng mục tiêu tối ưu chỗ nhưng khác nghĩa vụ thanh toán.”

**Mở code:** [CourseWaitlistService#join](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L71), [CourseWaitlistService#leave](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L99), [CourseWaitlistService#mineForReceptionist](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L117), [CourseWaitlistService#accept](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L131), [CourseWaitlistService#promote](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L161).

## D20. Ghi nhận phản hồi của hội viên khi trung tâm đổi lịch khóa

**Chuẩn bị:** ba response PENDING chưa đến responseDeadline cho ba hội viên/enrollment khác nhau. Lịch thay đổi do phía quản lý thực hiện trước; lễ tân không tự sửa lịch.

**Thao tác:**
1. `GET /reception/members/{memberAccountId}/course-schedule-changes`; giải thích thay đổi và hạn phản hồi cho hội viên.
2. `POST /reception/members/{memberAccountId}/course-schedule-changes/{responseId}/response` với lựa chọn CONTINUE. Tải lại để thấy đã ghi nhận tiếp tục học.
3. Với response thứ hai, chọn REQUEST_WITHDRAWAL; xem yêu cầu rút được tạo và xử lý tiếp tại D22.
4. Với response thứ ba, chọn REQUEST_TRANSFER; xem yêu cầu chuyển, chờ Manager chuẩn bị đợt đích/offer rồi tiếp tục D21.
5. Gửi lại cùng lựa chọn để kiểm tra replay; đổi lựa chọn sau khi đã trả lời để minh họa conflict.

**Logic:** response phải thuộc enrollment của đúng hội viên, đang PENDING và còn hạn. Xin rút phải thỏa withdrawalEligible. Một lựa chọn có thể tạo thêm request nghiệp vụ; nó không đồng nghĩa lễ tân đã duyệt rút hoặc hoàn tất chuyển.

**Nhánh lỗi:** response sai chủ, quá deadline, đã trả lời lựa chọn khác, không đủ điều kiện rút/chuyển.

**Lời nói:** “Lễ tân thông báo thay đổi và ghi lại quyết định của hội viên. Quyết định tiếp tục, xin rút hoặc xin chuyển dẫn tới ba luồng khác nhau; lễ tân không tự chọn đợt đích thay Manager.”

**Mở code:** [CourseReceptionActionsController#changes](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L78)/[CourseReceptionActionsController#respond](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L83); [CourseScheduleChangeService#listForReceptionist](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeService.java#L70)/[CourseScheduleChangeService#respond](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeService.java#L84).

## D21. Chấp nhận đề nghị chuyển khóa và xử lý chênh lệch

**Chuẩn bị:** offer OFFERED do Manager tạo từ quy trình chuyển, còn hạn; đợt đích hợp lệ. Chuẩn bị ba offer riêng để trình diễn ba kết quả tiền khác nhau.

**Thao tác:**
1. `GET /reception/members/{memberAccountId}/course-transfer-offers`; chỉ khóa nguồn, khóa đích, credit, tiền chênh lệch và hạn.
2. Khi hội viên đồng ý, `POST /reception/members/{memberAccountId}/course-transfer-offers/{offerId}/accept` với key riêng.
3. **Đích đắt hơn credit:** chỉ offer PENDING_PAYMENT và difference order; thanh toán phần chênh lệch theo API CourseOrder/Payment, sau đó mới kiểm tra chuyển hoàn tất.
4. **Không phải trả thêm:** kiểm tra hoàn tất chuyển trực tiếp; nguồn TRANSFERRED, target enrollment được tạo.
5. **Credit dư:** mở nghĩa vụ hoàn TRANSFER_CREDIT_EXCESS; giải thích cần đi qua phê duyệt/chi/xác minh D23, không mặc nhiên đã trả tiền cho khách.

**Logic:** khóa dữ liệu actor/member/offer/runs; kiểm tra hạn OFFERED, chỗ đợt đích và credit chưa thay đổi. Credit lấy từ giá trị buổi chưa dùng theo allocation/chính sách, không mặc định bằng toàn học phí đã đóng. Hạn đề nghị tối đa 24 giờ và bị giới hạn bởi lịch/đóng đăng ký.

**Nhánh lỗi:** chưa có offer, hết hạn, credit đã đổi, chỗ/đợt đích không hợp lệ, key tái sử dụng sai. Không gọi việc mới tạo difference order là đã chuyển xong.

**Lời nói:** “Hệ thống bảo toàn giá trị còn lại của hội viên: thiếu thì thu thêm, dư thì tạo nghĩa vụ hoàn. Chuyển quyền học và xử lý tiền được phối hợp để tránh vừa mất quyền cũ vừa chưa có quyền mới.”

**Mở code:** [CourseTransferService#offersForReceptionist](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L157)/[CourseTransferService#accept](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L169)/[CourseTransferService#completePaidDifference](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L214)/[CourseTransferService#completeTransfer](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L234); CourseTransferPolicy.

## D22. Tạo, xem, hủy và theo dõi yêu cầu rút khóa

**Chuẩn bị:** enrollment ACTIVE còn buổi chưa bắt đầu, không vướng yêu cầu chuyển mở. Dùng hai enrollment riêng nếu vừa muốn demo cancel vừa muốn demo approve.

**Thao tác:**
1. Đọc D18 để xem lý do/giá trị gợi ý.
2. `POST /reception/members/{memberAccountId}/course-enrollments/{enrollmentId}/withdrawal-requests` theo DTO; chỉ request PENDING, enrollment WITHDRAWAL_PENDING, snapshot số buổi/giá trị chưa dùng.
3. Xem `/course-withdrawal-requests` và chi tiết `/{requestId}` dưới tiền tố member.
4. Với yêu cầu đầu, gọi `POST .../course-withdrawal-requests/{requestId}/cancel`; kiểm tra request được hủy và enrollment trở lại ACTIVE theo điều kiện service.
5. Với yêu cầu khác, **Manager** approve; kiểm tra request APPROVED, enrollment/reservation WITHDRAWN và nghĩa vụ hoàn nếu có. Manager reject nhánh khác thì không hoàn tất rút.

**Logic:** còn ít nhất một buổi chưa bắt đầu mới có thể rút. Tự nguyện: nếu số buổi còn lại ≥70% tổng số buổi, tiền hoàn là floor(80% giá trị phân bổ phần chưa dùng); dưới 70% vẫn được rút nếu còn buổi nhưng hoàn 0. Trường hợp do thay đổi lịch đáng kể dùng cause từ quy trình đó, bỏ ngưỡng 70% nhưng vẫn dựa phần chưa dùng và hệ số 80%. Lễ tân không tự nhập cause tùy ý để tăng hoàn tiền.

**Ví dụ:** khóa 10 buổi, 3.000.000 chia đều: còn 7 buổi → phần chưa dùng 2.100.000 → hoàn 1.680.000; còn 6 buổi → tự nguyện hoàn 0. Code dùng allocation lưu thật từng buổi, không luôn chia đều lúc tính lại.

**Nhánh lỗi:** đã bắt đầu buổi cuối, enrollment không ACTIVE/phù hợp, trùng yêu cầu hoặc vướng chuyển khóa, cancel request đã duyệt. Không dùng trạng thái điểm danh thay cho thời điểm buổi học để xác định phần chưa dùng.

**Lời nói:** “Snapshot được chốt lúc xin rút để việc Manager duyệt chậm không tùy tiện làm thay đổi căn cứ hoàn tiền. Duyệt rút giải phóng quyền học, nhưng chi hoàn là bước riêng.”

**Mở code:** [CourseWithdrawalService#createForReceptionist](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L67)/[CourseWithdrawalService#listForReceptionist](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L114)/[CourseWithdrawalService#detailForReceptionist](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L120)/[CourseWithdrawalService#approve](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L150)/[CourseWithdrawalService#cancelForReceptionist](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L198); CourseWithdrawalPolicy.

## D23. Chi hoàn tiền khóa học và xác minh kết quả

**Chuẩn bị:** obligation APPROVED, còn version đúng và đã đủ thời điểm chi. Một obligation từ rút tự nguyện khi run chưa COMPLETED để demo nhánh bị chặn; một obligation thật sự eligible để hoàn tất.

**Thao tác:**
1. Xem `/reception/course-refunds`, lọc eligible; mở `/{refundId}`. Xem lịch sử theo member qua `/reception/members/{memberAccountId}/course-refund-obligations` và chi tiết obligation.
2. **Manager:** nếu còn PENDING_REVIEW, duyệt trước; lấy APPROVED và approvedAmountVnd.
3. Lễ tân chi thực tế theo CASH hoặc BANK_TRANSFER, rồi gọi `POST /reception/course-refund-obligations/{obligationId}/disburse` với key mới, expectedVersion, amountVnd, method, reference, evidenceReference.
4. Kiểm tra DISBURSEMENT_RECORDED, đúng số tiền và thông tin chi. Gửi lại cùng key/payload để chứng minh không ghi chi thêm.
5. **Manager:** verify bằng số tiền/mã tham chiếu/bằng chứng khớp; kiểm tra COMPLETED.

**Logic:** queue lễ tân chỉ hiển thị obligation APPROVED, nhưng được duyệt chưa đủ để chi. Thông thường run phải COMPLETED; COURSE_RUN_CANCELLED, PAYMENT_EXCEPTION, TRANSFER_CREDIT_EXCESS có ngoại lệ chi ngay; REDUCED_SESSION_COUNT có ngoại lệ khi run CANCELLED. Số chi phải đúng approvedAmountVnd; version và payload replay phải khớp.

**Trạng thái:** `PENDING_REVIEW → APPROVED → DISBURSEMENT_RECORDED → COMPLETED`. Bên trong method Manager verify, code chuyển qua PENDING_VERIFICATION rồi COMPLETED trong transaction; không nhất thiết có một màn hình/bước người dùng riêng để dừng tại PENDING_VERIFICATION.

**Nhánh lỗi:** chưa đủ thời điểm chi, số tiền khác đã duyệt, thiếu reference/evidence, version cũ, key dùng cho giao dịch khác, bằng chứng Manager verify không khớp. Endpoint disburse ghi nhận chi thực tế, không tự thực hiện lệnh chuyển khoản ngân hàng.

**Lời nói:** “Hoàn khóa khác hoàn gói: có obligation từ nhiều nguyên nhân, có điều kiện thời điểm chi, hỗ trợ ghi nhận tiền mặt hoặc chuyển khoản và cần đối chiếu chứng cứ cuối cùng.”

**Mở code:** [CourseRefundObligationService#listForReceptionist](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L106)/[CourseRefundObligationService#detailForReceptionist](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L113)/[CourseRefundObligationService#recordDisbursement](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L162)/[CourseRefundObligationService#verify](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L200); CourseRefundStateMachine.

## D24. Hồ sơ cá nhân lễ tân và ảnh đại diện

**Thao tác:** `GET /staff/me/profile`; `PATCH` các field được hỗ trợ như tên, phone, ngày sinh, thông tin liên hệ khẩn cấp; đọc lại. `POST /staff/me/profile/image` multipart để thay ảnh; `DELETE` cùng route để xóa ảnh; xem lại kết quả.

**Logic:** account lấy từ JWT của người hiện đăng nhập; không nhận một UUID tùy ý để sửa hồ sơ nhân viên khác. Validation và chống trùng định danh vẫn áp dụng. Thay đổi thật ghi audit STAFF_SELF_PROFILE_UPDATED. Email mới không được cập nhật tắt qua PATCH hồ sơ, phải qua D25.

**Nhánh lỗi:** phone trùng, dữ liệu hồ sơ sai, file ảnh không hợp lệ, PATCH email sang giá trị mới bị EMAIL_CHANGE_VERIFICATION_REQUIRED.

**Lời nói:** “Lễ tân có quyền quản lý hồ sơ cá nhân. Quyền này khác quản trị tài khoản nhân viên như tạo tài khoản hoặc đình chỉ, vốn thuộc Manager.”

**Mở code:** [StaffProfileController#get](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileController.java#L23)/[StaffProfileController#update](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileController.java#L28); [StaffProfileService#updateProfile](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileService.java#L57); [ProfileImageUploadService#uploadForStaff](../../src/main/java/com/scms/backend/member/ProfileImageUploadService.java#L65)/[ProfileImageUploadService#removeForStaff](../../src/main/java/com/scms/backend/member/ProfileImageUploadService.java#L79).

## D25. Đổi email cá nhân, gửi lại OTP, thay yêu cầu và hủy

**Chuẩn bị:** R1 biết mật khẩu hiện tại; email thử nghiệm mới truy cập được; cấu hình gửi email hoạt động. Thực hiện cuối phần nghiệp vụ để việc thay phiên đăng nhập không gián đoạn các phần trước.

**Thao tác:**
1. Khởi tạo yêu cầu qua `/staff/me/email-change`, cung cấp thông tin theo DTO gồm mật khẩu hiện tại và email mới.
2. Dùng GET status của luồng chung `/accounts/me/email-change` để theo dõi; email chưa đổi ngay khi begin.
3. Demo resend theo thời gian chờ/rate limit; với một yêu cầu thử, dùng cancel để hủy và kiểm tra email cũ còn hiệu lực.
4. Tạo lại yêu cầu; nếu cần thay địa chỉ đang chờ, dùng PUT replace của `/accounts/me/email-change` theo contract.
5. Nhập OTP thật từ email thử nghiệm, verify; đọc lại hồ sơ và đăng nhập lại theo email mới khi cần.

**Logic:** hai nhóm route dùng chung nghiệp vụ đổi email, không phải hai quy trình độc lập để chạy chồng. Kiểm tra mật khẩu, email mới, OTP/hạn/số lần thử; chỉ verify thành công mới cập nhật email. Có thu hồi refresh token/cập nhật version xác thực và xóa cookie theo luồng controller.

**Nhánh lỗi:** mật khẩu sai, email trùng, OTP sai/hết hạn, resend quá nhanh hoặc yêu cầu đã hủy. Không nói “gửi OTP thành công là email đã thay”.

**Lời nói:** “Email là định danh đăng nhập và kênh khôi phục, nên thay đổi cần chứng minh quyền sở hữu địa chỉ mới thay vì chỉ sửa một field hồ sơ.”

**Mở code:** StaffEmailChangeController; AccountEmailChangeController; [AccountEmailChangeService#begin](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L78)/[AccountEmailChangeService#replace](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L83)/[AccountEmailChangeService#verify](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L120)/[AccountEmailChangeService#resend](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L164)/[AccountEmailChangeService#cancel](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L191)/[AccountEmailChangeService#status](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L203).

## D26. Đổi mật khẩu cá nhân và quên mật khẩu qua email

**Nhánh đang đăng nhập:** `POST /auth/change-password` với currentPassword/newPassword theo DTO. Kiểm tra mật khẩu hiện tại, chính sách mật khẩu mới và khác mật khẩu cũ; lưu hash, thu hồi refresh token phiên hiện tại và xóa cookie. Kiểm tra đăng nhập lại bằng mật khẩu mới trong phiên thử nghiệm.

**Nhánh quên mật khẩu:**
1. Trong phiên riêng, `POST /auth/password-reset/request` với email thử nghiệm; nhận phản hồi accepted chung.
2. Nhận OTP; `/auth/password-reset/verify` với dữ liệu DTO; nhận cookie phiên reset.
3. `/auth/password-reset/confirm` với newPassword và cookie; đăng nhập lại để kiểm tra.
4. Thử dùng lại phiên reset đã hoàn tất hoặc OTP sai để trình diễn bị chặn.

**Logic:** request không tiết lộ email có tài khoản hay không; tạo OTP/session chỉ khi điều kiện tài khoản/tính năng hợp lệ. Verify kiểm tra OTP/hạn/số lần thử; phiên reset có TTL 10 phút. Confirm khóa dữ liệu, chặn session hết hạn/đã dùng/thu hồi, hash mật khẩu và thu hồi refresh token; session chỉ dùng một lần. Không khẳng định mọi access JWT cũ bị vô hiệu ngay vì Account.changePassword không tự tăng authVersion.

**Lời nói:** “Reset hội viên tại quầy là quyền hỗ trợ của nhân viên. Quên mật khẩu cá nhân là quy trình tự phục vụ chứng minh quyền qua email, dùng OTP và phiên reset một lần.”

**Mở code:** [AuthenticationService#changePassword](../../src/main/java/com/scms/backend/auth/AuthenticationService.java#L60); [PasswordResetService#request](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetService.java#L75)/[PasswordResetService#verify](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetService.java#L123)/[PasswordResetService#confirm](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetService.java#L159).

## D27. Thông báo, đọc một và đọc tất cả

**Thao tác:** sau một bước Manager duyệt có gửi notification cho lễ tân liên quan, mở `GET /notifications` với unreadOnly/page/size. Chọn một thông báo, `PATCH /notifications/{id}/read`; kiểm tra trạng thái/đếm chưa đọc. Gọi `PATCH /notifications/read-all` rồi tải lại.

**Logic:** chỉ đọc/sửa notification thuộc account đang đăng nhập. Outbox ghi sự kiện cùng transaction, dispatcher xử lý sau, nên có thể cần tải lại sau khi xử lý nền. Audit ghi lịch sử người làm; notification nhắc người nhận công việc cần tiếp tục. Không gọi mọi notification là email hoặc WebSocket.

**Nhánh lỗi:** dùng ID thông báo của tài khoản khác; bộ lọc/trang không hợp lệ. Nếu outbox chưa xử lý, chỉ trạng thái tác vụ nền thay vì kết luận nghiệp vụ trước thất bại.

**Lời nói:** “Thông báo giúp bàn giao công việc giữa các vai trò, ví dụ duyệt xong thì lễ tân biết cần thu phí hoặc chi tiền. Nó không thay thế bước tải lại trạng thái và version trước khi thực hiện.”

**Mở code:** [NotificationService#list](../../src/main/java/com/scms/backend/notification/NotificationService.java#L29)/[NotificationService#markRead](../../src/main/java/com/scms/backend/notification/NotificationService.java#L36)/[NotificationService#markAllRead](../../src/main/java/com/scms/backend/notification/NotificationService.java#L46); [NotificationWriter#write](../../src/main/java/com/scms/backend/notification/NotificationWriter.java#L21); [NotificationOutboxDispatcher#dispatchAvailable](../../src/main/java/com/scms/backend/notification/NotificationOutboxDispatcher.java#L29).

## D28. Đăng xuất và kết thúc ca demo

**Thao tác:** `POST /auth/logout` với cookie phiên hiện tại; kiểm tra cookie bị xóa và refresh cũ không tiếp tục cấp token. Không dùng kiểm tra access token đang còn hạn để khẳng định logout phải hủy tức thì mọi JWT.

**Lời chốt khi trình bày:** “Em đã đi qua ba giai đoạn công việc của lễ tân: tiếp nhận và bán dịch vụ; hỗ trợ sử dụng; tiếp nhận và thực hiện phần việc sau bán. Mỗi thao tác đều gắn người thực hiện, đúng đối tượng và trạng thái; các quyết định phê duyệt vẫn do Manager xử lý.”

**Mở code:** [AuthenticationController#logout](../../src/main/java/com/scms/backend/auth/AuthenticationController.java#L45) → [AuthenticationService#logout](../../src/main/java/com/scms/backend/auth/AuthenticationService.java#L55) → [RefreshTokenService#revokeIfPresent](../../src/main/java/com/scms/backend/auth/RefreshTokenService.java#L61).

## Bảng phân vai để tránh demo sai quyền

| Luồng | Lễ tân thực hiện | Manager/hệ thống phối hợp |
|---|---|---|
| Kích hoạt thủ công | Tìm tài khoản chờ, lập và theo dõi yêu cầu của mình | Manager approve/reject |
| Gói tập | Tư vấn, thu tiền, tạo QR, đối soát, xem lịch sử/receipt trong phạm vi | Hệ thống xác nhận callback, cấp gói, chạy vòng đời |
| Booking/PT | Xem, đặt/hủy hộ, xử lý waitlist | Manager quản lý lịch/assignment; Coach điểm danh |
| Bảo lưu | Lập yêu cầu, thu phí sau duyệt, kết thúc sớm khi ACTIVE | Manager duyệt; hệ thống thực thi theo ngày |
| Chấm dứt | Lập và theo dõi yêu cầu | Manager duyệt; hệ thống chấm dứt/cập nhật liên quan |
| Hoàn gói | Lập request, chi tiền mặt sau first approval | Manager duyệt mức và xác nhận cuối |
| Khóa học | Tư vấn, tạo order, thu tiền, tra enrollment | Manager quản lý khóa/lịch; hệ thống giữ/giải phóng chỗ |
| Đổi lịch/chuyển khóa | Ghi nhận lựa chọn, nhận offer thay hội viên, thu chênh lệch | Manager thay lịch/tạo offer; hệ thống chuyển quyền và tiền |
| Rút khóa | Tạo, theo dõi, hủy request còn hợp lệ | Manager quyết định; hệ thống snapshot và tạo nghĩa vụ hoàn |
| Hoàn khóa | Chi theo approved amount và điều kiện thời điểm | Manager duyệt/xác minh |

Không đưa “tạo khóa học”, “tạo lịch”, “phân công coach”, “duyệt yêu cầu của chính lễ tân” thành chức năng demo lễ tân. `POST /reception/courses` bị chặn trong cấu hình của commit này.

## Giải thích flow code trong 60–90 giây

```mermaid
flowchart LR
    A[Frontend hoặc API client] --> B[JWT và kiểm tra role]
    B --> C[Controller nhận DTO và actor]
    C --> D[Service kiểm tra đối tượng và nghiệp vụ]
    D --> E[Repository theo nghiệp vụ (JPA hoặc JDBC)]
    E --> F[(PostgreSQL)]
    D --> G[Audit và outbox khi nghiệp vụ yêu cầu]
    G --> H[Tác vụ gửi thông báo]
    D --> I[Response trạng thái và ID]
```

Lời giải thích: “Controller không tự quyết định khách đủ điều kiện. Service nhận actor từ JWT, tìm hội viên mục tiêu và kiểm tra trạng thái hiện tại. Repository theo nghiệp vụ (JPA hoặc JDBC) đọc/ghi DB trong transaction. Các thao tác nhiều người có khóa/ràng buộc để tránh tranh chỗ hoặc ghi trùng. Idempotency xử lý gửi lại; expectedVersion chặn quyết định trên dữ liệu cũ. Response cho biết trạng thái cuối và ID để thao tác tiếp.”

Ví dụ nên mở xuyên suốt: ReceptionMemberController → ReceptionMemberService → Account/MemberProfile; hoặc PaymentController → PaymentService → PaymentFulfillmentService → các bản ghi order/payment/membership/receipt. Repository JDBC có thể truy vấn nhiều bảng; không khẳng định mỗi bảng đều có một JPA Entity riêng.

## Các nhánh lỗi cần biểu diễn ít nhất một lần

| Tình huống | Mục đích kiểm soát | Cách xử lý đúng lúc demo |
|---|---|---|
| Trùng email/phone | Không tạo trùng định danh | Dùng liên hệ mới hoặc tra hội viên có sẵn |
| Nhầm MB-… với UUID | Đúng đối tượng API | Đọc kiểu path và ID trong response |
| Check-in thiếu ảnh/xác minh | Xác nhận đúng khách tại quầy | Hoàn thiện hồ sơ/xác minh trước |
| Lớp đầy/trùng lịch/thiếu benefit | Đúng quyền và sức chứa | Chọn buổi khác hoặc waitlist hợp lệ |
| Hủy sau mốc 2 giờ | Bảo vệ kế hoạch buổi học | Giải thích quy tắc; không bypass bằng lễ tân |
| Pending order/RESERVED trùng | Tránh đơn và gia hạn chồng nhau | Tiếp tục/xử lý đơn đang mở theo quy tắc |
| Thanh toán sai amount/reference | Đối chiếu đúng giao dịch | Kiểm tra bằng chứng, không sửa giá client |
| Hold khóa hết hạn | Không bán trùng chỗ đã giải phóng | Tạo quy trình mua mới nếu còn đủ điều kiện |
| Version cũ | Không ghi đè quyết định người khác | GET lại dữ liệu, đọc thay đổi, gửi version mới |
| Idempotency-Key dùng sai giao dịch | Không nhầm replay với thao tác mới | Key mới cho thao tác mới |
| Hoàn khóa APPROVED nhưng chưa eligible | Đúng thời điểm chi | Chờ điều kiện hoặc dùng fixture eligible cho demo |
| Token Receptionist gọi quyền Manager | Tách trách nhiệm | Chuyển đúng phiên ở bước phối hợp |

Khi trình bày lỗi, chỉ rõ mã lỗi/message thực trả và trạng thái trước/sau. Không đoán mọi lỗi là HTTP 400; handler quyết định 400/401/403/404/409 hoặc lỗi cấu hình tương ứng.

## Bản demo bảo vệ 30 phút

| Phút | Nội dung | Kết quả cần chỉ |
|---|---|---|
| 0–2 | D01 + phân vai | Login, role, actor khác member |
| 2–6 | D02–D03 | Hội viên mới, mã/UUID, ảnh, chưa có gói |
| 6–10 | D06 + D09; giải thích QR D07 | Payment/Order PAID, Membership ACTIVE, Receipt; RESERVED khi gia hạn |
| 10–14 | D10–D12 | Visit, booking/cancel, hàng chờ được đôn |
| 14–19 | D13–D15 với fixture sẵn | Bước lễ tân/Manager, phí bảo lưu, quy trình hoàn gói |
| 19–25 | D16–D23 chọn một giao dịch khóa + các trạng thái chuẩn bị | Hold 15 phút, Enrollment, rút/chuyển/hoàn khác Membership |
| 25–28 | D04–D05 và D24–D27 trình bày nhanh hoặc dùng kết quả chuẩn bị | Reset, activation, hồ sơ/email, thông báo |
| 28–30 | Flow code + D28 | Transaction, khóa, key/version, logout |

Đây là bản rút gọn để giới thiệu đủ nhóm chức năng, không phải chạy live đủ mọi nhánh trong 30 phút. Muốn trình diễn toàn bộ thao tác, đi lần lượt D01–D28 và dành thêm thời gian cho các phiên Manager/OTP/callback.

## Phiếu kiểm tra sau mỗi ca demo

1. Actor đang dùng đúng role? Đúng hội viên, đúng mã/UUID?
2. Đầu vào/fixture còn hợp lệ ở thời điểm demo, theo đúng múi giờ?
3. Response trả đúng trạng thái/ID, đọc lại bằng API xem chi tiết có khớp?
4. Nhánh lỗi có giữ nguyên dữ liệu cần giữ, không sinh giao dịch hoặc quyền trùng?
5. Có bước Manager/hệ thống nào chưa chạy? Nếu có, chỉ rõ đang chờ bước đó.
6. Nếu trình diễn replay, đối chiếu cùng ID, không chỉ nhìn message “thành công”.

## Nguồn để mở code và kiểm chứng

- [Bản đồ class và số dòng](CLASS_RECEP_THEO_DONG.md): vị trí class và method chính xác cho các tên được nêu ở từng ca.
- [Phụ lục API](API_VA_CLASS_RECEP.md): HTTP method, path, DTO và controller.
- [Giải thích nghiệp vụ đầy đủ](THUYET_TRINH_ROLE_RECEP.md): thêm quan hệ bảng, công thức và trạng thái.
- [Câu hỏi bảo vệ](KICH_BAN_VA_CAU_HOI.md): câu trả lời khi bị hỏi sâu.

Lượt soạn kịch bản này đối chiếu source và các vị trí code, không thay đổi backend, không chạy demo UI/API end-to-end và không chạy lại test. Các kết quả test đã ghi ở tài liệu kiểm chứng cũ không thay thế việc chạy thử kịch bản này trước buổi trình bày. Số dòng/trạng thái áp dụng cho commit nêu đầu tài liệu; fixture, email, SePay và lịch theo thời gian cần kiểm tra tại môi trường demo thực tế.

## Cập nhật ca phối hợp theo 9c08093

Các bước này bổ sung phần phối hợp Manager/Coach, không mở rộng quyền lễ tân. Xem mục 28–29 của THUYET_TRINH_ROLE_RECEP.md để giải thích chi tiết.

| Ca | Chuẩn bị và thao tác | Kết quả cần kiểm tra |
|---|---|---|
| Hủy cả buổi | Manager thử hủy một buổi trống và một buổi có hội viên | Buổi trống có thể hủy khi đủ điều kiện; buổi còn hội viên bị chặn. Phân biệt với hủy booking cá nhân. |
| Version và nghỉ coach theo batch | Manager chọn các buổi cùng loại; gửi version hiện tại rồi thử version cũ/nhóm có một mục không hợp lệ | Version cũ bị từ chối; batch lỗi không ghi nhận thành công từng phần. |
| Training summary | Coach đúng phân công thử PT/course đã kết thúc, hội viên hợp lệ; thử trước giờ kết thúc hoặc sai coach | Chỉ ca hợp lệ được ghi; PT cần plan version/quyền lợi, course không nhận plan version. Lễ tân không có quyền ghi. |

Kịch bản là hướng dẫn diễn tập API trên dữ liệu local. Không xem bảng này là bằng chứng đã thao tác thành công trên frontend đang deploy.
