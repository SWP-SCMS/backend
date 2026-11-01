package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class CourseApprovalIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired CourseManagementService service;
	@Autowired AccountRepository accounts;
	@Autowired JdbcTemplate db;

	private UUID managerId;
	private UUID receptionistId;
	private UUID disciplineId;

	@BeforeEach
	void setUp() {
		managerId = account(AccountRole.MANAGER).getId();
		receptionistId = account(AccountRole.RECEPTIONIST).getId();
		disciplineId = UUID.randomUUID();
		db.update("insert into disciplines(id,name,status) values (?,?,'ACTIVE')", disciplineId,
			"Approval discipline " + disciplineId);
	}

	@Test
	void receptionistSubmissionWaitsForManagerApprovalBeforeUsingExistingPublishFlow() {
		CourseApprovalResponse submitted = service.submitCourse(receptionistId, request("APPROVE-101"));

		assertThat(submitted.status()).isEqualTo(CourseStatus.PENDING_APPROVAL);
		assertThat(submitted.submittedByAccountId()).isEqualTo(receptionistId);
		assertThat(service.listCourseApprovals(managerId, PageRequest.of(0, 20)).content())
			.extracting(CourseApprovalResponse::id).contains(submitted.id());
		assertThatThrownBy(() -> service.publishCourse(managerId, submitted.id(),
			new VersionRequest(submitted.version())))
			.isInstanceOfSatisfying(CourseException.class,
				error -> assertThat(error.getCode()).isEqualTo("COURSE_INVALID_TRANSITION"));

		CourseApprovalResponse approved = service.approveCourse(managerId, submitted.id(),
			new CourseApprovalDecision(submitted.version(), "Catalog details verified"));
		assertThat(approved.status()).isEqualTo(CourseStatus.DRAFT);
		assertThat(approved.reviewedByAccountId()).isEqualTo(managerId);
		assertThat(approved.reviewReason()).isEqualTo("Catalog details verified");
		assertThat(service.publishCourse(managerId, approved.id(), new VersionRequest(approved.version())).status())
			.isEqualTo(CourseStatus.PUBLISHED);
		assertThat(auditCount("COURSE_SUBMITTED_FOR_APPROVAL", submitted.id())).isOne();
		assertThat(auditCount("COURSE_APPROVED", submitted.id())).isOne();
	}

	@Test
	void rejectionIsTerminalAndStaleOrRepeatedDecisionsDoNotOverwriteEvidence() {
		CourseApprovalResponse submitted = service.submitCourse(receptionistId, request("REJECT-101"));

		assertThatThrownBy(() -> service.rejectCourse(managerId, submitted.id(),
			new CourseApprovalDecision(submitted.version() + 1, "Stale")))
			.isInstanceOfSatisfying(CourseException.class,
				error -> assertThat(error.getCode()).isEqualTo("COURSE_CHANGED"));
		CourseApprovalResponse rejected = service.rejectCourse(managerId, submitted.id(),
			new CourseApprovalDecision(submitted.version(), "Incomplete curriculum"));
		assertThat(rejected.status()).isEqualTo(CourseStatus.REJECTED);
		assertThatThrownBy(() -> service.approveCourse(managerId, submitted.id(),
			new CourseApprovalDecision(rejected.version(), "Retry")))
			.isInstanceOfSatisfying(CourseException.class,
				error -> assertThat(error.getCode()).isEqualTo("COURSE_APPROVAL_INVALID_TRANSITION"));
		assertThat(auditCount("COURSE_REJECTED", submitted.id())).isOne();
		assertThat(auditCount("COURSE_APPROVED", submitted.id())).isZero();
	}

	@Test
	void managerCreatedCourseStillStartsAsOrdinaryDraftWithoutApprovalMetadata() {
		CourseResponse created = service.createCourse(managerId, request("MANAGER-101"));
		CourseApprovalPageResponse queue = service.listCourseApprovals(managerId, PageRequest.of(0, 20));

		assertThat(created.status()).isEqualTo(CourseStatus.DRAFT);
		assertThat(queue.content()).extracting(CourseApprovalResponse::id).doesNotContain(created.id());
		assertThat(db.queryForObject("select submitted_by_account_id is null from courses where id=?",
			Boolean.class, created.id())).isTrue();
	}

	private CourseCreateRequest request(String code) {
		return new CourseCreateRequest(code, "Course " + code, "Approval test", disciplineId, null, 2);
	}

	private Account account(AccountRole role) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, role + " " + id,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}

	private long auditCount(String action, UUID courseId) {
		return db.queryForObject("select count(*) from audit_events where action=? and target_id=?",
			Long.class, action, courseId);
	}
}
