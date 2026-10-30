package com.scms.backend.centervisit;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "center_visits")
class CenterVisit {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "member_account_id", nullable = false, updatable = false)
	private UUID memberAccountId;

	@Column(name = "membership_id", updatable = false)
	private UUID membershipId;

	@Enumerated(EnumType.STRING)
	@Column(name = "visit_source", nullable = false, updatable = false, length = 20)
	private CenterVisitSource source = CenterVisitSource.MEMBERSHIP;

	@Column(name = "course_enrollment_id", updatable = false)
	private UUID courseEnrollmentId;

	@Column(name = "course_session_id", updatable = false)
	private UUID courseSessionId;

	@Column(name = "checked_in_by_account_id", nullable = false, updatable = false)
	private UUID checkedInByAccountId;

	@Column(name = "checked_in_at", nullable = false, updatable = false)
	private Instant checkedInAt;

	@Column(name = "checked_out_at")
	private Instant checkedOutAt;

	@Column(name = "checked_out_by_account_id")
	private UUID checkedOutByAccountId;

	@Enumerated(EnumType.STRING)
	@Column(name = "checkout_source", length = 30)
	private CheckoutSource checkoutSource;

	@Column(name = "idempotency_key", length = 100, updatable = false)
	private String idempotencyKey;

	protected CenterVisit() {
	}

	CenterVisit(UUID id, UUID memberAccountId, UUID membershipId, UUID checkedInByAccountId, Instant checkedInAt) {
		this(id, memberAccountId, membershipId, checkedInByAccountId, checkedInAt, null);
	}

	CenterVisit(UUID id, UUID memberAccountId, UUID membershipId, UUID checkedInByAccountId, Instant checkedInAt,
			String idempotencyKey) {
		this.id = id;
		this.memberAccountId = memberAccountId;
		this.membershipId = membershipId;
		this.checkedInByAccountId = checkedInByAccountId;
		this.checkedInAt = checkedInAt;
		this.idempotencyKey = idempotencyKey;
	}

	static CenterVisit forCourse(UUID id, UUID memberAccountId, UUID courseEnrollmentId, UUID courseSessionId,
			UUID checkedInByAccountId, Instant checkedInAt, String idempotencyKey) {
		CenterVisit visit = new CenterVisit(id, memberAccountId, null, checkedInByAccountId, checkedInAt,
			idempotencyKey);
		visit.source = CenterVisitSource.COURSE;
		visit.courseEnrollmentId = courseEnrollmentId;
		visit.courseSessionId = courseSessionId;
		return visit;
	}

	void checkout(UUID actorId, CheckoutSource source, Instant at) {
		if (checkedOutAt != null) return;
		checkedOutAt = at;
		checkedOutByAccountId = actorId;
		checkoutSource = source;
	}

	UUID getId() { return id; }
	UUID getMemberAccountId() { return memberAccountId; }
	UUID getMembershipId() { return membershipId; }
	CenterVisitSource getSource() { return source; }
	UUID getCourseEnrollmentId() { return courseEnrollmentId; }
	UUID getCourseSessionId() { return courseSessionId; }
	UUID getCheckedInByAccountId() { return checkedInByAccountId; }
	Instant getCheckedInAt() { return checkedInAt; }
	Instant getCheckedOutAt() { return checkedOutAt; }
	UUID getCheckedOutByAccountId() { return checkedOutByAccountId; }
	CheckoutSource getCheckoutSource() { return checkoutSource; }
	String getIdempotencyKey() { return idempotencyKey; }
}
