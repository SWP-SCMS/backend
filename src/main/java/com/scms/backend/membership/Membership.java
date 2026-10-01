package com.scms.backend.membership;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "memberships")
public class Membership {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "member_account_id", nullable = false, updatable = false)
	private UUID memberAccountId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MembershipStatus status;

	protected Membership() {
	}
}
