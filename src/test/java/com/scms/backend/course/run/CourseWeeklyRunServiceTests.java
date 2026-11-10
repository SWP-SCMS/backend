package com.scms.backend.course.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.course.CourseException;
import com.scms.backend.course.management.CourseManagementService;
import com.scms.backend.course.session.CourseSessionCreateRequest;
import com.scms.backend.scheduling.resource.SharedResourceLockService;
import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CourseWeeklyRunServiceTests {

	final CourseManagementService management = mock(CourseManagementService.class);

	final AccountRepository accounts = mock(AccountRepository.class);

	final SharedResourceLockService locks = mock(SharedResourceLockService.class);

	final UUID manager = UUID.randomUUID(), course = UUID.randomUUID(), coach = UUID.randomUUID(), room = UUID.randomUUID();

	final CourseWeeklyRunService service = new CourseWeeklyRunService(management, accounts, locks, Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC));

	@BeforeEach
	void active() {
		when(accounts.existsByIdAndRoleAndStatus(manager, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
	}

	CourseWeeklyRunCreateRequest request(BigInteger tuition) {
		return new CourseWeeklyRunCreateRequest("WEEKLY", course, coach, room, tuition, 10, 2, Instant.parse("2030-01-01T00:00:00Z"), Instant.parse("2030-02-01T00:00:00Z"), true, new CourseWeeklyScheduleRequest(LocalDate.of(2030, 2, 4), List.of(1, 3, 5), LocalTime.of(18, 0), LocalTime.of(19, 0), 4));
	}

	@Test
	void locksResourcesThenCreatesCalculatedRunAndEveryNumberedSession() {
		UUID id = UUID.randomUUID();
		var run = mock(CourseRunResponse.class);
		when(run.id()).thenReturn(id);
		when(management.createRun(any(), any())).thenReturn(run);
		when(management.getRun(manager, id)).thenReturn(run);
		service.create(manager, request(BigInteger.valueOf(1001)));
		var runRequest = ArgumentCaptor.forClass(CourseRunCreateRequest.class);
		var slotRequest = ArgumentCaptor.forClass(CourseSessionCreateRequest.class);
		var order = inOrder(locks, management);
		order.verify(locks).lock(List.of(coach), List.of(room));
		order.verify(management).createRun(eq(manager), runRequest.capture());
		assertThat(runRequest.getValue().plannedStartAt()).isEqualTo(Instant.parse("2030-02-04T11:00:00Z"));
		assertThat(runRequest.getValue().plannedEndAt()).isEqualTo(Instant.parse("2030-02-11T12:00:00Z"));
		verify(management, times(4)).createSession(eq(manager), eq(id), slotRequest.capture());
		assertThat(slotRequest.getAllValues()).extracting(CourseSessionCreateRequest::unitNumber).containsExactly(1, 2, 3, 4);
		assertThat(slotRequest.getAllValues()).extracting(CourseSessionCreateRequest::allocationVnd).containsExactly(BigInteger.valueOf(250), BigInteger.valueOf(250), BigInteger.valueOf(250), BigInteger.valueOf(251));
	}

	@Test
	void inactiveManagerCannotPreviewOrCreate() {
		when(accounts.existsByIdAndRoleAndStatus(manager, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(false);
		assertThatThrownBy(() -> service.preview(manager, request(BigInteger.TEN).schedule())).isInstanceOf(InvalidAuthenticatedAccountException.class);
		assertThatThrownBy(() -> service.create(manager, request(BigInteger.TEN))).isInstanceOf(InvalidAuthenticatedAccountException.class);
		verifyNoInteractions(management, locks);
	}

	@Test
	void invalidTuitionIsRejectedBeforeAnyWrites() {
		assertThatThrownBy(() -> service.create(manager, request(BigInteger.ONE))).isInstanceOf(CourseException.class);
		verifyNoInteractions(management, locks);
	}

	@Test
	void conflictIsPropagatedToTransactionalBoundary() {
		var run = mock(CourseRunResponse.class);
		UUID id = UUID.randomUUID();
		when(run.id()).thenReturn(id);
		when(management.createRun(any(), any())).thenReturn(run);
		when(management.createSession(any(), any(), any())).thenReturn(null).thenThrow(CourseException.resourceConflict());
		assertThatThrownBy(() -> service.create(manager, request(BigInteger.valueOf(1001)))).isInstanceOf(CourseException.class);
		verify(management, times(2)).createSession(any(), any(), any());
		verify(management, never()).getRun(any(), any());
	}
}
