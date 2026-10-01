package com.scms.backend.membership;

import java.math.BigInteger;
import java.time.Instant;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "membership_orders")
public class MembershipOrder {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "order_number", nullable = false, length = 40, updatable = false)
	private String orderNumber;

	@Column(name = "member_account_id", nullable = false, updatable = false)
	private UUID memberAccountId;

	@Column(name = "created_by_account_id", nullable = false, updatable = false)
	private UUID createdByAccountId;

	@Column(name = "offer_id", nullable = false, updatable = false)
	private UUID offerId;

	@Column(name = "offer_name_snapshot", nullable = false, length = 200, updatable = false)
	private String offerNameSnapshot;

	@Enumerated(EnumType.STRING)
	@Column(name = "plan_code_snapshot", nullable = false, length = 10, updatable = false)
	private MembershipPlanCode planCodeSnapshot;

	@Column(name = "price_amount_snapshot", nullable = false, precision = 19, scale = 0, updatable = false)
	private BigInteger priceAmountSnapshot;

	@Column(name = "currency_code_snapshot", nullable = false, length = 3,
		columnDefinition = "char(3)", updatable = false)
	@JdbcTypeCode(SqlTypes.CHAR)
	private String currencyCodeSnapshot;

	@Column(name = "duration_days_snapshot", nullable = false, updatable = false)
	private int durationDaysSnapshot;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", nullable = false, length = 20, updatable = false)
	private PaymentMethod paymentMethod;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MembershipOrderStatus status;

	@Column(name = "expires_at")
	private Instant expiresAt;

	@Column(name = "paid_at")
	private Instant paidAt;

	@Generated(event = EventType.INSERT)
	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Generated(event = EventType.INSERT)
	@Column(name = "updated_at", nullable = false, insertable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	protected MembershipOrder() {
	}

	public MembershipOrder(UUID id, String orderNumber, UUID memberAccountId, UUID createdByAccountId,
			MembershipOffer offer) {
		this.id = Objects.requireNonNull(id);
		this.orderNumber = Objects.requireNonNull(orderNumber);
		this.memberAccountId = Objects.requireNonNull(memberAccountId);
		this.createdByAccountId = Objects.requireNonNull(createdByAccountId);
		this.offerId = Objects.requireNonNull(offer).getId();
		this.offerNameSnapshot = offer.getName();
		this.planCodeSnapshot = offer.getPlan().getPlanCode();
		this.priceAmountSnapshot = offer.getPriceAmount();
		this.currencyCodeSnapshot = offer.getCurrencyCode();
		this.durationDaysSnapshot = offer.getDurationDays();
		this.paymentMethod = PaymentMethod.BANK_TRANSFER;
		this.status = MembershipOrderStatus.PENDING_PAYMENT;
	}

	public UUID getId() {
		return id;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public UUID getMemberAccountId() {
		return memberAccountId;
	}

	public UUID getCreatedByAccountId() {
		return createdByAccountId;
	}

	public UUID getOfferId() {
		return offerId;
	}

	public String getOfferNameSnapshot() {
		return offerNameSnapshot;
	}

	public MembershipPlanCode getPlanCodeSnapshot() {
		return planCodeSnapshot;
	}

	public BigInteger getPriceAmountSnapshot() {
		return priceAmountSnapshot;
	}

	public String getCurrencyCodeSnapshot() {
		return currencyCodeSnapshot;
	}

	public int getDurationDaysSnapshot() {
		return durationDaysSnapshot;
	}

	public PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	public MembershipOrderStatus getStatus() {
		return status;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getPaidAt() {
		return paidAt;
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
