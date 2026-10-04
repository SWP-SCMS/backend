package com.scms.backend.membership;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "membership_offers")
public class MembershipOffer {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "plan_code", nullable = false, updatable = false)
	private MembershipPlan plan;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(nullable = false, columnDefinition = "text")
	private String description;

	@Column(name = "price_amount", nullable = false, precision = 19, scale = 0)
	private BigInteger priceAmount;

	@Column(name = "currency_code", nullable = false, length = 3, columnDefinition = "char(3)")
	@JdbcTypeCode(SqlTypes.CHAR)
	private String currencyCode;

	@Column(name = "duration_days", nullable = false)
	private int durationDays;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MembershipOfferStatus status;

	@Column(name = "created_by_account_id", nullable = false, updatable = false)
	private UUID createdByAccountId;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	protected MembershipOffer() {
	}

	public MembershipOffer(UUID id, MembershipPlan plan, String name, String description, BigInteger priceAmount,
			String currencyCode, int durationDays, MembershipOfferStatus status, UUID createdByAccountId) {
		this.id = id;
		this.plan = plan;
		this.name = name;
		this.description = description;
		this.priceAmount = priceAmount;
		this.currencyCode = currencyCode;
		this.durationDays = durationDays;
		this.status = status;
		this.createdByAccountId = createdByAccountId;
	}

	public void update(String name, String description, BigInteger priceAmount, int durationDays) {
		this.name = name;
		this.description = description;
		this.priceAmount = priceAmount;
		this.durationDays = durationDays;
	}

	public void patch(String name, String description, BigInteger priceAmount, Integer durationDays) {
		if (name != null) this.name = name;
		if (description != null) this.description = description;
		if (priceAmount != null) this.priceAmount = priceAmount;
		if (durationDays != null) this.durationDays = durationDays;
	}

	public void changeStatus(MembershipOfferStatus status) { this.status = status; }

	public UUID getId() {
		return id;
	}

	public MembershipPlan getPlan() {
		return plan;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public BigInteger getPriceAmount() {
		return priceAmount;
	}

	public String getCurrencyCode() {
		return currencyCode;
	}

	public int getDurationDays() {
		return durationDays;
	}

	public MembershipOfferStatus getStatus() { return status; }
}
