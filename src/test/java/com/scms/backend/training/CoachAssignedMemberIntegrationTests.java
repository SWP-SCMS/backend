package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;

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
class CoachAssignedMemberIntegrationTests {

	static {
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
	}

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired
	private CoachAssignedMemberService assignedMemberService;

	@Autowired
	private CoachAssignmentService assignmentService;

	@Autowired
	private AccountRepository accounts;

	@Autowired
	private MemberProfileRepository profiles;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void listAssignedMembers_returnsOnlyCurrentCoachScopeAndEndsExpiredAssignments() {
		Account manager = createAccount(AccountRole.MANAGER, AccountStatus.ACTIVE, "Manager US40");
		Account coach = createAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Coach US40");
		Account otherCoach = createAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Other Coach US40");
		Account currentMember = createMember("Current Member US40", "Build strength");
		Account expiredMember = createMember("Expired Member US40", "Improve mobility");
		Account otherCoachMember = createMember("Other Member US40", "Lose weight");

		Instant now = Instant.now();
		createMembership(currentMember.getId(), manager.getId(), now.minusSeconds(86400), now.plusSeconds(86400));
		createMembership(expiredMember.getId(), manager.getId(), now.minusSeconds(86400 * 30), now.minusSeconds(3600));
		createMembership(otherCoachMember.getId(), manager.getId(), now.minusSeconds(86400), now.plusSeconds(86400));

		CoachAssignmentResponse current = assignmentService.assign(manager.getId(), currentMember.getId(),
				new CoachAssignmentAssignRequest(coach.getId(), "US40 current assignment"));
		assignmentService.assign(manager.getId(), otherCoachMember.getId(),
				new CoachAssignmentAssignRequest(otherCoach.getId(), "Different coach scope"));

		UUID expiredAssignmentId = UUID.randomUUID();
		jdbc.update("""
			insert into coach_assignments (id, member_account_id, coach_account_id, assigned_by_account_id,
				status, starts_at, created_at, updated_at, version)
			values (?, ?, ?, ?, 'ACTIVE', ?, ?, ?, 0)
			""", expiredAssignmentId, expiredMember.getId(), coach.getId(), manager.getId(),
				Timestamp.from(now.minusSeconds(86400 * 20)), Timestamp.from(now.minusSeconds(86400 * 20)),
				Timestamp.from(now.minusSeconds(86400 * 20)));

		CoachAssignedMemberPageResponse result = assignedMemberService.listAssignedMembers(
				coach.getId(), null, PageRequest.of(0, 20));

		assertThat(result.totalElements()).isEqualTo(1);
		assertThat(result.content()).singleElement().satisfies(member -> {
			assertThat(member.memberAccountId()).isEqualTo(currentMember.getId());
			assertThat(member.fullName()).isEqualTo("Current Member US40");
			assertThat(member.fitnessGoal()).isEqualTo("Build strength");
			assertThat(member.assignmentId()).isEqualTo(current.id());
		});
		assertThat(jdbc.queryForObject(
				"select status from coach_assignments where id = ?", String.class, expiredAssignmentId))
				.isEqualTo("ENDED");
	}

	@Test
	void searchFiltersTheWholeAssignedScopeByCodeNameOrPhone() {
		Account manager = createAccount(AccountRole.MANAGER, AccountStatus.ACTIVE, "Search Manager US40");
		Account coach = createAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Search Coach US40");
		Account otherCoach = createAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Other Search Coach US40");
		Account target = createMember("Search Target US40", "Build strength");
		Account newerMember = createMember("Newest Assigned US40", "Improve mobility");
		Account outsideScope = createMember("Search Target Outside US40", "Lose weight");
		Instant now = Instant.now();
		for (Account member : java.util.List.of(target, newerMember, outsideScope)) {
			createMembership(member.getId(), manager.getId(), now.minusSeconds(86400), now.plusSeconds(86400));
		}
		assignmentService.assign(manager.getId(), target.getId(),
			new CoachAssignmentAssignRequest(coach.getId(), "Search target"));
		assignmentService.assign(manager.getId(), newerMember.getId(),
			new CoachAssignmentAssignRequest(coach.getId(), "Newer assignment"));
		assignmentService.assign(manager.getId(), outsideScope.getId(),
			new CoachAssignmentAssignRequest(otherCoach.getId(), "Outside scope"));

		CoachAssignedMemberPageResponse byName = assignedMemberService.listAssignedMembers(
			coach.getId(), "  search target  ", PageRequest.of(0, 1));
		String memberCode = profiles.findById(target.getId()).orElseThrow().getMemberCode();
		CoachAssignedMemberPageResponse byCode = assignedMemberService.listAssignedMembers(
			coach.getId(), memberCode.toLowerCase(), PageRequest.of(0, 20));
		CoachAssignedMemberPageResponse byPhone = assignedMemberService.listAssignedMembers(
			coach.getId(), target.getPhone().substring(2), PageRequest.of(0, 20));
		CoachAssignedMemberPageResponse unfiltered = assignedMemberService.listAssignedMembers(
			coach.getId(), "   ", PageRequest.of(0, 20));

		assertThat(byName.totalElements()).isEqualTo(1);
		assertThat(byName.content()).extracting(CoachAssignedMemberSummaryResponse::memberAccountId)
			.containsExactly(target.getId());
		assertThat(byCode.content()).extracting(CoachAssignedMemberSummaryResponse::memberAccountId)
			.containsExactly(target.getId());
		assertThat(byPhone.content()).extracting(CoachAssignedMemberSummaryResponse::memberAccountId)
			.containsExactly(target.getId());
		assertThat(unfiltered.totalElements()).isEqualTo(2);
	}

	private Account createAccount(AccountRole role, AccountStatus status, String name) {
		UUID id = UUID.randomUUID();
		String unique = id.toString().substring(0, 8);
		return accounts.saveAndFlush(new Account(id, role, status, name, "09" + unique,
				unique + "@example.test", LocalDate.of(1990, 1, 1), "{noop}secret"));
	}

	private Account createMember(String name, String fitnessGoal) {
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE, name);
		profiles.saveAndFlush(new MemberProfile(member, null, fitnessGoal));
		return member;
	}

	private void createMembership(UUID memberId, UUID actorId, Instant startsAt, Instant endsAt) {
		UUID offerId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();

		jdbc.update("""
			insert into membership_offers (id, plan_code, name, description, price_amount, duration_days,
				status, created_by_account_id)
			values (?, 'PLUS', ?, 'US40 test offer', 1000, 30, 'ACTIVE', ?)
			""", offerId, "Offer " + offerId, actorId);
		jdbc.update("""
			insert into membership_orders (id, order_number, member_account_id, created_by_account_id, offer_id,
				offer_name_snapshot, plan_code_snapshot, price_amount_snapshot, currency_code_snapshot,
				duration_days_snapshot, payment_method, status, paid_at)
			values (?, ?, ?, ?, ?, 'Offer', 'PLUS', 1000, 'VND', 30, 'CASH', 'PAID', ?)
			""", orderId, "ORD-" + orderId, memberId, actorId, offerId, Timestamp.from(startsAt));
		jdbc.update("""
			insert into payments (id, order_id, method, status, amount, currency_code,
				processed_by_account_id, paid_at)
			values (?, ?, 'CASH', 'PAID', 1000, 'VND', ?, ?)
			""", UUID.randomUUID(), orderId, actorId, Timestamp.from(startsAt));
		jdbc.update("""
			insert into memberships (id, member_account_id, order_id, offer_id, plan_code_snapshot,
				offer_name_snapshot, price_amount_snapshot, currency_code_snapshot, duration_days_snapshot,
				status, starts_at, ends_at)
			values (?, ?, ?, ?, 'PLUS', 'Offer', 1000, 'VND', 30, 'ACTIVE', ?, ?)
			""", UUID.randomUUID(), memberId, orderId, offerId, Timestamp.from(startsAt), Timestamp.from(endsAt));
	}
}
