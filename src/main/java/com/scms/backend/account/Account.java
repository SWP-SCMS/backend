package com.scms.backend.account;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "accounts")
public class Account {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20, updatable = false)
	private AccountRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AccountStatus status;

	@Column(name = "full_name", nullable = false, length = 200)
	private String fullName;

	@Column(nullable = false, length = 32)
	private String phone;

	@Column(nullable = false, length = 320)
	private String email;

	@Column(name = "birth_date", nullable = false)
	private LocalDate birthDate;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "email_verified_at")
	private Instant emailVerifiedAt;

	@Column(name = "email_verification_required", nullable = false)
	private boolean emailVerificationRequired;

	@Column(name = "registration_expires_at")
	private Instant registrationExpiresAt;

	@Column(name = "auth_version", nullable = false)
	private long authVersion;

	@Generated(event = EventType.INSERT)
	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Generated(event = EventType.INSERT)
	@Column(name = "updated_at", nullable = false, insertable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	protected Account() {
	}

	public Account(UUID id, AccountRole role, AccountStatus status, String fullName, String phone, String email,
			LocalDate birthDate, String passwordHash) {
		this.id = Objects.requireNonNull(id);
		this.role = Objects.requireNonNull(role);
		this.status = Objects.requireNonNull(status);
		this.fullName = Objects.requireNonNull(fullName);
		this.phone = Objects.requireNonNull(phone);
		this.email = Objects.requireNonNull(email);
		this.birthDate = Objects.requireNonNull(birthDate);
		this.passwordHash = Objects.requireNonNull(passwordHash);
	}

	public static Account pendingSelfRegistration(UUID id, String fullName, String phone, String email,
			LocalDate birthDate, String passwordHash, Instant registrationExpiresAt) {
		Account account = new Account(id, AccountRole.MEMBER, AccountStatus.INACTIVE, fullName, phone, email,
			birthDate, passwordHash);
		account.emailVerificationRequired = true;
		account.registrationExpiresAt = Objects.requireNonNull(registrationExpiresAt);
		return account;
	}

	public UUID getId() {
		return id;
	}

	public void updateMemberProfileDetails(String fullName, String phone, String email, LocalDate birthDate) {
		this.fullName = Objects.requireNonNull(fullName);
		this.phone = Objects.requireNonNull(phone);
		this.email = Objects.requireNonNull(email);
		this.birthDate = Objects.requireNonNull(birthDate);
	}

	public void changePassword(String passwordHash) {
		this.passwordHash = Objects.requireNonNull(passwordHash);
	}

	public void changeMemberStatus(AccountStatus newStatus) {
		Objects.requireNonNull(newStatus);
		if (emailVerificationRequired || (status == AccountStatus.INACTIVE && registrationExpiresAt != null)) {
			throw new IllegalStateException("Pending or expired self-registration cannot use member status changes");
		}
		if (role != AccountRole.MEMBER || (newStatus != AccountStatus.ACTIVE && newStatus != AccountStatus.SUSPENDED)) {
			throw new IllegalStateException("Only Member accounts can transition between ACTIVE and SUSPENDED");
		}
		this.status = newStatus;
	}

	public void verifyPendingEmail(Instant now) {
		ensurePending(now);
		status = AccountStatus.ACTIVE;
		emailVerificationRequired = false;
		emailVerifiedAt = now;
	}

	public void approveManualEmailVerificationBypass(Instant now) {
		ensurePending(now);
		status = AccountStatus.ACTIVE;
		emailVerificationRequired = false;
	}

	public void expirePendingRegistration(Instant now) {
		if (!emailVerificationRequired || status != AccountStatus.INACTIVE || registrationExpiresAt == null
				|| registrationExpiresAt.isAfter(now)) {
			throw new IllegalStateException("Registration is not eligible for expiration");
		}
		emailVerificationRequired = false;
	}

	public void changePendingEmail(String newEmail, Instant now) {
		ensurePending(now);
		email = Objects.requireNonNull(newEmail);
	}

	public void changeVerifiedEmail(String newEmail, Instant now) {
		if (status != AccountStatus.ACTIVE) {
			throw new IllegalStateException("Only an active account can change its verified email");
		}
		email = Objects.requireNonNull(newEmail);
		emailVerifiedAt = Objects.requireNonNull(now);
		authVersion++;
	}

	@Deprecated(forRemoval = false)
	public void changeVerifiedStaffEmail(String newEmail, Instant now) {
		if (role == AccountRole.MEMBER) {
			throw new IllegalStateException("Only a staff account can use the legacy email-change method");
		}
		changeVerifiedEmail(newEmail, now);
	}

	public void reviveSelfRegistration(String fullName,String phone,String email,LocalDate birthDate,
			String passwordHash,Instant registrationExpiresAt) {
		if (role!=AccountRole.MEMBER || status!=AccountStatus.INACTIVE || emailVerificationRequired
				|| this.registrationExpiresAt==null) throw new IllegalStateException("Account is not an expired self-registration");
		this.fullName=Objects.requireNonNull(fullName);this.phone=Objects.requireNonNull(phone);
		this.email=Objects.requireNonNull(email);this.birthDate=Objects.requireNonNull(birthDate);
		this.passwordHash=Objects.requireNonNull(passwordHash);this.registrationExpiresAt=Objects.requireNonNull(registrationExpiresAt);
		this.emailVerifiedAt=null;this.emailVerificationRequired=true;
	}

	private void ensurePending(Instant now) {
		if (role != AccountRole.MEMBER || status != AccountStatus.INACTIVE || !emailVerificationRequired
				|| registrationExpiresAt == null || !registrationExpiresAt.isAfter(now)) {
			throw new IllegalStateException("Account is not an active pending self-registration");
		}
	}

	public void deactivateStaff() {
		if (role == AccountRole.MEMBER) {
			throw new IllegalStateException("Member accounts cannot be deactivated as staff");
		}
		status = AccountStatus.INACTIVE;
	}
	public AccountRole getRole() {
		return role;
	}

	public AccountStatus getStatus() {
		return status;
	}

	public String getFullName() {
		return fullName;
	}

	public String getPhone() {
		return phone;
	}

	public String getEmail() {
		return email;
	}

	public LocalDate getBirthDate() {
		return birthDate;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Instant getEmailVerifiedAt() { return emailVerifiedAt; }

	public boolean isEmailVerificationRequired() { return emailVerificationRequired; }

	public Instant getRegistrationExpiresAt() { return registrationExpiresAt; }

	public long getAuthVersion() { return authVersion; }

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
