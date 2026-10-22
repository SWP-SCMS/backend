package com.scms.backend.membership;

import java.time.Instant;
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
@Table(name = "membership_termination_requests")
public class MembershipTerminationRequest {
	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;
	@Column(name = "membership_id", nullable = false, updatable = false)
	private UUID membershipId;
	@Column(name = "member_account_id", nullable = false, updatable = false)
	private UUID memberAccountId;
	@Column(name = "requester_account_id", nullable = false, updatable = false)
	private UUID requesterAccountId;
	@Column(name = "approver_account_id")
	private UUID approverAccountId;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20, updatable = false)
	private MembershipTerminationMode mode;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MembershipTerminationRequestStatus status;
	@Column(nullable = false, columnDefinition = "text", updatable = false)
	private String reason;
	@Column(name = "effective_at")
	private Instant effectiveAt;
	@Column(name = "approved_at")
	private Instant approvedAt;
	@Column(name = "rejected_at")
	private Instant rejectedAt;
	@Column(name = "withdrawn_at")
	private Instant withdrawnAt;
	@Generated(event = EventType.INSERT)
	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;
	@Generated(event = EventType.INSERT)
	@Column(name = "updated_at", nullable = false, insertable = false)
	private Instant updatedAt;
	@Version
	@Column(nullable = false)
	private Long version;

	protected MembershipTerminationRequest() { }

	MembershipTerminationRequest(UUID id, UUID membershipId, UUID memberAccountId, UUID requesterAccountId,
			MembershipTerminationMode mode, String reason) {
		this.id = Objects.requireNonNull(id);
		this.membershipId = Objects.requireNonNull(membershipId);
		this.memberAccountId = Objects.requireNonNull(memberAccountId);
		this.requesterAccountId = Objects.requireNonNull(requesterAccountId);
		this.mode = Objects.requireNonNull(mode);
		this.status = MembershipTerminationRequestStatus.PENDING;
		this.reason = Objects.requireNonNull(reason);
	}

	void approve(UUID approver, Instant effectiveAt, Instant now) {
		ensurePending();
		this.approverAccountId = Objects.requireNonNull(approver);
		this.effectiveAt = Objects.requireNonNull(effectiveAt);
		this.approvedAt = Objects.requireNonNull(now);
		this.status = MembershipTerminationRequestStatus.APPROVED;
	}

	void reject(UUID approver, Instant now) {
		ensurePending();
		this.approverAccountId = Objects.requireNonNull(approver);
		this.rejectedAt = Objects.requireNonNull(now);
		this.status = MembershipTerminationRequestStatus.REJECTED;
	}

	void withdraw(Instant now) {
		ensurePending();
		this.withdrawnAt = Objects.requireNonNull(now);
		this.status = MembershipTerminationRequestStatus.WITHDRAWN;
	}

	void execute() {
		if (status != MembershipTerminationRequestStatus.APPROVED) {
			throw new IllegalStateException("Only APPROVED termination requests can execute");
		}
		this.status = MembershipTerminationRequestStatus.EXECUTED;
	}

	void obsolete() {
		if (status != MembershipTerminationRequestStatus.APPROVED) {
			throw new IllegalStateException("Only APPROVED termination requests can become obsolete");
		}
		this.status = MembershipTerminationRequestStatus.OBSOLETE;
	}

	private void ensurePending() {
		if (status != MembershipTerminationRequestStatus.PENDING) {
			throw new IllegalStateException("Termination request is not pending");
		}
	}

	public UUID getId() { return id; }
	public UUID getMembershipId() { return membershipId; }
	public UUID getMemberAccountId() { return memberAccountId; }
	public UUID getRequesterAccountId() { return requesterAccountId; }
	public UUID getApproverAccountId() { return approverAccountId; }
	public MembershipTerminationMode getMode() { return mode; }
	public MembershipTerminationRequestStatus getStatus() { return status; }
	public String getReason() { return reason; }
	public Instant getEffectiveAt() { return effectiveAt; }
	public Instant getApprovedAt() { return approvedAt; }
	public Instant getRejectedAt() { return rejectedAt; }
	public Instant getWithdrawnAt() { return withdrawnAt; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public Long getVersion() { return version; }
}
