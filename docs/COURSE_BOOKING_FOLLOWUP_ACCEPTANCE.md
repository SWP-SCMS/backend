# Course Booking — bàn giao và ma trận kiểm chứng sau G15

## Trạng thái và phạm vi

Backend-only; không sửa FE. Baseline của chuỗi bổ sung: b25f40c. PR #200 squash 4fef6fa17e5d6589f575f94f60e50d8b99173237 đã có Main CI 41599102563 (unit + 4/4 PostgreSQL shards) và security 41599101912 thành công. Migration V70 lưu quyết định đối soát/retry, giữ lịch sử cũ. PR tiếp theo không thêm migration.

Tài liệu nguồn COURSE_BOOKING_GAPS (2).md trộn lịch sử và QA ngày 03/11/2026. Các ca QA được báo cáo không được tính là smoke đã chạy trong phiên này. Không có môi trường cô lập: SMOKE_UNVERIFIED; UI/E2E_UNVERIFIED. Docker local: NOT_RUN_LOCAL_DOCKER; kiểm chứng PostgreSQL qua CI là độc lập với giới hạn này.

## Phân loại phát hiện

| Phát hiện | Trạng thái/backend hoặc bàn giao |
|---|---|
| Reception tạo Course; pending vẫn được học; snapshot tính lúc duyệt; thiếu hủy; Run không hoàn tất; dời nhiều API; thiếu G14 | Đã triển khai ở #195–199; giữ regression, không triển khai lại |
| G15 không có Course trong đối soát Membership | Đã bổ sung API Course riêng ở #200; xem COURSE_PAYMENT_RECONCILIATION.md |
| QR 503 thiếu/sai checksum | Cấu hình; giữ fail-closed. Không phải bằng chứng mọi thanh toán Course hỏng |
| API attendance bỏ qua trạng thái Run dù màn Coach dùng policy | Regression mới xác nhận; API đọc Run đã khóa và dùng cùng CourseAttendancePolicy |
| Reception panel stale sau thu tiền/rút/hủy/chuyển | REPORTED/FE_PENDING |
| Diễn giải hoàn tiền lúc duyệt | REPORTED/FE_PENDING; dùng REQUEST_TIME, không tính lại |
| Timeline ENROLLED bị hiểu là đang có quyền học | REPORTED/FE_PENDING; phân biệt phân bổ lịch sử với lifecycle hiện tại |
| Buổi gốc hủy và buổi bù hiện như hai đơn vị | REPORTED/FE_PENDING; hiển thị trạng thái và quan hệ thay thế |
| Chấp nhận chuyển hộ dùng cảnh báo rút | REPORTED/FE_PENDING; cảnh báo theo hành động |
| Các ca thời gian/race/refund chưa chạy UI | UI/E2E_UNVERIFIED, không mặc nhiên là bug backend |

## Ma trận regression có thể truy vết

Các file test nằm dưới src/test/java/com/scms/backend; methods dưới đây là bằng chứng tự động, không phải nghiệm thu deployment.

| Yêu cầu | Test và phạm vi |
|---|---|
| Đối soát đúng quyền/owner/queue/retry | membership/PaymentFlowIntegrationTests: courseReconciliationQueuePaymentAndPrivateResultHaveCorrectOwnership, courseReconciliationRejectsInvalidMoneyRolesKeysAndTerminalChanges |
| Hết hạn, rollback, race/reuse | Cùng file: courseReconciliationNeverRevivesExpiredReservationsAndFailureReplays, courseReconciliationAuditFailureRollsBackTheEntireDecision, concurrentCourseReconciliationAndWebhookOnlyFulfillOnce, concurrentStaffCourseReconciliationSerializesKeysAndProviderClaims, concurrentCourseCashAndReconciliationDoNotDuplicateEnrollment, concurrentCourseExpiryAndFailedReconciliationExpireOnlyOnce, manualCourseTransactionCannotBeReusedByMembershipOrAnotherCourse |
| Chuyển đợt đắt hơn | course/CourseTransferIntegrationTests: manualReconciliationCompletesHigherPriceTransferAndPreservesBothActors; các ca cash/webhook hiện có vẫn chạy |
| 7/10 → 6/10 và duyệt trễ | course/CourseWithdrawalIntegrationTests: approvalUsesRequestTimeSnapshotEvenAfterAnotherUnitStarts, validRequestCanBeApprovedAfterRunCompletes |
| Pending giữ chỗ/ngừng học, hủy và cạnh tranh | Cùng file: pendingKeepsCapacityAndExactSeventyPercentApprovalCreatesFlooredRefund, pendingWithdrawalSuspendsRosterAndAttendance, cancellationRestoresRightsWithoutChangingHistoryOrGrantingMakeup, cancellationApprovalAndRejectionCompeteInRealTransactions, runCancellationClosesPendingRequestAndOnlyCreatesRunCancellationRefund |
| Course-only check-in và ghi attendance HTTP/DB | Cùng file: courseOnlyCheckInAndAttendancePersistThroughApiWithTimeAndAssignmentGuards; không Membership, check-in retry, pending không vào học, hủy khôi phục, thời gian trước/start/end+30, Coach khác, trạng thái DB/audit |
| Attendance Run chưa hoạt động | CourseEnrollmentServiceTests: inactiveRunCannotBypassCoachAttendancePolicy (đỏ trên code cũ); CourseWithdrawalIntegrationTests: attendanceApiCannotWriteForInactiveRunEvenInsideSessionWindow |
| Completion/makeup/legacy/đồng thời | course/CourseScheduleChangeIntegrationTests: legacySettlementPendingCompletesWithoutWaitingForPayout, pendingSnapshotDoesNotBlockCompletionAndRemainsApprovable, completionWaitsForLastEndAndPreservesAttendanceWindowAndEnrollment, missingUnitsAndLegacyFutureSessionsDoNotComplete, completionUsesEffectiveMakeupEndEvenWhenEarlierThanCancelledOriginal, concurrentSchedulerRunsCommitOneCompletionAndOneRefund |
| Chi hoàn thường sau completion | Cùng file: automaticallyCompletedRunStillRequiresManagerApprovalBeforeReceptionDisbursement, mở rộng đến verify COMPLETED và retry không thêm audit |
| BANK_TRANSFER, trả lại/thất bại | course/CourseRefundObligationIntegrationTests: bankTransferRefundHttpWorkflowRequiresCorrectRolesEvidenceAndTerminalState; managerCanFailApprovedObligationOrMarkRecordedDisbursementReturned; twoReceptionistsCompetingToDisburseCommitExactlyOneRecord |
| Proxy | CourseOrderIntegrationTests: proxyWaitlistCannotBypassCapacityOwnershipKeysOrInvitationExpiry, proxyWaitlistLeaveAuditsActorAndRejectsInactiveBeneficiaryAndStaff; CourseTransferIntegrationTests: proxyResponsePreservesOwnershipDeadlineAndContinueReplay, receptionistScheduleWithdrawalCapturesSnapshotAndActorOnce, proxyOfferCannotCrossOwnersOmitKeysOrAcceptAtExpiry; CourseWithdrawalIntegrationTests: receptionHistoryMatchesMemberDtoAndEnforcesBeneficiaryOwnership |
| Migration/contract | CoursePaymentMigrationV53Tests: v70UpgradePreservesPaymentSnapshotsAndCreatesEmptyDecisionLedger; existing V68/V69 migration tests; GeneratedOpenApiTests: courseReconciliationContractExistsIndependentlyOfMembership và toàn bộ parity suite |

Mỗi PR phải qua selector non-container của CI và 4/4 integration shards trên cả push/pull_request. Lỗi schema inventory V69→V70 ở lượt CI đầu #200 đã được cập nhật đúng version, không bỏ assertion hoặc sửa migration cũ. Không tạo test trùng cho các dòng đã được kiểm chứng.

## Handoff FE

1. Khi mutation thành công, invalidate/refetch theo memberId: orders/payment, enrollment nguồn/đích, timeline, waitlist, transfer, withdrawal, refund và khả năng thao tác liên quan. Không cần reload trang. Nếu refetch lỗi, báo cập nhật dữ liệu thất bại và cho tải lại; không tự gửi lại mutation thành công.
2. Hiển thị snapshot REQUEST_TIME và tiền đã chốt. Legacy thiếu snapshot hiển thị riêng; không tự tính số tiền từ lịch hiện tại hoặc nói tiền chốt lúc duyệt. Đã hủy/từ chối/duyệt là lịch sử, không tự mở request mới khi retry.
3. ENROLLED ở từng buổi là phân bổ lịch sử. Quyền học hiện tại còn phụ thuộc lifecycle, Run/session và thời gian. WITHDRAWAL_PENDING giữ chỗ nhưng tạm ngừng; WITHDRAWN/TRANSFERRED không học nguồn; Membership độc lập không bị suy diễn mất quyền. Hủy/từ chối không cấp buổi bù.
4. Lịch đăng ký lấy lịch hiệu lực. Nếu giữ buổi gốc bị hủy trong lịch sử, hiển thị nhãn và replacementSessionId/originalSessionId; không đếm hai bản ghi thành hai đơn vị.
5. Cảnh báo rút, hủy rút, chuyển, nhận chênh lệch phải riêng. Chấp nhận chuyển không dùng cảnh báo tạm ngừng do rút. Hiển thị đúng Member, Run nguồn/đích và số tiền cần trả/hoàn trước xác nhận.
6. Đối soát dùng /course-payments, không dùng /payments của Membership. Manager/Reception đều có quyền; giữ key và payload qua timeout; 409 hết hạn không được tự gia hạn đơn. Xem contract và ví dụ trong COURSE_PAYMENT_RECONCILIATION.md.
7. Attendance: Coach được phân công + ACTIVE enrollment + Run CONFIRMED/IN_PROGRESS/COMPLETED + session hợp lệ; cửa sổ [startAt,endAt+30 phút). 409 COURSE_ATTENDANCE_WINDOW_CLOSED gồm Run chưa hoạt động; 404 khi sai assignment hoặc Run bị hủy. FE không chỉ dựa vào nút disabled để bảo vệ API.

Không đổi các mục FE_PENDING thành FIXED chỉ vì backend đã merge. Phải kiểm đúng source FE tương ứng, vì checkout hiện có chưa xác minh commit baba59c.

## Smoke fixture và checklist môi trường cô lập

Chỉ chạy sau khi xác minh URL môi trường test, backend SHA, FE SHA (nếu kiểm UI), migration version và danh tính tài khoản. Không dùng production/DB nghiệp vụ. Tạo dữ liệu mới có prefix QA-COURSE-<timestamp>, không sửa/xóa QA/audit cũ; tiền đều sandbox/mock, không chuyển tiền thật.

| Fixture | Dữ liệu và điều kiện |
|---|---|
| Accounts | Manager ACTIVE, Reception ACTIVE, Coach A/B ACTIVE, Member A chỉ có Course, Member B có Membership độc lập, tài khoản inactive để kiểm từ chối |
| R-live | Course published, 10 đơn vị, capacity 2, học phí 100000 VND; lịch không xung đột; một buổi nằm trong cửa sổ attendance thực tế, một số buổi tương lai |
| R-transfer | Cùng discipline, Coach/phòng không xung đột; tuition 120000 VND và 80000 VND cho hai nhánh chênh lệch |
| R-payment | Run mở đăng ký, lịch tương lai; tạo một order còn hold, một order chờ hết hạn tự nhiên; lưu paymentId/reference/key qua response API |
| R-refund | Run ngắn với buổi chưa phục vụ được xử lý đúng API; chờ scheduler sau giờ kết thúc, không cập nhật status trực tiếp trong DB |

Checklist thực hiện bằng API hợp lệ, ghi kết quả từng bước cùng SHA/UTC instant/requestId; không lưu token/secret trong báo cáo:

- [ ] Tạo order và QR; cấu hình thiếu/sai checksum chỉ thử trong runtime test riêng. Cấu hình đúng trả snapshot, không thay đổi cấu hình production.
- [ ] Manager và Reception tìm payment trong queue, đối soát đúng amount/reference; đọc result bằng Member chủ sở hữu, Member khác bị chặn. Retry key/payload giữ nguyên kết quả; payload khác 409.
- [ ] Đơn hết hạn không PAID; FAILED chỉ theo trạng thái thực tế. Webhook sandbox và đối soát cùng giao dịch không tạo thêm enrollment/notification. Không ghi nhận tiền thật.
- [ ] Rút ở 7/10, lưu snapshot; kiểm duyệt ở 6/10/sau buổi cuối với thời gian thật trên fixture phù hợp. Nếu không có fixture thì UNVERIFIED, không sửa đồng hồ hệ thống hoặc DB để giả pass.
- [ ] Pending giữ chỗ, chặn học; hủy/từ chối khôi phục quyền từ lúc xử lý, giữ attendance cũ và không thêm makeup.
- [ ] Member Course-only check-in được trong cửa sổ; Coach A ghi attendance, B bị chặn; ngoài cửa sổ bị chặn. Membership của B vẫn dùng độc lập.
- [ ] Waitlist tự làm/làm hộ: join, leave, invite, accept; capacity/deadline và đúng actor. Phản hồi lịch và accept transfer làm hộ đúng owner.
- [ ] Chuyển đợt đắt hơn trả đúng phần thiếu qua sandbox; rẻ hơn tạo đúng nghĩa vụ hoàn; nguồn TRANSFERRED, đích ACTIVE; không tạo hoàn/đơn trùng khi retry.
- [ ] Scheduler hoàn tất Run sau lịch hiệu lực, tạo nghĩa vụ hoàn trước COMPLETED. Manager approve → Reception CASH/BANK_TRANSFER giả → Manager verify; nhánh fail/returned giữ lịch sử và tổng tiền.
- [ ] Sau mọi mutation, kiểm data/audit/outbox; UI nếu có đúng FE phải tự cập nhật panel và nội dung cảnh báo đúng. API pass không được ghi UI pass.

Kết quả mỗi ca: PASS / FAIL / NOT_RUN kèm lý do. Trạng thái tối đa khi chưa có môi trường: MERGED + MAIN_CI_PASS + SMOKE_UNVERIFIED; FE_PENDING và UI/E2E_UNVERIFIED vẫn còn. Không deploy thủ công trong nhiệm vụ này.
