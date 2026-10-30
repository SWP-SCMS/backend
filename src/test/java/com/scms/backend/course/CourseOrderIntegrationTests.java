package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CourseOrderIntegrationTests {

	private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired MockMvc mockMvc;
	@Autowired AccountRepository accounts;
	@Autowired MemberProfileRepository profiles;
	@Autowired JdbcTemplate db;
	@Autowired JwtEncoder jwtEncoder;
	@Autowired ObjectMapper objectMapper;
	@MockitoBean Clock clock;

	@BeforeEach
	void setUpClock() {
		when(clock.instant()).thenReturn(NOW);
	}

	@Test
	void createsAndReplaysTheSameCanonicalRequestButRejectsKeyReuse() throws Exception {
		MemberProfile member = member(AccountStatus.ACTIVE);
		RunFixture first = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(7200));
		RunFixture second = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(10800));

		MvcResult created = createSelf(member, first.runId(), "same-key").andExpect(status().isCreated())
			.andExpect(jsonPath("$.memberId").value(member.getMemberCode()))
			.andExpect(jsonPath("$.courseRunId").value(first.runId().toString()))
			.andExpect(jsonPath("$.orderReference").value(org.hamcrest.Matchers.matchesPattern("COR-[0-9A-F]{32}")))
			.andExpect(jsonPath("$.tuitionVnd").value(500000))
			.andExpect(jsonPath("$.orderStatus").value("PENDING_PAYMENT"))
			.andExpect(jsonPath("$.reservationStatus").value("HELD"))
			.andExpect(jsonPath("$.expiresAt").value(NOW.plusSeconds(900).toString()))
			.andReturn();
		String original = created.getResponse().getContentAsString();

		assertThat(createSelf(member, first.runId(), " same-key ").andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString()).isEqualTo(original);
		createSelf(member, second.runId(), "same-key").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURSE_IDEMPOTENCY_KEY_REUSED"));
		assertThat(count("course_orders", member.getAccountId())).isOne();
		assertThat(count("course_reservations", member.getAccountId())).isOne();
	}

	@Test
	void registrationWindowIsHalfOpenAndExpiryUsesTheEarlierDeadline() throws Exception {
		MemberProfile member = member(AccountStatus.ACTIVE);
		RunFixture beforeOpen = run(3, NOW.plusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(7200));
		RunFixture atOpen = run(3, NOW, NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(10800));
		RunFixture beforeClose = run(3, NOW.minusSeconds(3600), NOW.plusSeconds(300), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(14400));
		RunFixture atClose = run(3, NOW.minusSeconds(3600), NOW, "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(18000));
		RunFixture afterClose = run(3, NOW.minusSeconds(3600), NOW.minusSeconds(1), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(21600));

		createSelf(member, beforeOpen.runId(), "before-open").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURSE_REGISTRATION_NOT_OPEN"));
		createSelf(member, atOpen.runId(), "at-open").andExpect(status().isCreated())
			.andExpect(jsonPath("$.expiresAt").value(NOW.plusSeconds(900).toString()));
		createSelf(member, beforeClose.runId(), "before-close").andExpect(status().isCreated())
			.andExpect(jsonPath("$.expiresAt").value(NOW.plusSeconds(300).toString()));
		createSelf(member, atClose.runId(), "at-close").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURSE_REGISTRATION_CLOSED"));
		createSelf(member, afterClose.runId(), "after-close").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURSE_REGISTRATION_CLOSED"));
	}

	@Test
	void rejectsHiddenNonOpenInactiveDuplicateScheduleConflictAndFullCapacity() throws Exception {
		MemberProfile member = member(AccountStatus.ACTIVE);
		RunFixture hidden = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "INACTIVE",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(7200));
		RunFixture nonOpen = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED", "DRAFT",
			NOW.plusSeconds(10800));
		createSelf(member, hidden.runId(), "hidden").andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("COURSE_RUN_NOT_AVAILABLE"));
		createSelf(member, nonOpen.runId(), "non-open").andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("COURSE_RUN_NOT_AVAILABLE"));

		RunFixture held = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(14400));
		createSelf(member, held.runId(), "held-1").andExpect(status().isCreated());
		createSelf(member, held.runId(), "held-2").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURSE_RESERVATION_EXISTS"));

		RunFixture overlap = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", held.sessionStart().plusSeconds(1800));
		createSelf(member, overlap.runId(), "overlap").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURSE_SCHEDULE_CONFLICT"));

		RunFixture full = run(1, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(28800));
		createSelf(member(AccountStatus.ACTIVE), full.runId(), "seat-1").andExpect(status().isCreated());
		createSelf(member(AccountStatus.ACTIVE), full.runId(), "seat-2").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURSE_CAPACITY_FULL"));

		Account receptionist = account(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		MemberProfile suspended = member(AccountStatus.SUSPENDED);
		createReception(receptionist, suspended, full.runId(), "suspended").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MEMBER_NOT_ACTIVE"));
	}

	@Test
	void exactExpiryIsExpiredGetNeverExtendsAndNewCreateReplacesOnlyTheStaleHold() throws Exception {
		MemberProfile member = member(AccountStatus.ACTIVE);
		RunFixture fixture = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(7200));
		JsonNode created = json(createSelf(member, fixture.runId(), "expiry-1").andExpect(status().isCreated())
			.andReturn());
		UUID orderId = UUID.fromString(created.path("orderId").asText());
		Instant expiry = Instant.parse(created.path("expiresAt").asText());

		when(clock.instant()).thenReturn(expiry.minusNanos(1));
		getSelf(member, orderId).andExpect(status().isOk())
			.andExpect(jsonPath("$.reservationStatus").value("HELD"))
			.andExpect(jsonPath("$.expiresAt").value(expiry.toString()));
		when(clock.instant()).thenReturn(expiry);
		getSelf(member, orderId).andExpect(status().isOk())
			.andExpect(jsonPath("$.orderStatus").value("EXPIRED"))
			.andExpect(jsonPath("$.reservationStatus").value("EXPIRED"))
			.andExpect(jsonPath("$.expiresAt").value(expiry.toString()));
		when(clock.instant()).thenReturn(expiry.plusSeconds(1));
		getSelf(member, orderId).andExpect(status().isOk())
			.andExpect(jsonPath("$.reservationStatus").value("EXPIRED"));

		createSelf(member, fixture.runId(), "expiry-2").andExpect(status().isCreated());
		assertThat(db.queryForObject("""
			select count(*) from course_reservations
			where member_account_id=? and course_run_id=? and status='HELD'
			""", Long.class, member.getAccountId(), fixture.runId())).isOne();
		assertThat(db.queryForObject("""
			select count(*) from course_reservations
			where member_account_id=? and course_run_id=? and status='EXPIRED'
			""", Long.class, member.getAccountId(), fixture.runId())).isOne();
	}

	@Test
	void getIsOwnershipScopedForMemberAndReceptionist() throws Exception {
		MemberProfile owner = member(AccountStatus.ACTIVE);
		MemberProfile other = member(AccountStatus.ACTIVE);
		Account receptionist = account(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		RunFixture fixture = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(7200));
		UUID orderId = UUID.fromString(json(createSelf(owner, fixture.runId(), "owner-key")
			.andExpect(status().isCreated()).andReturn()).path("orderId").asText());

		getSelf(other, orderId).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("COURSE_ORDER_NOT_FOUND"));
		getReception(receptionist, other, orderId).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("COURSE_ORDER_NOT_FOUND"));
		getReception(receptionist, owner, orderId).andExpect(status().isOk())
			.andExpect(jsonPath("$.memberId").value(owner.getMemberCode()));
	}

	@Test
	void concurrentSameKeyCreatesOnceAndFinalSeatCannotOversell() throws Exception {
		MemberProfile sameMember = member(AccountStatus.ACTIVE);
		RunFixture replayRun = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(7200));
		List<MvcResult> replayResults = concurrent(
			() -> createSelf(sameMember, replayRun.runId(), "race-key").andReturn(),
			() -> createSelf(sameMember, replayRun.runId(), "race-key").andReturn());
		assertThat(replayResults).extracting(result -> result.getResponse().getStatus())
			.containsExactlyInAnyOrder(200, 201);
		assertThat(replayResults.get(0).getResponse().getContentAsString())
			.isEqualTo(replayResults.get(1).getResponse().getContentAsString());
		assertThat(count("course_orders", sameMember.getAccountId())).isOne();

		MemberProfile first = member(AccountStatus.ACTIVE);
		MemberProfile second = member(AccountStatus.ACTIVE);
		RunFixture lastSeat = run(1, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(14400));
		List<MvcResult> seatResults = concurrent(
			() -> createSelf(first, lastSeat.runId(), "final-1").andReturn(),
			() -> createSelf(second, lastSeat.runId(), "final-2").andReturn());
		assertThat(seatResults).extracting(result -> result.getResponse().getStatus())
			.containsExactlyInAnyOrder(201, 409);
		assertThat(db.queryForObject("""
			select count(*) from course_reservations
			where course_run_id=? and status='HELD' and expires_at>?
			""", Long.class, lastSeat.runId(), Timestamp.from(NOW))).isOne();
	}

	@Test
	void auditFailureRollsBackOrderReservationAndAudit() throws Exception {
		MemberProfile member = member(AccountStatus.ACTIVE);
		RunFixture fixture = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(7200));
		String constraint = "chk_test_reject_course_order_audit";
		db.execute("alter table audit_events add constraint " + constraint
			+ " check (action <> 'COURSE_ORDER_CREATED') not valid");
		try {
			createSelf(member, fixture.runId(), "rollback").andExpect(status().isInternalServerError());
			assertThat(count("course_orders", member.getAccountId())).isZero();
			assertThat(count("course_reservations", member.getAccountId())).isZero();
			assertThat(db.queryForObject("select count(*) from audit_events where action='COURSE_ORDER_CREATED'",
				Long.class)).isZero();
		}
		finally {
			db.execute("alter table audit_events drop constraint " + constraint);
		}
	}

	@Test
	void missingKeyAndClientControlledFieldsAreRejectedAndRoleRulesApply() throws Exception {
		MemberProfile member = member(AccountStatus.ACTIVE);
		RunFixture fixture = run(2, NOW.minusSeconds(1), NOW.plusSeconds(3600), "PUBLISHED",
			"OPEN_FOR_REGISTRATION", NOW.plusSeconds(7200));
		createSelf(member, fixture.runId(), null).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("COURSE_IDEMPOTENCY_KEY_REQUIRED"));
		mockMvc.perform(post("/api/v1/members/me/course-orders").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(member.getAccount()))
			.header("Idempotency-Key", "client-fields")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"courseRunId\":\"" + fixture.runId() + "\",\"expiresAt\":\"2030-01-01T00:00:00Z\"}"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
		Account coach = account(AccountRole.COACH, AccountStatus.ACTIVE);
		mockMvc.perform(post("/api/v1/members/me/course-orders").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(coach))
			.header("Idempotency-Key", "coach")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"courseRunId\":\"" + fixture.runId() + "\"}"))
			.andExpect(status().isForbidden());
		String activeToken = token(member.getAccount());
		db.update("update accounts set status='SUSPENDED' where id=?", member.getAccountId());
		mockMvc.perform(post("/api/v1/members/me/course-orders").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + activeToken)
			.header("Idempotency-Key", "suspended-actor")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"courseRunId\":\"" + fixture.runId() + "\"}"))
			.andExpect(status().isUnauthorized());
	}

	private org.springframework.test.web.servlet.ResultActions createSelf(MemberProfile member, UUID runId,
			String key) throws Exception {
		var request = post("/api/v1/members/me/course-orders").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(member.getAccount()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"courseRunId\":\"" + runId + "\"}");
		if (key != null) request.header("Idempotency-Key", key);
		return mockMvc.perform(request);
	}

	private org.springframework.test.web.servlet.ResultActions createReception(Account receptionist,
			MemberProfile member, UUID runId, String key) throws Exception {
		return mockMvc.perform(post("/api/v1/reception/members/{memberId}/course-orders", member.getMemberCode())
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionist))
			.header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON)
			.content("{\"courseRunId\":\"" + runId + "\"}"));
	}

	private org.springframework.test.web.servlet.ResultActions getSelf(MemberProfile member, UUID orderId)
			throws Exception {
		return mockMvc.perform(get("/api/v1/members/me/course-orders/{orderId}", orderId).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(member.getAccount())));
	}

	private org.springframework.test.web.servlet.ResultActions getReception(Account receptionist,
			MemberProfile member, UUID orderId) throws Exception {
		return mockMvc.perform(get("/api/v1/reception/members/{memberId}/course-orders/{orderId}",
			member.getMemberCode(), orderId).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionist)));
	}

	private List<MvcResult> concurrent(ThrowingRequest first, ThrowingRequest second) throws Exception {
		CyclicBarrier barrier = new CyclicBarrier(2);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var one = executor.submit(() -> { barrier.await(10, TimeUnit.SECONDS); return first.run(); });
			var two = executor.submit(() -> { barrier.await(10, TimeUnit.SECONDS); return second.run(); });
			return List.of(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS));
		}
	}

	private JsonNode json(MvcResult result) throws Exception {
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	private long count(String table, UUID memberId) {
		return db.queryForObject("select count(*) from " + table + " where member_account_id=?", Long.class,
			memberId);
	}

	private MemberProfile member(AccountStatus status) {
		return profiles.saveAndFlush(new MemberProfile(account(AccountRole.MEMBER, status)));
	}

	private Account account(AccountRole role, AccountStatus status) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, status, role + " Course Order",
			"08" + id.toString().replace("-", "").substring(0, 8), id + "@course-order.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}

	private RunFixture run(int capacity, Instant opensAt, Instant closesAt, String courseStatus,
			String runStatus, Instant sessionStart) {
		Account coach = account(AccountRole.COACH, AccountStatus.ACTIVE);
		UUID discipline = UUID.randomUUID();
		UUID course = UUID.randomUUID();
		UUID run = UUID.randomUUID();
		UUID room = UUID.randomUUID();
		UUID session = UUID.randomUUID();
		Instant plannedStart = closesAt.plusSeconds(3600);
		Instant plannedEnd = plannedStart.plusSeconds(86400);
		db.update("insert into disciplines(id,name,status) values (?,?,'ACTIVE')", discipline, "D-" + discipline);
		db.update("insert into rooms(id,name,capacity,usage_mode,status) values (?,?,?,'EXCLUSIVE','ACTIVE')",
			room, "Room " + room, capacity);
		db.update("insert into courses(id,code,name,discipline_id,status) values (?,?,?, ?,?)",
			course, "C-" + course, "Course " + course, discipline, courseStatus);
		db.update("""
			insert into course_runs(id,run_code,course_id,primary_coach_account_id,tuition_vnd,capacity,
				minimum_enrollment,registration_opens_at,registration_closes_at,planned_start_at,
				planned_end_at,total_units,named_coach_commitment,policy_version_snapshot,status)
			values (?,?,?,?,500000,?,1,?,?,?,?,1,true,?,?)
			""", run, "R-" + run, course, coach.getId(), capacity, Timestamp.from(opensAt),
			Timestamp.from(closesAt), Timestamp.from(plannedStart), Timestamp.from(plannedEnd),
			"DRAFT".equals(runStatus) ? null : "P1", runStatus);
		db.update("""
			insert into course_sessions(id,course_run_id,unit_number,room_id,coach_account_id,start_at,end_at,
				original_start_at,original_end_at,allocation_vnd,status)
			values (?,?,?,?,?,?,?,?,?,100000,'SCHEDULED')
			""", session, run, 1, room, coach.getId(), Timestamp.from(sessionStart),
			Timestamp.from(sessionStart.plusSeconds(3600)), Timestamp.from(sessionStart),
			Timestamp.from(sessionStart.plusSeconds(3600)));
		return new RunFixture(run, sessionStart);
	}

	private String token(Account account) {
		Instant issued = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().subject(account.getId().toString()).issuedAt(issued)
			.expiresAt(issued.plusSeconds(900)).claim("role", account.getRole().name()).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();
	}

	@FunctionalInterface
	private interface ThrowingRequest {
		MvcResult run() throws Exception;
	}

	private record RunFixture(UUID runId, Instant sessionStart) { }
}
