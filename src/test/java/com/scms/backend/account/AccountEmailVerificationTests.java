package com.scms.backend.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AccountEmailVerificationTests {
	private static final Instant NOW=Instant.parse("2026-10-20T07:00:00Z");

	@Test
	void verifyPendingEmailActivatesAndMarksEmailVerified(){
		Account account=pending(NOW.plusSeconds(60));
		account.verifyPendingEmail(NOW);
		assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(account.isEmailVerificationRequired()).isFalse();
		assertThat(account.getEmailVerifiedAt()).isEqualTo(NOW);
	}

	@Test
	void manualBypassActivatesWithoutMarkingEmailVerified(){
		Account account=pending(NOW.plusSeconds(60));
		account.approveManualEmailVerificationBypass(NOW);
		assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(account.isEmailVerificationRequired()).isFalse();
		assertThat(account.getEmailVerifiedAt()).isNull();
	}

	@Test
	void expirationOnlyAcceptsExpiredPendingRegistration(){
		Account activePending=pending(NOW.plusSeconds(1));
		assertThatThrownBy(()->activePending.expirePendingRegistration(NOW)).isInstanceOf(IllegalStateException.class);
		Account expired=pending(NOW.minusSeconds(1));
		expired.expirePendingRegistration(NOW);
		assertThat(expired.getStatus()).isEqualTo(AccountStatus.INACTIVE);
		assertThat(expired.isEmailVerificationRequired()).isFalse();
	}

	@Test
	void invalidStatesCannotBypassPendingLifecycle(){
		Account expired=pending(NOW.minusSeconds(1));
		assertThatThrownBy(()->expired.verifyPendingEmail(NOW)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(()->expired.approveManualEmailVerificationBypass(NOW)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(()->expired.changeMemberStatus(AccountStatus.ACTIVE)).isInstanceOf(IllegalStateException.class);
	}

	private Account pending(Instant expiresAt){return Account.pendingSelfRegistration(UUID.randomUUID(),"Pending Member",
		"0900000000","pending@example.test",LocalDate.of(2000,1,1),"hash",expiresAt);}
}
