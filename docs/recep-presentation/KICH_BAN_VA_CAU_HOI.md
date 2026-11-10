# Kịch bản thuyết trình, demo và câu hỏi bảo vệ role Receptionist

Nguồn nội dung: backend-refactor local từ commit 9c08093505df43da557b7fa6ff91478f4bef770d. Kịch bản dưới đây là đề xuất trình diễn; không phải báo cáo đã chạy thành công một ca end-to-end. Đọc THUYET_TRINH_ROLE_RECEP.md để lấy điều kiện và nhánh lỗi chi tiết.

## 1. Bố cục thuyết trình khoảng 25–30 phút

### Phần 1 — Vai trò và phạm vi, 2 phút

“Trong hệ thống SCMS, em phụ trách vai trò Receptionist, tức lễ tân tại quầy. Vai trò này trực tiếp hỗ trợ hội viên từ lúc tiếp nhận đến khi sử dụng dịch vụ và xử lý các yêu cầu sau bán. Em chia chức năng thành sáu nhóm: tài khoản và hồ sơ, gói và thanh toán, ra vào trung tâm, đặt lịch, bảo lưu/chấm dứt/hoàn tiền, và khóa học.

Điểm kiểm soát của thiết kế là lễ tân thực hiện tác nghiệp, còn những quyết định như phê duyệt bảo lưu, phê duyệt chấm dứt và xác nhận hoàn tiền cuối cùng do Manager thực hiện. Quyền được kiểm tra ở backend chứ không chỉ ẩn nút trên giao diện.”

Mở SecurityConfiguration để chỉ các matcher /reception/** liên quan, rồi chỉ POST /reception/courses bị denyAll. Không khẳng định Manager tự động được dùng mọi API Receptionist; role được cấp theo từng matcher/service.

### Phần 2 — Kiến trúc và định danh, 3 phút

“Frontend gửi HTTP request tới API có prefix /api/v1. Request đi qua JWT validation và kiểm tra role trước khi tới controller. Controller lấy người thao tác từ JWT sub, nhận DTO và gọi service. Service xử lý điều kiện nghiệp vụ, dùng Repository theo nghiệp vụ (JPA hoặc JDBC) truy cập PostgreSQL. Thay đổi quan trọng nằm trong transaction, có audit và có thể ghi notification outbox.

Em phân biệt actor với hội viên mục tiêu. Khi lễ tân A tạo booking cho hội viên B, A lấy từ token còn B lấy từ path. Hệ thống vì vậy biết ai sử dụng dịch vụ và ai thực hiện thao tác.

Một điểm tích hợp cần chú ý là mã MB-… và account UUID khác nhau. Hồ sơ và Membership dùng mã hội viên ở nhiều endpoint; booking, khóa học và checkout dùng UUID.”

Mở ReceptionMemberController, BookingController và CenterVisitController để so String memberId với UUID memberId/memberAccountId.

### Phần 3 — Tiếp nhận và quản lý hội viên, 3 phút

“Tại quầy, lễ tân nhập tên, điện thoại, email và ngày sinh. DTO kiểm tra định dạng; service kiểm tra ít nhất 16 tuổi và định danh chưa trùng. Sau đó hệ thống tạo Account MEMBER ACTIVE và MemberProfile có mã hội viên. Mật khẩu khởi tạo trong code là số điện thoại đã được BCrypt. Thao tác có audit ghi lễ tân là người tạo và thông báo cho hội viên.

Việc tạo hội viên chưa tạo gói tập. Sau đó lễ tân có thể tìm bằng mã hoặc điện thoại, cập nhật hồ sơ và tải ảnh. Hội viên ACTIVE hoặc SUSPENDED đều có thể xuất hiện trong tra cứu này, nhưng các thao tác mua gói, đặt lịch hoặc vào cửa tiếp tục có điều kiện riêng.

Nếu hội viên quên mật khẩu, lễ tân có endpoint reset dành riêng cho hội viên ACTIVE. Nếu là tài khoản online đang chờ xác minh email, lễ tân tạo đề nghị kích hoạt thủ công và Manager duyệt.”

Mở ReceptionMemberService.createMember, ReceptionMemberProfileService.search, ResetPasswordService.receptionistResetMember.

### Phần 4 — Bán gói và thanh toán, 4 phút

“Hệ thống tách Plan, Offer, Order, Payment, Membership và Receipt. Plan là quyền lợi; Offer là sản phẩm bán; Order là ý định mua; Payment ghi nhận thu tiền; Membership là quyền được cấp sau thanh toán; Receipt là bằng chứng giao dịch.

Với chuyển khoản, lễ tân tạo order từ offerId, sau đó tạo payment SePay và QR. Giá được lấy từ backend, lưu snapshot trên đơn. Cửa sổ 24 giờ bắt đầu khi tạo payment đầu tiên, không phải ngay lúc tạo order. Webhook kiểm tra API key, nội dung, số tiền và tài khoản nhận rồi mới cấp Membership.

Với tiền mặt Membership, endpoint chuẩn nhận offerId và tự tạo order cùng payment trong luồng thu tiền. Service fulfillment được dùng chung để tạo Membership, Receipt, audit và notification trong transaction.

Nếu hội viên đang có gói ACTIVE, giao dịch gia hạn tạo gói RESERVED. Hệ thống không cho nhiều pending order hoặc nhiều RESERVED cùng hội viên theo quy tắc hiện tại.

Đối soát thủ công cần chứng cứ, số tiền và mã giao dịch đúng. Mỗi giao dịch ngân hàng không được dùng hai lần. Lễ tân đọc biên lai trong phạm vi payment do mình xử lý.”

Mở PaymentService.cash, SepayService.createPayment, PaymentFulfillmentService.createArtifacts và PaymentService.receipt.

### Phần 5 — Ra vào và đặt lịch, 4 phút

“Check-in yêu cầu lễ tân xác nhận danh tính, hội viên ACTIVE và có ảnh. Nếu vào theo gói, phải có Membership còn hiệu lực với CENTER_ACCESS và không đang bảo lưu. Nếu vào học khóa, hệ thống kiểm tra enrollment và cửa sổ quanh giờ học, thay vì bắt buộc quyền từ Membership.

CenterVisit ghi lượt vào ra. Nếu có lượt chưa đóng mà hội viên vào lại, hệ thống tự đóng lượt cũ rồi tạo lượt mới. Check-in không phải điểm danh buổi học; điểm danh là nghiệp vụ của Coach.

Khi đặt lịch thay hội viên, lễ tân vẫn chịu các điều kiện như hội viên: session chưa bắt đầu, đủ benefit, không trùng giờ, chưa đặt trùng và còn chỗ. Với PT, coach phải đúng assignment của hội viên. Hủy chỉ được trước buổi học ít nhất hai giờ.

Nếu lớp đầy, lễ tân đưa hội viên vào waitlist. Khi có chỗ, hệ thống kiểm tra lại điều kiện người đứng đầu và tạo booking nếu hợp lệ.”

Mở CenterVisitService.checkIn, CenterVisitRepository, BookingService.book/cancel, WaitlistPromotionService.promoteAvailable.

### Phần 6 — Bảo lưu, chấm dứt và hoàn gói, 4 phút

“Bảo lưu bắt đầu bằng yêu cầu của hội viên hoặc lễ tân tạo hộ. Manager duyệt và tính phí, lễ tân thu phí tiền mặt, sau đó hệ thống lên lịch và kích hoạt đúng ngày. Phí là 20% đơn giá ngày nhân số ngày bảo lưu. Khi kết thúc, hệ thống cộng số ngày thực bảo lưu vào hạn gói. Membership không có trạng thái FROZEN; kỳ bảo lưu là dữ liệu riêng.

Chấm dứt cũng cần Manager duyệt, với hai chế độ ngay lập tức hoặc cuối chu kỳ. Chấm dứt không xóa tài khoản và không tự hoàn tiền.

Hoàn tiền Membership hiện hỗ trợ nguồn tiền mặt đã PAID, liên kết gói TERMINATED hoặc RESERVED. Lễ tân tạo request, Manager duyệt mức tiền, lễ tân chi và ghi bằng chứng, Manager xác nhận cuối để tạo credit note. Công thức của TERMINATED xét phần thời gian còn lại và ngưỡng 30%; RESERVED dùng mức 80% giá gói.”

Mở MembershipFreezeService.fee/collectCash, MembershipTerminationExecutionService.executeImmediate, MembershipRefundService.cashDisbursement/finalApproval.

### Phần 7 — Khóa học, 5 phút

“Khóa học dùng mô hình Course, CourseRun, CourseSession và Enrollment. Khi lễ tân đăng ký hộ, hệ thống kiểm tra trạng thái đợt, thời gian đăng ký, xung đột lịch và sức chứa. Chỗ đã dùng không chỉ gồm người đã trả tiền mà còn gồm hold, lời mời waitlist và transfer hold.

Order khóa giữ chỗ tối đa 15 phút, hoặc ngắn hơn nếu sắp đóng đăng ký. Sau thu tiền mặt hoặc SePay, hệ thống tạo Enrollment và phân bổ học phí theo từng buổi. Không áp dụng cửa sổ 24 giờ của Membership cho khóa học.

Waitlist khóa gửi lời mời, sau đó lễ tân có thể accept hộ để tạo order; nó không tự ghi danh miễn phí. Khi trung tâm đổi lịch, lễ tân có thể ghi nhận lựa chọn tiếp tục, xin rút hoặc xin chuyển của hội viên.

Chuyển khóa cần đề nghị của Manager; nếu phải trả thêm thì tạo đơn phần chênh lệch, còn credit dư sẽ tạo nghĩa vụ hoàn. Rút khóa lưu snapshot giá trị chưa dùng ngay lúc yêu cầu; dưới ngưỡng hoàn tiền vẫn có thể rút nếu còn buổi chưa bắt đầu.

Hoàn tiền khóa dùng nghĩa vụ hoàn riêng. Manager phê duyệt, lễ tân ghi nhận chi CASH hoặc BANK_TRANSFER khi đủ điều kiện, rồi Manager xác minh số tiền và chứng cứ. Nó không dùng cùng quy trình hoàn Membership.”

Mở CourseOrderService.create, CourseWaitlistService.accept, CourseWithdrawalPolicy, CourseRefundObligationService.recordDisbursement.

### Phần 8 — Tính nhất quán và kiểm chứng, 2 phút

“Em dùng transaction để các bản ghi của một thao tác cùng thành công hoặc rollback. Khóa hàng và constraint tránh tranh chỗ cuối hay tạo giao dịch trùng. Idempotency xử lý việc client gửi lại do mất mạng; expectedVersion tránh quyết định trên dữ liệu đã cũ. Audit lưu người thao tác, notification chuyển việc tới người xử lý tiếp theo.

Bản cleanup được kiểm chứng bằng unit/web, integration trên PostgreSQL Testcontainers riêng, so sánh OpenAPI và ma trận phân quyền. Em tra số test và trạng thái gate cuối tại [bảng kiểm chứng](../source-guide/VALIDATION.md); demo frontend vẫn cần thực hiện riêng.”

## 2. Phiên bản rút gọn khi chỉ có 10 phút

1. Vai trò, ranh giới quyền và kiến trúc: 1 phút.
2. Tạo hội viên → mua gói tiền mặt → Membership/Receipt: 2 phút.
3. Check-in → booking → cancel/waitlist: 2 phút.
4. Freeze/termination/refund và các bước Manager duyệt: 2 phút.
5. CourseOrder/hold 15 phút → enrollment, khác Membership: 2 phút.
6. Transaction, khóa, idempotency và kết quả kiểm chứng: 1 phút.

Nếu thời gian ít, không đọc danh sách toàn bộ DTO. Chọn một luồng chính để mở code xuyên suốt, dùng phụ lục trả lời các chức năng còn lại.

## 3. Kịch bản demo A — Từ khách mới tới sử dụng dịch vụ

### Chuẩn bị dữ liệu

Một Receptionist ACTIVE; một Manager cho các bước duyệt; Offer/Plan ACTIVE có CENTER_ACCESS và benefit đúng lớp; một ClassSession tương lai, còn chỗ, cách giờ demo đủ xa để hủy; một ảnh hợp lệ; email/phone hội viên chưa tồn tại. Với PT phải có CoachAssignment phù hợp do Manager tạo.

Không dùng khách hàng thật để minh họa thao tác reset, thu tiền, check-in hay hoàn tiền. Không gọi webhook production để tạo tiền giả. Dữ liệu demo nên ở môi trường local/test đã chuẩn bị.

### Trình tự trình diễn

1. Đăng nhập Receptionist, chỉ role trong AuthResponse.
2. POST /api/v1/reception/members tạo hội viên; ghi lại accountId và memberId. Nói rõ hai loại ID.
3. GET search bằng code/phone; chỉ ra chưa có activeMembership.
4. Upload ảnh qua multipart file.
5. POST cash-payments với offerId và key riêng; chỉ payment PAID, membershipId, receiptId. Luồng này không tạo bank order trước.
6. GET memberships để thấy gói ACTIVE; GET receipt bằng actor vừa xử lý tiền mặt.
7. POST center-visits với identityVerified=true; chỉ 201 và visit source MEMBERSHIP.
8. Gửi lại cùng key; kiểm tra trả visit cũ và không thêm lượt.
9. GET class-sessions; dùng UUID accountId đặt booking.
10. GET bookings; hủy trước mốc 2 giờ; chỉ BOOKED → CANCELLED.
11. GET center-visits/open; checkout bằng UUID; chỉ checkedOutAt/source.
12. Mở notification; đánh dấu đọc. Audit nếu muốn trình diễn phải dùng tài khoản Manager có quyền, không dùng lễ tân.

### Các request mẫu

Các ID trong dấu <…> phải thay bằng dữ liệu trả từ API trước, không phải UUID hợp lệ sẵn.

~~~http
POST /api/v1/reception/members
Authorization: Bearer <receptionist-access-token>
Content-Type: application/json

{
  "fullName": "Nguyễn Minh An",
  "phone": "0901234567",
  "email": "minhan-demo@example.com",
  "birthDate": "2000-01-01"
}
~~~

~~~http
POST /api/v1/reception/members/<member-code>/cash-payments
Authorization: Bearer <receptionist-access-token>
Idempotency-Key: demo-cash-001
Content-Type: application/json

{"offerId":"<offer-uuid>"}
~~~

~~~http
POST /api/v1/reception/center-visits
Authorization: Bearer <receptionist-access-token>
Idempotency-Key: demo-visit-001
Content-Type: application/json

{"memberId":"<member-code>","identityVerified":true}
~~~

~~~http
POST /api/v1/reception/members/<member-account-uuid>/bookings
Authorization: Bearer <receptionist-access-token>
Content-Type: application/json

{"sessionId":"<class-session-uuid>"}
~~~

## 4. Kịch bản demo B — Các luồng phải duyệt

### Bảo lưu

Lễ tân tạo request ngày hợp lệ → đổi sang Manager duyệt → lấy version mới → đổi về lễ tân thu phí → thấy SCHEDULED. Muốn minh họa ACTIVE/COMPLETED thì dùng test clock/fixture phù hợp; không sửa đồng hồ máy hoặc sửa DB production để ép trạng thái.

Body tạo: startDate, endDate, reason. Body duyệt/thu tiền: expectedVersion lấy từ dữ liệu mới nhất. Lấy ví dụ gói 30 ngày giá 3 triệu và freeze 10 ngày để giải thích phí 200.000 VND.

### Hoàn Membership

Chuẩn bị Membership nguồn CASH đã TERMINATED còn đủ ngưỡng hoặc RESERVED. GET refund-eligible-sources → lễ tân tạo request theo đúng paymentId/membershipId và trần suggested → Manager first approval → lễ tân cash-disbursement với evidence → Manager final approval → chỉ COMPLETED và credit note. Không cố hoàn một gói ACTIVE bình thường rồi cho rằng API hỏng.

## 5. Kịch bản demo C — Khóa học

1. Chuẩn bị Course PUBLISHED, CourseRun OPEN_FOR_REGISTRATION, còn thời gian và chỗ.
2. Lễ tân tạo CourseOrder cho UUID hội viên với courseRunId/key; chỉ reservation HELD và expiresAt.
3. Thu tiền mặt trên chính course order đó; chỉ order PAID/reservation ENROLLED/enrollment.
4. Tra cứu enrollment và next session.
5. Nếu check-in khóa: chọn CourseSession có thời gian đúng cửa sổ −30/+30 phút và enrollment ACTIVE. Không dùng một buổi còn nhiều ngày rồi mong check-in được.
6. Demo rút khóa bằng request có reason; chỉ snapshot remainingUnits/refund và trạng thái WITHDRAWAL_PENDING.
7. Manager duyệt → obligation PENDING_REVIEW → Manager duyệt refund. Lễ tân chỉ chi khi disbursementEligible; đối với rút tự nguyện thường phải đợi run COMPLETED.

Muốn demo waitlist nên dùng một run khác đã đầy và fixture đủ điều kiện; muốn demo transfer phải có schedule-change response và offer do Manager duyệt. Không lấy một offer chưa tồn tại để minh họa đường đi thành công.

## 6. Những nhánh lỗi nên chuẩn bị để demo

- Tạo hội viên với điện thoại/email đã có → xung đột định danh.
- Search gửi cả memberId và phone → validation, vì phải đúng một tiêu chí.
- Check-in thiếu identityVerified hoặc ảnh → bị chặn trước khi tạo visit.
- Booking chồng giờ/thiếu benefit/đầy chỗ → không tạo booking.
- Hủy booking sau mốc start − 2 giờ → bị chặn.
- Tạo MembershipOrder khi có pending hoặc RESERVED → bị chặn.
- Dùng key CourseOrder đã dùng cho run/member khác → conflict.
- Duyệt/chi refund với expectedVersion cũ → conflict, cần tải lại.
- Dùng token Receptionist gọi endpoint Manager → 403 ở tầng quyền.
- Course reconciliation khi hold hết hạn → không hồi sinh chỗ.

## 7. Câu hỏi phản biện và câu trả lời gợi ý

### 1. Role lễ tân được xác định ở đâu?

Role thuộc Account và được phát trong JWT; SecurityConfiguration ánh xạ role thành authority. ActiveAccountJwtValidator kiểm tra trạng thái/authVersion DB; service còn kiểm tra actor và đối tượng. Frontend không có quyền tự đổi role.

### 2. Tại sao kiểm tra service khi đã có SecurityConfiguration?

Security quyết định ai được vào endpoint; service kiểm tra account/đối tượng/trạng thái hiện tại và điều kiện nghiệp vụ. Ví dụ có role Receptionist chưa đủ để tạo booking cho member bị đình chỉ hoặc lớp đầy.

### 3. Lễ tân có thể đặt lịch hộ mọi người mà không cần gói không?

Không. Booking theo ClassSession vẫn phải có Membership đủ benefit tại giờ buổi học, không freeze, không trùng lịch và còn chỗ. Khóa học có entitlement riêng từ enrollment, không dùng chính xác luồng booking Membership.

### 4. Vì sao tạo member ở quầy ACTIVE ngay còn đăng ký online lại chờ email?

Đó là hai luồng triển khai khác nhau. ReceptionMemberService tạo ACTIVE trực tiếp; online có email verification. Với online chưa xác minh, lễ tân chỉ tạo manual activation request để Manager quyết định.

### 5. Có hash mật khẩu không? Mật khẩu ban đầu là gì?

Có, PasswordEncoder dùng BCrypt. Tạo/reset hội viên ở quầy lấy số điện thoại hiện tại làm mật khẩu đầu vào trước khi hash. Đây là hành vi cần giải thích thẳng, không gọi nó là mật khẩu ngẫu nhiên hoặc OTP.

### 6. Reset có đăng xuất mọi thiết bị ngay không?

Thu hồi tất cả refresh token của hội viên, nhưng Account.changePassword không tăng authVersion. Vì vậy không khẳng định toàn bộ access token JWT đã phát mất hiệu lực ngay; chúng còn chịu TTL và kiểm tra ACTIVE/authVersion.

### 7. Không thấy hội viên khi search là do không tồn tại?

Chưa chắc. Search thường chỉ hiển thị MEMBER ACTIVE/SUSPENDED; online INACTIVE chờ email có endpoint pending-accounts riêng. Có thể sai loại ID hoặc tiêu chí không đúng.

### 8. Vì sao không truyền giá từ frontend khi thu tiền?

Giá phải lấy từ Offer/Plan hoặc CourseRun đã kiểm tra tại backend và lưu snapshot. Tin giá client sẽ cho phép sửa giá và làm sai lịch sử giao dịch.

### 9. Hai lễ tân mua chỗ cuối cùng có bị vượt capacity không?

Service khóa hàng ClassSession hoặc CourseRun trước khi kiểm tra và ghi chỗ; số chỗ được tính lại trong transaction. DB constraints bảo vệ thêm. Unit tests không thay thế integration test concurrency, nên nói rõ mức kiểm chứng.

### 10. Vì sao cần Idempotency-Key?

Client có thể gửi lại vì mất mạng hoặc người dùng bấm nhiều lần. Key giúp nhận ra cùng một thao tác và trả kết quả đã có. Không dùng key cũ cho giao dịch mới; phạm vi replay phụ thuộc từng service.

### 11. Snapshot và version khác nhau thế nào?

Snapshot giữ giá trị lịch sử như giá/học phí/căn cứ hoàn. Version phát hiện dữ liệu hiện tại đã bị người khác thay đổi trước khi mình quyết định. Chúng giải quyết hai vấn đề khác nhau.

### 12. Receipt và Payment có phải một bản ghi không?

Không. Payment là trạng thái thu tiền; Receipt là chứng từ sinh khi hoàn tất giao dịch Membership. Credit note phản ánh hoàn/điều chỉnh, giữ lại receipt gốc. Course dùng payment/enrollment riêng, không mặc nhiên có receipt trong cùng bảng.

### 13. Gia hạn là cộng số ngày vào gói đang dùng?

Theo code này, mua khi có ACTIVE tạo RESERVED. Lifecycle kích hoạt gói kế tiếp khi đủ điều kiện. Bảo lưu mới có cơ chế cộng creditedDays vào endsAt của gói.

### 14. Tại sao webhook có thể trả 200 nhưng chưa PAID?

200 có thể chỉ xác nhận đã nhận và lưu kết quả xử lý sự kiện. Nếu amount/reference/destination/time không khớp thì processed=false và outcome giải thích; phải đối soát, không cấp quyền sai.

### 15. Lễ tân có quyền đọc mọi receipt không?

Không. Với Membership, list và detail kiểm tra processed_by_account_id là chính actor lễ tân. Manager có phạm vi khác; quyền đọc payment result cũng khác quyền đọc receipt.

### 16. Check-in có cần phải BOOKED không?

Check-in theo Membership kiểm tra gói có CENTER_ACCESS, không buộc phải có booking. Check-in khóa dùng enrollment/session hợp lệ. Booking là giữ chỗ học; visit là vào trung tâm.

### 17. Đủ gói nhưng thiếu ảnh có vào được không?

CenterVisitService.checkIn chặn khi profileImageUrl trống và khi identityVerified chưa true. Lễ tân cần hoàn thành xác minh/ảnh trước.

### 18. Hủy đúng trước 2 giờ có được không?

Có. Điều kiện từ chối là now.isAfter(startTime.minus(2 hours)), nên đúng ranh giới còn được. Đây là lý do phải đọc toán tử và kiểm tra mốc thời gian cụ thể.

### 19. Tại sao waitlist phải kiểm tra lại lúc có chỗ?

Giữa lúc vào hàng và được promote, gói có thể hết hạn, member bị đình chỉ hoặc đã đặt một lịch trùng. Điều kiện lúc join không đảm bảo lúc promote.

### 20. Bảo lưu có đổi Membership.status không?

Không có enum FROZEN trong MembershipStatus. Freeze được thể hiện bằng request và period; các query kiểm tra kỳ bảo lưu hiệu lực để chặn sử dụng.

### 21. Chấm dứt có tự hoàn tiền không?

Không. Termination xử lý quyền sử dụng, visit/booking/assignment; refund kiểm tra nguồn tiền, trạng thái, ngưỡng và đi qua các bước duyệt/chi riêng.

### 22. Vì sao Manager phải xác nhận lại sau khi lễ tân trả tiền?

Đó là kiểm soát hai vai trò. Duyệt số tiền trước chi chưa chứng minh tiền đã thực trả đúng. Bước cuối kiểm tra dữ liệu chi và hoàn thành ghi nhận tài chính.

### 23. Tự nguyện rút khóa còn dưới 70% số buổi có bị cấm rút không?

Không. Khi còn ít nhất một buổi chưa bắt đầu và trạng thái phù hợp vẫn có thể rút, nhưng refund tự nguyện bằng 0 nếu dưới 70%. Ngưỡng này là điều kiện tiền hoàn, không phải mọi điều kiện rút.

### 24. Vì sao học phí chia theo từng buổi?

Để xác định phần giá trị chưa dùng khi rút, chuyển hoặc gián đoạn. Chia bằng số nguyên và phần dư giữ tổng allocation bằng đúng số tiền đã đóng, tránh sai số số thực.

### 25. Course refund APPROVED có trả ngay được không?

Không phải mọi nguồn. Thường phải chờ run COMPLETED; một số nguồn như hủy run, payment exception, transfer credit dư có ngoại lệ. Cần đọc disbursementEligible và điều kiện service.

### 26. Endpoint disburse có thực sự chuyển tiền ngân hàng không?

Code này ghi nhận số tiền, method, reference và evidence của việc chi, rồi chờ Manager verify. Không có lệnh chuyển tiền ngân hàng trong recordDisbursement.

### 27. Tại sao notification không gửi trực tiếp trong transaction?

NotificationWriter enqueue outbox cùng giao dịch; dispatcher xử lý sau. Điều này giữ sự kiện đã ghi nhận để thử xử lý lại và tránh mất thông báo vì tác vụ phát sinh bên ngoài thất bại.

### 28. Có phải mọi chức năng lễ tân nằm trong package reception?

Không. Mã được chia theo feature: identity/receptionmember, membership, scheduling, centervisit, course, emailverification và các chức năng chung. Role là phạm vi quyền đi qua nhiều feature, không đồng nghĩa một package duy nhất.

### 29. Test đã chứng minh được gì?

Các bộ unit/web và integration được ghi trong [bảng kiểm chứng](../source-guide/VALIDATION.md). Integration dùng PostgreSQL thật trong container riêng, có kiểm tra transaction/rollback và các tình huống cạnh tranh trong test hiện có. Chưa chứng minh kết nối ngân hàng/email/storage thật hoặc thao tác giao diện frontend.

### 30. Nếu hỏi một quy tắc không nhớ?

Nêu đúng controller/service rồi mở phương thức trong phụ lục. Không suy đoán theo tên endpoint hoặc README cũ. Nói: “Em kiểm tra lại điều kiện tại service vì đây là nơi quyết định trạng thái này.” Sau đó chỉ vào điều kiện và test liên quan.

## 8. Thứ tự mở code nên tập trước

1. SecurityConfiguration → giải thích quyền và JWT.
2. ReceptionMemberController → ReceptionMemberCreateRequest → ReceptionMemberService → Account/MemberProfile.
3. PaymentController.cash → PaymentService.cash → PaymentFulfillmentService.createArtifacts.
4. CenterVisitController.checkIn → CenterVisitService.checkIn → CenterVisitRepository.
5. BookingController.bookForMember → BookingService.book/cancel → WaitlistPromotionService.
6. MembershipFreezeService.fee/collectCash → MembershipFreezeExecutionService.activate.
7. MembershipRefundService.create/cashDisbursement/finalApproval.
8. CourseOrderService.create/holdExpiresAt → PaymentFulfillmentService.fulfillCourse.
9. CourseWithdrawalPolicy → CourseWithdrawalService.approve → CourseRefundObligationService.recordDisbursement.
10. NotificationWriter và một test đã chạy để kết thúc phần kỹ thuật.

Mỗi lần mở code, trình bày theo cùng công thức: **ai gọi → đầu vào gì → kiểm tra gì → đọc/ghi bảng nào → trạng thái đổi ra sao → kết quả/lỗi gì**. Không đọc tuần tự từng dòng import/constructor trước hội đồng.

## Câu hỏi bổ sung sau cleanup

- **Tại sao class lễ tân vẫn ở nhiều domain?** Vì role là quyền gọi, còn package tổ chức theo nghiệp vụ; payment, booking, membership và course cùng phục vụ nhiều role. Dùng API_INDEX để đi từ URL tới facade, rồi mở component/repository cùng package.
- **Tách class có tạo transaction mới không?** Không. Facade giữ annotation cũ; component nội bộ tham gia công việc trong transaction đang có. Riêng policy after-commit và noRollbackFor giữ nguyên như source trước cleanup.
- **Vì sao Swagger có v3 nhưng API có v1?** `/api/v1` là prefix API của ứng dụng; `/v3/api-docs` là đường dẫn tài liệu OpenAPI 3. Không chứng minh có nghiệp vụ API v2/v3 chỉ từ tên này. Swagger UI được cấu hình ở `/api/v1/docs`.
- **Hủy booking có phải hủy session không?** Không. Booking là chỗ của một hội viên; hủy session là tác động cả buổi và bị chặn khi còn hội viên theo rule hiện tại.
- **Dòng code thay đổi thì tra thế nào?** Dùng `Class#method` làm khóa chính, số dòng là liên kết tiện dụng của bản bàn giao. Tự luyện mười câu trong `docs/source-guide/REHEARSAL.md`.
