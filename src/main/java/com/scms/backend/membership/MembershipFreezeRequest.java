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
@Table(name = "membership_freeze_requests")
public class MembershipFreezeRequest {
	@Id @Column(nullable = false, updatable = false) private UUID id;
	@Column(name = "membership_id", nullable = false, updatable = false) private UUID membershipId;
	@Column(name = "member_account_id", nullable = false, updatable = false) private UUID memberAccountId;
	@Column(name = "requester_account_id", nullable = false, updatable = false) private UUID requesterAccountId;
	@Column(name = "approver_account_id") private UUID approverAccountId;
	@Column(name = "start_date", nullable = false, updatable = false) private LocalDate startDate;
	@Column(name = "end_date", nullable = false, updatable = false) private LocalDate endDate;
	@Column(nullable = false, columnDefinition = "text", updatable = false) private String reason;
	@Column(name = "fee_amount") private BigInteger feeAmount;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private MembershipFreezeRequestStatus status;
	@Column(name = "approved_at") private Instant approvedAt;
	@Column(name = "rejected_at") private Instant rejectedAt;
	@Generated(event = EventType.INSERT) @Column(name = "created_at", nullable = false, insertable = false, updatable = false) private Instant createdAt;
	@Generated(event = EventType.INSERT) @Column(name = "updated_at", nullable = false, insertable = false) private Instant updatedAt;
	@Version @Column(nullable = false) private Long version;

	protected MembershipFreezeRequest() { }
	MembershipFreezeRequest(UUID id, UUID membershipId, UUID memberAccountId, UUID requesterAccountId,
			LocalDate startDate, LocalDate endDate, String reason) {
		this.id = Objects.requireNonNull(id); this.membershipId = Objects.requireNonNull(membershipId);
		this.memberAccountId = Objects.requireNonNull(memberAccountId); this.requesterAccountId = Objects.requireNonNull(requesterAccountId);
		this.startDate = Objects.requireNonNull(startDate); this.endDate = Objects.requireNonNull(endDate);
		this.reason = Objects.requireNonNull(reason); this.status = MembershipFreezeRequestStatus.PENDING;
	}
	void approve(UUID approver, BigInteger fee, Instant now) {
		if (status != MembershipFreezeRequestStatus.PENDING) throw new IllegalStateException("Freeze request is not pending");
		approverAccountId = Objects.requireNonNull(approver); feeAmount = Objects.requireNonNull(fee); approvedAt = Objects.requireNonNull(now);
		status = MembershipFreezeRequestStatus.APPROVED_PENDING_PAYMENT;
	}
	void reject(UUID approver, Instant now) {
		if (status != MembershipFreezeRequestStatus.PENDING) throw new IllegalStateException("Freeze request is not pending");
		approverAccountId = Objects.requireNonNull(approver); rejectedAt = Objects.requireNonNull(now); status = MembershipFreezeRequestStatus.REJECTED;
	}
	void schedule() {
		if (status != MembershipFreezeRequestStatus.APPROVED_PENDING_PAYMENT) throw new IllegalStateException("Freeze request is not approved for payment");
		status = MembershipFreezeRequestStatus.SCHEDULED;
	}
	void activate() {
		if (status != MembershipFreezeRequestStatus.SCHEDULED) throw new IllegalStateException("Freeze request is not scheduled");
		status = MembershipFreezeRequestStatus.ACTIVE;
	}
	void complete() {
		if (status != MembershipFreezeRequestStatus.ACTIVE) throw new IllegalStateException("Freeze request is not active");
		status = MembershipFreezeRequestStatus.COMPLETED;
	}
	void obsolete() {
		if (status != MembershipFreezeRequestStatus.SCHEDULED && status != MembershipFreezeRequestStatus.APPROVED_PENDING_PAYMENT) throw new IllegalStateException("Freeze request cannot become obsolete");
		status = MembershipFreezeRequestStatus.OBSOLETE;
	}
	public UUID getId() { return id; } public UUID getMembershipId() { return membershipId; }
	public UUID getMemberAccountId() { return memberAccountId; } public UUID getRequesterAccountId() { return requesterAccountId; }
	public UUID getApproverAccountId() { return approverAccountId; } public LocalDate getStartDate() { return startDate; }
	public LocalDate getEndDate() { return endDate; } public String getReason() { return reason; }
	public BigInteger getFeeAmount() { return feeAmount; } public MembershipFreezeRequestStatus getStatus() { return status; }
	public Instant getApprovedAt() { return approvedAt; } public Instant getRejectedAt() { return rejectedAt; }
	public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; } public Long getVersion() { return version; }
}
