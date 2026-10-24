package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CoachLeaveLifecycleServiceTests {

	@Mock CoachLeaveRepository leaves;
	@Mock AuditEventRepository audits;
	Instant now = Instant.parse("2026-10-24T01:00:00Z");

	@Test
	void catchUpExpiresStartedPendingRequestsAndAuditsThem() {
		CoachLeaveRequest leave = leave(now.minusSeconds(1));
		when(leaves.findPendingStartedForUpdate(now)).thenReturn(List.of(leave));

		int expired = new CoachLeaveLifecycleService(leaves, audits).catchUp(now);

		assertThat(expired).isOne();
		assertThat(leave.getStatus()).isEqualTo(CoachLeaveStatus.EXPIRED);
		ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(event.capture());
		assertThat(event.getValue().getAction()).isEqualTo("COACH_LEAVE_EXPIRED");
		assertThat(event.getValue().getAfterData()).containsEntry("status", "EXPIRED");
	}

	@Test
	void futurePendingRequestDoesNotExpire() {
		CoachLeaveRequest leave = leave(now.plusSeconds(1));
		when(leaves.findByIdForUpdate(leave.getId())).thenReturn(Optional.of(leave));

		boolean expired = new CoachLeaveLifecycleService(leaves, audits)
			.expireIfStarted(leave.getId(), now);

		assertThat(expired).isFalse();
		assertThat(leave.getStatus()).isEqualTo(CoachLeaveStatus.PENDING);
		verify(audits, never()).save(any());
	}

	private CoachLeaveRequest leave(Instant start) {
		UUID coachId = UUID.randomUUID();
		Account coach = new Account(coachId, AccountRole.COACH, AccountStatus.ACTIVE, "Coach",
			"0900000000", coachId + "@example.test", LocalDate.of(1990, 1, 1), "{noop}password");
		return new CoachLeaveRequest(UUID.randomUUID(), coach, start, start.plusSeconds(3600), "Leave");
	}
}
