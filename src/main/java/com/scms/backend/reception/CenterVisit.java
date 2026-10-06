package com.scms.backend.reception;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

	@Column(name = "membership_id", nullable = false, updatable = false)
	private UUID membershipId;

	@Column(name = "checked_in_by_account_id", nullable = false, updatable = false)
	private UUID checkedInByAccountId;

	@Column(name = "checked_in_at", nullable = false, updatable = false)
	private Instant checkedInAt;

	protected CenterVisit() {
	}

	CenterVisit(UUID id, UUID memberAccountId, UUID membershipId, UUID checkedInByAccountId, Instant checkedInAt) {
		this.id = id;
		this.memberAccountId = memberAccountId;
		this.membershipId = membershipId;
		this.checkedInByAccountId = checkedInByAccountId;
		this.checkedInAt = checkedInAt;
	}

	UUID getId() { return id; }
	UUID getMemberAccountId() { return memberAccountId; }
	UUID getMembershipId() { return membershipId; }
	UUID getCheckedInByAccountId() { return checkedInByAccountId; }
	Instant getCheckedInAt() { return checkedInAt; }
}
