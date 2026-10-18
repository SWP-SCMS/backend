package com.scms.backend.membership;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MembershipBenefitStatus status;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	protected MembershipBenefit() { }

	public String getCode() { return code; }
	public String getDisplayName() { return displayName; }
	public String getDescription() { return description; }
	public MembershipBenefitStatus getStatus() { return status; }
}
