package com.scms.backend.membership;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

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

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "membership_plan_benefits",
		joinColumns = @JoinColumn(name = "plan_code", referencedColumnName = "plan_code"),
		inverseJoinColumns = @JoinColumn(name = "benefit_code", referencedColumnName = "code"))
	private Set<MembershipBenefit> benefits = new HashSet<>();

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

	public Set<MembershipBenefit> getBenefits() {
		return Set.copyOf(benefits);
	}
}
