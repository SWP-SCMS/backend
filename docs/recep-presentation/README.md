# Bộ tài liệu thuyết trình role Receptionist — SCMS

> Đường dẫn đã cập nhật theo bản cleanup local. Các thay đổi nghiệp vụ có sẵn trong 9c08093 được ghi ở phần cập nhật của tài liệu thuyết trình.

Ngày đối chiếu: 10/11/2026. Nguồn: bản sao backend-refactor local từ commit 9c08093505df43da557b7fa6ff91478f4bef770d.

Đây là tài liệu giải thích **backend của bản local đã cleanup**. Chưa đối chiếu frontend với commit này. Các bước thao tác màn hình trong kịch bản demo là gợi ý trình diễn API, không xác nhận tên nút hay màn hình frontend hiện có.

Đọc theo thứ tự:

**Kịch bản demo đầy đủ mới:** [QUY_TRINH_DEMO_DAY_DU_RECEP.md](QUY_TRINH_DEMO_DAY_DU_RECEP.md) — 28 ca demo và ba tình huống phối hợp bổ sung ở cuối tài liệu; dữ liệu chuẩn bị, thao tác, kết quả, logic, nhánh lỗi, lời nói và phối hợp Manager/hệ thống. [Bảng đối chiếu API với ca demo](DOI_CHIEU_API_DEMO.md) giúp tra chức năng tương ứng.

**Tra cứu theo class và số dòng:** [CLASS_RECEP_THEO_DONG.md](CLASS_RECEP_THEO_DONG.md). Hai nhóm class trọng tâm và phụ thuộc mở rộng giúp lần theo luồng; dùng [SYMBOL_INDEX](../source-guide/SYMBOL_INDEX.md) để tra đầy đủ kiểu lồng nhau và overload.

1. [THUYET_TRINH_ROLE_RECEP.md](THUYET_TRINH_ROLE_RECEP.md): toàn bộ nghiệp vụ, luồng thực thi, trạng thái, công thức, dữ liệu và các điểm cần giải thích khi bảo vệ.
2. [API_VA_CLASS_RECEP.md](API_VA_CLASS_RECEP.md): phụ lục được trích từ controller, có HTTP method, đường dẫn, tham số, lời gọi service và liên kết mã nguồn. Bao gồm chức năng riêng của Receptionist và chức năng dùng chung.
3. [KICH_BAN_VA_CAU_HOI.md](KICH_BAN_VA_CAU_HOI.md): lời nói theo phần, kịch bản demo và câu hỏi phản biện.
4. [KIEM_CHUNG.md](KIEM_CHUNG.md): phạm vi kiểm chứng, kết quả test, giới hạn và các lưu ý khác biệt giữa tài liệu cũ với code.

Mọi API trong tài liệu có prefix thực tế **/api/v1**. Ví dụ /reception/members nghĩa là /api/v1/reception/members.

Tài liệu không chứa tài khoản, mật khẩu hay dữ liệu khách hàng thật. Các số tiền và tên người trong ví dụ là dữ liệu minh họa.
