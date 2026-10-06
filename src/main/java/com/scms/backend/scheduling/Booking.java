package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

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

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	protected Booking() {
	}

	Booking(UUID id, UUID classSessionId, UUID memberAccountId, UUID membershipId, UUID bookedByAccountId,
			Instant createdAt) {
		this.id = id;
		this.classSessionId = classSessionId;
		this.memberAccountId = memberAccountId;
		this.membershipId = membershipId;
		this.status = BookingStatus.BOOKED;
		this.bookedByAccountId = bookedByAccountId;
		this.createdAt = createdAt;
		this.updatedAt = createdAt;
	}

	UUID getId() { return id; }
	UUID getClassSessionId() { return classSessionId; }
	UUID getMemberAccountId() { return memberAccountId; }
	UUID getMembershipId() { return membershipId; }
	BookingStatus getStatus() { return status; }
	UUID getBookedByAccountId() { return bookedByAccountId; }
	Instant getCreatedAt() { return createdAt; }
}
