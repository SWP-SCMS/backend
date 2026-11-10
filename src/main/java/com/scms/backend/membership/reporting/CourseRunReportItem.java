package com.scms.backend.membership.reporting;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

record CourseRunReportItem(UUID runId, String runCode, UUID courseId, String courseCode, String courseName, String status, Instant plannedStartAt, Instant plannedEndAt, BigInteger tuitionVnd, long activeEnrollments, long transferredEnrollments, long completedEnrollments, long withdrawalPendingEnrollments, long withdrawnEnrollments, long attendancePresent, long attendanceAbsent, long attendanceNotRecorded, BigInteger grossAmountVnd, BigInteger paidAmountVnd, long pendingPayments, BigInteger pendingPaymentAmountVnd, long failedPayments, BigInteger failedPaymentAmountVnd, long outstandingRefundObligations, BigInteger outstandingRefundAmountVnd, long completedRefunds, BigInteger completedRefundAmountVnd, long waitlistWaiting, long waitlistInvited, long waitlistAccepted, long transferPending, long transferOffered, long transferRejected, long transferCompleted, long transferExpired) {
}
