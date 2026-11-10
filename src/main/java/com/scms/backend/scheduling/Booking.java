package com.scms.backend.scheduling;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "class_session_id", nullable = false, updatable = false)
	private UUID classSessionId;

	@Column(name = "member_account_id", nullable = false, updatable = false)
	private UUID memberAccountId;

	@Column(name = "membership_id", nullable = false, updatable = false)
	private UUID membershipId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BookingStatus status;

	@Column(name = "booked_by_account_id", nullable = false, updatable = false)
	private UUID bookedByAccountId;

	@Column(name = "cancelled_by_account_id")
	private UUID cancelledByAccountId;

	@Column(name = "cancellation_source", length = 30)
	private String cancellationSource;

	@Column(name = "cancelled_at")
	private Instant cancelledAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	protected Booking() {
	}

	public Booking(UUID id, UUID classSessionId, UUID memberAccountId, UUID membershipId, UUID bookedByAccountId, Instant createdAt) {
		this.id = id;
		this.classSessionId = classSessionId;
		this.memberAccountId = memberAccountId;
		this.membershipId = membershipId;
		this.status = BookingStatus.BOOKED;
		this.bookedByAccountId = bookedByAccountId;
		this.createdAt = createdAt;
		this.updatedAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getClassSessionId() {
		return classSessionId;
	}

	public UUID getMemberAccountId() {
		return memberAccountId;
	}

	public UUID getMembershipId() {
		return membershipId;
	}

	public BookingStatus getStatus() {
		return status;
	}

	public UUID getBookedByAccountId() {
		return bookedByAccountId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public UUID getCancelledByAccountId() {
		return cancelledByAccountId;
	}

	public String getCancellationSource() {
		return cancellationSource;
	}

	public Instant getCancelledAt() {
		return cancelledAt;
	}

	public void cancel(UUID actorId, String source, Instant now) {
		this.status = BookingStatus.CANCELLED;
		this.cancelledByAccountId = actorId;
		this.cancellationSource = source;
		this.cancelledAt = now;
		this.updatedAt = now;
	}
}
