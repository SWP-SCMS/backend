package com.scms.backend.scheduling.coachleave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.scms.backend.course.session.CourseLeaveBatchActions;
import com.scms.backend.scheduling.ClassSession;
import com.scms.backend.scheduling.CoachLeaveException;
import com.scms.backend.scheduling.CoachLeaveRequest;
import com.scms.backend.scheduling.CoachLeaveStatus;
import com.scms.backend.scheduling.RecurringScheduleException;
import com.scms.backend.scheduling.resource.SharedResourceLockService;
import com.scms.backend.scheduling.session.AvailableCoachResponse;
import com.scms.backend.scheduling.session.ClassSessionCancellationService;
import com.scms.backend.scheduling.session.ClassSessionRepository;
import com.scms.backend.scheduling.session.ClassSessionService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

class CoachLeaveBatchServiceTests {

	final UUID manager = UUID.randomUUID(), leave = UUID.randomUUID(), coach = UUID.randomUUID(), replacement = UUID.randomUUID();

	final CoachLeaveImpactService impacts = mock(CoachLeaveImpactService.class);

	final CoachLeaveRepository leaves = mock(CoachLeaveRepository.class);

	final ClassSessionRepository sessions = mock(ClassSessionRepository.class);

	final ClassSessionService assignments = mock(ClassSessionService.class);

	final ClassSessionCancellationService cancellations = mock(ClassSessionCancellationService.class);

	final CourseLeaveBatchActions courses = mock(CourseLeaveBatchActions.class);

	final SharedResourceLockService resources = mock(SharedResourceLockService.class);

	final CoachLeaveBatchService service = new CoachLeaveBatchService(impacts, leaves, sessions, assignments, cancellations, courses, resources);

	final Instant start = Instant.parse("2030-11-10T01:00:00Z");

	CoachLeaveImpactService.AffectedSession session(int index, long members) {
		return new CoachLeaveImpactService.AffectedSession(new UUID(0, index), "CLASS", "Class " + index, null, null, null, start.plusSeconds(index * 86400L), start.plusSeconds(index * 86400L + 3600), coach, "Coach", null, "Room", 10, members, "SCHEDULED", 2, true, true, members == 0);
	}

	void setup(List<CoachLeaveImpactService.AffectedSession> selected) {
		var request = new CoachLeaveResponse(leave, coach, "Coach", "090", start, start.plusSeconds(864000), "Leave", CoachLeaveStatus.PENDING, null, null, null, start.minusSeconds(86400), selected.stream().map(s -> s.id()).toList());
		when(impacts.detail(manager, leave)).thenReturn(new CoachLeaveImpactService.Impact(request, selected));
		var lockedLeave = mock(CoachLeaveRequest.class);
		when(lockedLeave.getStatus()).thenReturn(CoachLeaveStatus.PENDING);
		when(leaves.findByIdForUpdate(leave)).thenReturn(Optional.of(lockedLeave));
	}

	CoachLeaveBatchService.Command command(List<CoachLeaveImpactService.AffectedSession> selected, String action) {
		return new CoachLeaveBatchService.Command(selected.stream().map(s -> new CoachLeaveBatchService.Selection(s.type(), s.id(), s.version())).toList(), action, replacement, "Coach leave");
	}

	@Test
	void reassignsAllInOneCallWithoutApprovingLeave() {
		var selected = List.of(session(1, 1), session(2, 0));
		setup(selected);
		assertThat(service.resolve(manager, leave, command(selected, "reassign")).processedCount()).isEqualTo(2);
		verify(assignments, times(2)).updateAssignment(eq(manager), any(), argThat(r -> r.coachId().equals(replacement) && r.expectedVersion() == 2));
		verifyNoInteractions(cancellations, courses);
	}

	@Test
	void transactionRollsBackWhenSecondMutationFails() {
		var selected = List.of(session(1, 1), session(2, 0));
		setup(selected);
		when(assignments.updateAssignment(eq(manager), eq(selected.get(1).id()), any())).thenThrow(RecurringScheduleException.sessionConflict());
		var transactions = mock(PlatformTransactionManager.class);
		var status = mock(TransactionStatus.class);
		when(transactions.getTransaction(any(TransactionDefinition.class))).thenReturn(status);
		var proxy = new ProxyFactory(service);
		proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource(false)));
		var transactional = (CoachLeaveBatchService) proxy.getProxy();
		assertThatThrownBy(() -> transactional.resolve(manager, leave, command(selected, "reassign"))).isInstanceOf(CoachLeaveException.class).hasFieldOrPropertyWithValue("code", "COACH_LEAVE_BATCH_FAILED").hasFieldOrPropertyWithValue("affectedSessionIds", List.of(selected.get(1).id()));
		verify(assignments, times(2)).updateAssignment(any(), any(), any());
		verify(transactions).rollback(status);
		verify(transactions, never()).commit(any());
	}

	@Test
	void rejectsEntireCancellationIfAnySessionHasMembers() {
		var selected = List.of(session(1, 0), session(2, 1));
		setup(selected);
		assertThatThrownBy(() -> service.resolve(manager, leave, command(selected, "cancel"))).isInstanceOf(CoachLeaveException.class);
		verifyNoInteractions(cancellations, assignments, courses);
	}

	@Test
	void rejectsStaleOrDuplicateSelectionBeforeMutating() {
		var selected = List.of(session(1, 0));
		setup(selected);
		var stale = new CoachLeaveBatchService.Command(List.of(new CoachLeaveBatchService.Selection("CLASS", selected.getFirst().id(), 1L)), "reassign", replacement, null);
		assertThatThrownBy(() -> service.resolve(manager, leave, stale)).isInstanceOf(CoachLeaveException.class);
		assertThatThrownBy(() -> service.resolve(manager, leave, command(List.of(selected.getFirst(), selected.getFirst()), "reassign"))).isInstanceOf(CoachLeaveException.class);
		verifyNoInteractions(assignments, cancellations, courses);
	}

	@Test
	void candidatesIntersectAcrossTheWholeSelectionWithOneImpactRead() {
		var selected = List.of(session(1, 0), session(2, 0));
		setup(selected);
		var a = new AvailableCoachResponse(replacement, "Replacement", "090");
		var b = new AvailableCoachResponse(UUID.randomUUID(), "Other", "091");
		when(impacts.available(coach, selected.get(0))).thenReturn(List.of(a, b));
		when(impacts.available(coach, selected.get(1))).thenReturn(List.of(a));
		assertThat(service.available(manager, leave, command(selected, "reassign"))).containsExactly(a);
		verify(impacts).detail(manager, leave);
	}

	@Test
	void courseBatchUsesExistingCourseRulesAndVersions() {
		var original = session(1, 0);
		UUID run = UUID.randomUUID(), room = UUID.randomUUID();
		var course = new CoachLeaveImpactService.AffectedSession(original.id(), "COURSE", "Course", run, "AERO", 1, original.startTime(), original.endTime(), coach, "Coach", room, "Room", 10, 0, "SCHEDULED", 2, true, true, true);
		setup(List.of(course));
		assertThat(service.resolve(manager, leave, command(List.of(course), "reassign")).processedCount()).isOne();
		verify(courses).reassign(manager, run, course.id(), 2, room, replacement, course.startTime(), course.endTime());
		verifyNoInteractions(assignments, cancellations);
	}

	@Test
	void cancellationChecksLockedVersionBeforeCallingExistingGuard() {
		var selected = List.of(session(1, 0));
		setup(selected);
		var locked = mock(ClassSession.class);
		when(locked.getVersion()).thenReturn(3L);
		when(sessions.findByIdForUpdate(selected.getFirst().id())).thenReturn(Optional.of(locked));
		assertThatThrownBy(() -> service.resolve(manager, leave, command(selected, "cancel"))).isInstanceOf(CoachLeaveException.class);
		verifyNoInteractions(cancellations);
		when(locked.getVersion()).thenReturn(2L);
		assertThat(service.resolve(manager, leave, command(selected, "cancel")).processedCount()).isOne();
		verify(cancellations).cancel(eq(manager), eq(selected.getFirst().id()), any());
	}
}
