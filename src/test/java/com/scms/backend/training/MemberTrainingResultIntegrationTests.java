package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class MemberTrainingResultIntegrationTests {

	static {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired MemberTrainingResultService service;
	@Autowired AccountRepository accounts;
	@Autowired CoachAssignmentRepository assignments;
	@Autowired TrainingPlanRepository plans;
	@Autowired TrainingPlanVersionRepository planVersions;
	@Autowired TrainingResultRepository results;
	@Autowired JdbcTemplate jdbc;

	@Test
	void memberSeesOwnPersonalAndSessionHistoryAfterAssignmentEnds() {
		Account manager = createAccount(AccountRole.MANAGER, "Manager US47");
		Account coach = createAccount(AccountRole.COACH, "Coach US47");
		Account member = createAccount(AccountRole.MEMBER, "Member US47");
		Account otherMember = createAccount(AccountRole.MEMBER, "Other Member US47");
		Instant base = Instant.parse("2030-01-01T00:00:00Z");

		CoachAssignment assignment = assignments.saveAndFlush(new CoachAssignment(UUID.randomUUID(),
			member.getId(), coach.getId(), manager.getId(), CoachAssignmentStatus.ACTIVE,
			base.minusSeconds(7200), base.minusSeconds(7200)));
		TrainingPlan plan = plans.saveAndFlush(new TrainingPlan(UUID.randomUUID(), member.getId(),
			assignment.getId(), "Historical plan", coach.getId(), base.minusSeconds(7000)));
		TrainingPlanVersion version = planVersions.saveAndFlush(new TrainingPlanVersion(UUID.randomUUID(),
			plan.getId(), 1, Map.of("goal", "Strength"), coach.getId(), base.minusSeconds(6900)));
		TrainingResult personal = results.saveAndFlush(new TrainingResult(UUID.randomUUID(), member.getId(),
			assignment.getId(), version.getId(), base.plusSeconds(3600), Map.of("notes", "Personal"),
			coach.getId(), base.plusSeconds(3600)));
		assignment.end(base.plusSeconds(4000), "Membership expired");
		assignments.saveAndFlush(assignment);

		UUID sessionId = createYogaSession(manager.getId(), coach.getId());
		TrainingResult session = results.saveAndFlush(TrainingResult.forClassSession(UUID.randomUUID(),
			member.getId(), sessionId, base.plusSeconds(7200), Map.of("notes", "Yoga"), coach.getId(),
			base.plusSeconds(7200)));
		results.saveAndFlush(TrainingResult.forClassSession(UUID.randomUUID(), otherMember.getId(), sessionId,
			base.plusSeconds(10800), Map.of("notes", "Other member"), coach.getId(), base.plusSeconds(10800)));

		List<MemberTrainingResultResponse> history = service.viewOwnHistory(member.getId());

		assertThat(history).extracting(MemberTrainingResultResponse::id)
			.containsExactly(session.getId(), personal.getId());
		assertThat(history).allMatch(result -> result.memberAccountId().equals(member.getId()));
		assertThat(history.get(0).classSessionId()).isEqualTo(sessionId);
		assertThat(history.get(1).coachAssignmentId()).isEqualTo(assignment.getId());
		assertThat(assignments.findById(assignment.getId()).orElseThrow().getStatus())
			.isEqualTo(CoachAssignmentStatus.ENDED);
	}

	private UUID createYogaSession(UUID managerId, UUID coachId) {
		UUID disciplineId = UUID.randomUUID();
		UUID classId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		jdbc.update("insert into disciplines(id, name, status) values (?, ?, 'ACTIVE')",
			disciplineId, "Yoga " + disciplineId);
		jdbc.update("insert into sport_classes(id, discipline_id, name, class_type, status) "
			+ "values (?, ?, ?, 'YOGA', 'ACTIVE')", classId, disciplineId, "Vinyasa " + classId);
		jdbc.update("insert into rooms(id, name, capacity, status) values (?, ?, 20, 'ACTIVE')",
			roomId, "Studio " + roomId);
		jdbc.update("insert into class_sessions(id, sport_class_id, teaching_coach_account_id, room_id, "
			+ "start_time, end_time, capacity, status, created_by_account_id) "
			+ "values (?, ?, ?, ?, '2030-01-01T02:00:00Z', '2030-01-01T03:00:00Z', 12, 'SCHEDULED', ?)",
			sessionId, classId, coachId, roomId, managerId);
		return sessionId;
	}

	private Account createAccount(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		String unique = id.toString().substring(0, 8);
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name, "09" + unique,
			unique + "@example.test", LocalDate.of(1990, 1, 1), "{noop}secret"));
	}
}
