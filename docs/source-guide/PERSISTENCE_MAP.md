# Bản đồ persistence

Javac đã phân giải 454 lời gọi JDBC trong production source. Không còn lời gọi JDBC nghiệp vụ chưa phân loại ở controller/service. SQL/row mapping thuộc repository theo nghiệp vụ; các thành phần infrastructure có sẵn giữ trách nhiệm dữ liệu của chúng.

## Repository JDBC

| Class | Domain | Số lời gọi JDBC | Source |
|---|---|---:|---|
| `CourseEnrollmentRepository` | `course` | 6 | [CourseEnrollmentRepository.java](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentRepository.java#L14) |
| `ReceptionCourseEnrollmentRepository` | `course` | 3 | [ReceptionCourseEnrollmentRepository.java](../../src/main/java/com/scms/backend/course/enrollment/ReceptionCourseEnrollmentRepository.java#L51) |
| `CourseManagementRepository` | `course` | 3 | [CourseManagementRepository.java](../../src/main/java/com/scms/backend/course/management/CourseManagementRepository.java#L11) |
| `CourseOrderRepository` | `course` | 6 | [CourseOrderRepository.java](../../src/main/java/com/scms/backend/course/order/CourseOrderRepository.java#L49) |
| `CourseReservationExpiryRepository` | `course` | 11 | [CourseReservationExpiryRepository.java](../../src/main/java/com/scms/backend/course/order/CourseReservationExpiryRepository.java#L12) |
| `CourseRefundObligationRepository` | `course` | 21 | [CourseRefundObligationRepository.java](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L27) |
| `CourseUnitRefundsRepository` | `course` | 9 | [CourseUnitRefundsRepository.java](../../src/main/java/com/scms/backend/course/refund/CourseUnitRefundsRepository.java#L13) |
| `CourseRunClosureRepository` | `course` | 19 | [CourseRunClosureRepository.java](../../src/main/java/com/scms/backend/course/run/CourseRunClosureRepository.java#L12) |
| `CourseRunRescheduleRepository` | `course` | 8 | [CourseRunRescheduleRepository.java](../../src/main/java/com/scms/backend/course/run/CourseRunRescheduleRepository.java#L12) |
| `CourseScheduleChangeRepository` | `course` | 17 | [CourseScheduleChangeRepository.java](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeRepository.java#L30) |
| `CourseCoachSessionRepository` | `course` | 3 | [CourseCoachSessionRepository.java](../../src/main/java/com/scms/backend/course/session/CourseCoachSessionRepository.java#L116) |
| `CourseResourceAvailabilityRepository` | `course` | 1 | [CourseResourceAvailabilityRepository.java](../../src/main/java/com/scms/backend/course/session/CourseResourceAvailabilityRepository.java#L12) |
| `CourseSessionDisruptionRepository` | `course` | 16 | [CourseSessionDisruptionRepository.java](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionRepository.java#L11) |
| `CourseTransferRepository` | `course` | 53 | [CourseTransferRepository.java](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L28) |
| `CourseWaitlistRepository` | `course` | 24 | [CourseWaitlistRepository.java](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L16) |
| `CourseWithdrawalRepository` | `course` | 26 | [CourseWithdrawalRepository.java](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L26) |
| `AccountEmailChangeRepository` | `emailverification` | 13 | [AccountEmailChangeRepository.java](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeRepository.java#L18) |
| `ManualActivationRepository` | `emailverification` | 15 | [ManualActivationRepository.java](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationRepository.java#L49) |
| `PasswordResetRepository` | `emailverification` | 16 | [PasswordResetRepository.java](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetRepository.java#L16) |
| `EmailVerificationRepository` | `emailverification` | 20 | [EmailVerificationRepository.java](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L18) |
| `PendingRegistrationCleanupRepository` | `emailverification` | 8 | [PendingRegistrationCleanupRepository.java](../../src/main/java/com/scms/backend/emailverification/verification/PendingRegistrationCleanupRepository.java#L12) |
| `ReceptionMemberProfileRepository` | `identity` | 1 | [ReceptionMemberProfileRepository.java](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberProfileRepository.java#L17) |
| `MemberStatusRepository` | `manager` | 2 | [MemberStatusRepository.java](../../src/main/java/com/scms/backend/manager/MemberStatusRepository.java#L12) |
| `StaffStatusRepository` | `manager` | 3 | [StaffStatusRepository.java](../../src/main/java/com/scms/backend/manager/StaffStatusRepository.java#L10) |
| `MembershipFreezeExecutionRepository` | `membership` | 7 | [MembershipFreezeExecutionRepository.java](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeExecutionRepository.java#L11) |
| `MembershipFreezeRepository` | `membership` | 5 | [MembershipFreezeRepository.java](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRepository.java#L14) |
| `CourseReconciliationRepository` | `membership` | 4 | [CourseReconciliationRepository.java](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationRepository.java#L38) |
| `PaymentEvidenceRepository` | `membership` | 6 | [PaymentEvidenceRepository.java](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceRepository.java#L12) |
| `PaymentFulfillmentRepository` | `membership` | 40 | [PaymentFulfillmentRepository.java](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L24) |
| `PaymentProviderEventRepository` | `membership` | 4 | [PaymentProviderEventRepository.java](../../src/main/java/com/scms/backend/membership/payment/PaymentProviderEventRepository.java#L22) |
| `PaymentRepository` | `membership` | 22 | [PaymentRepository.java](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L42) |
| `SepayRepository` | `membership` | 18 | [SepayRepository.java](../../src/main/java/com/scms/backend/membership/payment/SepayRepository.java#L12) |
| `MembershipRefundRepository` | `membership` | 5 | [MembershipRefundRepository.java](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundRepository.java#L23) |
| `ManagerReportingRepository` | `membership` | 8 | [ManagerReportingRepository.java](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingRepository.java#L144) |
| `NotificationOutboxRepository` | `notification` | 5 | [NotificationOutboxRepository.java](../../src/main/java/com/scms/backend/notification/NotificationOutboxRepository.java#L21) |
| `CoachLeaveImpactRepository` | `scheduling` | 2 | [CoachLeaveImpactRepository.java](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactRepository.java#L64) |
| `RoomSuspensionJdbcRepository` | `scheduling` | 1 | [RoomSuspensionJdbcRepository.java](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionJdbcRepository.java#L10) |
| `ClassSessionCancellationRepository` | `scheduling` | 2 | [ClassSessionCancellationRepository.java](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCancellationRepository.java#L11) |
| `CoachAssignmentReminderRepository` | `training` | 1 | [CoachAssignmentReminderRepository.java](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentReminderRepository.java#L9) |
| `BookedSessionTrainingRepository` | `training` | 3 | [BookedSessionTrainingRepository.java](../../src/main/java/com/scms/backend/training/session/BookedSessionTrainingRepository.java#L23) |

## Infrastructure giữ nguyên trách nhiệm

| Class | Lý do | Source |
|---|---|---|
| `CourseRefundObligationWriter` | Writer persistence đã có cho refund obligation; tiếp tục dùng trong transaction của caller. | [CourseRefundObligationWriter](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationWriter.java#L29) |
| `DatabaseRateLimiter` | Infrastructure rate limiting dùng database để phối hợp giữa request. | [DatabaseRateLimiter](../../src/main/java/com/scms/backend/emailverification/DatabaseRateLimiter.java#L30) |
| `ReferencedEmailKeyValidator` | Kiểm tra key mã hóa được dữ liệu đang lưu tham chiếu khi khởi động. | [ReferencedEmailKeyValidator](../../src/main/java/com/scms/backend/emailverification/ReferencedEmailKeyValidator.java#L25) |
| `EmailOutboxDeliveryService` | Worker delivery sở hữu trạng thái outbox và retry. | [EmailOutboxDeliveryService](../../src/main/java/com/scms/backend/emailverification/delivery/EmailOutboxDeliveryService.java#L49) |
| `EmailOutboxEnqueuer` | Ghi outbox email; đã là thành phần persistence. | [EmailOutboxEnqueuer](../../src/main/java/com/scms/backend/emailverification/delivery/EmailOutboxEnqueuer.java#L48) |
| `InitialManagerProvisioner` | Provision tài khoản khởi tạo qua cấu hình; không thêm lớp bọc SQL. | [InitialManagerProvisioner](../../src/main/java/com/scms/backend/support/InitialManagerProvisioner.java#L62) |

Hai native query qua EntityManager vẫn thuộc [BookingCancellationRepositoryImpl](../../src/main/java/com/scms/backend/scheduling/booking/BookingCancellationRepositoryImpl.java). Repository JPA và JPQL giữ cơ chế hiện có. Không đổi JPA sang JDBC, không đổi SQL/index/migration trong cleanup.

Con số ở bảng là lời gọi trong source, không phải số query chạy cho một request. Một method repository có thể được gọi nhiều lần; thứ tự lock và transaction vẫn do service/caller quyết định.
