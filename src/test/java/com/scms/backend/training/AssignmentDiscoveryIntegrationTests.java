package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = {
	"scms.membership-lifecycle-initial-delay-ms=86400000",
	"scms.coach-assignment.maximum-member-count=2"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AssignmentDiscoveryIntegrationTests {

	static {
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
	}

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired AssignmentDiscoveryService discovery;
	@Autowired CoachAssignmentService assignmentService;
	@Autowired CoachAssignmentRepository assignments;
	@Autowired AccountRepository accounts;
	@Autowired MemberProfileRepository profiles;
	@Autowired JdbcTemplate jdbc;
	@Autowired MockMvc mockMvc;
	@Autowired JwtEncoder jwtEncoder;

	@Test
	void candidatesUseEffectivePlusEligibilityFiltersSearchPaginationAndStableOrdering() {
		Account manager = createAccount(AccountRole.MANAGER, AccountStatus.ACTIVE, "Manager Discovery");
		Account coach = createAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Coach Discovery");
		Account inactiveCoach = createAccount(AccountRole.COACH, AccountStatus.INACTIVE, "Inactive Coach");
		Instant now = Instant.now();

		MemberProfile unassignedSoon = createMember(AccountStatus.ACTIVE, "Searchable Member", "0911111111",
			"searchable@example.test");
		MemberProfile unassignedLater = createMember(AccountStatus.ACTIVE, "Later Member", "0922222222",
			"later@example.test");
		MemberProfile assigned = createMember(AccountStatus.ACTIVE, "Assigned Member", "0933333333",
			"assigned@example.test");
		MemberProfile assignedInactiveCoach = createMember(AccountStatus.ACTIVE, "Inactive Coach Member", "0944444444",
			"inactive-coach-member@example.test");
		MemberProfile suspended = createMember(AccountStatus.SUSPENDED, "Suspended Member", "0955555555",
			"suspended@example.test");
		MemberProfile basic = createMember(AccountStatus.ACTIVE, "Basic Member", "0977777777",
			"basic@example.test");
		MemberProfile expired = createMember(AccountStatus.ACTIVE, "Expired Member", "0988888888",
			"expired@example.test");

		createMembership(unassignedSoon.getAccountId(), manager.getId(), "PLUS", now.minusSeconds(3600),
			now.plusSeconds(86400));
		createMembership(unassignedLater.getAccountId(), manager.getId(), "PLUS", now.minusSeconds(3600),
			now.plusSeconds(172800));
		createMembership(assigned.getAccountId(), manager.getId(), "PLUS", now.minusSeconds(3600),
			now.plusSeconds(1800));
		createMembership(assignedInactiveCoach.getAccountId(), manager.getId(), "PLUS", now.minusSeconds(3600),
			now.plusSeconds(7200));
		createMembership(suspended.getAccountId(), manager.getId(), "PLUS", now.minusSeconds(3600),
			now.plusSeconds(86400));
		createMembership(basic.getAccountId(), manager.getId(), "BASIC", now.minusSeconds(3600),
			now.plusSeconds(86400));
		UUID expiredMembershipId = createMembership(expired.getAccountId(), manager.getId(), "PLUS",
			now.minusSeconds(172800), now.minusSeconds(60), "EXPIRED");

		assignmentService.assign(manager.getId(), assigned.getAccountId(),
			new CoachAssignmentAssignRequest(coach.getId(), "Candidate test"));
		UUID inactiveCoachAssignmentId = insertActiveAssignment(assignedInactiveCoach.getAccountId(),
			inactiveCoach.getId(), manager.getId(), now.minusSeconds(1800));
		UUID expiredAssignmentId = insertActiveAssignment(expired.getAccountId(), coach.getId(), manager.getId(),
			now.minusSeconds(86400));

		AssignmentCandidatePageResponse all = discovery.candidates(manager.getId(), null,
			AssignmentCandidateStatus.ALL, PageRequest.of(0, 20));

		assertThat(all.content()).extracting(AssignmentCandidateResponse::memberAccountId)
			.containsExactly(unassignedSoon.getAccountId(), unassignedLater.getAccountId(),
				assigned.getAccountId(), assignedInactiveCoach.getAccountId());
		assertThat(all.content()).extracting(AssignmentCandidateResponse::assignmentStatus)
			.containsExactly(AssignmentCandidateStatus.UNASSIGNED, AssignmentCandidateStatus.UNASSIGNED,
				AssignmentCandidateStatus.ASSIGNED, AssignmentCandidateStatus.ASSIGNED);
		assertThat(all.content()).extracting(AssignmentCandidateResponse::memberAccountId)
			.doesNotContain(suspended.getAccountId(), basic.getAccountId(), expired.getAccountId());

		assertThat(discovery.candidates(manager.getId(), null, AssignmentCandidateStatus.UNASSIGNED,
			PageRequest.of(0, 20)).content())
			.extracting(AssignmentCandidateResponse::memberAccountId)
			.containsExactly(unassignedSoon.getAccountId(), unassignedLater.getAccountId());
		assertThat(discovery.candidates(manager.getId(), null, AssignmentCandidateStatus.ASSIGNED,
			PageRequest.of(0, 20)).content())
			.extracting(AssignmentCandidateResponse::memberAccountId)
			.containsExactly(assigned.getAccountId(), assignedInactiveCoach.getAccountId());

		assertThat(discovery.candidates(manager.getId(), "Searchable", AssignmentCandidateStatus.ALL,
			PageRequest.of(0, 20)).content()).extracting(AssignmentCandidateResponse::memberAccountId)
			.containsExactly(unassignedSoon.getAccountId());
		assertThat(discovery.candidates(manager.getId(), unassignedSoon.getMemberCode(), AssignmentCandidateStatus.ALL,
			PageRequest.of(0, 20)).content()).extracting(AssignmentCandidateResponse::memberAccountId)
			.containsExactly(unassignedSoon.getAccountId());
		assertThat(discovery.candidates(manager.getId(), "091111", AssignmentCandidateStatus.ALL,
			PageRequest.of(0, 20)).content()).extracting(AssignmentCandidateResponse::memberAccountId)
			.containsExactly(unassignedSoon.getAccountId());
		assertThat(discovery.candidates(manager.getId(), "SEARCHABLE@EXAMPLE", AssignmentCandidateStatus.ALL,
			PageRequest.of(0, 20)).content()).extracting(AssignmentCandidateResponse::memberAccountId)
			.containsExactly(unassignedSoon.getAccountId());

		AssignmentCandidatePageResponse secondPage = discovery.candidates(manager.getId(), null,
			AssignmentCandidateStatus.ALL, PageRequest.of(1, 1));
		assertThat(secondPage.totalElements()).isEqualTo(4);
		assertThat(secondPage.content()).extracting(AssignmentCandidateResponse::memberAccountId)
			.containsExactly(unassignedLater.getAccountId());

		assertThat(jdbc.queryForObject("select status from memberships where id = ?", String.class,
			expiredMembershipId)).isEqualTo("EXPIRED");
		assertThat(jdbc.queryForObject("select status from coach_assignments where id = ?", String.class,
			expiredAssignmentId)).isEqualTo("ENDED");
		assertThat(jdbc.queryForObject("select status from coach_assignments where id = ?", String.class,
			inactiveCoachAssignmentId)).isEqualTo("ACTIVE");
	}

	@Test
	void workloadsGroupActiveCoachesReconcileExpiryAndDoNotBlockMutation() {
		Account manager = createAccount(AccountRole.MANAGER, AccountStatus.ACTIVE, "Manager Workload");
		Account coachAlpha = createAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Coach Alpha");
		Account coachBeta = createAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Coach Beta");
		Account inactiveCoach = createAccount(AccountRole.COACH, AccountStatus.INACTIVE, "Coach Inactive");
		Instant now = Instant.now();

		MemberProfile first = plusMember(manager, "First Member", now.plusSeconds(86400));
		MemberProfile second = plusMember(manager, "Second Member", now.plusSeconds(86400));
		assignmentService.assign(manager.getId(), first.getAccountId(),
			new CoachAssignmentAssignRequest(coachAlpha.getId(), "First"));
		assignmentService.assign(manager.getId(), second.getAccountId(),
			new CoachAssignmentAssignRequest(coachAlpha.getId(), "Second"));

		MemberProfile expired = createMember(AccountStatus.ACTIVE, "Expired Workload Member", "0900000101",
			"expired-workload@example.test");
		createMembership(expired.getAccountId(), manager.getId(), "PLUS", now.minusSeconds(86400),
			now.minusSeconds(60));
		UUID expiredAssignment = insertActiveAssignment(expired.getAccountId(), coachAlpha.getId(), manager.getId(),
			now.minusSeconds(3600));

		MemberProfile inactiveCoachMember = plusMember(manager, "Inactive Coach Workload Member",
			now.plusSeconds(86400));
		UUID inactiveCoachAssignment = insertActiveAssignment(inactiveCoachMember.getAccountId(), inactiveCoach.getId(),
			manager.getId(), now.minusSeconds(3600));

		List<CoachAssignmentWorkloadResponse> workloads = discovery.workloads(manager.getId());
		assertThat(workloads).extracting(CoachAssignmentWorkloadResponse::coachAccountId)
			.containsExactly(coachAlpha.getId(), coachBeta.getId());
		assertThat(workloads.get(0).activeMemberCount()).isEqualTo(2);
		assertThat(workloads.get(1).activeMemberCount()).isZero();
		assertThat(workloads).allSatisfy(workload -> assertThat(workload.maximumMemberCount()).isEqualTo(2));
		assertThat(workloads.get(0).availableForAssignment()).isFalse();
		assertThat(workloads.get(1).availableForAssignment()).isTrue();
		assertThat(jdbc.queryForObject("select status from coach_assignments where id = ?", String.class,
			expiredAssignment)).isEqualTo("ENDED");
		assertThat(jdbc.queryForObject("select status from coach_assignments where id = ?", String.class,
			inactiveCoachAssignment)).isEqualTo("ACTIVE");

		MemberProfile third = plusMember(manager, "Third Member", now.plusSeconds(86400));
		assertThatThrownBy(() -> assignmentService.assign(manager.getId(), third.getAccountId(),
			new CoachAssignmentAssignRequest(coachAlpha.getId(), "Capacity enforced")))
			.isInstanceOf(CoachAssignmentException.class)
			.hasFieldOrPropertyWithValue("code", "COACH_CAPACITY_EXCEEDED");
		assertThat(discovery.workloads(manager.getId()).get(0).activeMemberCount()).isEqualTo(2);
	}

	@Test
	void reconciliationUsesLatestPastPlusExpiryWhenAFuturePlusMembershipExists() {
		Account manager = createAccount(AccountRole.MANAGER, AccountStatus.ACTIVE, "Manager Reconciliation");
		Account coach = createAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Coach Reconciliation");
		Instant now = Instant.parse("2026-10-18T12:00:00Z");
		Instant latestPastExpiry = now.minusSeconds(300);

		MemberProfile singleMember = createMember(AccountStatus.ACTIVE, "Single Reconciliation Member",
			"0900000201", "single-reconciliation@example.test");
		createMembership(singleMember.getAccountId(), manager.getId(), "PLUS", now.minusSeconds(10800),
			now.minusSeconds(7200), "EXPIRED");
		createMembership(singleMember.getAccountId(), manager.getId(), "PLUS", now.minusSeconds(7200),
			latestPastExpiry, "EXPIRED");
		createMembership(singleMember.getAccountId(), manager.getId(), "PLUS", now.plusSeconds(3600),
			now.plusSeconds(86400));
		UUID singleAssignment = insertActiveAssignment(singleMember.getAccountId(), coach.getId(), manager.getId(),
			now.minusSeconds(14400));

		assertThat(assignments.endActiveAssignmentIfPersonalCoachingExpired(singleMember.getAccountId(), now))
			.isEqualTo(1);
		assertEndedAt(singleAssignment, latestPastExpiry);

		MemberProfile bulkMember = createMember(AccountStatus.ACTIVE, "Bulk Reconciliation Member",
			"0900000202", "bulk-reconciliation@example.test");
		createMembership(bulkMember.getAccountId(), manager.getId(), "PLUS", now.minusSeconds(10800),
			now.minusSeconds(7200), "EXPIRED");
		createMembership(bulkMember.getAccountId(), manager.getId(), "PLUS", now.minusSeconds(7200),
			latestPastExpiry, "EXPIRED");
		createMembership(bulkMember.getAccountId(), manager.getId(), "PLUS", now.plusSeconds(3600),
			now.plusSeconds(86400));
		UUID bulkAssignment = insertActiveAssignment(bulkMember.getAccountId(), coach.getId(), manager.getId(),
			now.minusSeconds(14400));

		assertThat(assignments.endAllActiveAssignmentsWithExpiredPersonalCoaching(now)).isEqualTo(1);
		assertEndedAt(bulkAssignment, latestPastExpiry);
	}

	@Test
	void candidatesUseMemberCodeBeforeUuidAndCreationForStablePageBoundaries() {
		Account manager = createAccount(AccountRole.MANAGER, AccountStatus.ACTIVE, "Manager Stable Ordering");
		Instant now = Instant.now();
		Instant membershipEndsAt = now.plusSeconds(86400);
		Account createdFirstLowUuid = createAccount(UUID.fromString("00000000-0000-0000-0000-000000000010"),
			AccountRole.MEMBER, AccountStatus.ACTIVE, "Created First");
		MemberProfile createdFirstProfile = profiles.saveAndFlush(new MemberProfile(createdFirstLowUuid));
		Account createdSecondHighUuid = createAccount(UUID.fromString("ffffffff-ffff-ffff-ffff-fffffffffff0"),
			AccountRole.MEMBER, AccountStatus.ACTIVE, "Created Second");
		MemberProfile createdSecondProfile = profiles.saveAndFlush(new MemberProfile(createdSecondHighUuid));

		String lowerMemberCode = createdFirstProfile.getMemberCode();
		String higherMemberCode = createdSecondProfile.getMemberCode();
		jdbc.update("update member_profiles set member_code = 'MB-99999999999999999' where account_id = ?",
			createdFirstProfile.getAccountId());
		jdbc.update("update member_profiles set member_code = ? where account_id = ?", lowerMemberCode,
			createdSecondProfile.getAccountId());
		jdbc.update("update member_profiles set member_code = ? where account_id = ?", higherMemberCode,
			createdFirstProfile.getAccountId());

		MemberProfile lowerCodeProfile = createdSecondProfile;
		MemberProfile higherCodeProfile = createdFirstProfile;
		createMembership(lowerCodeProfile.getAccountId(), manager.getId(), "PLUS",
			now.minusSeconds(3600), membershipEndsAt);
		createMembership(higherCodeProfile.getAccountId(), manager.getId(), "PLUS",
			now.minusSeconds(3600), membershipEndsAt);

		assertThat(lowerMemberCode).isLessThan(higherMemberCode);
		AssignmentCandidatePageResponse firstPage = discovery.candidates(manager.getId(), null,
			AssignmentCandidateStatus.ALL, PageRequest.of(0, 1));
		AssignmentCandidatePageResponse secondPage = discovery.candidates(manager.getId(), null,
			AssignmentCandidateStatus.ALL, PageRequest.of(1, 1));

		assertThat(firstPage.totalElements()).isEqualTo(2);
		assertThat(firstPage.totalPages()).isEqualTo(2);
		assertThat(firstPage.content()).extracting(AssignmentCandidateResponse::memberAccountId)
			.containsExactly(lowerCodeProfile.getAccountId());
		assertThat(firstPage.content()).extracting(AssignmentCandidateResponse::memberCode)
			.containsExactly(lowerMemberCode);
		assertThat(secondPage.content()).extracting(AssignmentCandidateResponse::memberAccountId)
			.containsExactly(higherCodeProfile.getAccountId());
		assertThat(secondPage.content()).extracting(AssignmentCandidateResponse::memberCode)
			.containsExactly(higherMemberCode);
	}

	@Test
	void inactiveManagerTokenIsRejectedByBothEndpoints() throws Exception {
		Account inactiveManager = createAccount(AccountRole.MANAGER, AccountStatus.INACTIVE, "Inactive Manager");
		for (String path : List.of("/api/v1/manager/coach-assignments/candidates",
			"/api/v1/manager/coaches/assignment-workloads")) {
			mockMvc.perform(get(path).contextPath("/api/v1")
				.header("Authorization", bearer(inactiveManager)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
		}
	}

	private MemberProfile plusMember(Account manager, String name, Instant endsAt) {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		MemberProfile member = createMember(AccountStatus.ACTIVE, name, "09" + suffix,
			suffix + "@example.test");
		createMembership(member.getAccountId(), manager.getId(), "PLUS", Instant.now().minusSeconds(3600), endsAt);
		return member;
	}

	private Account createAccount(AccountRole role, AccountStatus status, String name) {
		return createAccount(UUID.randomUUID(), role, status, name);
	}

	private Account createAccount(UUID accountId, AccountRole role, AccountStatus status, String name) {
		String unique = UUID.randomUUID().toString().substring(0, 8);
		return accounts.saveAndFlush(new Account(accountId, role, status, name, "08" + unique,
			unique + "@example.test", LocalDate.of(1990, 1, 1), "{noop}secret"));
	}

	private MemberProfile createMember(AccountStatus status, String name, String phone, String email) {
		Account account = accounts.saveAndFlush(new Account(UUID.randomUUID(), AccountRole.MEMBER, status, name,
			phone, email, LocalDate.of(1995, 1, 1), "{noop}secret"));
		return profiles.saveAndFlush(new MemberProfile(account));
	}

	private UUID createMembership(UUID memberId, UUID actorId, String planCode, Instant startsAt, Instant endsAt) {
		return createMembership(memberId, actorId, planCode, startsAt, endsAt, "ACTIVE");
	}

	private UUID createMembership(UUID memberId, UUID actorId, String planCode, Instant startsAt, Instant endsAt,
			String status) {
		UUID offerId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		jdbc.update("""
			insert into membership_offers (id, plan_code, name, description, price_amount, duration_days,
				status, created_by_account_id)
			values (?, ?, ?, 'Discovery test offer', 1000, 30, 'ACTIVE', ?)
			""", offerId, planCode, "Offer " + offerId, actorId);
		jdbc.update("""
			insert into membership_orders (id, order_number, member_account_id, created_by_account_id, offer_id,
				offer_name_snapshot, plan_code_snapshot, price_amount_snapshot, currency_code_snapshot,
				duration_days_snapshot, payment_method, status, paid_at)
			values (?, ?, ?, ?, ?, 'Offer', ?, 1000, 'VND', 30, 'CASH', 'PAID', ?)
			""", orderId, "ORD-" + orderId, memberId, actorId, offerId, planCode, Timestamp.from(startsAt));
		jdbc.update("""
			insert into payments (id, order_id, method, status, amount, currency_code,
				processed_by_account_id, paid_at)
			values (?, ?, 'CASH', 'PAID', 1000, 'VND', ?, ?)
			""", UUID.randomUUID(), orderId, actorId, Timestamp.from(startsAt));
		jdbc.update("""
			insert into memberships (id, member_account_id, order_id, offer_id, plan_code_snapshot,
				offer_name_snapshot, price_amount_snapshot, currency_code_snapshot, duration_days_snapshot,
				status, starts_at, ends_at)
			values (?, ?, ?, ?, ?, 'Offer', 1000, 'VND', 30, ?, ?, ?)
			""", membershipId, memberId, orderId, offerId, planCode, status, Timestamp.from(startsAt),
			Timestamp.from(endsAt));
		return membershipId;
	}

	private UUID insertActiveAssignment(UUID memberId, UUID coachId, UUID managerId, Instant startsAt) {
		UUID assignmentId = UUID.randomUUID();
		jdbc.update("""
			insert into coach_assignments (id, member_account_id, coach_account_id, assigned_by_account_id,
				status, starts_at, created_at, updated_at, version)
			values (?, ?, ?, ?, 'ACTIVE', ?, ?, ?, 0)
			""", assignmentId, memberId, coachId, managerId, Timestamp.from(startsAt), Timestamp.from(startsAt),
			Timestamp.from(startsAt));
		return assignmentId;
	}

	private void assertEndedAt(UUID assignmentId, Instant expectedEndsAt) {
		assertThat(jdbc.queryForObject("select status from coach_assignments where id = ?", String.class,
			assignmentId)).isEqualTo("ENDED");
		assertThat(jdbc.queryForObject("select ends_at from coach_assignments where id = ?", Timestamp.class,
			assignmentId).toInstant()).isEqualTo(expectedEndsAt);
	}

	private String bearer(Account account) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().subject(account.getId().toString()).issuedAt(now)
			.expiresAt(now.plusSeconds(900)).claim("role", account.getRole().name()).build();
		return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(
			JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
	}
}
