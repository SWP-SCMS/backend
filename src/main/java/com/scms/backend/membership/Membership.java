package com.scms.backend.membership;

import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "memberships")
public class Membership {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "member_account_id", nullable = false, updatable = false)
	private UUID memberAccountId;

	@Column(name = "order_id", nullable = false, updatable = false)
	private UUID orderId;

	@Column(name = "offer_id", nullable = false, updatable = false)
	private UUID offerId;

	@Column(name = "plan_code_snapshot", nullable = false, updatable = false, length = 50)
	private String planCodeSnapshot;

	@Column(name = "offer_name_snapshot", nullable = false, updatable = false, length = 200)
	private String offerNameSnapshot;

	@Column(name = "price_amount_snapshot", nullable = false, updatable = false)
	private BigInteger priceAmountSnapshot;

	@Column(name = "currency_code_snapshot", nullable = false, updatable = false, length = 3,
		columnDefinition = "char(3)")
	@JdbcTypeCode(SqlTypes.CHAR)
	private String currencyCodeSnapshot;

	@Column(name = "duration_days_snapshot", nullable = false, updatable = false)
	private int durationDaysSnapshot;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MembershipStatus status;

	@Column(name = "starts_at")
	private Instant startsAt;

	@Column(name = "ends_at")
	private Instant endsAt;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	protected Membership() {
	}

	Membership(UUID id, UUID memberAccountId, UUID orderId, UUID offerId, String planCodeSnapshot,
			String offerNameSnapshot, BigInteger priceAmountSnapshot, String currencyCodeSnapshot,
			int durationDaysSnapshot, MembershipStatus status, Instant startsAt, Instant endsAt) {
		this.id = Objects.requireNonNull(id);
		this.memberAccountId = Objects.requireNonNull(memberAccountId);
		this.orderId = Objects.requireNonNull(orderId);
		this.offerId = Objects.requireNonNull(offerId);
		this.planCodeSnapshot = Objects.requireNonNull(planCodeSnapshot);
		this.offerNameSnapshot = Objects.requireNonNull(offerNameSnapshot);
		this.priceAmountSnapshot = Objects.requireNonNull(priceAmountSnapshot);
		this.currencyCodeSnapshot = Objects.requireNonNull(currencyCodeSnapshot);
		if (durationDaysSnapshot < 1) throw new IllegalArgumentException("durationDaysSnapshot must be positive");
		this.durationDaysSnapshot = durationDaysSnapshot;
		this.status = Objects.requireNonNull(status);
		this.startsAt = startsAt;
		this.endsAt = endsAt;
	}

	void expire() {
		if (status != MembershipStatus.ACTIVE) throw new IllegalStateException("Only ACTIVE Membership can expire");
		status = MembershipStatus.EXPIRED;
	}

	void activate(Instant startsAt) {
		if (status != MembershipStatus.RESERVED) throw new IllegalStateException("Only RESERVED Membership can activate");
		this.startsAt = Objects.requireNonNull(startsAt);
		this.endsAt = startsAt.plus(durationDaysSnapshot, ChronoUnit.DAYS);
		status = MembershipStatus.ACTIVE;
	}

	void terminate(Instant terminatedAt) {
		if (status != MembershipStatus.ACTIVE) throw new IllegalStateException("Only ACTIVE Membership can terminate");
		Instant effectiveEnd = Objects.requireNonNull(terminatedAt);
		if (!effectiveEnd.isAfter(startsAt)) effectiveEnd = startsAt.plusMillis(1);
		if (effectiveEnd.isBefore(endsAt)) endsAt = effectiveEnd;
		status = MembershipStatus.TERMINATED;
	}

	void extendTo(Instant newEnd) {
		if (status != MembershipStatus.ACTIVE || !Objects.requireNonNull(newEnd).isAfter(endsAt)) {
			throw new IllegalStateException("ACTIVE Membership end must be extended");
		}
		endsAt = newEnd;
	}

	public MembershipStatus getStatus() { return status; }
	public UUID getId() { return id; }
	public UUID getMemberAccountId() { return memberAccountId; }
	public Instant getStartsAt() { return startsAt; }
	public Instant getEndsAt() { return endsAt; }
	public BigInteger getPriceAmountSnapshot() { return priceAmountSnapshot; }
	public int getDurationDaysSnapshot() { return durationDaysSnapshot; }
}
