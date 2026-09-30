package com.scms.backend.membership;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "membership_plans")
public class MembershipPlan {

	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "plan_code", nullable = false, length = 10, updatable = false)
	private MembershipPlanCode planCode;

	@Column(name = "display_name", nullable = false, length = 100)
	private String displayName;

	@Column(name = "supports_booking", nullable = false)
	private boolean supportsBooking;

	@Column(name = "supports_personal_coaching", nullable = false)
	private boolean supportsPersonalCoaching;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	protected MembershipPlan() {
	}

	public MembershipPlanCode getPlanCode() {
		return planCode;
	}

	public String getDisplayName() {
		return displayName;
	}

	public boolean supportsBooking() {
		return supportsBooking;
	}

	public boolean supportsPersonalCoaching() {
		return supportsPersonalCoaching;
	}
}
