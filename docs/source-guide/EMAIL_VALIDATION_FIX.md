# Bổ sung trước merge: regex email không backtracking

CodeQL của PR #207 chỉ ra hai cảnh báo `java/polynomial-redos` có sẵn từ baseline tại `MemberProfileDetailsValidator` và `StaffAccountService`. Bổ sung này sửa hai regex, tách khỏi các commit cleanup trước đó; không dismiss cảnh báo hoặc tắt quy tắc scanning.

Regex mới chọn dấu chấm đầu tiên có ít nhất một ký tự domain ở trước nó. Các đoạn lặp dùng possessive quantifier, không thử lại nhiều cách chia chuỗi. Phần sau dấu chấm vẫn nhận dấu chấm như quy tắc cũ, nên không vô tình siết thêm cú pháp email. Giữ nguyên trim, lowercase, giới hạn 320 ký tự, field và thông báo lỗi.

- [MemberProfileDetailsValidator#normalizeEmail](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileDetailsValidator.java#L53)
- [StaffAccountService#normalizeEmail](../../src/main/java/com/scms/backend/manager/StaffAccountService.java#L133)
- [EmailValidationCompatibilityTests](../../src/test/java/com/scms/backend/manager/EmailValidationCompatibilityTests.java)

Ba test characterization được chạy trên code trước khi sửa và chạy lại sau sửa. Test đối chiếu 18.670 input có dấu chấm, nhiều `@`, whitespace, Unicode và chuỗi lặp với regex cũ qua cả hai validator; kiểm tra thêm normalization, giới hạn 320/321 ký tự và error contract.

Bảng 1.733 test và manifest `verification/tested-input-sha256.json` ghi lại checkpoint cleanup **trước** bổ sung này. Chúng được giữ làm bằng chứng lịch sử, không được hiểu là hash của hai file production đã sửa. Bổ sung có ba test unit mới; kết quả đầy đủ của commit cuối phải xem trên [PR #207](https://github.com/SWP-SCMS/backend/pull/207) và CI của commit merge.

Không thay đổi filter chain/CSRF, phân quyền, SQL, transaction, schema hoặc dependency trong bổ sung này. Các cảnh báo bảo mật khác cần được đánh giá riêng theo phạm vi thực tế.

Kiểm chứng local sau sửa: 35 unit test liên quan và 46 PostgreSQL integration test hồ sơ member/reception/staff đạt, không failure/error/skip. Các log được giữ ngoài repository; full CI được xác nhận trên SHA cuối trước merge.
