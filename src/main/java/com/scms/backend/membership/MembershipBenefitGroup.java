package com.scms.backend.membership;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "membership_benefit_groups")
public class MembershipBenefitGroup {

	@Id
	@Column(nullable = false, length = 50, updatable = false)
	private String code;

	@Column(name = "display_name", nullable = false, length = 150)
	private String displayName;

	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false)
	private Instant updatedAt;

	protected MembershipBenefitGroup() { }

	public String getCode() { return code; }
	public String getDisplayName() { return displayName; }
	public String getDescription() { return description; }
	public int getDisplayOrder() { return displayOrder; }
}
