# Đối chiếu 104 ánh xạ API với kịch bản demo lễ tân

Nguồn: phụ lục API đã đối chiếu source ở commit 9c08093. Bảng này kiểm tra phạm vi tài liệu, không phải kết quả test hoặc báo cáo API đã chạy. Mọi path thêm prefix /api/v1.

Kịch bản có 28 mục D01–D28. Một callback nhà cung cấp và một endpoint deprecated được ghi riêng; không tính chúng là thao tác nghiệp vụ trực tiếp của lễ tân. Các bước Manager phối hợp và tài nguyên ảnh được mô tả trong kịch bản, không nằm trong 104 ánh xạ controller này.

| # | HTTP và đường dẫn | Mục demo | Phạm vi |
|---|---|---|---|
| 1 | [POST /auth/login](API_VA_CLASS_RECEP.md#L7) | [D01 — Đăng nhập, refresh và chứng minh đúng quyền lễ tân](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L63) | Lễ tân / API dùng chung |
| 2 | [POST /auth/refresh](API_VA_CLASS_RECEP.md#L17) | [D01 — Đăng nhập, refresh và chứng minh đúng quyền lễ tân](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L63) | Lễ tân / API dùng chung |
| 3 | [POST /auth/logout](API_VA_CLASS_RECEP.md#L27) | [D28 — Đăng xuất và kết thúc ca demo](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L542) | Lễ tân / API dùng chung |
| 4 | [POST /auth/change-password](API_VA_CLASS_RECEP.md#L37) | [D26 — Đổi mật khẩu cá nhân và quên mật khẩu qua email](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L514) | Lễ tân / API dùng chung |
| 5 | [POST /auth/password-reset/request](API_VA_CLASS_RECEP.md#L427) | [D26 — Đổi mật khẩu cá nhân và quên mật khẩu qua email](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L514) | Lễ tân / API dùng chung |
| 6 | [POST /auth/password-reset/verify](API_VA_CLASS_RECEP.md#L437) | [D26 — Đổi mật khẩu cá nhân và quên mật khẩu qua email](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L514) | Lễ tân / API dùng chung |
| 7 | [POST /auth/password-reset/confirm](API_VA_CLASS_RECEP.md#L447) | [D26 — Đổi mật khẩu cá nhân và quên mật khẩu qua email](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L514) | Lễ tân / API dùng chung |
| 8 | [POST /reception/members/{memberId}/reset-password](API_VA_CLASS_RECEP.md#L497) | [D04 — Reset mật khẩu hội viên](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L116) | Lễ tân / API dùng chung |
| 9 | [POST /reception/members](API_VA_CLASS_RECEP.md#L507) | [D02 — Tạo hội viên mới tại quầy](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L81) | Lễ tân / API dùng chung |
| 10 | [GET /reception/members/search](API_VA_CLASS_RECEP.md#L517) | [D03 — Tìm kiếm, xem, sửa hồ sơ và tải ảnh hội viên](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L99) | Lễ tân / API dùng chung |
| 11 | [GET /reception/members/{memberId}/profile](API_VA_CLASS_RECEP.md#L527) | [D03 — Tìm kiếm, xem, sửa hồ sơ và tải ảnh hội viên](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L99) | Lễ tân / API dùng chung |
| 12 | [PATCH /reception/members/{memberId}/profile](API_VA_CLASS_RECEP.md#L537) | [D03 — Tìm kiếm, xem, sửa hồ sơ và tải ảnh hội viên](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L99) | Lễ tân / API dùng chung |
| 13 | [POST /reception/members/{memberId}/profile/image](API_VA_CLASS_RECEP.md#L907) | [D03 — Tìm kiếm, xem, sửa hồ sơ và tải ảnh hội viên](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L99) | Lễ tân / API dùng chung |
| 14 | [POST /staff/me/profile/image](API_VA_CLASS_RECEP.md#L917) | [D24 — Hồ sơ cá nhân lễ tân và ảnh đại diện](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L483) | Lễ tân / API dùng chung |
| 15 | [DELETE /staff/me/profile/image](API_VA_CLASS_RECEP.md#L927) | [D24 — Hồ sơ cá nhân lễ tân và ảnh đại diện](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L483) | Lễ tân / API dùng chung |
| 16 | [GET /reception/email-verification/pending-accounts](API_VA_CLASS_RECEP.md#L457) | [D05 — Đề nghị kích hoạt thủ công cho hội viên chờ email](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L128) | Lễ tân / API dùng chung |
| 17 | [POST /reception/email-verification/manual-activation-requests](API_VA_CLASS_RECEP.md#L467) | [D05 — Đề nghị kích hoạt thủ công cho hội viên chờ email](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L128) | Lễ tân / API dùng chung |
| 18 | [GET /reception/email-verification/manual-activation-requests](API_VA_CLASS_RECEP.md#L477) | [D05 — Đề nghị kích hoạt thủ công cho hội viên chờ email](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L128) | Lễ tân / API dùng chung |
| 19 | [GET /reception/email-verification/manual-activation-requests/{requestId}](API_VA_CLASS_RECEP.md#L487) | [D05 — Đề nghị kích hoạt thủ công cho hội viên chờ email](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L128) | Lễ tân / API dùng chung |
| 20 | [GET /membership-offers](API_VA_CLASS_RECEP.md#L607) | [D06 — Tư vấn gói và bán Membership bằng tiền mặt](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L145) | Lễ tân / API dùng chung |
| 21 | [GET /membership-offers/{offerId}](API_VA_CLASS_RECEP.md#L617) | [D06 — Tư vấn gói và bán Membership bằng tiền mặt](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L145) | Lễ tân / API dùng chung |
| 22 | [POST /reception/members/{memberId}/membership-orders](API_VA_CLASS_RECEP.md#L627) | [D07 — Mua Membership bằng chuyển khoản/QR](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L163) | Lễ tân / API dùng chung |
| 23 | [GET /reception/members/{memberId}/membership-orders/pending](API_VA_CLASS_RECEP.md#L637) | [D07 — Mua Membership bằng chuyển khoản/QR](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L163) | Lễ tân / API dùng chung |
| 24 | [POST /reception/members/{memberId}/cash-payments](API_VA_CLASS_RECEP.md#L687) | [D06 — Tư vấn gói và bán Membership bằng tiền mặt](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L145) | Lễ tân / API dùng chung |
| 25 | [POST /reception/membership-orders/{orderId}/cash](API_VA_CLASS_RECEP.md#L697) | [D06 — Tư vấn gói và bán Membership bằng tiền mặt](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L145) | Deprecated: nhận diện ở D06, không demo thành công |
| 26 | [POST /payments/{paymentId}/reconcile](API_VA_CLASS_RECEP.md#L707) | [D08 — Chứng từ, hàng đợi đối soát, xác nhận và hủy đơn gói](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L182) | Lễ tân / API dùng chung |
| 27 | [PATCH /membership-orders/{orderId}/cancel](API_VA_CLASS_RECEP.md#L717) | [D08 — Chứng từ, hàng đợi đối soát, xác nhận và hủy đơn gói](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L182) | Lễ tân / API dùng chung |
| 28 | [GET /payments/reconciliation-queue](API_VA_CLASS_RECEP.md#L727) | [D08 — Chứng từ, hàng đợi đối soát, xác nhận và hủy đơn gói](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L182) | Lễ tân / API dùng chung |
| 29 | [GET /reception/members/{memberId}/receipts](API_VA_CLASS_RECEP.md#L737) | [D09 — Biên lai, lịch sử gói và gia hạn](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L202) | Lễ tân / API dùng chung |
| 30 | [GET /receipts/{receiptId}](API_VA_CLASS_RECEP.md#L747) | [D09 — Biên lai, lịch sử gói và gia hạn](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L202) | Lễ tân / API dùng chung |
| 31 | [GET /payments/{paymentId}/result](API_VA_CLASS_RECEP.md#L757) | [D08 — Chứng từ, hàng đợi đối soát, xác nhận và hủy đơn gói](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L182) | Lễ tân / API dùng chung |
| 32 | [GET /reception/members/{memberCode}/memberships](API_VA_CLASS_RECEP.md#L767) | [D09 — Biên lai, lịch sử gói và gia hạn](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L202) | Lễ tân / API dùng chung |
| 33 | [POST /reception/members/{memberId}/membership-orders/{orderId}/payments/sepay](API_VA_CLASS_RECEP.md#L807) | [D07 — Mua Membership bằng chuyển khoản/QR](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L163) | Lễ tân / API dùng chung |
| 34 | [POST /reception/members/{memberId}/course-orders/{orderId}/payments/sepay](API_VA_CLASS_RECEP.md#L817) | [D17 — Thu học phí tiền mặt, QR và đối soát khóa học](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L353) | Lễ tân / API dùng chung |
| 35 | [POST /reception/members/{memberId}/course-orders/{orderId}/payments/cash](API_VA_CLASS_RECEP.md#L827) | [D17 — Thu học phí tiền mặt, QR và đối soát khóa học](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L353) | Lễ tân / API dùng chung |
| 36 | [POST /payments/sepay/webhook](API_VA_CLASS_RECEP.md#L837) | [D07 — Mua Membership bằng chuyển khoản/QR](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L163) | Callback nhà cung cấp; D07 và D17, không phải thao tác lễ tân |
| 37 | [GET /payments/{paymentId}/evidence-images](API_VA_CLASS_RECEP.md#L777) | [D08 — Chứng từ, hàng đợi đối soát, xác nhận và hủy đơn gói](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L182) | Lễ tân / API dùng chung |
| 38 | [POST /payments/{paymentId}/evidence-images](API_VA_CLASS_RECEP.md#L787) | [D08 — Chứng từ, hàng đợi đối soát, xác nhận và hủy đơn gói](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L182) | Lễ tân / API dùng chung |
| 39 | [GET /payments/{paymentId}/evidence-images/{evidenceId}](API_VA_CLASS_RECEP.md#L797) | [D08 — Chứng từ, hàng đợi đối soát, xác nhận và hủy đơn gói](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L182) | Lễ tân / API dùng chung |
| 40 | [POST /reception/center-visits](API_VA_CLASS_RECEP.md#L47) | [D10 — Check-in, danh sách đang ở trung tâm và check-out](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L217) | Lễ tân / API dùng chung |
| 41 | [PATCH /reception/members/{memberAccountId}/center-visits/current/checkout](API_VA_CLASS_RECEP.md#L57) | [D10 — Check-in, danh sách đang ở trung tâm và check-out](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L217) | Lễ tân / API dùng chung |
| 42 | [GET /reception/center-visits/open](API_VA_CLASS_RECEP.md#L67) | [D10 — Check-in, danh sách đang ở trung tâm và check-out](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L217) | Lễ tân / API dùng chung |
| 43 | [GET /reception/class-sessions](API_VA_CLASS_RECEP.md#L1007) | [D11 — Xem lịch nhóm/PT, đặt và hủy lịch thay hội viên](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L236) | Lễ tân / API dùng chung |
| 44 | [GET /reception/pt-sessions](API_VA_CLASS_RECEP.md#L997) | [D11 — Xem lịch nhóm/PT, đặt và hủy lịch thay hội viên](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L236) | Lễ tân / API dùng chung |
| 45 | [POST /reception/members/{memberId}/bookings](API_VA_CLASS_RECEP.md#L967) | [D11 — Xem lịch nhóm/PT, đặt và hủy lịch thay hội viên](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L236) | Lễ tân / API dùng chung |
| 46 | [PATCH /reception/members/{memberId}/bookings/{bookingId}/cancel](API_VA_CLASS_RECEP.md#L977) | [D11 — Xem lịch nhóm/PT, đặt và hủy lịch thay hội viên](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L236) | Lễ tân / API dùng chung |
| 47 | [GET /reception/members/{memberId}/bookings](API_VA_CLASS_RECEP.md#L987) | [D11 — Xem lịch nhóm/PT, đặt và hủy lịch thay hội viên](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L236) | Lễ tân / API dùng chung |
| 48 | [POST /reception/members/{memberId}/class-sessions/{sessionId}/waitlist](API_VA_CLASS_RECEP.md#L1017) | [D12 — Danh sách chờ lớp tập và tự động đôn chỗ](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L256) | Lễ tân / API dùng chung |
| 49 | [DELETE /reception/members/{memberId}/class-sessions/{sessionId}/waitlist](API_VA_CLASS_RECEP.md#L1027) | [D12 — Danh sách chờ lớp tập và tự động đôn chỗ](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L256) | Lễ tân / API dùng chung |
| 50 | [GET /reception/class-sessions/{sessionId}/waitlist](API_VA_CLASS_RECEP.md#L1037) | [D12 — Danh sách chờ lớp tập và tự động đôn chỗ](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L256) | Lễ tân / API dùng chung |
| 51 | [GET /reception/members/{memberCode}/membership-freeze-requests](API_VA_CLASS_RECEP.md#L567) | [D13 — Yêu cầu bảo lưu, thu phí và kết thúc sớm](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L273) | Lễ tân / API dùng chung |
| 52 | [POST /reception/members/{memberCode}/membership-freeze-requests](API_VA_CLASS_RECEP.md#L577) | [D13 — Yêu cầu bảo lưu, thu phí và kết thúc sớm](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L273) | Lễ tân / API dùng chung |
| 53 | [POST /reception/membership-freeze-requests/{requestId}/cash-payment](API_VA_CLASS_RECEP.md#L587) | [D13 — Yêu cầu bảo lưu, thu phí và kết thúc sớm](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L273) | Lễ tân / API dùng chung |
| 54 | [POST /reception/membership-freeze-requests/{requestId}/end-early](API_VA_CLASS_RECEP.md#L597) | [D13 — Yêu cầu bảo lưu, thu phí và kết thúc sớm](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L273) | Lễ tân / API dùng chung |
| 55 | [GET /reception/members/{memberCode}/membership-termination-requests](API_VA_CLASS_RECEP.md#L887) | [D14 — Yêu cầu chấm dứt gói ngay hoặc cuối chu kỳ](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L297) | Lễ tân / API dùng chung |
| 56 | [POST /reception/members/{memberCode}/membership-termination-requests](API_VA_CLASS_RECEP.md#L897) | [D14 — Yêu cầu chấm dứt gói ngay hoặc cuối chu kỳ](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L297) | Lễ tân / API dùng chung |
| 57 | [GET /reception/refund-requests](API_VA_CLASS_RECEP.md#L847) | [D15 — Hoàn tiền Membership nguồn tiền mặt](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L313) | Lễ tân / API dùng chung |
| 58 | [GET /reception/members/{memberCode}/refund-eligible-sources](API_VA_CLASS_RECEP.md#L857) | [D15 — Hoàn tiền Membership nguồn tiền mặt](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L313) | Lễ tân / API dùng chung |
| 59 | [POST /reception/members/{memberCode}/refund-requests](API_VA_CLASS_RECEP.md#L867) | [D15 — Hoàn tiền Membership nguồn tiền mặt](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L313) | Lễ tân / API dùng chung |
| 60 | [POST /reception/refund-requests/{requestId}/cash-disbursement](API_VA_CLASS_RECEP.md#L877) | [D15 — Hoàn tiền Membership nguồn tiền mặt](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L313) | Lễ tân / API dùng chung |
| 61 | [GET /courses](API_VA_CLASS_RECEP.md#L77) | [D16 — Tư vấn khóa học và tạo đơn giữ chỗ](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L335) | Lễ tân / API dùng chung |
| 62 | [GET /courses/{courseId}](API_VA_CLASS_RECEP.md#L87) | [D16 — Tư vấn khóa học và tạo đơn giữ chỗ](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L335) | Lễ tân / API dùng chung |
| 63 | [GET /course-runs](API_VA_CLASS_RECEP.md#L97) | [D16 — Tư vấn khóa học và tạo đơn giữ chỗ](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L335) | Lễ tân / API dùng chung |
| 64 | [GET /course-runs/{runId}](API_VA_CLASS_RECEP.md#L107) | [D16 — Tư vấn khóa học và tạo đơn giữ chỗ](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L335) | Lễ tân / API dùng chung |
| 65 | [POST /reception/members/{memberId}/course-orders](API_VA_CLASS_RECEP.md#L257) | [D16 — Tư vấn khóa học và tạo đơn giữ chỗ](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L335) | Lễ tân / API dùng chung |
| 66 | [GET /reception/members/{memberId}/course-orders/{orderId}](API_VA_CLASS_RECEP.md#L267) | [D16 — Tư vấn khóa học và tạo đơn giữ chỗ](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L335) | Lễ tân / API dùng chung |
| 67 | [GET /reception/members/{memberId}/course-enrollments](API_VA_CLASS_RECEP.md#L117) | [D18 — Tra cứu ghi danh khóa học và điều kiện rút](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L372) | Lễ tân / API dùng chung |
| 68 | [GET /reception/members/{memberId}/course-enrollments/{enrollmentId}](API_VA_CLASS_RECEP.md#L127) | [D18 — Tra cứu ghi danh khóa học và điều kiện rút](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L372) | Lễ tân / API dùng chung |
| 69 | [GET /reception/members/{memberId}/course-waitlists](API_VA_CLASS_RECEP.md#L137) | [D19 — Waitlist khóa học: chờ → được mời → nhận chỗ → trả tiền](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L384) | Lễ tân / API dùng chung |
| 70 | [POST /reception/members/{memberId}/course-runs/{runId}/waitlist](API_VA_CLASS_RECEP.md#L147) | [D19 — Waitlist khóa học: chờ → được mời → nhận chỗ → trả tiền](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L384) | Lễ tân / API dùng chung |
| 71 | [DELETE /reception/members/{memberId}/course-runs/{runId}/waitlist](API_VA_CLASS_RECEP.md#L157) | [D19 — Waitlist khóa học: chờ → được mời → nhận chỗ → trả tiền](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L384) | Lễ tân / API dùng chung |
| 72 | [POST /reception/members/{memberId}/course-waitlists/{waitlistId}/accept](API_VA_CLASS_RECEP.md#L167) | [D19 — Waitlist khóa học: chờ → được mời → nhận chỗ → trả tiền](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L384) | Lễ tân / API dùng chung |
| 73 | [GET /reception/members/{memberId}/course-schedule-changes](API_VA_CLASS_RECEP.md#L177) | [D20 — Ghi nhận phản hồi của hội viên khi trung tâm đổi lịch khóa](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L403) | Lễ tân / API dùng chung |
| 74 | [POST /reception/members/{memberId}/course-schedule-changes/{responseId}/response](API_VA_CLASS_RECEP.md#L187) | [D20 — Ghi nhận phản hồi của hội viên khi trung tâm đổi lịch khóa](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L403) | Lễ tân / API dùng chung |
| 75 | [GET /reception/members/{memberId}/course-transfer-offers](API_VA_CLASS_RECEP.md#L197) | [D21 — Chấp nhận đề nghị chuyển khóa và xử lý chênh lệch](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L422) | Lễ tân / API dùng chung |
| 76 | [POST /reception/members/{memberId}/course-transfer-offers/{offerId}/accept](API_VA_CLASS_RECEP.md#L207) | [D21 — Chấp nhận đề nghị chuyển khóa và xử lý chênh lệch](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L422) | Lễ tân / API dùng chung |
| 77 | [GET /reception/members/{memberId}/course-withdrawal-requests](API_VA_CLASS_RECEP.md#L217) | [D22 — Tạo, xem, hủy và theo dõi yêu cầu rút khóa](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L441) | Lễ tân / API dùng chung |
| 78 | [GET /reception/members/{memberId}/course-withdrawal-requests/{requestId}](API_VA_CLASS_RECEP.md#L227) | [D22 — Tạo, xem, hủy và theo dõi yêu cầu rút khóa](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L441) | Lễ tân / API dùng chung |
| 79 | [GET /reception/members/{memberId}/course-refund-obligations](API_VA_CLASS_RECEP.md#L237) | [D23 — Chi hoàn tiền khóa học và xác minh kết quả](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L462) | Lễ tân / API dùng chung |
| 80 | [GET /reception/members/{memberId}/course-refund-obligations/{obligationId}](API_VA_CLASS_RECEP.md#L247) | [D23 — Chi hoàn tiền khóa học và xác minh kết quả](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L462) | Lễ tân / API dùng chung |
| 81 | [POST /reception/members/{memberId}/course-enrollments/{enrollmentId}/withdrawal-requests](API_VA_CLASS_RECEP.md#L307) | [D22 — Tạo, xem, hủy và theo dõi yêu cầu rút khóa](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L441) | Lễ tân / API dùng chung |
| 82 | [POST /reception/members/{memberId}/course-withdrawal-requests/{requestId}/cancel](API_VA_CLASS_RECEP.md#L317) | [D22 — Tạo, xem, hủy và theo dõi yêu cầu rút khóa](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L441) | Lễ tân / API dùng chung |
| 83 | [GET /reception/course-refunds](API_VA_CLASS_RECEP.md#L277) | [D23 — Chi hoàn tiền khóa học và xác minh kết quả](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L462) | Lễ tân / API dùng chung |
| 84 | [GET /reception/course-refunds/{refundId}](API_VA_CLASS_RECEP.md#L287) | [D23 — Chi hoàn tiền khóa học và xác minh kết quả](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L462) | Lễ tân / API dùng chung |
| 85 | [POST /reception/course-refund-obligations/{obligationId}/disburse](API_VA_CLASS_RECEP.md#L297) | [D23 — Chi hoàn tiền khóa học và xác minh kết quả](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L462) | Lễ tân / API dùng chung |
| 86 | [GET /course-payments/reconciliation-queue](API_VA_CLASS_RECEP.md#L647) | [D17 — Thu học phí tiền mặt, QR và đối soát khóa học](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L353) | Lễ tân / API dùng chung |
| 87 | [GET /course-payments/{paymentId}](API_VA_CLASS_RECEP.md#L657) | [D17 — Thu học phí tiền mặt, QR và đối soát khóa học](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L353) | Lễ tân / API dùng chung |
| 88 | [GET /course-payments/{paymentId}/result](API_VA_CLASS_RECEP.md#L667) | [D17 — Thu học phí tiền mặt, QR và đối soát khóa học](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L353) | Lễ tân / API dùng chung |
| 89 | [POST /course-payments/{paymentId}/reconcile](API_VA_CLASS_RECEP.md#L677) | [D17 — Thu học phí tiền mặt, QR và đối soát khóa học](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L353) | Lễ tân / API dùng chung |
| 90 | [GET /staff/me/profile](API_VA_CLASS_RECEP.md#L547) | [D24 — Hồ sơ cá nhân lễ tân và ảnh đại diện](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L483) | Lễ tân / API dùng chung |
| 91 | [PATCH /staff/me/profile](API_VA_CLASS_RECEP.md#L557) | [D24 — Hồ sơ cá nhân lễ tân và ảnh đại diện](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L483) | Lễ tân / API dùng chung |
| 92 | [POST /staff/me/email-change](API_VA_CLASS_RECEP.md#L387) | [D25 — Đổi email cá nhân, gửi lại OTP, thay yêu cầu và hủy](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L495) | Lễ tân / API dùng chung |
| 93 | [POST /staff/me/email-change/verify](API_VA_CLASS_RECEP.md#L397) | [D25 — Đổi email cá nhân, gửi lại OTP, thay yêu cầu và hủy](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L495) | Lễ tân / API dùng chung |
| 94 | [POST /staff/me/email-change/resend](API_VA_CLASS_RECEP.md#L407) | [D25 — Đổi email cá nhân, gửi lại OTP, thay yêu cầu và hủy](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L495) | Lễ tân / API dùng chung |
| 95 | [DELETE /staff/me/email-change](API_VA_CLASS_RECEP.md#L417) | [D25 — Đổi email cá nhân, gửi lại OTP, thay yêu cầu và hủy](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L495) | Lễ tân / API dùng chung |
| 96 | [GET /accounts/me/email-change](API_VA_CLASS_RECEP.md#L327) | [D25 — Đổi email cá nhân, gửi lại OTP, thay yêu cầu và hủy](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L495) | Lễ tân / API dùng chung |
| 97 | [POST /accounts/me/email-change](API_VA_CLASS_RECEP.md#L337) | [D25 — Đổi email cá nhân, gửi lại OTP, thay yêu cầu và hủy](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L495) | Lễ tân / API dùng chung |
| 98 | [PUT /accounts/me/email-change](API_VA_CLASS_RECEP.md#L347) | [D25 — Đổi email cá nhân, gửi lại OTP, thay yêu cầu và hủy](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L495) | Lễ tân / API dùng chung |
| 99 | [POST /accounts/me/email-change/verify](API_VA_CLASS_RECEP.md#L357) | [D25 — Đổi email cá nhân, gửi lại OTP, thay yêu cầu và hủy](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L495) | Lễ tân / API dùng chung |
| 100 | [POST /accounts/me/email-change/resend](API_VA_CLASS_RECEP.md#L367) | [D25 — Đổi email cá nhân, gửi lại OTP, thay yêu cầu và hủy](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L495) | Lễ tân / API dùng chung |
| 101 | [DELETE /accounts/me/email-change](API_VA_CLASS_RECEP.md#L377) | [D25 — Đổi email cá nhân, gửi lại OTP, thay yêu cầu và hủy](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L495) | Lễ tân / API dùng chung |
| 102 | [GET /notifications](API_VA_CLASS_RECEP.md#L937) | [D27 — Thông báo, đọc một và đọc tất cả](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L530) | Lễ tân / API dùng chung |
| 103 | [PATCH /notifications/{notificationId}/read](API_VA_CLASS_RECEP.md#L947) | [D27 — Thông báo, đọc một và đọc tất cả](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L530) | Lễ tân / API dùng chung |
| 104 | [PATCH /notifications/read-all](API_VA_CLASS_RECEP.md#L957) | [D27 — Thông báo, đọc một và đọc tất cả](QUY_TRINH_DEMO_DAY_DU_RECEP.md#L530) | Lễ tân / API dùng chung |

Kiểm tra tự động khi soạn: đủ 104 ánh xạ duy nhất; tất cả trỏ đến mục demo tồn tại. Điều này không thay thế chạy request với dữ liệu, token, cookie, clock và cấu hình môi trường thật.
