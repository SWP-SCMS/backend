package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.scheduling.Discipline;
import com.scms.backend.scheduling.DisciplineRepository;
import com.scms.backend.scheduling.DisciplineStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class CourseManagementServiceTests {

	private final CourseRepository courses = mock(CourseRepository.class);
	private final CourseRunRepository runs = mock(CourseRunRepository.class);
	private final CourseSessionRepository sessions = mock(CourseSessionRepository.class);
	private final DisciplineRepository disciplines = mock(DisciplineRepository.class);
	private final AccountRepository accounts = mock(AccountRepository.class);
	private final AuditEventRepository audits = mock(AuditEventRepository.class);
	private final CourseSessionService sessionService = mock(CourseSessionService.class);
	private final CourseManagementService service = new CourseManagementService(courses, runs, sessions,
		disciplines, accounts, audits, sessionService);
	private UUID managerId;

	@BeforeEach
	void activeManager() {
		managerId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);
	}

	@Test
	void createsDraftCourseAndWritesSanitizedAudit() {
		UUID disciplineId = UUID.randomUUID();
		Discipline discipline = mock(Discipline.class);
		when(discipline.getId()).thenReturn(disciplineId);
		when(discipline.getName()).thenReturn("Swimming");
		when(discipline.getStatus()).thenReturn(DisciplineStatus.ACTIVE);
		when(disciplines.findById(disciplineId)).thenReturn(Optional.of(discipline));
		when(courses.saveAndFlush(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CourseResponse response = service.createCourse(managerId,
			new CourseCreateRequest(" SWIM-101 ", " Beginner Swim ", " Basics ", disciplineId, null, 3));

		assertThat(response.code()).isEqualTo("SWIM-101");
		assertThat(response.name()).isEqualTo("Beginner Swim");
		assertThat(response.status()).isEqualTo(CourseStatus.DRAFT);
		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(audit.getValue().getAction()).isEqualTo("COURSE_CREATED");
		assertThat(audit.getValue().getActorAccountId()).isEqualTo(managerId);
		assertThat(audit.getValue().getAfterData()).containsEntry("code", "SWIM-101")
			.doesNotContainKeys("managerName", "managerEmail");
	}

	@Test
	void rejectsStaleCoursePatchBeforeChangingTheEntity() {
		Course course = new Course(UUID.randomUUID(), "SWIM-101", "Beginner Swim", null,
			mock(Discipline.class), null, 3);
		ReflectionTestUtils.setField(course, "version", 2L);
		when(courses.findByIdForUpdate(course.getId())).thenReturn(Optional.of(course));

		assertThatThrownBy(() -> service.patchCourse(managerId, course.getId(),
			new CoursePatchRequest(1L, "Changed", null, null, null)))
			.isInstanceOfSatisfying(CourseException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("COURSE_CHANGED"));
		assertThat(course.getName()).isEqualTo("Beginner Swim");
	}

	@Test
	void publishUsesCourseLifecycleAndRejectsRepeatedTransition() {
		Course course = new Course(UUID.randomUUID(), "SWIM-101", "Beginner Swim", null,
			mock(Discipline.class), null, 3);
		ReflectionTestUtils.setField(course, "version", 0L);
		when(courses.findByIdForUpdate(course.getId())).thenReturn(Optional.of(course));
		when(courses.saveAndFlush(course)).thenReturn(course);

		assertThat(service.publishCourse(managerId, course.getId(), new VersionRequest(0L)).status())
			.isEqualTo(CourseStatus.PUBLISHED);
		assertThatThrownBy(() -> service.publishCourse(managerId, course.getId(), new VersionRequest(0L)))
			.isInstanceOfSatisfying(CourseException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("COURSE_INVALID_TRANSITION"));
	}
}
