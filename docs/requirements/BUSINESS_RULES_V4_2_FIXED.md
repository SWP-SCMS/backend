# SWP GYM

BẢN CƠ SỞ QUY TẮC NGHIỆP VỤ TỔNG THỂ

Hệ thống Quản lý Trung tâm Thể thao SCMS

| Trạng thái tài liệu | Bản cơ sở của nhóm - thay thế các phiên bản Quy tắc nghiệp vụ trước đây có nội dung xung đột |
| --- | --- |
| Phiên bản | v4.2 |
| Ngày | 26/09/2026 |
| Mục đích sử dụng chính | Luồng nghiệp vụ, biểu đồ Hoạt động và Trạng thái, ERD logic, ràng buộc API và backend, kiểm tra frontend và truy vết công việc |
| Hiệu lực áp dụng | Cả sáu phần có tính quy phạm trong tài liệu này được áp dụng đồng thời; phần Tóm tắt vòng đời là góc nhìn tham khảo của các quy tắc chi phối |

Bản cơ sở v4.2 xác định quyền hạn, điều kiện, kiểm soát vòng đời, kiểm tra dữ liệu, biện pháp bảo mật, yêu cầu toàn vẹn kỹ thuật và ranh giới MVP cho SCMS. Nếu một Quy tắc nghiệp vụ trước đây xung đột với tài liệu này, v4.2 là nguồn tham chiếu hiện hành.

## 1 Phạm vi hệ thống và vai trò

Phần này xác định ranh giới sản phẩm, bốn vai trò nghiệp vụ và mô hình tài khoản được mọi quy tắc theo lĩnh vực sử dụng.

| ID | Quy tắc nghiệp vụ |
| --- | --- |
| BR-SCP-01 | SCMS có bốn vai trò nghiệp vụ: MEMBER, RECEPTIONIST, COACH và MANAGER. |
| BR-SCP-02 | Mỗi Account có đúng một vai trò. Vai trò được gán khi Account được tạo và được giữ cố định trong toàn bộ vòng đời của Account đó. |
| BR-SCP-03 | Account lưu dữ liệu xác thực, vai trò và trạng thái. Profile lưu thông tin cá nhân và thông tin riêng theo vai trò gắn với Account đó. |
| BR-SCP-04 | MVP có hai Membership Plan cố định: BASIC và PLUS. Manager quản lý các Membership Offer có thể bán dựa trên các Plan này. |
| BR-SCP-05 | MVP hỗ trợ Session lớp Group hoặc Yoga và Session PT 1-1. Cả hai đều được lập lịch và đặt chỗ theo từng Session riêng lẻ. |
| BR-SCP-06 | Phạm vi sản phẩm bao gồm Account và Profile, Membership, Order, Payment và Receipt, Class và Session, Booking, Center Visit, Attendance, Personal Coach và Training, Notification, Audit và Reporting. |
| BR-SCP-07 | Các quy tắc trong phần Kiểm tra dữ liệu, Bảo mật và toàn vẹn kỹ thuật, cùng Giới hạn MVP là các phần có tính quy phạm của bản cơ sở này. |

### Tóm tắt trách nhiệm theo vai trò

| Vai trò | Trách nhiệm chính | Vòng đời Account |
| --- | --- | --- |
| MEMBER | Duy trì hồ sơ của mình, mua Membership, đặt các Session đủ điều kiện, xem biên nhận và lịch sử của mình, đồng thời sử dụng Center Check-in. | ACTIVE <-> SUSPENDED |
| RECEPTIONIST | Hỗ trợ đăng ký Member và cập nhật hồ sơ, tạo Order và Payment tại quầy lễ tân, đặt hoặc hủy chỗ cho Member và ghi nhận mọi lần vào trung tâm. | ACTIVE -> INACTIVE |
| COACH | Xem các Session được phân công, ghi Attendance và quản lý dữ liệu Training trong phạm vi Coaching được giao. | ACTIVE -> INACTIVE |
| MANAGER | Quản lý Account nhân viên và Manager, Offer, Class, Room, lịch, Session, báo cáo và Audit History. | ACTIVE -> INACTIVE với cơ chế bảo vệ Manager ACTIVE cuối cùng |

## 2 Quy tắc nghiệp vụ theo lĩnh vực

Các quy tắc dưới đây nêu quyền hạn của tác nhân, điều kiện nghiệp vụ, hành vi vòng đời và quan hệ phụ thuộc giữa các lĩnh vực. Yêu cầu về định dạng và toàn vẹn được xác định ở phần sau của tài liệu.

### Tài khoản và hồ sơ

| ID | Quy tắc nghiệp vụ |
| --- | --- |
| BR-ACC-01 | Member có thể tự đăng ký. Account Member vượt qua kiểm tra hợp lệ sẽ được tạo ở trạng thái ACTIVE. |
| BR-ACC-02 | Receptionist có thể tạo Account MEMBER tại quầy lễ tân. Account được tạo ở trạng thái ACTIVE và dùng số điện thoại đăng ký hiện tại của Member làm mật khẩu mặc định. |
| BR-ACC-03 | Manager có thể tạo Account COACH, RECEPTIONIST và MANAGER. Mỗi Account mới được tạo ở trạng thái ACTIVE và dùng số điện thoại đăng ký hiện tại của người đó làm mật khẩu mặc định. |
| BR-ACC-04 | Manager đang ACTIVE có thể tạo một Account MANAGER khác. |
| BR-ACC-05 | SCMS không cung cấp thao tác đổi vai trò. Account của nhân viên hoặc Manager giữ nguyên vai trò đã được gán trong toàn bộ vòng đời của Account đó. |
| BR-ACC-06 | Khi một người thay đổi vị trí công việc, Manager phải chuyển Account nhân viên hoặc Manager cũ sang INACTIVE và tạo Account mới với vai trò mới. Account cũ và toàn bộ lịch sử vẫn được lưu giữ. |
| BR-ACC-07 | Account có thể ở trạng thái ACTIVE mà không có Membership còn hiệu lực. Trạng thái Account và hiệu lực Membership là hai cơ chế kiểm soát độc lập. |
| BR-ACC-08 | Ảnh hồ sơ là tùy chọn khi tạo Account Member, nhưng Profile Member phải có ảnh hồ sơ trước khi Receptionist có thể hoàn tất lần Center Check-in đầu tiên của Member. |
| BR-ACC-09 | Mục tiêu thể chất và liên hệ khẩn cấp là các trường tùy chọn của Profile Member. Member hoặc Receptionist có thể bổ sung hoặc cập nhật các trường này sau khi Account được tạo. |
| BR-ACC-10 | Member có thể xem và cập nhật các trường được phép trong Profile của chính mình. Receptionist có thể xem và cập nhật Profile Member khi hỗ trợ tại quầy lễ tân. |
| BR-ACC-11 | Member và Receptionist có thể thêm hoặc cập nhật ảnh hồ sơ của Member trong phạm vi Profile được phép. |
| BR-ACC-12 | Manager có thể xem, tìm kiếm và cập nhật thông tin Account hoặc Profile được phép của các Account COACH, RECEPTIONIST và MANAGER. |
| BR-ACC-13 | Chỉ Manager mới có thể SUSPEND hoặc REACTIVATE Account MEMBER. |
| BR-ACC-14 | Member ở trạng thái SUSPENDED không thể đăng nhập, hoàn tất Center Check-in, tạo Booking hoặc sử dụng quyền lợi Membership. |
| BR-ACC-15 | Khi Member bị SUSPENDED, tất cả Booking BOOKED trong tương lai được chuyển sang CANCELLED và sức chứa Session tương ứng được giải phóng. |
| BR-ACC-16 | Việc đình chỉ không xóa lịch sử và không tạm dừng hoặc gia hạn thời gian Membership. Thời hạn Membership vẫn tiếp tục trôi khi Member ở trạng thái SUSPENDED. |
| BR-ACC-17 | Account Member không bị xóa cứng. Dữ liệu lịch sử về Membership, Order, Payment, Booking, Attendance, Center Visit và Training phải tiếp tục được liên kết với Account đó. |
| BR-ACC-18 | Chỉ Manager mới có thể chuyển Account COACH, RECEPTIONIST hoặc một MANAGER khác sang INACTIVE. INACTIVE là trạng thái kết thúc việc làm của Account đó. |
| BR-ACC-19 | Account nhân viên hoặc Manager ở trạng thái INACTIVE không thể đăng nhập hoặc thực hiện nghiệp vụ mới. Mọi bản ghi nghiệp vụ đã tạo trước đó vẫn được cung cấp cho mục đích xem lịch sử và báo cáo theo quyền hạn. |
| BR-ACC-20 | Manager chỉ có thể chuyển một Manager khác sang INACTIVE khi sau thao tác vẫn còn ít nhất một Account MANAGER khác ở trạng thái ACTIVE. |
| BR-ACC-21 | Trước khi một Coach chuyển sang INACTIVE, Manager phải thay thế hoặc xử lý theo cách khác mọi Session tương lai được gán cho Coach đó, đồng thời kết thúc hoặc phân công lại mọi Personal Coach Assignment đang hoạt động. |
| BR-ACC-22 | Manager đang ACTIVE có thể đặt lại mật khẩu cho Account MEMBER, COACH, RECEPTIONIST và một MANAGER khác. Receptionist có thể đặt lại mật khẩu cho Account MEMBER. |
| BR-ACC-23 | Thao tác đặt lại được ủy quyền sẽ đặt mật khẩu thành số điện thoại đăng ký hiện tại của chủ Account. Người dùng không bị bắt buộc đổi mật khẩu ở lần đăng nhập tiếp theo, nhưng có thể đổi sau khi đăng nhập. |
| BR-ACC-24 | Người dùng đã xác thực có thể đổi mật khẩu của Account của chính mình sau khi cung cấp đúng mật khẩu hiện tại; mật khẩu mới phải vượt qua các quy tắc kiểm tra mật khẩu hiện hành. |
| BR-ACC-25 | Mọi thao tác tạo Account, đặt lại mật khẩu, đình chỉ hoặc kích hoạt lại Member, và chuyển staff hoặc Manager sang INACTIVE phải tạo Audit event. |
| BR-ACC-26 | Account MANAGER đầu tiên được hệ thống provision/seed khi triển khai. Sau đó, chỉ Manager đang ACTIVE mới có thể tạo thêm Account MANAGER theo BR-ACC-04. |
| BR-ACC-27 | Khi Account MEMBER được tạo thành công, hệ thống sinh một Member ID duy nhất cho Member đó. Member ID được giữ cố định và có thể dùng cùng số điện thoại để Receptionist tra cứu Member tại quầy. |
| BR-AUTH-01 | User đăng nhập bằng email hoặc số điện thoại đã đăng ký kết hợp với mật khẩu của Account. |
| BR-AUTH-02 | Đăng nhập chỉ thành công khi thông tin đăng nhập khớp với Account và trạng thái Account cho phép đăng nhập. MEMBER phải ở trạng thái ACTIVE; COACH, RECEPTIONIST và MANAGER phải ở trạng thái ACTIVE. |
| BR-AUTH-03 | Yêu cầu đăng nhập có thông tin xác thực không hợp lệ hoặc Account không đủ điều kiện đăng nhập phải bị từ chối. |
| BR-AUTH-04 | Thông báo lỗi đăng nhập không được tiết lộ riêng lẻ rằng email, số điện thoại hay mật khẩu nào là thành phần sai của thông tin xác thực. |

### Gói hội viên, ưu đãi và Membership

| ID | Quy tắc nghiệp vụ |
| --- | --- |
| BR-PKG-01 | Membership BASIC còn hiệu lực cho phép Member sử dụng khu vực gym và tập luyện của trung tâm. |
| BR-PKG-02 | Membership PLUS còn hiệu lực bao gồm quyền truy cập của BASIC và bổ sung điều kiện để đặt Session Yoga hoặc Group, Session PT 1-1, cũng như sử dụng các tính năng Personal Coach và Training. |
| BR-OFF-01 | Manager có thể tạo, xem, cập nhật và chuyển Membership Offer giữa ACTIVE và INACTIVE. |
| BR-OFF-02 | Mỗi Membership Offer thuộc đúng một Plan cố định là BASIC hoặc PLUS, đồng thời xác định giá bán, thời hạn và thông tin hiển thị của Offer đó. |
| BR-OFF-03 | Chỉ Membership Offer ở trạng thái ACTIVE mới có thể được chọn khi tạo Order mới. |
| BR-OFF-04 | Offer đã phát sinh Order hoặc Membership không bị xóa cứng. Manager dùng INACTIVE để dừng bán trong tương lai. |
| BR-OFF-05 | Việc cập nhật Offer không làm thay đổi giá, thời hạn, Plan hoặc ảnh chụp dữ liệu lịch sử khác đã lưu cho Order hoặc Membership trước đó. |
| BR-MEM-01 | Một Member không thể có các khoảng thời gian Membership ACTIVE chồng lấn nhau. |
| BR-MEM-02 | Một Membership thuộc đúng một Member và được lưu giữ làm lịch sử sau khi hết hạn. |
| BR-MEM-03 | Membership mới chỉ chuyển sang ACTIVE sau khi một Payment hợp lệ được xác nhận PAID. startTime của Membership là thời điểm hệ thống hoàn tất việc cấp quyền lợi. |
| BR-MEM-04 | Khi kết thúc thời hạn hiệu lực, Membership chuyển từ ACTIVE sang EXPIRED. |
| BR-MEM-05 | Membership EXPIRED không được kích hoạt lại hoặc thay đổi. Muốn tiếp tục dịch vụ phải chọn Offer mới, tạo Order, Payment và Membership mới. |
| BR-MEM-06 | Membership hết hạn không làm thay đổi trạng thái Account; việc này chỉ loại bỏ các quyền lợi phụ thuộc vào Membership còn hiệu lực. |
| BR-MEM-07 | Member và các vai trò vận hành được ủy quyền có thể xem lịch sử Membership của Member và thông tin thanh toán liên quan trong phạm vi quyền truy cập. |
| BR-MEM-08 | Member đang có Membership ở trạng thái ACTIVE không được tạo Order mua Membership mới. Việc mua Membership mới chỉ được thực hiện sau khi Membership hiện tại chuyển sang EXPIRED. |

### Đơn hàng, thanh toán và biên nhận

| ID | Quy tắc nghiệp vụ |
| --- | --- |
| BR-ORD-01 | Việc mua Membership bắt đầu bằng một Order; quyền lợi không được cấp trực tiếp từ Offer. |
| BR-ORD-02 | Member có thể tạo Order Bank Transfer cho Account của chính mình. Receptionist có thể tạo Order cho Member sau khi xác nhận Member và Offer được chọn; tùy chọn Cash chỉ được sử dụng trong nghiệp vụ thanh toán tại quầy lễ tân. |
| BR-ORD-03 | Đối với Bank Transfer, Order được tạo ở trạng thái PENDING_PAYMENT và xác định Member, Offer ACTIVE được chọn, số tiền và phương thức thanh toán. Đối với Cash, Order chưa được ghi nhận chính thức cho đến khi Receptionist xác nhận đã thực sự nhận tiền; khi đó Order được tạo trực tiếp ở trạng thái PAID cùng Payment tiền mặt tương ứng. |
| BR-ORD-04 | Khi Payment hợp lệ liên quan chuyển sang PAID, Order chuyển sang PAID. |
| BR-ORD-05 | Đối với Bank Transfer, mốc 24 giờ là hạn thanh toán tự động. Nếu sau 24 giờ Payment vẫn chưa có kết quả xác định từ provider, Order vẫn giữ PENDING_PAYMENT để Receptionist hoặc Manager thực hiện manual reconciliation; hết 24 giờ tự nó không làm Order EXPIRED. |
| BR-ORD-06 | Bank Transfer Payment FAILED không tự làm Order thất bại khi Order vẫn còn đủ điều kiện cho một Payment attempt mới. Order chỉ chuyển EXPIRED khi manual reconciliation sau payment window xác định không có giao dịch hợp lệ hoặc khi một quy tắc nghiệp vụ khác kết thúc khả năng thanh toán của Order. |
| BR-PAY-01 | MVP hỗ trợ Bank Transfer và Cash. Cash chỉ được sử dụng tại quầy lễ tân và chỉ Receptionist được xác nhận đã nhận tiền mặt; Member tự thanh toán online sử dụng Bank Transfer. |
| BR-PAY-02 | Bank Transfer Payment được tạo ở trạng thái PENDING và thuộc đúng một Order. Cash Payment chỉ được tạo sau khi Receptionist xác nhận đã thực sự nhận tiền và được ghi nhận trực tiếp ở trạng thái PAID. |
| BR-PAY-03 | Đối với chuyển khoản ngân hàng, hệ thống tạo mã QR có đúng số tiền và nội dung chuyển khoản của Order. |
| BR-PAY-04 | Payment chuyển khoản ngân hàng chỉ chuyển sang PAID sau khi hệ thống nhận được xác nhận hợp lệ từ nhà cung cấp hoặc API thanh toán. Việc hiển thị hoặc quét mã QR không phải là xác nhận thanh toán. |
| BR-PAY-05 | Đối với Cash, hệ thống chỉ ghi nhận Order và Payment sau khi Receptionist bấm xác nhận đã thực sự nhận tiền. Khi xác nhận, Cash Payment được tạo trực tiếp ở trạng thái PAID. Nếu khách đổi ý trước khi xác nhận, hệ thống không tạo Order hoặc Payment cho giao dịch tiền mặt chưa hoàn tất. |
| BR-PAY-06 | Khi Payment chuyển sang PAID, hệ thống chuyển Order sang PAID, tạo Membership, ghi nhận giao dịch, phát hành Receipt và tạo Notification liên quan. |
| BR-PAY-07 | Một lần Payment được xác định thất bại chuyển sang FAILED và không tạo Membership. Chỉ khi Payment attempt trước đã ở trạng thái terminal và Order vẫn đủ điều kiện thanh toán thì hệ thống mới cho phép tạo Payment attempt mới; bản ghi Payment FAILED không được tái sử dụng. |
| BR-PAY-08 | Khi manual reconciliation xác định Bank Transfer không có giao dịch hợp lệ và Order chuyển sang EXPIRED, Payment PENDING thuộc Order đó chuyển sang FAILED. Payment đã PAID không bị thay đổi bởi việc hết hạn Order hoặc callback tới trễ. |
| BR-PAY-09 | Bản ghi Payment và giao dịch không bị xóa cứng. Mọi manual reconciliation phải giữ nguyên lịch sử ban đầu và tạo Audit evidence; Payment đã PAID không được chỉnh sửa trực tiếp để thay đổi lịch sử giao dịch đã chốt. |
| BR-RCP-01 | Member có thể xem và tải xuống hoặc in Receipt thuộc Payment của chính mình. |
| BR-RCP-02 | Receptionist có thể xem và in Receipt của giao dịch do chính Receptionist xử lý hoặc của Member đang được hỗ trợ tại quầy lễ tân. |
| BR-RCP-03 | Manager có thể xem Receipt thông qua lịch sử giao dịch và báo cáo được ủy quyền. |
| BR-PAY-10 | Bank Transfer có payment window tự động 24 giờ kể từ khi QR/payment request được tạo thành công. Hệ thống sử dụng thời điểm hết hạn do backend hoặc payment provider ghi nhận làm nguồn thời gian chuẩn. Nếu hết 24 giờ mà kết quả provider vẫn chưa xác định, Payment và Order tiếp tục PENDING để manual reconciliation thay vì tự động FAILED/EXPIRED. |
| BR-PAY-11 | Khi cùng một Order đang có Payment attempt ở trạng thái PENDING, hệ thống không cho tạo thêm Payment attempt mới cho Order đó. |
| BR-PAY-12 | Khi Bank Transfer vẫn PENDING do chưa nhận được confirmation tự động hoặc kết quả provider chưa rõ ràng, Receptionist hoặc Manager được thực hiện manual reconciliation, kể cả sau mốc 24 giờ, sau khi kiểm tra giao dịch thực tế theo Order, amount, transfer content/order code và bằng chứng transaction/provider có sẵn. |
| BR-PAY-13 | Nếu manual reconciliation xác minh giao dịch hợp lệ đã được nhận cho đúng Order, người được ủy quyền có thể chuyển Payment từ PENDING sang PAID. Manual confirmation phải ghi actor, timestamp, reason và evidence/reference vào Audit. |
| BR-PAY-14 | Manual confirmation và automatic provider confirmation phải tuân cùng nguyên tắc idempotent. Nếu Payment hoặc Order đã được xác nhận PAID trước đó, callback đến sau hoặc callback trùng chỉ được ghi nhận/log và không được chạy lại các tác vụ hậu thanh toán. |
| BR-PAY-15 | Mỗi Order chỉ được fulfill thành công một lần. Một lần fulfillment thành công tạo tối đa một Membership và đúng một Receipt; callback hoặc xác nhận bổ sung không được tạo thêm Membership, Receipt, doanh thu hoặc quyền lợi trùng lặp. |
| BR-PAY-16 | PAID và FAILED là trạng thái terminal của Payment. Sau khi Payment ở trạng thái PAID, thao tác thông thường không được sửa status, amount, provider transaction/reference hoặc paid timestamp và không được chuyển Payment ngược về PENDING hoặc FAILED. |
| BR-ORD-07 | Một Member chỉ được có tối đa một Membership Order ở trạng thái PENDING_PAYMENT tại một thời điểm. Member phải hoàn tất hoặc để Order hiện tại được xử lý sang trạng thái kết thúc trước khi tạo Membership Order PENDING_PAYMENT mới. |
| BR-PAY-17 | Nếu manual reconciliation sau payment window xác định không có giao dịch Bank Transfer hợp lệ, người được ủy quyền chuyển Payment từ PENDING sang FAILED và Order tương ứng sang EXPIRED. Thao tác phải ghi actor, timestamp, reason và evidence/reference vào Audit. |

### Lớp học, phòng, lịch định kỳ và buổi học

| ID | Quy tắc nghiệp vụ |
| --- | --- |
| BR-CLS-01 | Manager quản lý Discipline, Class, Room và việc phân công Teaching Coach được Session sử dụng. |
| BR-CLS-02 | Một Class có nhiều Session và mỗi Session thuộc đúng một Class. |
| BR-CLS-03 | Mỗi Session có đúng một Teaching Coach và một Room trong thời gian đã lập lịch. |
| BR-CLS-04 | Sức chứa Room phải lớn hơn hoặc bằng sức chứa Session. |
| BR-CLS-05 | Không tạo Session khi Room của Session đó xung đột với một Session khác. |
| BR-CLS-06 | Không tạo Session khi Teaching Coach của Session đó xung đột với một Session khác. |
| BR-CLS-07 | Chỉ Account COACH đang ACTIVE mới có thể được phân công làm Teaching Coach cho Session mới hoặc Session được cập nhật. |
| BR-CLS-08 | Manager có thể tạo, xem và cập nhật các bản ghi Discipline, Class và Room. |
| BR-CLS-09 | Discipline, Class hoặc Room đã được một Session sử dụng phải được lưu giữ; Manager dùng INACTIVE để ngăn việc lập lịch mới. |
| BR-CLS-10 | Discipline, Class hoặc Room ở trạng thái INACTIVE không thể được dùng để tạo hoặc sinh Session mới. |
| BR-CLS-11 | Không thể giảm sức chứa Session xuống thấp hơn số lượng Booking BOOKED hiện tại. |
| BR-CLS-12 | Coach có thể xem các Session mà Coach đó được phân công làm Teaching Coach. |
| BR-REC-01 | Để tạo lịch định kỳ, Manager cung cấp Class, Teaching Coach, Room, ngày bắt đầu, các thứ trong tuần, giờ bắt đầu và giờ kết thúc. |
| BR-REC-02 | Một yêu cầu lịch định kỳ sinh đúng 30 lần Session, bắt đầu vào hoặc sau ngày bắt đầu và tuân theo các thứ trong tuần cùng khoảng thời gian đã chọn. |
| BR-REC-03 | Trước khi ghi bất kỳ Session nào, hệ thống kiểm tra cả 30 lần về xung đột Room, xung đột Coach, tài nguyên đang hoạt động, tính hợp lệ của thời gian và sức chứa. |
| BR-REC-04 | Việc sinh lịch định kỳ là toàn bộ hoặc không có gì. Nếu bất kỳ lần nào không hợp lệ, hệ thống từ chối toàn bộ lô và không tạo Session nào từ yêu cầu đó. |
| BR-SES-01 | Vòng đời Session là SCHEDULED -> IN_PROGRESS -> COMPLETED hoặc SCHEDULED -> CANCELLED. |
| BR-SES-02 | Session chuyển từ SCHEDULED sang IN_PROGRESS khi currentTime đạt startTime và từ IN_PROGRESS sang COMPLETED khi currentTime đạt endTime. |
| BR-SES-03 | Chỉ Manager mới có thể hủy Session và chỉ được phép hủy khi Session đang SCHEDULED. |
| BR-SES-04 | Session CANCELLED hoặc COMPLETED được lưu giữ làm lịch sử và không bị xóa cứng. |
| BR-SES-05 | Việc thay đổi ngày hoặc giờ của Session yêu cầu Manager hủy Session cũ và tạo Session mới. |
| BR-SES-06 | Trong khi Session đang SCHEDULED, Manager có thể thay Teaching Coach hoặc Room nếu vượt qua kiểm tra xung đột và Room đáp ứng sức chứa Session. |
| BR-SES-07 | Việc thay Teaching Coach hoặc Room không hủy các Booking hiện có vì ngày và giờ của Session không thay đổi. |
| BR-SES-08 | Manager có thể tạo một Single Session độc lập bằng cách cung cấp Class, Teaching Coach, Room, ngày diễn ra, startTime, endTime và capacity. |
| BR-SES-09 | Single Session phải vượt qua cùng các kiểm tra về tài nguyên ACTIVE, startTime < endTime, Coach conflict, Room conflict và Room/Session capacity như Session được sinh từ recurring schedule. |

### Đặt chỗ và hủy đặt chỗ

| ID | Quy tắc nghiệp vụ |
| --- | --- |
| BR-BKG-01 | Mỗi Booking thuộc một Member và một Session riêng lẻ. |
| BR-BKG-02 | Member có thể tạo Booking vào bất kỳ thời điểm nào trước Session.startTime. Receptionist có thể tạo Booking cho Member theo cùng các điều kiện. |
| BR-BKG-03 | Booking yêu cầu Account Member ở trạng thái ACTIVE và Membership PLUS có thời hạn hiệu lực bao gồm Session.startTime. |
| BR-BKG-04 | Trước khi xác nhận Booking, hệ thống kiểm tra Session đang ở trạng thái SCHEDULED, còn sức chứa, chưa đạt startTime, không có Booking BOOKED trùng cho cùng Session và không xung đột lịch với các Booking BOOKED hiện có của Member. |
| BR-BKG-05 | BASIC không cấp điều kiện đặt chỗ cho Yoga, Group Class hoặc PT 1-1. Receptionist không thể bỏ qua quyền lợi Membership hoặc bất kỳ kiểm tra Booking nào khác. |
| BR-BKG-06 | Member có thể hủy Booking khi cancellationTime cách Session.startTime ít nhất hai giờ. Đúng hai giờ được chấp nhận; dưới hai giờ bị từ chối. |
| BR-BKG-07 | Receptionist chỉ có thể hủy cho Member theo cùng hạn hủy và các kiểm tra áp dụng cho Member. |
| BR-BKG-08 | Một lần hủy hợp lệ chuyển Booking từ BOOKED sang CANCELLED và giải phóng sức chứa Session. Bản ghi được lưu giữ. |
| BR-BKG-09 | Đặt lại sau khi hủy sẽ tạo yêu cầu Booking mới và chạy lại toàn bộ kiểm tra hiện tại; bản ghi CANCELLED không quay lại BOOKED. |
| BR-BKG-10 | Khi Manager hủy Session, mọi Booking BOOKED bị ảnh hưởng chuyển sang CANCELLED với nguồn là việc hủy Session và các Member bị ảnh hưởng nhận Notification. |
| BR-BKG-11 | Member có thể xem các Class và Session đang mở cùng lịch, Room, Teaching Coach và tình trạng chỗ trống trước khi đặt chỗ. |

### Lượt vào trung tâm

| ID | Quy tắc nghiệp vụ |
| --- | --- |
| BR-VIS-01 | Center Check-in và Class Attendance là hai quy trình nghiệp vụ riêng biệt. |
| BR-VIS-02 | Mỗi lần Member vào trung tâm, Member phải check-in qua quầy lễ tân và Receptionist phải tạo một bản ghi Center Visit mới cho lần vào đó. |
| BR-VIS-03 | Nếu Member rời đi rồi quay lại sau đó, Receptionist thực hiện Check-in khác và tạo Center Visit khác. Visit trước đó không được tái sử dụng cho lần vào mới. |
| BR-VIS-04 | Tại quầy lễ tân, Member cung cấp Member ID hoặc số điện thoại. Receptionist tìm Profile, xem ảnh hồ sơ và xác minh người đó trước khi hoàn tất Check-in. |
| BR-VIS-05 | Center Check-in yêu cầu Account Member ở trạng thái ACTIVE, Membership BASIC hoặc PLUS hiện còn hiệu lực và Profile Member có ảnh hồ sơ. |
| BR-VIS-06 | Member phải có Center Visit được tạo cho lần vào hiện tại trước khi tham gia lớp và trước khi Teaching Coach có thể ghi PRESENT. |
| BR-VIS-07 | Center Check-in không tự động tạo hoặc thay đổi Class Attendance. |
| BR-VIS-08 | Member có thể có nhiều bản ghi Center Visit và mọi bản ghi Visit đều được lưu giữ cho lịch sử và báo cáo. |

### Điểm danh lớp học

| ID | Quy tắc nghiệp vụ |
| --- | --- |
| BR-ATT-01 | Chỉ Teaching Coach được phân công cho Session mới có thể ghi hoặc sửa Attendance của Session đó. |
| BR-ATT-02 | Mỗi Booking BOOKED có tối đa một bản ghi Attendance, với trạng thái được phép là PRESENT hoặc ABSENT. |
| BR-ATT-03 | Attendance của Member đã BOOKED mặc định là ABSENT. |
| BR-ATT-04 | Teaching Coach có thể bắt đầu điểm danh tại Session.startTime. |
| BR-ATT-05 | Từ Session.startTime đến 30 phút sau Session.endTime, Teaching Coach có thể thay đổi Attendance giữa PRESENT và ABSENT. |
| BR-ATT-06 | Tại thời điểm Session.endTime cộng 30 phút, Attendance bị khóa đối với Teaching Coach. |
| BR-ATT-07 | Trước khi Attendance bị khóa, các giá trị ABSENT mặc định chỉ là tạm thời. Báo cáo Attendance chính thức sử dụng các giá trị sau khi cửa sổ điểm danh đóng. |
| BR-ATT-08 | ABSENT chỉ phục vụ điểm danh và báo cáo. Trạng thái này không tự động áp dụng cảnh báo, hình phạt, thay đổi Membership hoặc đình chỉ Account. |
| BR-ATT-09 | Attendance không tự động tạo Training Result. |
| BR-ATT-10 | Tại Session.startTime, hệ thống tạo một Attendance mặc định ABSENT cho mỗi Booking vẫn đang ở trạng thái BOOKED của Session đó. |
| BR-ATT-11 | Booking đã ở trạng thái CANCELLED trước Session.startTime không tạo Attendance. |

### Huấn luyện viên cá nhân, kế hoạch, kết quả và phản hồi

| ID | Quy tắc nghiệp vụ |
| --- | --- |
| BR-COA-01 | Member có Membership PLUS còn hiệu lực có thể sử dụng các tính năng Personal Coach, Training Plan, Training Result, Feedback và PT 1-1. |
| BR-COA-02 | Manager có thể phân công Coach đang ACTIVE làm Personal Coach cho Member có Membership PLUS còn hiệu lực. |
| BR-COA-03 | Một Member có tối đa một Personal Coach Assignment ACTIVE tại một thời điểm. Một Coach có thể được phân công làm Personal Coach cho nhiều Member. |
| BR-COA-04 | Lịch sử Personal Coach Assignment được lưu giữ. Khi reassign Personal Coach, Assignment ACTIVE cũ phải kết thúc trước khi Assignment mới trở thành ACTIVE; việc kết thúc hoặc phân công lại không xóa dữ liệu Training liên quan. |
| BR-COA-05 | Khi Membership PLUS của Member hết hạn, các Personal Coach Assignment đang hoạt động kết thúc và không thể sử dụng quyền lợi Training mới; lịch sử Plan, Result và Feedback vẫn có thể xem được. |
| BR-COA-06 | Coach ở trạng thái INACTIVE không thể nhận Assignment mới hoặc tạo bản ghi Training Plan, Result hay Feedback mới. |
| BR-COA-07 | Personal Coach có thể xem Profile Member và thông tin mục tiêu thể chất cần thiết trong phạm vi Coaching được phân công. |
| BR-TRP-01 | Training Plan thuộc đúng một Member và được tạo hoặc quản lý bởi Personal Coach đang có Assignment ACTIVE với Member đó. |
| BR-TRP-02 | Việc cập nhật Training Plan tạo hoặc lưu giữ một Plan Version; nội dung trước đó không bị ghi đè mà không có lịch sử. |
| BR-TRP-03 | Member có thể xem các Training Plan và Plan Version của chính mình. |
| BR-RES-01 | Đối với dữ liệu Personal Training không gắn với Group/Yoga Session, Personal Coach đang có Assignment ACTIVE với Member được tạo hoặc cập nhật Training Result sau hoạt động liên quan. Attendance PRESENT không tự động tạo Result. |
| BR-RES-02 | Training Result có thể tham chiếu Plan hoặc Session liên quan và phải lưu giữ lịch sử thay đổi quan trọng. |
| BR-RES-03 | Member có thể xem các Training Result của chính mình. |
| BR-FDB-01 | Personal Coach hiện đang được phân công cho Member có thể tạo hoặc cập nhật Feedback trong ngữ cảnh Training Plan hoặc Result liên quan, đồng thời lưu giữ lịch sử. |
| BR-FDB-02 | Member có thể xem Feedback thuộc dữ liệu Training của chính mình. |
| BR-TRP-04 | Personal Coach chỉ được cập nhật Training Plan/Plan Version của Member mà Coach đó đang có Assignment ACTIVE; Coach khác không được ghi đè Training Plan ngoài phạm vi Personal Coach Assignment của mình. |
| BR-RES-04 | Nếu Training Result tham chiếu một Group/Yoga Session, chỉ Teaching Coach được phân công cho Session đó mới được tạo hoặc cập nhật Result thuộc Session đó. Quy tắc này độc lập với Personal Coach Assignment của Member. |
| BR-FDB-03 | Feedback thuộc Personal Training chỉ được tạo hoặc cập nhật bởi Personal Coach đang có Assignment ACTIVE với Member. Nếu Feedback gắn với Result của Group/Yoga Session, quyền cập nhật tuân theo Teaching Coach của Session đó; Coach khác không được ghi đè dữ liệu ngoài phạm vi được phân công. |

### Thông báo, nhật ký kiểm toán và báo cáo

| ID | Quy tắc nghiệp vụ |
| --- | --- |
| BR-NOT-01 | Mỗi Notification có đúng một người nhận. Một sự kiện nhiều người nhận sẽ tạo một Notification riêng cho từng người |
| BR-NOT-02 | Notification được tạo cho các sự kiện liên quan đến tạo Account, Payment, Membership, Booking hoặc hủy Booking, hủy Session, đình chỉ hoặc kích hoạt lại Member và Training. |
| BR-AUD-01 | SCMS lưu giữ bằng chứng Audit cho Payment và đối soát, Booking và hủy Booking, thay đổi Attendance, đình chỉ và kích hoạt lại Member, hủy Session, tạo và chuyển nhân viên hoặc Manager sang INACTIVE, đặt lại mật khẩu và các thay đổi Training quan trọng. |
| BR-AUD-02 | Một Audit event ghi nhận actor, action, target, timestamp và reason hoặc các giá trị trước và sau khi thao tác yêu cầu. |
| BR-AUD-03 | Manager có thể xem và tìm kiếm Audit History. |
| BR-AUD-04 | Người dùng thông thường không thể sửa hoặc xóa Audit event. |
| BR-RPT-01 | Manager có thể xem báo cáo về Member, Membership, Payment và doanh thu, Booking, Attendance, Center Visit và mức độ sử dụng lớp. |
| BR-RPT-02 | Doanh thu chỉ bao gồm Payment đã được xác nhận PAID; các lần PENDING và FAILED bị loại trừ. |
| BR-RPT-03 | Mức tham gia lớp được tính từ Attendance, không phải từ Center Visit. |
| BR-RPT-04 | Báo cáo lớp có thể phân biệt BOOKED, PRESENT, ABSENT và CANCELLED. |
| BR-RPT-05 | Member chỉ có thể xem dữ liệu nghiệp vụ của chính mình. |
| BR-RPT-06 | Coach có thể xem Member trong các Session do Coach giảng dạy hoặc trong phạm vi Coaching và Training được phân công. |
| BR-RPT-07 | Receptionist có thể truy cập dữ liệu Profile Member, Order, Payment, Receipt, Booking và Center Visit cần cho nhiệm vụ tại quầy lễ tân, nhưng không được cấp quyền quản trị Audit của Manager. |
| BR-NOT-03 | User có thể xem danh sách Notification được gửi tới chính Account của mình và không được xem Notification thuộc Account khác. |
| BR-NOT-04 | Notification mới được tạo ở trạng thái UNREAD. Người nhận có thể đánh dấu Notification của chính mình từ UNREAD sang READ; READ không làm thay đổi nghiệp vụ đã tạo Notification. |

## 3 Quy tắc kiểm tra dữ liệu

Các quy tắc này xác định đầu vào bắt buộc, giá trị được phép, tính duy nhất và kiểm tra giữa các bản ghi. Việc kiểm tra phía máy chủ có tính quyết định ngay cả khi frontend thực hiện cùng các kiểm tra.

### Kiểm tra tài khoản và hồ sơ

| ID | Quy tắc nghiệp vụ |
| --- | --- |
| BR-VAL-ACC-01 | Member tự đăng ký phải cung cấp họ và tên, số điện thoại, email, mật khẩu và ngày sinh. |
| BR-VAL-ACC-02 | MEMBER do Receptionist tạo phải có họ và tên, số điện thoại, email và ngày sinh. Hệ thống cấp mật khẩu mặc định từ số điện thoại hiện tại. |
| BR-VAL-ACC-03 | COACH, RECEPTIONIST hoặc MANAGER do Manager tạo phải có họ và tên, số điện thoại, email, ngày sinh và vai trò. Vai trò phải là một trong ba giá trị đó. |
| BR-VAL-ACC-04 | Ảnh hồ sơ là tùy chọn khi tạo Account. Mục tiêu thể chất và liên hệ khẩn cấp là các trường tùy chọn của Profile Member. |
| BR-VAL-ACC-05 | Mật khẩu do người dùng nhập hoặc thay đổi phải có ít nhất 8 ký tự. |
| BR-VAL-ACC-06 | Số điện thoại và email phải theo định dạng được hệ thống chấp nhận; ngày sinh không được là ngày trong tương lai. |
| BR-VAL-ACC-07 | Số điện thoại và email phải là duy nhất trong các Account không ở trạng thái INACTIVE. Số điện thoại hoặc email của Account COACH, RECEPTIONIST hoặc MANAGER ở trạng thái INACTIVE có thể được tái sử dụng khi tạo Account mới. |
| BR-VAL-ACC-08 | Việc tái sử dụng số điện thoại hoặc email không bao giờ thay đổi hoặc xóa Account INACTIVE cũ; mọi tham chiếu vẫn gắn với Account ID ban đầu. |
| BR-VAL-ACC-09 | Các giá trị họ và tên, số điện thoại, email, ngày sinh và vai trò mà luồng tạo yêu cầu không được để trống sau khi chuẩn hóa khoảng trắng. |
| BR-VAL-ACC-10 | Yêu cầu chuyển sang INACTIVE phải xác định Account mục tiêu và ghi lại lý do. Yêu cầu chuyển Manager sang INACTIVE còn phải vượt qua kiểm tra Manager ACTIVE cuối cùng. |
| BR-VAL-ACC-11 | Member ID do hệ thống sinh phải là duy nhất và không được để user tự chọn hoặc sửa. |

### Kiểm tra giao dịch

| ID | Quy tắc nghiệp vụ |
| --- | --- |
| BR-VAL-COM-01 | Membership Offer phải có Plan, giá bán, thời hạn, thông tin hiển thị và trạng thái; giá bán phải lớn hơn 0 và thời hạn phải lớn hơn 0. |
| BR-VAL-COM-02 | Order phải có Member, Offer ACTIVE, ảnh chụp số tiền và một phương thức thanh toán được phép. |
| BR-VAL-COM-03 | Số tiền Payment và nội dung chuyển khoản phải khớp với Order liên quan trước khi xác nhận chuyển khoản ngân hàng được chấp nhận. |
| BR-VAL-COM-04 | Mỗi Payment PAID được dùng để fulfill Order phát hành đúng một Receipt. Receipt tham chiếu Payment thành công và lưu giữ số tiền giao dịch, phương thức thanh toán, Member, Order và thời điểm phát hành. |
| BR-VAL-COM-05 | Manual reconciliation chỉ được xác nhận PAID khi giao dịch thực tế khớp Order, amount và transfer content/order code; transaction/provider reference hoặc bằng chứng đối soát phải được ghi nhận khi có. |
| BR-VAL-COM-06 | Trước khi tạo Membership Order Bank Transfer mới, hệ thống phải kiểm tra Member không có Order khác ở trạng thái PENDING_PAYMENT. |
| BR-VAL-COM-07 | Cash Order/Payment chỉ được persist sau khi Receptionist xác nhận đã nhận đủ số tiền phải thu; trước xác nhận không được tồn tại Payment tiền mặt chưa hoàn tất trong dữ liệu nghiệp vụ chính thức. |

### Kiểm tra lịch, đặt chỗ và điểm danh

| ID | Quy tắc nghiệp vụ |
| --- | --- |
| BR-VAL-SCH-01 | Lịch định kỳ yêu cầu Class, Teaching Coach, Room, ngày bắt đầu, ít nhất một thứ trong tuần, giờ bắt đầu và giờ kết thúc. |
| BR-VAL-SCH-02 | Giờ bắt đầu phải sớm hơn giờ kết thúc. Ngày của các lần được sinh tuân theo các thứ trong tuần đã chọn vào hoặc sau ngày bắt đầu. |
| BR-VAL-SCH-03 | Sức chứa Session phải là số dương, không được vượt quá sức chứa Room và không được giảm xuống dưới số lượng BOOKED hiện tại. |
| BR-VAL-SCH-04 | Kiểm tra xung đột Coach và Room so sánh toàn bộ khoảng thời gian Session cho mọi Session bị ảnh hưởng. |
| BR-VAL-BKG-01 | Kiểm tra Booking sử dụng trạng thái Account, Membership, trạng thái Session = SCHEDULED, sức chứa, thời gian bắt đầu, trùng lặp và xung đột tại thời điểm yêu cầu được ghi nhận chính thức. |
| BR-VAL-BKG-02 | Hiệu lực PLUS phải bao phủ Session.startTime; Membership hết hạn trước Session.startTime không thể hỗ trợ Booking đó. |
| BR-VAL-BKG-03 | Member không thể có nhiều hơn một Booking BOOKED cho cùng một Session. |
| BR-VAL-BKG-04 | Ngưỡng hủy hai giờ được tính từ cancellationTime đến Session.startTime. |
| BR-VAL-VIS-01 | Receptionist phải xác định đúng một Account Member trước khi tạo Center Visit, bằng Member ID hoặc số điện thoại và xác minh trực quan qua ảnh hồ sơ. |
| BR-VAL-ATT-01 | Trạng thái Attendance chỉ chấp nhận PRESENT hoặc ABSENT. |
| BR-VAL-ATT-02 | PRESENT yêu cầu có Center Visit cho lần vào trung tâm hiện tại của Member trước khi cập nhật Attendance. |
| BR-VAL-SCH-05 | Single Session yêu cầu Class, Teaching Coach, Room, ngày diễn ra, startTime, endTime và capacity; các dữ liệu này phải vượt qua cùng kiểm tra thời gian, tài nguyên, conflict và capacity áp dụng cho recurring-generated Session. |

## 4 Quy tắc bảo mật và toàn vẹn kỹ thuật

Các biện pháp kiểm soát này bảo vệ thông tin xác thực, thực thi phân quyền và giữ cho các nghiệp vụ nhiều bản ghi nhất quán khi thử lại và khi có yêu cầu đồng thời.

| ID | Quy tắc nghiệp vụ |
| --- | --- |
| BR-SEC-01 | Mật khẩu phải được lưu bằng hàm băm mật khẩu một chiều có salt. Nghiêm cấm lưu mật khẩu dạng văn bản thuần và lưu mật khẩu bằng mã hóa có thể đảo ngược. |
| BR-SEC-02 | Hệ thống không được hiển thị, khôi phục, ghi log, ghi Audit hoặc đưa mật khẩu hay hàm băm mật khẩu vào Notification. |
| BR-SEC-03 | Thao tác đặt lại mật khẩu dùng số điện thoại hiện tại của Account làm đầu vào thông tin xác thực mới, nhưng chỉ lưu hàm băm bảo mật của giá trị đó. |
| BR-SEC-04 | Việc phân quyền theo vai trò và quyền sở hữu phải được backend thực thi cho mọi thao tác được bảo vệ; ẩn một điều khiển trên frontend không phải là phân quyền. |
| BR-SEC-05 | Khi Account chuyển sang SUSPENDED hoặc INACTIVE, quyền truy cập đã xác thực hiện có không còn được phép thực hiện các nghiệp vụ được bảo vệ. |
| BR-INT-01 | Xử lý Payment thành công phải có tính idempotent cho automatic provider callback, manual confirmation và Cash confirmation. Callback, double-click hoặc xác nhận trùng/lặp không được tạo trùng Membership, Receipt, doanh thu, Order fulfillment hoặc quyền lợi. |
| BR-INT-02 | Xác nhận Payment thành công cập nhật Payment, Order, Membership, giao dịch và Receipt như một thao tác nhất quán. Một Order chỉ được commit successful fulfillment một lần; Notification được gửi sau khi giao dịch nghiệp vụ đã commit. |
| BR-INT-03 | Lô 30 Session định kỳ có tính giao dịch: hoặc mọi Session đã kiểm tra đều được tạo, hoặc không Session nào được tạo. |
| BR-INT-04 | Xác nhận Booking và phân bổ sức chứa phải có tính nguyên tử để các yêu cầu đồng thời không thể vượt quá sức chứa Session hoặc tạo Booking trùng lặp. |
| BR-INT-05 | Audit event chỉ được phép ghi thêm và được bảo vệ khỏi việc cập nhật hoặc xóa bởi người dùng ứng dụng thông thường. |
| BR-INT-06 | Lỗi gửi Notification không hoàn tác giao dịch nghiệp vụ đã hoàn tất. Lần gửi thất bại phải tiếp tục đủ điều kiện để thử lại. |
| BR-INT-07 | Bản ghi nghiệp vụ lịch sử sử dụng thay đổi trạng thái và tham chiếu được lưu giữ thay vì xóa cứng ở những nơi bản cơ sở này yêu cầu giữ lịch sử. |
| BR-INT-08 | Các mốc thời gian vòng đời và Audit được tạo bởi đồng hồ máy chủ đáng tin cậy và được lưu trữ nhất quán. |
| BR-INT-09 | Thao tác quản trị nhạy cảm ghi nhận actor đã xác thực và Account mục tiêu; các giá trị trước và sau được lưu giữ khi cần thiết cho việc rà soát. |
| BR-INT-10 | Provider transaction/reference identifier phải được xử lý như khóa chống trùng khi provider cung cấp giá trị này. Cùng một provider transaction/reference không được dùng để fulfill nhiều Payment hoặc Order. |

## 5 Giới hạn MVP và ngoài phạm vi

Các mục sau được chủ ý loại khỏi MVP. Chúng được nhóm tại đây để các tuyên bố phạm vi phủ định không làm lu mờ các quy tắc nghiệp vụ cốt lõi.

| ID | Quy tắc nghiệp vụ |
| --- | --- |
| BR-LIM-01 | MVP không có luồng kích hoạt qua email, liên kết kích hoạt hoặc trạng thái PENDING_ACTIVATION. Việc tạo Account hợp lệ hoàn tất ở trạng thái ACTIVE. |
| BR-LIM-02 | MVP không có tính năng đổi vai trò. Vai trò nhân viên khác yêu cầu Account cũ chuyển sang INACTIVE và Account mới được tạo. |
| BR-LIM-03 | Membership Plan chỉ giới hạn ở BASIC và PLUS. BASIC không có phần bổ sung theo Session để mở khóa quyền lợi Booking của PLUS. |
| BR-LIM-04 | MVP không gia hạn, nâng cấp hoặc sửa trực tiếp Membership đang ACTIVE. Sau khi hết hạn, Member mua Membership mới. |
| BR-LIM-05 | Một Payment sử dụng tiền mặt hoặc chuyển khoản ngân hàng. Không hỗ trợ thanh toán chia nhỏ giữa tiền mặt và chuyển khoản ngân hàng. |
| BR-LIM-06 | Ngày và giờ của Session không được đổi lịch tại chỗ. Manager hủy Session cũ và tạo Session mới. |
| BR-LIM-07 | Booking được thực hiện theo từng Session. Không hỗ trợ Booking theo tuần và Booking toàn khóa. |
| BR-LIM-08 | Center Visit chỉ ghi nhận Check-in. MVP không có thao tác Check-out hoặc trạng thái Visit đang mở hay đã đóng. |
| BR-LIM-09 | Attendance chỉ sử dụng PRESENT và ABSENT. LATE không phải là trạng thái trong MVP. |
| BR-LIM-10 | ABSENT không tự động tạo cảnh báo, chế tài, khấu trừ Membership hoặc đình chỉ Account. |
| BR-LIM-11 | Attendance không tự động tạo Training Result. |
| BR-LIM-12 | MVP không bắt buộc đổi mật khẩu sau khi Account được tạo với mật khẩu mặc định là số điện thoại hoặc sau khi đặt lại được ủy quyền. |
| BR-LIM-13 | Việc xác nhận Bank Transfer tự động phụ thuộc vào payment provider bên thứ ba (ví dụ SePay). Độ trễ, downtime, webhook/API failure hoặc delayed confirmation từ provider có thể làm chậm việc cập nhật Payment; MVP sử dụng manual reconciliation làm fallback khi Member/nhân viên cần đối soát một Payment còn PENDING. |
| BR-LIM-14 | MVP không hỗ trợ refund, partial refund hoặc chargeback. Nếu khách thực tế chuyển dư hoặc chuyển trùng cho một Order đã được fulfill, giao dịch bổ sung không tạo thêm Membership, Receipt hoặc quyền lợi; việc hoàn tiền/giải quyết tài chính cho khoản dư hoặc trùng được xử lý ngoài hệ thống, trong khi SCMS giữ thông tin đối soát/Audit khi dữ liệu provider cho phép. |

## 6 Tóm tắt vòng đời

Bảng này dùng để hỗ trợ tra cứu. Nếu nội dung tóm tắt xung đột với một quy tắc chi tiết ở trên, quy tắc chi tiết được ưu tiên áp dụng.

| Đối tượng | Vòng đời | Điều kiện chính hoặc hành vi kết thúc |
| --- | --- | --- |
| Account MEMBER | ACTIVE <-> SUSPENDED | Manager kiểm soát các lần chuyển trạng thái. Việc đình chỉ hủy Booking tương lai nhưng không tạm dừng thời gian Membership. |
| Account COACH | ACTIVE -> INACTIVE | Phải xử lý trước các Session tương lai và Personal Coach Assignment đang hoạt động. INACTIVE là trạng thái kết thúc của Account đó. |
| Account RECEPTIONIST | ACTIVE -> INACTIVE | INACTIVE chặn đăng nhập và công việc mới trong khi vẫn giữ lại lịch sử. |
| Account MANAGER | ACTIVE -> INACTIVE | Manager khác chỉ có thể chuyển Account này sang INACTIVE khi vẫn còn ít nhất một MANAGER ở trạng thái ACTIVE. |
| Membership Offer | ACTIVE <-> INACTIVE | INACTIVE dừng Order mới; ảnh chụp dữ liệu Order và Membership trước đó không thay đổi. |
| Membership | ACTIVE -> EXPIRED | EXPIRED là trạng thái kết thúc. Muốn tiếp tục dịch vụ phải dùng Order, Payment và Membership mới. |
| Order | Bank Transfer: PENDING_PAYMENT -> PAID hoặc EXPIRED; Cash: tạo trực tiếp PAID khi xác nhận | Bank Transfer dùng payment window tự động 24 giờ; hết 24 giờ mà provider vẫn chưa rõ kết quả thì Order vẫn PENDING_PAYMENT để manual reconciliation. Mỗi Member chỉ có tối đa một Membership Order PENDING_PAYMENT và mỗi Order chỉ được fulfill một lần. |
| Payment | Bank Transfer: PENDING -> PAID hoặc FAILED; Cash: tạo trực tiếp PAID khi Receptionist xác nhận | PAID và FAILED là terminal. Bank Transfer unresolved sau 24 giờ vẫn PENDING cho tới manual reconciliation; automatic callback, manual confirmation và Cash confirmation đều phải idempotent. |
| Session | SCHEDULED -> IN_PROGRESS -> COMPLETED hoặc SCHEDULED -> CANCELLED | Chỉ Manager được hủy và chỉ khi đang SCHEDULED. |
| Booking | BOOKED -> CANCELLED | Đặt lại tạo một bản ghi mới sau khi kiểm tra hợp lệ. |
| Attendance | Mặc định ABSENT; PRESENT <-> ABSENT trong cửa sổ cho phép; sau đó bị khóa | Teaching Coach được chỉnh sửa từ startTime đến endTime cộng 30 phút. |
| Center Visit | CREATED | Mỗi lần Check-in tại quầy lễ tân tạo một bản ghi mới; MVP không có trạng thái Check-out. |
| Personal Coach Assignment | ACTIVE -> ENDED | Mỗi Member có tối đa một Assignment ACTIVE. Reassign phải kết thúc Assignment cũ trước; PLUS hết hạn hoặc Coach chuyển INACTIVE cũng kết thúc Assignment nhưng giữ toàn bộ lịch sử Training. |
| Training Plan | Có phiên bản | Các lần cập nhật giữ lại Plan Version trước đó. |
| Receipt | ISSUED | Được phát hành từ Payment thành công và lưu giữ cùng lịch sử giao dịch. |
| Audit event | CHỈ GHI THÊM | Người dùng thông thường không thể cập nhật hoặc xóa. |
| Notification | UNREAD -> READ | Chỉ người nhận xem và đánh dấu READ Notification của chính Account mình. |
