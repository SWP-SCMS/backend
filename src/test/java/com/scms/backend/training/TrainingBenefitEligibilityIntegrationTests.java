package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = "scms.membership-lifecycle-initial-delay-ms=86400000")
@ActiveProfiles("test")
@Transactional
class TrainingBenefitEligibilityIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired CoachAssignmentService assignments;
	@Autowired MemberCoachAssignmentService memberAssignments;
	@Autowired AssignmentDiscoveryService discovery;
	@Autowired TrainingPlanService plans;
	@Autowired TrainingFeedbackService feedback;
	@Autowired TrainingResultService results;
	@Autowired SessionTrainingFeedbackService sessionFeedback;
	@Autowired SessionTrainingResultService sessionResults;
	@Autowired MemberTrainingPlanService memberPlans;
	@Autowired MemberTrainingFeedbackService memberFeedback;
	@Autowired MemberTrainingResultService memberResults;
	@Autowired AccountRepository accounts;
	@Autowired JdbcTemplate jdbc;

	@Test
	void assignmentAndCandidatesUsePersonalCoachingBenefitFromMembershipSnapshot() {
		Account manager = createAccount(AccountRole.MANAGER, AccountStatus.ACTIVE, "Benefit Manager");
		Account coach = createAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Benefit Coach");
		Account eligible = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE, "Eligible Member");
		Account ineligible = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE, "Ineligible Member");
		Instant now = Instant.now();

		createPlan("CUSTOM_COACHING", "PERSONAL_COACHING");
		createPlan("CUSTOM_NO_COACHING", "TRAINING_PLAN");
		createMembership(eligible.getId(), manager.getId(), "CUSTOM_COACHING",
			now.minusSeconds(3600), now.plusSeconds(86400));
		createMembership(ineligible.getId(), manager.getId(), "CUSTOM_NO_COACHING",
			now.minusSeconds(3600), now.plusSeconds(86400));
		jdbc.update("update membership_plans set status = 'INACTIVE' where plan_code = 'CUSTOM_COACHING'");

		CoachAssignmentResponse assigned = assignments.assign(manager.getId(), eligible.getId(),
			new CoachAssignmentAssignRequest(coach.getId(), "Benefit-based assignment"));
		assertThat(assigned.status()).isEqualTo(CoachAssignmentStatus.ACTIVE);
		assertThat(discovery.candidates(manager.getId(), null, AssignmentCandidateStatus.ALL,
			PageRequest.of(0, 20)).content()).extracting(AssignmentCandidateResponse::memberAccountId)
			.contains(eligible.getId()).doesNotContain(ineligible.getId());

		assertThatThrownBy(() -> assignments.assign(manager.getId(), ineligible.getId(),
			new CoachAssignmentAssignRequest(coach.getId(), "Missing coaching Benefit")))
			.isInstanceOfSatisfying(CoachAssignmentException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("PLUS_MEMBERSHIP_REQUIRED"));

		jdbc.update("delete from membership_plan_benefits where plan_code = 'CUSTOM_COACHING' "
			+ "and benefit_code = 'PERSONAL_COACHING'");
		assertThat(memberAssignments.getActive(eligible.getId())).isEmpty();
		assertThat(jdbc.queryForObject("select end_reason from coach_assignments where id = ?",
			String.class, assigned.id())).contains("PERSONAL_COACHING");
	}

	@Test
	void personalTrainingMutationsRequireTheirExactBenefitsAndHistorySurvivesExpiry() {
		Account manager = createAccount(AccountRole.MANAGER, AccountStatus.ACTIVE, "Personal Manager");
		Account coach = createAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Personal Coach");
		Account eligible = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE, "Personal Eligible");
		Account planOnly = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE, "Plan Only");
		Instant now = Instant.now();

		createPlan("CUSTOM_TRAINING_ALL", "PERSONAL_COACHING", "TRAINING_PLAN", "TRAINING_FEEDBACK",
			"TRAINING_RESULT");
		createPlan("CUSTOM_PLAN_ONLY", "PERSONAL_COACHING", "TRAINING_PLAN");
		UUID eligibleMembership = createMembership(eligible.getId(), manager.getId(), "CUSTOM_TRAINING_ALL",
			now.minusSeconds(3600), now.plusSeconds(86400));
		createMembership(planOnly.getId(), manager.getId(), "CUSTOM_PLAN_ONLY",
			now.minusSeconds(3600), now.plusSeconds(86400));
		assignments.assign(manager.getId(), eligible.getId(),
			new CoachAssignmentAssignRequest(coach.getId(), "All training Benefits"));
		assignments.assign(manager.getId(), planOnly.getId(),
			new CoachAssignmentAssignRequest(coach.getId(), "Plan Benefit only"));

		TrainingPlanResponse plan = plans.create(coach.getId(), eligible.getId(),
			new CreateTrainingPlanRequest("Custom Benefit plan", Map.of("goal", "Strength")));
		TrainingResultResponse result = results.create(coach.getId(), eligible.getId(),
			new PersonalTrainingResultRequest(plan.versionId(), now, Map.of("sets", 3)));
		TrainingFeedbackResponse note = feedback.create(coach.getId(), eligible.getId(),
			new PersonalTrainingFeedbackRequest(null, result.id(), "Good progress"));
		assertThat(plan.id()).isNotNull();
		assertThat(result.id()).isNotNull();
		assertThat(note.id()).isNotNull();

		TrainingPlanResponse limitedPlan = plans.create(coach.getId(), planOnly.getId(),
			new CreateTrainingPlanRequest("Allowed plan", Map.of("goal", "Mobility")));
		assertThatThrownBy(() -> results.create(coach.getId(), planOnly.getId(),
			new PersonalTrainingResultRequest(limitedPlan.versionId(), now, Map.of())))
			.isInstanceOfSatisfying(TrainingResultException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("PLUS_MEMBERSHIP_REQUIRED"));
		assertThatThrownBy(() -> feedback.create(coach.getId(), planOnly.getId(),
			new PersonalTrainingFeedbackRequest(limitedPlan.versionId(), null, "Denied")))
			.isInstanceOfSatisfying(TrainingFeedbackException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("PLUS_MEMBERSHIP_REQUIRED"));

		jdbc.update("update memberships set status = 'EXPIRED', ends_at = ? where id = ?",
			Timestamp.from(now.minusSeconds(60)), eligibleMembership);
		jdbc.update("update membership_plans set status = 'INACTIVE' where plan_code = 'CUSTOM_TRAINING_ALL'");
		assertThat(memberPlans.viewOwnPlans(eligible.getId())).extracting(MemberTrainingPlanResponse::id)
			.contains(plan.id());
		assertThat(memberResults.viewOwnHistory(eligible.getId())).extracting(MemberTrainingResultResponse::id)
			.contains(result.id());
		assertThat(memberFeedback.viewOwnHistory(eligible.getId())).extracting(MemberTrainingFeedbackResponse::id)
			.contains(note.id());
	}

	@Test
	void trainingPlanMutationRejectsPersonalCoachingPlanWithoutTrainingPlanBenefit() {
		Account manager = createAccount(AccountRole.MANAGER, AccountStatus.ACTIVE, "Plan Manager");
		Account coach = createAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Plan Coach");
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE, "Plan Member");
		Instant now = Instant.now();
		createPlan("CUSTOM_COACH_ONLY", "PERSONAL_COACHING");
		createMembership(member.getId(), manager.getId(), "CUSTOM_COACH_ONLY",
			now.minusSeconds(3600), now.plusSeconds(86400));
		assignments.assign(manager.getId(), member.getId(),
			new CoachAssignmentAssignRequest(coach.getId(), "Coaching without plan Benefit"));

		assertThatThrownBy(() -> plans.create(coach.getId(), member.getId(),
			new CreateTrainingPlanRequest("Denied", Map.of())))
			.isInstanceOfSatisfying(TrainingPlanException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("PLUS_MEMBERSHIP_REQUIRED"));
	}

	@Test
	void sessionResultMutationsRequireTrainingResultBenefitWithoutChangingTeacherOrBookingChecks() {
		Account manager = createAccount(AccountRole.MANAGER, AccountStatus.ACTIVE, "Session Result Manager");
		Account coach = createAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Session Result Coach");
		Account eligible = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE, "Session Result Eligible");
		Account ineligible = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE, "Session Result Ineligible");
		Instant now = Instant.now();
		createPlan("CUSTOM_SESSION_RESULT", "CLASS_BOOKING", "TRAINING_RESULT");
		createPlan("CUSTOM_BOOKING_ONLY", "CLASS_BOOKING");
		UUID eligibleMembership = createMembership(eligible.getId(), manager.getId(), "CUSTOM_SESSION_RESULT",
			now.minusSeconds(3600), now.plusSeconds(86400));
		UUID ineligibleMembership = createMembership(ineligible.getId(), manager.getId(), "CUSTOM_BOOKING_ONLY",
			now.minusSeconds(3600), now.plusSeconds(86400));
		UUID eligibleSession = createBookedYogaSession(manager.getId(), coach.getId(), eligible.getId(),
			eligibleMembership, now.plusSeconds(3600));
		UUID ineligibleSession = createBookedYogaSession(manager.getId(), coach.getId(), ineligible.getId(),
			ineligibleMembership, now.plusSeconds(7200));

		SessionTrainingResultResponse created = sessionResults.create(coach.getId(), eligibleSession,
			eligible.getId(), new SessionTrainingResultRequest(now, Map.of("reps", 10)));
		assertThat(created.id()).isNotNull();
		assertThatThrownBy(() -> sessionResults.create(coach.getId(), ineligibleSession, ineligible.getId(),
			new SessionTrainingResultRequest(now, Map.of("reps", 10))))
			.isInstanceOfSatisfying(TrainingResultException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("PLUS_MEMBERSHIP_REQUIRED"));
	}

	@Test
	void sessionFeedbackMutationsRequireTrainingFeedbackBenefitWithoutChangingTeacherOrResultChecks() {
		Account manager = createAccount(AccountRole.MANAGER, AccountStatus.ACTIVE, "Session Feedback Manager");
		Account coach = createAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Session Feedback Coach");
		Account eligible = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE, "Session Feedback Eligible");
		Account ineligible = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE, "Session Feedback Ineligible");
		Instant now = Instant.now();
		createPlan("CUSTOM_SESSION_ALL", "CLASS_BOOKING", "TRAINING_RESULT", "TRAINING_FEEDBACK");
		createPlan("CUSTOM_RESULT_ONLY", "CLASS_BOOKING", "TRAINING_RESULT");
		UUID eligibleMembership = createMembership(eligible.getId(), manager.getId(), "CUSTOM_SESSION_ALL",
			now.minusSeconds(3600), now.plusSeconds(86400));
		UUID ineligibleMembership = createMembership(ineligible.getId(), manager.getId(), "CUSTOM_RESULT_ONLY",
			now.minusSeconds(3600), now.plusSeconds(86400));
		UUID eligibleSession = createBookedYogaSession(manager.getId(), coach.getId(), eligible.getId(),
			eligibleMembership, now.plusSeconds(3600));
		UUID ineligibleSession = createBookedYogaSession(manager.getId(), coach.getId(), ineligible.getId(),
			ineligibleMembership, now.plusSeconds(7200));
		SessionTrainingResultResponse eligibleResult = sessionResults.create(coach.getId(), eligibleSession,
			eligible.getId(), new SessionTrainingResultRequest(now, Map.of("reps", 10)));
		SessionTrainingResultResponse ineligibleResult = sessionResults.create(coach.getId(), ineligibleSession,
			ineligible.getId(), new SessionTrainingResultRequest(now, Map.of("reps", 10)));

		assertThat(sessionFeedback.create(coach.getId(), eligibleSession, eligible.getId(),
			new SessionTrainingFeedbackRequest(eligibleResult.id(), "Good form")).id()).isNotNull();
		assertThatThrownBy(() -> sessionFeedback.create(coach.getId(), ineligibleSession, ineligible.getId(),
			new SessionTrainingFeedbackRequest(ineligibleResult.id(), "Denied")))
			.isInstanceOfSatisfying(TrainingFeedbackException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("PLUS_MEMBERSHIP_REQUIRED"));
	}

	private Account createAccount(AccountRole role, AccountStatus status, String name) {
		UUID id = UUID.randomUUID();
		String unique = id.toString().substring(0, 8);
		Account account = accounts.saveAndFlush(new Account(id, role, status, name, "09" + unique,
			unique + "@example.test", LocalDate.of(1990, 1, 1), "{noop}secret"));
		if (role == AccountRole.MEMBER) jdbc.update("insert into member_profiles(account_id) values (?)", id);
		return account;
	}

	private void createPlan(String code, String... benefitCodes) {
		jdbc.update("insert into membership_plans(plan_code, display_name, supports_booking, "
			+ "supports_personal_coaching, status) values (?, ?, false, false, 'ACTIVE')", code, code);
		for (String benefitCode : benefitCodes) {
			jdbc.update("insert into membership_plan_benefits(plan_code, benefit_code) values (?, ?)",
				code, benefitCode);
		}
	}

	private UUID createMembership(UUID memberId, UUID actorId, String planCode, Instant startsAt, Instant endsAt) {
		UUID offerId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		jdbc.update("insert into membership_offers(id, plan_code, name, description, price_amount, duration_days, "
			+ "status, created_by_account_id) values (?, ?, ?, 'Benefit test offer', 1000, 30, 'ACTIVE', ?)",
			offerId, planCode, "Offer " + offerId, actorId);
		jdbc.update("insert into membership_orders(id, order_number, member_account_id, created_by_account_id, "
			+ "offer_id, offer_name_snapshot, plan_code_snapshot, price_amount_snapshot, currency_code_snapshot, "
			+ "duration_days_snapshot, payment_method, status, paid_at) "
			+ "values (?, ?, ?, ?, ?, 'Offer', ?, 1000, 'VND', 30, 'CASH', 'PAID', ?)",
			orderId, "BEN-" + orderId, memberId, actorId, offerId, planCode, Timestamp.from(startsAt));
		jdbc.update("insert into payments(id, order_id, method, status, amount, currency_code, "
			+ "processed_by_account_id, paid_at) values (?, ?, 'CASH', 'PAID', 1000, 'VND', ?, ?)",
			UUID.randomUUID(), orderId, actorId, Timestamp.from(startsAt));
		jdbc.update("insert into memberships(id, member_account_id, order_id, offer_id, plan_code_snapshot, "
			+ "offer_name_snapshot, price_amount_snapshot, currency_code_snapshot, duration_days_snapshot, "
			+ "status, starts_at, ends_at) values (?, ?, ?, ?, ?, 'Offer', 1000, 'VND', 30, 'ACTIVE', ?, ?)",
			membershipId, memberId, orderId, offerId, planCode, Timestamp.from(startsAt), Timestamp.from(endsAt));
		return membershipId;
	}

	private UUID createBookedYogaSession(UUID managerId, UUID coachId, UUID memberId, UUID membershipId,
			Instant startsAt) {
		UUID disciplineId = UUID.randomUUID();
		UUID classId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		jdbc.update("insert into disciplines(id, name, status) values (?, ?, 'ACTIVE')",
			disciplineId, "Yoga " + disciplineId);
		jdbc.update("insert into sport_classes(id, discipline_id, name, class_type, status) "
			+ "values (?, ?, ?, 'YOGA', 'ACTIVE')", classId, disciplineId, "Yoga " + classId);
		jdbc.update("insert into rooms(id, name, capacity, status) values (?, ?, 20, 'ACTIVE')",
			roomId, "Room " + roomId);
		jdbc.update("insert into class_sessions(id, sport_class_id, teaching_coach_account_id, room_id, "
			+ "start_time, end_time, capacity, status, created_by_account_id) "
			+ "values (?, ?, ?, ?, ?, ?, 12, 'SCHEDULED', ?)", sessionId, classId, coachId, roomId,
			Timestamp.from(startsAt), Timestamp.from(startsAt.plusSeconds(3600)), managerId);
		jdbc.update("insert into bookings(id, class_session_id, member_account_id, membership_id, status, "
			+ "booked_by_account_id) values (?, ?, ?, ?, 'BOOKED', ?)", UUID.randomUUID(), sessionId,
			memberId, membershipId, memberId);
		return sessionId;
	}
}
