package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "session_waitlists")
class SessionWaitlist {
	@Id @Column(nullable = false, updatable = false) private UUID id;
	@Column(name = "class_session_id", nullable = false, updatable = false) private UUID sessionId;
	@Column(name = "member_account_id", nullable = false, updatable = false) private UUID memberId;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private WaitlistStatus status;
	@Column(name = "joined_by_account_id", nullable = false, updatable = false) private UUID joinedBy;
	@Column(nullable = false, length = 20, updatable = false) private String source;
	@Column(name = "terminal_reason_code", length = 80) private String terminalReasonCode;
	@Column(name = "terminal_at") private Instant terminalAt;
	@Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
	@Column(name = "updated_at", nullable = false) private Instant updatedAt;

	protected SessionWaitlist() { }
	SessionWaitlist(UUID id, UUID sessionId, UUID memberId, UUID joinedBy, String source, Instant now) {
		this.id = id; this.sessionId = sessionId; this.memberId = memberId; this.joinedBy = joinedBy;
		this.source = source; this.status = WaitlistStatus.WAITING; this.createdAt = now; this.updatedAt = now;
	}
	UUID getId() { return id; }
	UUID getSessionId() { return sessionId; }
	UUID getMemberId() { return memberId; }
	WaitlistStatus getStatus() { return status; }
	UUID getJoinedBy() { return joinedBy; }
	String getSource() { return source; }
	String getTerminalReasonCode() { return terminalReasonCode; }
	Instant getTerminalAt() { return terminalAt; }
	Instant getCreatedAt() { return createdAt; }
	void promote(Instant now) { finish(WaitlistStatus.PROMOTED, "PROMOTED", now); }
	void cancel(String reason, Instant now) { finish(WaitlistStatus.CANCELLED, reason, now); }
	void skip(String reason, Instant now) { finish(WaitlistStatus.SKIPPED, reason, now); }
	void expire(Instant now) { finish(WaitlistStatus.EXPIRED, "SESSION_STARTED", now); }
	private void finish(WaitlistStatus target, String reason, Instant now) {
		status = target; terminalReasonCode = reason; terminalAt = now; updatedAt = now;
	}
}
