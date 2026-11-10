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
@Table(name = "attendance")
public class Attendance {

	@Id
	UUID id;

	@Column(name = "booking_id")
	UUID bookingId;

	@Enumerated(EnumType.STRING)
	AttendanceStatus status;

	@Column(name = "recorded_at")
	Instant recordedAt;

	@Column(name = "updated_at")
	Instant updatedAt;

	@Column(name = "center_visit_id")
	UUID centerVisitId;

	@Column(name = "recorded_by_account_id")
	UUID recordedBy;

	@Column(name = "recording_source")
	String recordingSource;

	@Version
	Long version;

	protected Attendance() {
	}

	public Attendance(UUID id, UUID bookingId, AttendanceStatus status, Instant at, UUID visit, UUID by, String source) {
		this.id = id;
		this.bookingId = bookingId;
		this.status = status;
		this.recordedAt = at;
		this.updatedAt = at;
		this.centerVisitId = visit;
		this.recordedBy = by;
		this.recordingSource = source;
	}

	public UUID getId() {
		return id;
	}

	public UUID getBookingId() {
		return bookingId;
	}

	public AttendanceStatus getStatus() {
		return status;
	}

	public Instant getRecordedAt() {
		return recordedAt;
	}

	public UUID getCenterVisitId() {
		return centerVisitId;
	}

	public void mark(AttendanceStatus s, UUID v, UUID by, Instant at) {
		status = s;
		centerVisitId = v;
		recordedBy = by;
		recordingSource = "COACH";
		updatedAt = at;
	}
}
