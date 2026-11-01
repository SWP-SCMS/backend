package com.scms.backend.membership;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record CourseRunReportPageResponse(List<CourseRunReportItem> content, int page, int size,
	long totalElements, int totalPages) { }

record CourseRunReportItem(UUID runId, String runCode, UUID courseId, String courseCode, String courseName,
	String status, Instant plannedStartAt, Instant plannedEndAt, BigInteger tuitionVnd,
	long activeEnrollments, long transferredEnrollments, long completedEnrollments,
	long withdrawalPendingEnrollments, long withdrawnEnrollments,
	long attendancePresent, long attendanceAbsent, long attendanceNotRecorded,
	BigInteger grossAmountVnd, BigInteger paidAmountVnd,
	long pendingPayments, BigInteger pendingPaymentAmountVnd,
	long failedPayments, BigInteger failedPaymentAmountVnd,
	long outstandingRefundObligations, BigInteger outstandingRefundAmountVnd,
	long completedRefunds, BigInteger completedRefundAmountVnd,
	long waitlistWaiting, long waitlistInvited, long waitlistAccepted,
	long transferPending, long transferOffered, long transferRejected,
	long transferCompleted, long transferExpired) { }
