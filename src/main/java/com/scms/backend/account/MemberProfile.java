package com.scms.backend.account;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "member_profiles")
public class MemberProfile {

	@Id
	@Column(name = "account_id", nullable = false, updatable = false)
	private UUID accountId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false, updatable = false)
	private Account account;

	@Generated(event = EventType.INSERT)
	@Column(name = "member_code", nullable = false, length = 20, insertable = false, updatable = false)
	private String memberCode;

	@Column(name = "profile_image_url", columnDefinition = "text")
	private String profileImageUrl;

	@Column(name = "fitness_goal", columnDefinition = "text")
	private String fitnessGoal;

	@Column(name = "emergency_contact_name", length = 200)
	private String emergencyContactName;

	@Column(name = "emergency_contact_phone", length = 32)
	private String emergencyContactPhone;

	@Generated(event = EventType.INSERT)
	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Generated(event = EventType.INSERT)
	@Column(name = "updated_at", nullable = false, insertable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	protected MemberProfile() {
	}

	public MemberProfile(Account account) {
		this(account, null, null);
	}

	public MemberProfile(Account account, String profileImageUrl, String fitnessGoal) {
		this.account = Objects.requireNonNull(account);
		this.accountId = account.getId();
		this.profileImageUrl = profileImageUrl;
		this.fitnessGoal = fitnessGoal;
	}

	public UUID getAccountId() {
		return accountId;
	}

	public Account getAccount() {
		return account;
	}

	public String getMemberCode() {
		return memberCode;
	}

	public String getProfileImageUrl() {
		return profileImageUrl;
	}

	public String getFitnessGoal() {
		return fitnessGoal;
	}

	public String getEmergencyContactName() {
		return emergencyContactName;
	}

	public String getEmergencyContactPhone() {
		return emergencyContactPhone;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Long getVersion() {
		return version;
	}
}
