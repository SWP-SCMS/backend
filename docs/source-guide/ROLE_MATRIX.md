# Endpoint–role matrix

Đây là quyền tại filter chain. Điều kiện account ACTIVE, ownership, trạng thái và quyền lợi nằm trong service. PUBLIC không loại trừ xác thực webhook riêng.

| Method | Path | Quyền | Rule |
|---|---|---|---|
| GET | `/manager/audit-events` | MANAGER | SecurityConfiguration.java:277 |
| POST | `/auth/login` | PUBLIC | SecurityConfiguration.java:129 |
| POST | `/auth/refresh` | PUBLIC | SecurityConfiguration.java:129 |
| POST | `/auth/logout` | PUBLIC | SecurityConfiguration.java:129 |
| POST | `/auth/change-password` | Authenticated (fallback) | SecurityConfiguration#securityFilterChain |
| POST | `/auth/register` | PUBLIC | SecurityConfiguration.java:129 |
| POST | `/reception/center-visits` | RECEPTIONIST | SecurityConfiguration.java:180 |
| GET | `/members/me/center-visits/current` | MEMBER | SecurityConfiguration.java:181 |
| PATCH | `/members/me/center-visits/current/checkout` | MEMBER | SecurityConfiguration.java:181 |
| PATCH | `/reception/members/{memberAccountId}/center-visits/current/checkout` | RECEPTIONIST | SecurityConfiguration.java:179 |
| GET | `/reception/center-visits/open` | RECEPTIONIST | SecurityConfiguration.java:180 |
| GET | `/courses` | PUBLIC | SecurityConfiguration.java:135 |
| GET | `/courses/{courseId}` | PUBLIC | SecurityConfiguration.java:135 |
| GET | `/course-runs` | PUBLIC | SecurityConfiguration.java:135 |
| GET | `/course-runs/{runId}` | PUBLIC | SecurityConfiguration.java:135 |
| POST | `/manager/courses/images` | MANAGER | SecurityConfiguration.java:191 |
| GET | `/reception/members/{memberId}/course-enrollments` | RECEPTIONIST | SecurityConfiguration.java:172 |
| GET | `/reception/members/{memberId}/course-enrollments/{enrollmentId}` | RECEPTIONIST | SecurityConfiguration.java:172 |
| GET | `/members/me/course-enrollments` | MEMBER | SecurityConfiguration.java:250 |
| GET | `/members/me/course-schedule-changes` | Authenticated (fallback) | SecurityConfiguration#securityFilterChain |
| POST | `/members/me/course-schedule-changes/{responseId}/response` | Authenticated (fallback) | SecurityConfiguration#securityFilterChain |
| GET | `/coach/course-sessions/{sessionId}/roster` | COACH | SecurityConfiguration.java:256 |
| PATCH | `/coach/course-sessions/{sessionId}/roster/{enrollmentSessionId}/attendance` | COACH | SecurityConfiguration.java:256 |
| GET | `/reception/members/{memberId}/course-waitlists` | RECEPTIONIST | SecurityConfiguration.java:179 |
| POST | `/reception/members/{memberId}/course-runs/{runId}/waitlist` | RECEPTIONIST | SecurityConfiguration.java:179 |
| DELETE | `/reception/members/{memberId}/course-runs/{runId}/waitlist` | RECEPTIONIST | SecurityConfiguration.java:179 |
| POST | `/reception/members/{memberId}/course-waitlists/{waitlistId}/accept` | RECEPTIONIST | SecurityConfiguration.java:179 |
| GET | `/reception/members/{memberId}/course-schedule-changes` | RECEPTIONIST | SecurityConfiguration.java:179 |
| POST | `/reception/members/{memberId}/course-schedule-changes/{responseId}/response` | RECEPTIONIST | SecurityConfiguration.java:179 |
| GET | `/reception/members/{memberId}/course-transfer-offers` | RECEPTIONIST | SecurityConfiguration.java:179 |
| POST | `/reception/members/{memberId}/course-transfer-offers/{offerId}/accept` | RECEPTIONIST | SecurityConfiguration.java:179 |
| GET | `/reception/members/{memberId}/course-withdrawal-requests` | RECEPTIONIST | SecurityConfiguration.java:179 |
| GET | `/reception/members/{memberId}/course-withdrawal-requests/{requestId}` | RECEPTIONIST | SecurityConfiguration.java:179 |
| GET | `/reception/members/{memberId}/course-refund-obligations` | RECEPTIONIST | SecurityConfiguration.java:179 |
| GET | `/reception/members/{memberId}/course-refund-obligations/{obligationId}` | RECEPTIONIST | SecurityConfiguration.java:179 |
| GET | `/manager/course-approval-requests` | MANAGER | SecurityConfiguration.java:192 |
| GET | `/manager/course-approval-requests/{courseId}` | MANAGER | SecurityConfiguration.java:192 |
| POST | `/manager/course-approval-requests/{courseId}/approve` | MANAGER | SecurityConfiguration.java:192 |
| POST | `/manager/course-approval-requests/{courseId}/reject` | MANAGER | SecurityConfiguration.java:192 |
| GET | `/manager/courses` | MANAGER | SecurityConfiguration.java:191 |
| POST | `/manager/courses` | MANAGER | SecurityConfiguration.java:191 |
| GET | `/manager/courses/{courseId}` | MANAGER | SecurityConfiguration.java:191 |
| PATCH | `/manager/courses/{courseId}` | MANAGER | SecurityConfiguration.java:191 |
| POST | `/manager/courses/{courseId}/publish` | MANAGER | SecurityConfiguration.java:191 |
| POST | `/manager/courses/{courseId}/deactivate` | MANAGER | SecurityConfiguration.java:191 |
| POST | `/manager/courses/{courseId}/archive` | MANAGER | SecurityConfiguration.java:191 |
| GET | `/manager/course-runs` | MANAGER | SecurityConfiguration.java:191 |
| POST | `/manager/course-runs` | MANAGER | SecurityConfiguration.java:191 |
| GET | `/manager/course-runs/{runId}` | MANAGER | SecurityConfiguration.java:191 |
| PATCH | `/manager/course-runs/{runId}` | MANAGER | SecurityConfiguration.java:191 |
| POST | `/manager/course-runs/{runId}/open-registration` | MANAGER | SecurityConfiguration.java:191 |
| PATCH | `/manager/course-runs/{runId}/schedule` | MANAGER | SecurityConfiguration.java:191 |
| POST | `/manager/course-runs/{runId}/cancel` | MANAGER | SecurityConfiguration.java:191 |
| GET | `/manager/course-runs/{runId}/cancellation-impact` | MANAGER | SecurityConfiguration.java:191 |
| POST | `/manager/course-runs/{runId}/reschedule` | MANAGER | SecurityConfiguration.java:191 |
| GET | `/manager/course-runs/{runId}/sessions` | MANAGER | SecurityConfiguration.java:191 |
| POST | `/manager/course-runs/{runId}/sessions` | MANAGER | SecurityConfiguration.java:191 |
| GET | `/manager/course-runs/{runId}/sessions/{sessionId}` | MANAGER | SecurityConfiguration.java:191 |
| PATCH | `/manager/course-runs/{runId}/sessions/{sessionId}` | MANAGER | SecurityConfiguration.java:191 |
| POST | `/manager/course-runs/{runId}/sessions/{sessionId}/cancel` | MANAGER | SecurityConfiguration.java:191 |
| POST | `/manager/course-runs/{runId}/sessions/{sessionId}/cancel-with-makeup` | MANAGER | SecurityConfiguration.java:191 |
| POST | `/manager/course-runs/{runId}/sessions/{sessionId}/makeup` | MANAGER | SecurityConfiguration.java:191 |
| POST | `/members/me/course-orders` | MEMBER | SecurityConfiguration.java:166 |
| GET | `/members/me/course-orders/{orderId}` | MEMBER | SecurityConfiguration.java:167 |
| POST | `/reception/members/{memberId}/course-orders` | RECEPTIONIST | SecurityConfiguration.java:170 |
| GET | `/reception/members/{memberId}/course-orders/{orderId}` | RECEPTIONIST | SecurityConfiguration.java:171 |
| GET | `/manager/course-refund-obligations` | MANAGER | SecurityConfiguration.java:195 |
| GET | `/manager/course-refund-obligations/{obligationId}` | MANAGER | SecurityConfiguration.java:195 |
| GET | `/reception/course-refunds` | RECEPTIONIST | SecurityConfiguration.java:196 |
| GET | `/reception/course-refunds/{refundId}` | RECEPTIONIST | SecurityConfiguration.java:196 |
| POST | `/manager/course-refund-obligations/{obligationId}/approve` | MANAGER | SecurityConfiguration.java:195 |
| POST | `/manager/course-refund-obligations/{obligationId}/reject` | MANAGER | SecurityConfiguration.java:195 |
| POST | `/reception/course-refund-obligations/{obligationId}/disburse` | RECEPTIONIST | SecurityConfiguration.java:196 |
| POST | `/manager/course-refund-obligations/{obligationId}/verify` | MANAGER | SecurityConfiguration.java:195 |
| POST | `/manager/course-refund-obligations/{obligationId}/fail` | MANAGER | SecurityConfiguration.java:195 |
| POST | `/manager/course-refund-obligations/{obligationId}/return` | MANAGER | SecurityConfiguration.java:195 |
| GET | `/members/me/course-refund-obligations` | MEMBER | SecurityConfiguration.java:252 |
| GET | `/members/me/course-refund-obligations/{obligationId}` | MEMBER | SecurityConfiguration.java:252 |
| POST | `/manager/course-runs/preview-schedule` | MANAGER | SecurityConfiguration.java:191 |
| POST | `/manager/course-runs/with-schedule` | MANAGER | SecurityConfiguration.java:191 |
| GET | `/coach/course-sessions` | COACH | SecurityConfiguration.java:255 |
| GET | `/coach/course-sessions/{sessionId}` | COACH | SecurityConfiguration.java:255 |
| POST | `/manager/course-runs/resource-availability` | MANAGER | SecurityConfiguration.java:191 |
| GET | `/manager/course-transfer-requests` | MANAGER | SecurityConfiguration.java:193 |
| GET | `/manager/course-transfer-requests/{requestId}/targets` | MANAGER | SecurityConfiguration.java:193 |
| POST | `/manager/course-transfer-requests/{requestId}/approve` | MANAGER | SecurityConfiguration.java:193 |
| POST | `/manager/course-transfer-requests/{requestId}/reject` | MANAGER | SecurityConfiguration.java:193 |
| GET | `/members/me/course-transfer-offers` | MEMBER | SecurityConfiguration.java:249 |
| POST | `/members/me/course-transfer-offers/{offerId}/accept` | MEMBER | SecurityConfiguration.java:249 |
| POST | `/course-runs/{runId}/waitlist` | MEMBER | SecurityConfiguration.java:254 |
| DELETE | `/course-runs/{runId}/waitlist` | MEMBER | SecurityConfiguration.java:254 |
| GET | `/members/me/course-waitlists` | MEMBER | SecurityConfiguration.java:254 |
| POST | `/course-waitlists/{waitlistId}/accept` | MEMBER | SecurityConfiguration.java:254 |
| POST | `/members/me/course-enrollments/{enrollmentId}/withdrawal-requests` | MEMBER | SecurityConfiguration.java:251 |
| GET | `/members/me/course-withdrawal-requests` | MEMBER | SecurityConfiguration.java:251 |
| GET | `/members/me/course-withdrawal-requests/{requestId}` | MEMBER | SecurityConfiguration.java:251 |
| POST | `/reception/members/{memberId}/course-enrollments/{enrollmentId}/withdrawal-requests` | RECEPTIONIST | SecurityConfiguration.java:179 |
| POST | `/members/me/course-withdrawal-requests/{requestId}/cancel` | MEMBER | SecurityConfiguration.java:251 |
| POST | `/reception/members/{memberId}/course-withdrawal-requests/{requestId}/cancel` | RECEPTIONIST | SecurityConfiguration.java:179 |
| GET | `/manager/course-withdrawal-requests` | MANAGER | SecurityConfiguration.java:194 |
| GET | `/manager/course-withdrawal-requests/{requestId}` | MANAGER | SecurityConfiguration.java:194 |
| POST | `/manager/course-withdrawal-requests/{requestId}/approve` | MANAGER | SecurityConfiguration.java:194 |
| POST | `/manager/course-withdrawal-requests/{requestId}/reject` | MANAGER | SecurityConfiguration.java:194 |
| POST | `/manager/members/{memberId}/course-enrollments/{enrollmentId}/withdrawal-requests` | MANAGER | SecurityConfiguration.java:194 |
| GET | `/accounts/me/email-change` | Authenticated | SecurityConfiguration.java:270 |
| POST | `/accounts/me/email-change` | Authenticated | SecurityConfiguration.java:270 |
| PUT | `/accounts/me/email-change` | Authenticated | SecurityConfiguration.java:270 |
| POST | `/accounts/me/email-change/verify` | Authenticated | SecurityConfiguration.java:270 |
| POST | `/accounts/me/email-change/resend` | Authenticated | SecurityConfiguration.java:270 |
| DELETE | `/accounts/me/email-change` | Authenticated | SecurityConfiguration.java:270 |
| POST | `/staff/me/email-change` | MANAGER, RECEPTIONIST, COACH | SecurityConfiguration.java:283 |
| POST | `/staff/me/email-change/verify` | MANAGER, RECEPTIONIST, COACH | SecurityConfiguration.java:283 |
| POST | `/staff/me/email-change/resend` | MANAGER, RECEPTIONIST, COACH | SecurityConfiguration.java:283 |
| DELETE | `/staff/me/email-change` | MANAGER, RECEPTIONIST, COACH | SecurityConfiguration.java:283 |
| GET | `/manager/email-verification/manual-activation-requests` | MANAGER | SecurityConfiguration.java:187 |
| GET | `/manager/email-verification/manual-activation-requests/{requestId}` | MANAGER | SecurityConfiguration.java:187 |
| POST | `/manager/email-verification/manual-activation-requests/{requestId}/approve` | MANAGER | SecurityConfiguration.java:187 |
| POST | `/manager/email-verification/manual-activation-requests/{requestId}/reject` | MANAGER | SecurityConfiguration.java:187 |
| POST | `/auth/password-reset/request` | PUBLIC | SecurityConfiguration.java:130 |
| POST | `/auth/password-reset/verify` | PUBLIC | SecurityConfiguration.java:130 |
| POST | `/auth/password-reset/confirm` | PUBLIC | SecurityConfiguration.java:130 |
| POST | `/auth/email-verification/verify` | PUBLIC | SecurityConfiguration.java:131 |
| POST | `/auth/email-verification/resend` | PUBLIC | SecurityConfiguration.java:131 |
| POST | `/auth/email-verification/change-email` | PUBLIC | SecurityConfiguration.java:131 |
| GET | `/reception/email-verification/pending-accounts` | RECEPTIONIST | SecurityConfiguration.java:178 |
| POST | `/reception/email-verification/manual-activation-requests` | RECEPTIONIST | SecurityConfiguration.java:178 |
| GET | `/reception/email-verification/manual-activation-requests` | RECEPTIONIST | SecurityConfiguration.java:178 |
| GET | `/reception/email-verification/manual-activation-requests/{requestId}` | RECEPTIONIST | SecurityConfiguration.java:178 |
| GET | `/members/me/profile` | MEMBER | SecurityConfiguration.java:284 |
| PATCH | `/members/me/profile` | MEMBER | SecurityConfiguration.java:284 |
| POST | `/reception/members/{memberId}/reset-password` | RECEPTIONIST | SecurityConfiguration.java:179 |
| POST | `/reception/members` | RECEPTIONIST | SecurityConfiguration.java:174 |
| GET | `/reception/members/search` | RECEPTIONIST | SecurityConfiguration.java:179 |
| GET | `/reception/members/{memberId}/profile` | RECEPTIONIST | SecurityConfiguration.java:179 |
| PATCH | `/reception/members/{memberId}/profile` | RECEPTIONIST | SecurityConfiguration.java:179 |
| GET | `/staff/me/profile` | MANAGER, RECEPTIONIST, COACH | SecurityConfiguration.java:282 |
| PATCH | `/staff/me/profile` | MANAGER, RECEPTIONIST, COACH | SecurityConfiguration.java:282 |
| GET | `/manager/members` | MANAGER | SecurityConfiguration.java:279 |
| GET | `/manager/members/{id}` | MANAGER | SecurityConfiguration.java:279 |
| PATCH | `/manager/members/{id}` | MANAGER | SecurityConfiguration.java:279 |
| PATCH | `/manager/members/{memberAccountId}/status` | MANAGER | SecurityConfiguration.java:279 |
| POST | `/manager/staff-accounts` | MANAGER | SecurityConfiguration.java:186 |
| GET | `/manager/staff-accounts` | MANAGER | SecurityConfiguration.java:186 |
| GET | `/manager/staff-accounts/{accountId}` | MANAGER | SecurityConfiguration.java:186 |
| PATCH | `/manager/staff-accounts/{accountId}` | MANAGER | SecurityConfiguration.java:186 |
| PATCH | `/manager/staff-accounts/{accountId}/status` | MANAGER | SecurityConfiguration.java:186 |
| POST | `/manager/staff-accounts/{accountId}/reset-password` | MANAGER | SecurityConfiguration.java:186 |
| GET | `/members/me/membership-freeze-requests` | MEMBER | SecurityConfiguration.java:152 |
| POST | `/members/me/membership-freeze-requests` | MEMBER | SecurityConfiguration.java:153 |
| DELETE | `/members/me/membership-freeze-requests/{requestId}` | MEMBER | SecurityConfiguration.java:154 |
| POST | `/members/me/membership-freeze-requests/{requestId}/end-early` | Authenticated (fallback) | SecurityConfiguration#securityFilterChain |
| GET | `/reception/members/{memberCode}/membership-freeze-requests` | RECEPTIONIST | SecurityConfiguration.java:155 |
| POST | `/reception/members/{memberCode}/membership-freeze-requests` | RECEPTIONIST | SecurityConfiguration.java:156 |
| GET | `/manager/membership-freeze-requests` | MANAGER | SecurityConfiguration.java:158 |
| POST | `/manager/membership-freeze-requests/{requestId}/approve` | MANAGER | SecurityConfiguration.java:159 |
| POST | `/manager/membership-freeze-requests/{requestId}/reject` | MANAGER | SecurityConfiguration.java:159 |
| POST | `/reception/membership-freeze-requests/{requestId}/cash-payment` | RECEPTIONIST | SecurityConfiguration.java:157 |
| POST | `/reception/membership-freeze-requests/{requestId}/end-early` | Authenticated (fallback) | SecurityConfiguration#securityFilterChain |
| GET | `/manager/membership-offers` | MANAGER | SecurityConfiguration.java:243 |
| GET | `/manager/membership-offers/{offerId}` | MANAGER | SecurityConfiguration.java:243 |
| POST | `/manager/membership-offers` | MANAGER | SecurityConfiguration.java:243 |
| PATCH | `/manager/membership-offers/{id}` | MANAGER | SecurityConfiguration.java:243 |
| GET | `/membership-offers` | PUBLIC | SecurityConfiguration.java:133 |
| GET | `/membership-offers/{offerId}` | PUBLIC | SecurityConfiguration.java:133 |
| POST | `/members/me/membership-orders` | MEMBER | SecurityConfiguration.java:164 |
| GET | `/members/me/membership-orders/pending` | MEMBER | SecurityConfiguration.java:165 |
| POST | `/reception/members/{memberId}/membership-orders` | RECEPTIONIST | SecurityConfiguration.java:168 |
| GET | `/reception/members/{memberId}/membership-orders/pending` | RECEPTIONIST | SecurityConfiguration.java:169 |
| GET | `/course-payments/reconciliation-queue` | MANAGER, RECEPTIONIST | SecurityConfiguration.java:273 |
| GET | `/course-payments/{paymentId}` | MANAGER, RECEPTIONIST | SecurityConfiguration.java:273 |
| GET | `/course-payments/{paymentId}/result` | MEMBER, RECEPTIONIST, MANAGER | SecurityConfiguration.java:271 |
| POST | `/course-payments/{paymentId}/reconcile` | MANAGER, RECEPTIONIST | SecurityConfiguration.java:273 |
| POST | `/reception/members/{memberId}/cash-payments` | RECEPTIONIST | SecurityConfiguration.java:173 |
| POST | `/reception/membership-orders/{orderId}/cash` | RECEPTIONIST | SecurityConfiguration.java:245 |
| POST | `/payments/{paymentId}/reconcile` | MANAGER, RECEPTIONIST | SecurityConfiguration.java:274 |
| PATCH | `/membership-orders/{orderId}/cancel` | RECEPTIONIST, MANAGER | SecurityConfiguration.java:272 |
| GET | `/payments/reconciliation-queue` | MANAGER, RECEPTIONIST | SecurityConfiguration.java:274 |
| GET | `/members/me/receipts` | MEMBER | SecurityConfiguration.java:258 |
| GET | `/reception/members/{memberId}/receipts` | RECEPTIONIST, MANAGER | SecurityConfiguration.java:175 |
| GET | `/receipts/{receiptId}` | MEMBER, RECEPTIONIST, MANAGER | SecurityConfiguration.java:271 |
| GET | `/payments/{paymentId}/result` | MEMBER, RECEPTIONIST, MANAGER | SecurityConfiguration.java:271 |
| GET | `/members/me/memberships` | MEMBER | SecurityConfiguration.java:258 |
| GET | `/reception/members/{memberCode}/memberships` | RECEPTIONIST | SecurityConfiguration.java:179 |
| GET | `/manager/members/{memberAccountId}/memberships` | MANAGER | SecurityConfiguration.java:279 |
| GET | `/manager/reports/membership-revenue` | MANAGER | SecurityConfiguration.java:275 |
| GET | `/payments/{paymentId}/evidence-images` | MANAGER, RECEPTIONIST | SecurityConfiguration.java:274 |
| POST | `/payments/{paymentId}/evidence-images` | MANAGER, RECEPTIONIST | SecurityConfiguration.java:274 |
| GET | `/payments/{paymentId}/evidence-images/{evidenceId}` | MANAGER, RECEPTIONIST | SecurityConfiguration.java:274 |
| POST | `/members/me/membership-orders/{orderId}/payments/sepay` | MEMBER | SecurityConfiguration.java:246 |
| POST | `/members/me/membership-orders/{orderId}/bank-transfer` | MEMBER | SecurityConfiguration.java:246 |
| POST | `/reception/members/{memberId}/membership-orders/{orderId}/payments/sepay` | RECEPTIONIST | SecurityConfiguration.java:179 |
| POST | `/members/me/course-orders/{orderId}/payments/sepay` | MEMBER | SecurityConfiguration.java:248 |
| POST | `/reception/members/{memberId}/course-orders/{orderId}/payments/sepay` | RECEPTIONIST | SecurityConfiguration.java:179 |
| POST | `/reception/members/{memberId}/course-orders/{orderId}/payments/cash` | RECEPTIONIST | SecurityConfiguration.java:179 |
| POST | `/payments/sepay/webhook` | PUBLIC | SecurityConfiguration.java:268 |
| GET | `/manager/membership-benefits` | MANAGER | SecurityConfiguration.java:244 |
| GET | `/manager/membership-plans` | MANAGER | SecurityConfiguration.java:244 |
| GET | `/manager/membership-plans/{planCode}` | MANAGER | SecurityConfiguration.java:244 |
| POST | `/manager/membership-plans` | MANAGER | SecurityConfiguration.java:244 |
| PATCH | `/manager/membership-plans/{planCode}` | MANAGER | SecurityConfiguration.java:244 |
| GET | `/reception/refund-requests` | RECEPTIONIST | SecurityConfiguration.java:148 |
| GET | `/reception/members/{memberCode}/refund-eligible-sources` | RECEPTIONIST | SecurityConfiguration.java:179 |
| POST | `/reception/members/{memberCode}/refund-requests` | RECEPTIONIST | SecurityConfiguration.java:149 |
| POST | `/reception/refund-requests/{requestId}/cash-disbursement` | RECEPTIONIST | SecurityConfiguration.java:149 |
| GET | `/manager/refund-requests` | MANAGER | SecurityConfiguration.java:150 |
| POST | `/manager/refund-requests/{requestId}/first-approval` | MANAGER | SecurityConfiguration.java:151 |
| POST | `/manager/refund-requests/{requestId}/final-approval` | MANAGER | SecurityConfiguration.java:151 |
| POST | `/manager/refund-requests/{requestId}/reject` | MANAGER | SecurityConfiguration.java:151 |
| GET | `/manager/dashboard` | MANAGER | SecurityConfiguration.java:276 |
| GET | `/manager/reports/members` | MANAGER | SecurityConfiguration.java:275 |
| GET | `/manager/reports/bookings` | MANAGER | SecurityConfiguration.java:275 |
| GET | `/manager/reports/attendance` | MANAGER | SecurityConfiguration.java:275 |
| GET | `/manager/reports/center-visits` | MANAGER | SecurityConfiguration.java:275 |
| GET | `/manager/reports/class-utilization` | MANAGER | SecurityConfiguration.java:275 |
| GET | `/manager/reports/course-runs` | MANAGER | SecurityConfiguration.java:275 |
| GET | `/members/me/membership-termination-requests` | MEMBER | SecurityConfiguration.java:140 |
| POST | `/members/me/membership-termination-requests` | MEMBER | SecurityConfiguration.java:141 |
| DELETE | `/members/me/membership-termination-requests/{requestId}` | MEMBER | SecurityConfiguration.java:142 |
| GET | `/reception/members/{memberCode}/membership-termination-requests` | RECEPTIONIST | SecurityConfiguration.java:143 |
| POST | `/reception/members/{memberCode}/membership-termination-requests` | RECEPTIONIST | SecurityConfiguration.java:144 |
| GET | `/manager/membership-termination-requests` | MANAGER | SecurityConfiguration.java:145 |
| POST | `/manager/membership-termination-requests/{requestId}/approve` | MANAGER | SecurityConfiguration.java:146 |
| POST | `/manager/membership-termination-requests/{requestId}/reject` | MANAGER | SecurityConfiguration.java:146 |
| POST | `/manager/members/{memberAccountId}/membership-termination-requests` | MANAGER | SecurityConfiguration.java:147 |
| POST | `/members/me/profile/image` | MEMBER | SecurityConfiguration.java:263 |
| POST | `/reception/members/{memberId}/profile/image` | RECEPTIONIST | SecurityConfiguration.java:179 |
| POST | `/staff/me/profile/image` | MANAGER, RECEPTIONIST, COACH | SecurityConfiguration.java:281 |
| DELETE | `/staff/me/profile/image` | MANAGER, RECEPTIONIST, COACH | SecurityConfiguration.java:280 |
| GET | `/notifications` | Authenticated | SecurityConfiguration.java:269 |
| PATCH | `/notifications/{notificationId}/read` | Authenticated | SecurityConfiguration.java:269 |
| PATCH | `/notifications/read-all` | Authenticated | SecurityConfiguration.java:269 |
| GET | `/coach/class-sessions/{sessionId}/attendance` | COACH | SecurityConfiguration.java:213 |
| PATCH | `/coach/class-sessions/{sessionId}/attendance/{attendanceId}` | COACH | SecurityConfiguration.java:213 |
| POST | `/members/class-sessions/{sessionId}/bookings` | MEMBER | SecurityConfiguration.java:231 |
| PATCH | `/members/me/bookings/{bookingId}/cancel` | MEMBER | SecurityConfiguration.java:235 |
| GET | `/members/me/bookings` | MEMBER | SecurityConfiguration.java:236 |
| POST | `/reception/members/{memberId}/bookings` | RECEPTIONIST | SecurityConfiguration.java:179 |
| PATCH | `/reception/members/{memberId}/bookings/{bookingId}/cancel` | RECEPTIONIST | SecurityConfiguration.java:179 |
| GET | `/reception/members/{memberId}/bookings` | RECEPTIONIST | SecurityConfiguration.java:179 |
| POST | `/manager/coach-leave-requests/{requestId}/batch/available-coaches` | MANAGER | SecurityConfiguration.java:199 |
| POST | `/manager/coach-leave-requests/{requestId}/batch` | MANAGER | SecurityConfiguration.java:199 |
| POST | `/coach/leave-requests` | COACH | SecurityConfiguration.java:200 |
| GET | `/coach/me/leave-requests` | COACH | SecurityConfiguration.java:200 |
| DELETE | `/coach/leave-requests/{requestId}` | COACH | SecurityConfiguration.java:200 |
| PATCH | `/coach/leave-requests/{requestId}/cancel` | COACH | SecurityConfiguration.java:200 |
| GET | `/manager/coach-leave-requests` | MANAGER | SecurityConfiguration.java:199 |
| GET | `/manager/coach-leave-requests` | MANAGER | SecurityConfiguration.java:199 |
| PATCH | `/manager/coach-leave-requests/{requestId}/approve` | MANAGER | SecurityConfiguration.java:199 |
| PATCH | `/manager/coach-leave-requests/{requestId}/reject` | MANAGER | SecurityConfiguration.java:199 |
| GET | `/manager/coach-leave-requests/{requestId}/impact` | MANAGER | SecurityConfiguration.java:199 |
| GET | `/manager/coach-leave-requests/{requestId}/affected-sessions/{type}/{sessionId}/available-coaches` | MANAGER | SecurityConfiguration.java:199 |
| GET | `/manager/disciplines` | MANAGER | SecurityConfiguration.java:188 |
| POST | `/manager/disciplines` | MANAGER | SecurityConfiguration.java:188 |
| GET | `/manager/disciplines/{disciplineId}` | MANAGER | SecurityConfiguration.java:188 |
| PATCH | `/manager/disciplines/{disciplineId}` | MANAGER | SecurityConfiguration.java:188 |
| GET | `/manager/rooms` | MANAGER | SecurityConfiguration.java:190 |
| GET | `/manager/rooms/availability` | MANAGER | SecurityConfiguration.java:190 |
| POST | `/manager/rooms` | MANAGER | SecurityConfiguration.java:190 |
| GET | `/manager/rooms/{roomId}` | MANAGER | SecurityConfiguration.java:190 |
| PATCH | `/manager/rooms/{roomId}` | MANAGER | SecurityConfiguration.java:190 |
| GET | `/manager/rooms/{roomId}/suspensions` | MANAGER | SecurityConfiguration.java:190 |
| POST | `/manager/rooms/{roomId}/suspensions` | MANAGER | SecurityConfiguration.java:190 |
| PATCH | `/manager/rooms/{roomId}/suspensions/{suspensionId}` | MANAGER | SecurityConfiguration.java:190 |
| POST | `/manager/rooms/{roomId}/impact-preview` | MANAGER | SecurityConfiguration.java:190 |
| POST | `/manager/rooms/{roomId}/impact-apply` | MANAGER | SecurityConfiguration.java:190 |
| POST | `/manager/class-sessions` | MANAGER | SecurityConfiguration.java:198 |
| GET | `/manager/class-sessions` | MANAGER | SecurityConfiguration.java:198 |
| GET | `/manager/class-sessions/{id}` | MANAGER | SecurityConfiguration.java:198 |
| GET | `/manager/class-sessions/{id}/available-coaches` | MANAGER | SecurityConfiguration.java:198 |
| PATCH | `/manager/class-sessions/{id}/assignment` | MANAGER | SecurityConfiguration.java:198 |
| PATCH | `/manager/class-sessions/{id}/schedule` | MANAGER | SecurityConfiguration.java:198 |
| PATCH | `/manager/class-sessions/{id}/cancel` | MANAGER | SecurityConfiguration.java:198 |
| POST | `/manager/class-sessions/cancellation-preview` | MANAGER | SecurityConfiguration.java:198 |
| POST | `/manager/class-sessions/bulk-cancel` | MANAGER | SecurityConfiguration.java:198 |
| GET | `/manager/coaches/available` | Authenticated (fallback) | SecurityConfiguration#securityFilterChain |
| GET | `/coach/class-sessions` | COACH | SecurityConfiguration.java:208 |
| GET | `/coach/class-sessions/{id}` | COACH | SecurityConfiguration.java:208 |
| GET | `/members/class-sessions` | MEMBER | SecurityConfiguration.java:201 |
| GET | `/members/pt-sessions` | MEMBER | SecurityConfiguration.java:202 |
| GET | `/reception/pt-sessions` | RECEPTIONIST | SecurityConfiguration.java:203 |
| GET | `/manager/pt-sessions` | MANAGER | SecurityConfiguration.java:204 |
| GET | `/coach/pt-sessions` | COACH | SecurityConfiguration.java:205 |
| GET | `/reception/class-sessions` | RECEPTIONIST | SecurityConfiguration.java:176 |
| POST | `/manager/recurring-schedules` | MANAGER | SecurityConfiguration.java:197 |
| GET | `/manager/classes` | MANAGER | SecurityConfiguration.java:189 |
| GET | `/manager/classes/types` | MANAGER | SecurityConfiguration.java:189 |
| POST | `/manager/classes` | MANAGER | SecurityConfiguration.java:189 |
| GET | `/manager/classes/{classId}` | MANAGER | SecurityConfiguration.java:189 |
| PATCH | `/manager/classes/{classId}` | MANAGER | SecurityConfiguration.java:189 |
| POST | `/members/class-sessions/{sessionId}/waitlist` | MEMBER | SecurityConfiguration.java:232 |
| DELETE | `/members/class-sessions/{sessionId}/waitlist` | MEMBER | SecurityConfiguration.java:232 |
| GET | `/members/me/waitlists` | MEMBER | SecurityConfiguration.java:233 |
| POST | `/reception/members/{memberId}/class-sessions/{sessionId}/waitlist` | RECEPTIONIST | SecurityConfiguration.java:179 |
| DELETE | `/reception/members/{memberId}/class-sessions/{sessionId}/waitlist` | RECEPTIONIST | SecurityConfiguration.java:179 |
| GET | `/reception/class-sessions/{sessionId}/waitlist` | RECEPTIONIST | SecurityConfiguration.java:234 |
| GET | `/manager/class-sessions/{sessionId}/waitlist` | MANAGER | SecurityConfiguration.java:198 |
| GET | `/manager/coach-assignments/candidates` | MANAGER | SecurityConfiguration.java:278 |
| GET | `/manager/coaches/assignment-workloads` | MANAGER | SecurityConfiguration.java:278 |
| GET | `/coach/members` | COACH | SecurityConfiguration.java:221 |
| GET | `/coach/members/{memberId}` | COACH | SecurityConfiguration.java:221 |
| POST | `/manager/members/{memberId}/coach-assignment` | MANAGER | SecurityConfiguration.java:279 |
| GET | `/manager/members/{memberId}/coach-assignment` | MANAGER | SecurityConfiguration.java:279 |
| GET | `/manager/members/{memberId}/coach-assignment/impact` | MANAGER | SecurityConfiguration.java:279 |
| GET | `/manager/members/{memberId}/coach-assignments` | MANAGER | SecurityConfiguration.java:279 |
| PATCH | `/manager/members/{memberId}/coach-assignment/end` | MANAGER | SecurityConfiguration.java:279 |
| GET | `/members/me/coach-assignment` | MEMBER | SecurityConfiguration.java:262 |
| GET | `/members/me/feedback` | MEMBER | SecurityConfiguration.java:260 |
| GET | `/coach/members/{memberId}/feedback` | COACH | SecurityConfiguration.java:229 |
| POST | `/coach/members/{memberId}/feedback` | COACH | SecurityConfiguration.java:228 |
| PUT | `/coach/members/{memberId}/feedback/{feedbackId}` | COACH | SecurityConfiguration.java:230 |
| GET | `/members/me/training-plans` | MEMBER | SecurityConfiguration.java:261 |
| GET | `/coach/members/{memberId}/training-plans` | COACH | SecurityConfiguration.java:223 |
| POST | `/coach/members/{memberId}/training-plans` | COACH | SecurityConfiguration.java:222 |
| PUT | `/coach/members/{memberId}/training-plans/{planId}` | COACH | SecurityConfiguration.java:224 |
| GET | `/coach/members/{memberId}/training-plans/{planId}/versions` | Authenticated (fallback) | SecurityConfiguration#securityFilterChain |
| POST | `/coach/members/{memberId}/training-plans/{planId}/archive` | Authenticated (fallback) | SecurityConfiguration#securityFilterChain |
| GET | `/members/me/training-results` | MEMBER | SecurityConfiguration.java:259 |
| GET | `/coach/members/{memberId}/training-results` | COACH | SecurityConfiguration.java:226 |
| POST | `/coach/members/{memberId}/training-results` | COACH | SecurityConfiguration.java:225 |
| PUT | `/coach/members/{memberId}/training-results/{resultId}` | COACH | SecurityConfiguration.java:227 |
| GET | `/coach/pt-sessions/{sessionId}/members/{memberId}/training-summary` | COACH | SecurityConfiguration.java:206 |
| PUT | `/coach/pt-sessions/{sessionId}/members/{memberId}/training-summary` | COACH | SecurityConfiguration.java:207 |
| GET | `/coach/course-sessions/{sessionId}/members/{memberId}/training-summary` | COACH | SecurityConfiguration.java:206 |
| PUT | `/coach/course-sessions/{sessionId}/members/{memberId}/training-summary` | COACH | SecurityConfiguration.java:207 |
| GET | `/coach/class-sessions/{sessionId}/members/{memberId}/feedback` | COACH | SecurityConfiguration.java:219 |
| POST | `/coach/class-sessions/{sessionId}/members/{memberId}/feedback` | COACH | SecurityConfiguration.java:218 |
| PUT | `/coach/class-sessions/{sessionId}/members/{memberId}/feedback/{feedbackId}` | COACH | SecurityConfiguration.java:220 |
| GET | `/coach/class-sessions/{sessionId}/members/{memberId}/training-results` | COACH | SecurityConfiguration.java:216 |
| POST | `/coach/class-sessions/{sessionId}/members/{memberId}/training-results` | COACH | SecurityConfiguration.java:215 |
| PUT | `/coach/class-sessions/{sessionId}/members/{memberId}/training-results/{resultId}` | COACH | SecurityConfiguration.java:217 |
| PUT | `/coach/class-sessions/{sessionId}/members/{memberId}/training-summary` | COACH | SecurityConfiguration.java:214 |
