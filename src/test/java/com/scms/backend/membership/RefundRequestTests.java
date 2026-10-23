package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class RefundRequestTests {
	private static final Instant NOW = Instant.parse("2026-10-23T00:00:00Z");

	@Test
	void refundMustFollowApprovalDisbursementAndCompletionOrder() {
		RefundRequest request = requested();
		request.firstApprove(UUID.randomUUID(), BigInteger.valueOf(700), BigInteger.valueOf(800), 7, LocalDate.of(2026, 10, 23), NOW);
		request.cashDisburse(UUID.randomUUID(), "cash-drawer-42", NOW.plusSeconds(1));
		request.complete(UUID.randomUUID(), NOW.plusSeconds(2));

		assertThat(request.getStatus()).isEqualTo(RefundRequestStatus.COMPLETED);
		assertThat(request.getApprovedAmount()).isEqualTo(BigInteger.valueOf(700));
		assertThat(request.getRemainingDaysSnapshot()).isEqualTo(7);
		assertThat(request.getCalculationDate()).isEqualTo(LocalDate.of(2026, 10, 23));
	}

	@Test
	void cannotDisburseBeforeFirstApprovalOrCompleteBeforeCashDisbursement() {
		RefundRequest request = requested();
		assertThatThrownBy(() -> request.cashDisburse(UUID.randomUUID(), "evidence", NOW))
			.isInstanceOf(IllegalStateException.class);

		request.firstApprove(UUID.randomUUID(), BigInteger.TEN, BigInteger.TEN, 1, LocalDate.of(2026, 10, 23), NOW);
		assertThatThrownBy(() -> request.complete(UUID.randomUUID(), NOW))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void approvalAmountMustBePositiveAndNotExceedRequestedAmount() {
		RefundRequest request = requested();
		assertThatThrownBy(() -> request.firstApprove(UUID.randomUUID(), BigInteger.ZERO, BigInteger.TEN, 7,
			LocalDate.of(2026, 10, 23), NOW)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> request.firstApprove(UUID.randomUUID(), BigInteger.valueOf(1_001), BigInteger.valueOf(2_000), 7,
			LocalDate.of(2026, 10, 23), NOW)).isInstanceOf(IllegalArgumentException.class);
	}

	private RefundRequest requested() {
		return new RefundRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
			UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), RefundType.TERMINATED_MEMBERSHIP,
			BigInteger.valueOf(1_000), BigInteger.valueOf(1_000), 10, LocalDate.of(2026, 10, 23),
			"Member termination", "refund-key-1");
	}
}
