package com.scms.backend.membership;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "refund_requests")
class RefundRequest {
	@Id @Column(nullable = false, updatable = false) private UUID id;
	@Column(name = "payment_id", nullable = false, updatable = false) private UUID paymentId;
	@Column(name = "order_id", nullable = false, updatable = false) private UUID orderId;
	@Column(name = "membership_id", nullable = false, updatable = false) private UUID membershipId;
	@Column(name = "receipt_id", nullable = false, updatable = false) private UUID receiptId;
	@Column(name = "member_account_id", nullable = false, updatable = false) private UUID memberAccountId;
	@Column(name = "requester_account_id", nullable = false, updatable = false) private UUID requesterAccountId;
	@Column(name = "first_approver_account_id") private UUID firstApproverAccountId;
	@Column(name = "final_approver_account_id") private UUID finalApproverAccountId;
	@Column(name = "disbursed_by_account_id") private UUID disbursedByAccountId;
	@Enumerated(EnumType.STRING) @Column(name = "refund_type", nullable = false, updatable = false) private RefundType type;
	@Enumerated(EnumType.STRING) @Column(nullable = false) private RefundRequestStatus status;
	@Column(name = "requested_amount", nullable = false, updatable = false) private BigInteger requestedAmount;
	@Column(name = "suggested_amount") private BigInteger suggestedAmount;
	@Column(name = "approved_amount") private BigInteger approvedAmount;
	@Column(name = "remaining_days_snapshot") private Integer remainingDaysSnapshot;
	@Column(name = "calculation_date") private LocalDate calculationDate;
	@Column(nullable = false, columnDefinition = "text", updatable = false) private String reason;
	@Column(name = "disbursement_evidence", columnDefinition = "text") private String disbursementEvidence;
	@Column(name = "rejection_reason", columnDefinition = "text") private String rejectionReason;
	@Column(name = "idempotency_key", nullable = false, updatable = false) private String idempotencyKey;
	@Generated(event = EventType.INSERT) @Column(name = "requested_at", nullable = false, insertable = false, updatable = false) private Instant requestedAt;
	@Column(name = "first_approved_at") private Instant firstApprovedAt;
	@Column(name = "cash_disbursed_at") private Instant cashDisbursedAt;
	@Column(name = "completed_at") private Instant completedAt;
	@Column(name = "rejected_at") private Instant rejectedAt;
	@Column(name = "cancelled_at") private Instant cancelledAt;
	@Generated(event = EventType.INSERT) @Column(name = "created_at", nullable = false, insertable = false, updatable = false) private Instant createdAt;
	@Generated(event = EventType.INSERT) @Column(name = "updated_at", nullable = false, insertable = false) private Instant updatedAt;
	@Version @Column(nullable = false) private Long version = 0L;

	protected RefundRequest() { }

	RefundRequest(UUID id, UUID paymentId, UUID orderId, UUID membershipId, UUID receiptId, UUID memberAccountId,
			UUID requesterAccountId, RefundType type, BigInteger requestedAmount, BigInteger suggestedAmount,
			Integer remainingDays, LocalDate calculationDate, String reason, String idempotencyKey) {
		this.id = Objects.requireNonNull(id); this.paymentId = Objects.requireNonNull(paymentId);
		this.orderId = Objects.requireNonNull(orderId); this.membershipId = Objects.requireNonNull(membershipId);
		this.receiptId = Objects.requireNonNull(receiptId); this.memberAccountId = Objects.requireNonNull(memberAccountId);
		this.requesterAccountId = Objects.requireNonNull(requesterAccountId); this.type = Objects.requireNonNull(type);
		this.status = RefundRequestStatus.REQUESTED; this.requestedAmount = Objects.requireNonNull(requestedAmount);
		if (requestedAmount.signum() <= 0) throw new IllegalArgumentException("requestedAmount must be positive");
		this.suggestedAmount = Objects.requireNonNull(suggestedAmount);
		this.remainingDaysSnapshot = remainingDays;
		this.calculationDate = Objects.requireNonNull(calculationDate);
		this.reason = Objects.requireNonNull(reason); this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
	}

	void firstApprove(UUID actor, BigInteger amount, BigInteger suggested, Integer remainingDays, LocalDate date, Instant now) {
		if (status != RefundRequestStatus.REQUESTED) throw new IllegalStateException("Refund request is not awaiting first approval");
		if (amount == null || amount.signum() <= 0 || amount.compareTo(requestedAmount) > 0
				|| suggested == null || amount.compareTo(suggested) > 0) throw new IllegalArgumentException("approved amount exceeds refund limit");
		firstApproverAccountId = Objects.requireNonNull(actor); approvedAmount = amount; suggestedAmount = suggested;
		remainingDaysSnapshot = remainingDays; calculationDate = Objects.requireNonNull(date);
		firstApprovedAt = Objects.requireNonNull(now); status = RefundRequestStatus.FIRST_APPROVED;
	}

	void cashDisburse(UUID actor, String evidence, Instant now) {
		if (status != RefundRequestStatus.FIRST_APPROVED) throw new IllegalStateException("Refund is not approved for cash disbursement");
		if (evidence == null || evidence.isBlank()) throw new IllegalArgumentException("disbursement evidence is required");
		disbursedByAccountId = Objects.requireNonNull(actor); disbursementEvidence = evidence.trim();
		cashDisbursedAt = Objects.requireNonNull(now); status = RefundRequestStatus.CASH_DISBURSED;
	}

	void complete(UUID actor, Instant now) {
		if (status != RefundRequestStatus.CASH_DISBURSED) throw new IllegalStateException("Refund cash has not been disbursed");
		finalApproverAccountId = Objects.requireNonNull(actor); completedAt = Objects.requireNonNull(now);
		status = RefundRequestStatus.COMPLETED;
	}

	void reject(String reason, Instant now) {
		if (status != RefundRequestStatus.REQUESTED && status != RefundRequestStatus.FIRST_APPROVED)
			throw new IllegalStateException("Refund request cannot be rejected in its current state");
		if (reason == null || reason.isBlank()) throw new IllegalArgumentException("rejection reason is required");
		rejectionReason = reason.trim(); rejectedAt = Objects.requireNonNull(now); status = RefundRequestStatus.REJECTED;
	}

	UUID getId() { return id; }
	UUID getPaymentId() { return paymentId; }
	UUID getOrderId() { return orderId; }
	UUID getMembershipId() { return membershipId; }
	UUID getReceiptId() { return receiptId; }
	UUID getMemberAccountId() { return memberAccountId; }
	UUID getRequesterAccountId() { return requesterAccountId; }
	UUID getFirstApproverAccountId() { return firstApproverAccountId; }
	UUID getFinalApproverAccountId() { return finalApproverAccountId; }
	UUID getDisbursedByAccountId() { return disbursedByAccountId; }
	RefundType getType() { return type; }
	RefundRequestStatus getStatus() { return status; }
	BigInteger getRequestedAmount() { return requestedAmount; }
	BigInteger getSuggestedAmount() { return suggestedAmount; }
	BigInteger getApprovedAmount() { return approvedAmount; }
	Integer getRemainingDaysSnapshot() { return remainingDaysSnapshot; }
	LocalDate getCalculationDate() { return calculationDate; }
	String getReason() { return reason; }
	String getDisbursementEvidence() { return disbursementEvidence; }
	String getRejectionReason() { return rejectionReason; }
	String getIdempotencyKey() { return idempotencyKey; }
	Instant getRequestedAt() { return requestedAt; }
	Instant getFirstApprovedAt() { return firstApprovedAt; }
	Instant getCashDisbursedAt() { return cashDisbursedAt; }
	Instant getCompletedAt() { return completedAt; }
	Instant getRejectedAt() { return rejectedAt; }
	Long getVersion() { return version; }
}
