package com.scms.backend.membership;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "membership_benefits")
public class MembershipBenefit {
	@Id
	@Column(nullable = false, length = 50, updatable = false)
	private String code;

	@Column(name = "display_name", nullable = false, length = 150)
	private String displayName;

	@Column(nullable = false, columnDefinition = "text")
	private String description;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "group_code")
	private MembershipBenefitGroup group;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MembershipBenefitStatus status;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	protected MembershipBenefit() { }

	MembershipBenefit(String code, String displayName, String description, MembershipBenefitStatus status) {
		this.code = code;
		this.displayName = displayName;
		this.description = description;
		this.status = status;
	}

	public String getCode() { return code; }
	public String getDisplayName() { return displayName; }
	public String getDescription() { return description; }
	public int getDisplayOrder() { return displayOrder; }
	public MembershipBenefitGroup getGroup() { return group; }
	public MembershipBenefitStatus getStatus() { return status; }
}
