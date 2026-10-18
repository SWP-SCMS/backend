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
import jakarta.persistence.Version;

@Entity
@Table(name = "membership_plans")
public class MembershipPlan {

	@Id
	@Column(name = "plan_code", nullable = false, length = 50, updatable = false)
	private String planCode;

	@Column(name = "display_name", nullable = false, length = 100)
	private String displayName;

	@Column(name = "supports_booking", nullable = false)
	private boolean supportsBooking;

	@Column(name = "supports_personal_coaching", nullable = false)
	private boolean supportsPersonalCoaching;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MembershipPlanStatus status;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "membership_plan_benefits",
		joinColumns = @JoinColumn(name = "plan_code", referencedColumnName = "plan_code"),
		inverseJoinColumns = @JoinColumn(name = "benefit_code", referencedColumnName = "code"))
	private Set<MembershipBenefit> benefits = new HashSet<>();

	protected MembershipPlan() {
	}

	MembershipPlan(String planCode, String displayName, MembershipPlanStatus status, Set<MembershipBenefit> benefits) {
		this.planCode = planCode;
		this.displayName = displayName;
		this.status = status;
		this.benefits = new HashSet<>(benefits);
		this.supportsBooking = benefits.stream().anyMatch(benefit -> "CLASS_BOOKING".equals(benefit.getCode()));
		this.supportsPersonalCoaching = benefits.stream().anyMatch(benefit -> "PERSONAL_COACHING".equals(benefit.getCode()));
	}

	public String getPlanCode() {
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

	public MembershipPlanStatus getStatus() { return status; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public Long getVersion() { return version; }

	void update(String displayName, MembershipPlanStatus status, Set<MembershipBenefit> benefits) {
		this.displayName = displayName;
		this.status = status;
		this.benefits = new HashSet<>(benefits);
		this.supportsBooking = benefits.stream().anyMatch(benefit -> "CLASS_BOOKING".equals(benefit.getCode()));
		this.supportsPersonalCoaching = benefits.stream().anyMatch(benefit -> "PERSONAL_COACHING".equals(benefit.getCode()));
	}
}
