package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class MembershipFreezeServiceTests {
	@Test
	void feeUsesHalfOpenBusinessDaysAndRoundsDownVnd() {
		Membership membership = membership(new BigInteger("1000000"), 31);
		assertThat(MembershipFreezeService.fee(membership, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4)))
			.isEqualTo(new BigInteger("19354"));
	}
	@Test
	void requestMovesOnlyThroughTheInitialPaymentWorkflow() {
		MembershipFreezeRequest request = new MembershipFreezeRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), "travel");
		request.approve(UUID.randomUUID(), BigInteger.valueOf(100), Instant.parse("2026-10-22T00:00:00Z"));
		assertThat(request.getStatus()).isEqualTo(MembershipFreezeRequestStatus.APPROVED_PENDING_PAYMENT);
		request.schedule();
		assertThat(request.getStatus()).isEqualTo(MembershipFreezeRequestStatus.SCHEDULED);
		assertThatThrownBy(request::schedule).isInstanceOf(IllegalStateException.class);
	}
	private Membership membership(BigInteger price, int duration) {
		return new Membership(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "PLUS", "Offer", price, "VND", duration, MembershipStatus.ACTIVE, Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-11-01T00:00:00Z"));
	}
}
